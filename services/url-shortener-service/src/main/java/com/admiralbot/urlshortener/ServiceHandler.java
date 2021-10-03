package com.admiralbot.urlshortener;

import com.admiralbot.framework.exception.server.NoSuchResourceException;
import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.framework.exception.server.ResourceExpiredException;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.UrlShortenerConfig;
import com.admiralbot.sharedutil.*;
import com.admiralbot.urlshortener.model.*;
import com.admiralbot.urlshortener.tokenv1.V1TokenProcessor;
import com.admiralbot.urlshortener.tokenv1.V1UrlInfoBean;
import com.admiralbot.urlshortener.tokenv1.V1UrlInfoBeanSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceHandler implements IUrlShortener {

    static {
        XrayUtils.setServiceName("UrlShortener");
        AppContext.setLambda();
    }

    private final Logger logger = LoggerFactory.getLogger(ServiceHandler.class);

    private final DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(SdkUtils.client(DynamoDbClient.builder()))
            .build();

    private final DynamoDbTable<V1UrlInfoBean> v1table = ddbClient.table(UrlShortenerConfig.DYNAMO_TABLE_NAME,
            V1UrlInfoBeanSchema.INSTANCE);
    private final V1TokenProcessor v1Processor = new V1TokenProcessor();

    private final Pattern basicValidUrlPattern = Pattern.compile("(?<schema>[a-z]+)://"
            + "(?<domain>[a-zA-Z0-9-.]+)"
            + ".*");

    @Override
    public CreateShortUrlResponse createShortUrl(CreateShortUrlRequest request) {

        String fullUrl = request.getFullUrl();
        long ttlSeconds = request.getTtlSeconds();

        if (isInvalidUrl(request.getFullUrl())) {
            logger.error("URL requested for storage is not valid/allowed");
            throw new RequestValidationException("Provided URL is invalid or not allowed");
        }

        if (!Utils.inRangeInclusive(ttlSeconds, 1, UrlShortenerConfig.MAX_TTL_SECONDS)) {
            logger.error("TTL parameter out of range");
            throw new RequestValidationException("Provided TTL parameter is out of range");
        }

        // With validation finished, actually generate a user token and a persistent storage item for V1

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
        String shortUrl = "https://"
                + UrlShortenerConfig.SUBDOMAIN
                + "."
                + CommonConfig.SYSTEM_ROOT_DOMAIN_NAME.getValue()
                + "/1/"
                + encodedToken;

        return new CreateShortUrlResponse(shortUrl);

    }

    @Override
    public GetFullUrlResponse getFullUrl(GetFullUrlRequest request) {

        if (request.getTokenVersion() != 1) {
            throw new RequestValidationException("This API only supports version 1");
        }

        String token = request.getUrlToken();

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

        if (isInvalidUrl(fullUrl)) {
            logger.error("Stored URL is somehow invalid. Should never occur due to validation on store ('{}')", fullUrl);
            throw new RequestHandlingException("Invalid result URL for id="+id);
        }

        return new GetFullUrlResponse(fullUrl);

    }

    private boolean isInvalidUrl(String url) {

        if (url.length() > UrlShortenerConfig.MAX_URL_LENGTH) {
            logger.warn("URL size ({}) exceeds shortener limit of {}",
                    url.length(), UrlShortenerConfig.MAX_URL_LENGTH);
            return true;
        }

        Matcher m = basicValidUrlPattern.matcher(url);
        if (!m.matches()) {
            logger.warn("URL does not match regex ({})", url);
            return true;
        }

        String schema = m.group("schema");
        String domain = m.group("domain");
        logger.info("Parsed URL fields schema={} domain={} for URL '{}'",
                schema, domain, url);

        if (!schema.equals("https")) {
            logger.warn("URL validation failure: not HTTPS");
            return true;
        }

        String[] requestedLabels = domain.split("\\.");
        if (Arrays.stream(requestedLabels).anyMatch(String::isEmpty)) {
            logger.warn("URL validation failure: bad label separation");
            return true;
        }

        if (getAllowedApexDomains().stream().noneMatch(allowedDomain ->
                domain.endsWith("."+allowedDomain) || domain.equals(allowedDomain))) {
            logger.warn("URL validation failure: not an allowed apex domain");
            return true;
        }

        return false;

    }

    private List<String> getAllowedApexDomains() {
        List<String> allowedDomains = new ArrayList<>(UrlShortenerConfig.ADDITIONAL_ALLOWED_DOMAINS);
        allowedDomains.add(CommonConfig.SYSTEM_ROOT_DOMAIN_NAME.getValue());
        allowedDomains.add(CommonConfig.APP_ROOT_DOMAIN_NAME.getValue());
        return allowedDomains;
    }

}
