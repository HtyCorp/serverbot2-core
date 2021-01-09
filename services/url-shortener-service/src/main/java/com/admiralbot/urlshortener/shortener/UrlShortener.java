package com.admiralbot.urlshortener.shortener;

import com.admiralbot.framework.exception.server.NoSuchResourceException;
import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.framework.exception.server.ResourceExpiredException;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.UrlShortenerConfig;
import com.admiralbot.sharedutil.Pair;
import com.admiralbot.sharedutil.SdkUtils;
import com.admiralbot.urlshortener.shortener.tokenv1.V1TokenProcessor;
import com.admiralbot.urlshortener.shortener.tokenv1.V1UrlInfoBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlShortener {

    private final Logger logger = LogManager.getLogger(UrlShortener.class);

    private final DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(SdkUtils.client(DynamoDbClient.builder()))
            .build();

    private final DynamoDbTable<V1UrlInfoBean> v1table = ddbClient.table(UrlShortenerConfig.DYNAMO_TABLE_NAME,
            TableSchema.fromBean(V1UrlInfoBean.class));
    private final V1TokenProcessor v1Processor = new V1TokenProcessor();

    public String generateShortUrl(String fullUrl, long ttlSeconds) {

        Pair<String,V1UrlInfoBean> tokenAndBean;
        try {
            tokenAndBean = v1Processor.generateTokenAndBean(fullUrl, ttlSeconds);
        } catch (RuntimeException e) {
            logger.error("Exception occurred while generating URL token", e);
            throw new RequestHandlingException("Unknown error while generating URL");
        }
        logger.info("Generated new V1 token '{}' for URL info bean:\n{}", tokenAndBean.a(), tokenAndBean.b());

        // Store the V1 URL info in DDB

        v1table.putItem(r -> r.item(tokenAndBean.b()));

        // Use the returned token to generate a shortened URL and send it back

        // Note: this should already be URL-safe but encode it anyway in case the underlying encoding is changed
        String encodedToken = URLEncoder.encode(tokenAndBean.a(), StandardCharsets.UTF_8);

        return "https://"
                + UrlShortenerConfig.SUBDOMAIN
                + "."
                + CommonConfig.SYSTEM_ROOT_DOMAIN_NAME.getValue()
                + "/1/"
                + encodedToken;

    }

    public String getFullUrl(int tokenVersion, String token) {

        if (tokenVersion != 1) {
            throw new RequestValidationException("This API only supports version 1");
        }

        String id;
        try {
            id = v1Processor.extractIdFromToken(token);
        } catch (InvalidTokenException e) {
            logger.error("Failed to get ID from user-supplied token", e);
            throw new NoSuchResourceException("Sorry, this URL is invalid. Ensure you are using the correct link.");
        }

        V1UrlInfoBean urlInfoBean = v1table.getItem(Key.builder().partitionValue(id).sortValue(1).build());
        if (urlInfoBean == null) {
            logger.error("No DDB item found with schema version 1 and ID {}", id);
            throw new RequestValidationException("Sorry, this URL does not exist. It may be invalid or may have been deleted."
                    + " Try getting a new link from wherever you got this one.");
        }
        logger.info("Retrieved URL info bean for id={}:\n{}", id, urlInfoBean);

        String fullUrl;
        try {
            fullUrl = v1Processor.extractFullUrlFromTokenAndBean(token, urlInfoBean);
        } catch (InvalidTokenException e) {
            logger.error("Failed to extract URL for id="+id, e);
            throw new RequestValidationException("Bad URL extract for id="+id+": "+e.getMessage());
        } catch (UrlRevokedException e) {
            logger.error("Extracted URL for ID="+id+" is revoked", e);
            throw new ResourceExpiredException("URL revocation on id="+id+": "+e.getMessage());
        }
        logger.info("ID '{}' URL is '{}'", id, fullUrl);

        return fullUrl;

    }

}
