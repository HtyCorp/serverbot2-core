package io.mamish.serverbot2.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.UrlShortenerConfig;
import io.mamish.serverbot2.sharedutil.Pair;
import io.mamish.serverbot2.urlshortener.tokenv1.V1TokenProcessor;
import io.mamish.serverbot2.urlshortener.tokenv1.V1UrlInfoBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiGatewayLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Logger logger = LogManager.getLogger(ApiGatewayLambdaHandler.class);

    private final DynamoDbEnhancedClient ddbClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(DynamoDbClient.builder()
                    .httpClient(UrlConnectionHttpClient.create())
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build()
            ).build();

    private final DynamoDbTable<V1UrlInfoBean> v1table = ddbClient.table(UrlShortenerConfig.DYNAMO_TABLE_NAME,
            TableSchema.fromBean(V1UrlInfoBean.class));
    private final V1TokenProcessor v1Processor = new V1TokenProcessor();

    private final Pattern basicValidUrlPattern = Pattern.compile("(?<schema>[a-z]+)://"
            + "(?<domain>[a-zA-Z0-9-.]+)"
            + ".*");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        logger.info("Proxy request:\n" + request);
        APIGatewayProxyResponseEvent response = enact(request, context);
        logger.info("Proxy response:\n" + response);
        return response;
    }

    private APIGatewayProxyResponseEvent enact(APIGatewayProxyRequestEvent request, Context context) {

        String[] pathSegments = request.getPath().split("/");
        if (pathSegments.length < 2) {
            logger.error("Got less than 2 path segments, path={}", Arrays.toString(pathSegments));
            return generateHttpRawError("Resource not found", 404);
        }
        String baseResource = pathSegments[1]; // not [0]: there is a leading "/" so [0] is just empty
        String[] subResources = Arrays.copyOfRange(pathSegments, 2, pathSegments.length);

        if (baseResource.equals(UrlShortenerConfig.URL_ADMIN_PATH)) {
            return enactAdminRequest(request, context, subResources);
        } else if (baseResource.equals("1")) {
            return enactV1RedirectRequest(request, context, subResources);
        } else {
            return generateHttpRawError("Page not found", 404);
        }

    }

    private APIGatewayProxyResponseEvent enactAdminRequest(APIGatewayProxyRequestEvent request, Context context,
                                                           String[] subpathSegments) {

        // Important: should check that we are IAM-authorised just in case.
        // APIGW config should guarantee this and pass the principal ARN in request context.
        // This API will only be called by other services, not app users (they won't have permission).

        try {
            String userArn = request.getRequestContext().getIdentity().getUserArn();
            Objects.requireNonNull(userArn);
            logger.info("Admin request authorised by principal {}", userArn);
        } catch (NullPointerException e) {
            logger.error("Admin request not signed by an authorised IAM principal");
            return generateHttpRawError("Unauthorized", 401);
        }

        // Request validation

        if (!subpathSegments[0].equals(UrlShortenerConfig.URL_ADMIN_SUBPATH_NEW)) {
            logger.error("Unknown admin resource '{}'", subpathSegments[0]);
            return generateHttpRawError("Resource not found", 404);
        }

        if (!request.getHttpMethod().equals("POST")) {
            logger.error("Got bad HTTP method {}, expected POST", request.getHttpMethod());
            return generateHttpRawError("Method not allowed", 405);
        }

        if (request.getBody() == null) {
            logger.error("No body data supplied, expecting JSON");
            return generateHttpRawError("Missing required body data", 400);
        }

        String urlParam;
        long ttlParam;
        try {
            JsonObject bodyJson = JsonParser.parseString(request.getBody()).getAsJsonObject();
            urlParam = bodyJson.get(UrlShortenerConfig.URL_ADMIN_SUBPATH_NEW_JSONKEY_URL).getAsString();
            ttlParam = bodyJson.get(UrlShortenerConfig.URL_ADMIN_SUBPATH_NEW_JSONKEY_TTLSECONDS).getAsLong();
        } catch (RuntimeException e) {
            logger.error("Body params are missing or of wrong type, body='{}'", request.getBody(), e);
            return generateHttpRawError("Body params are missing or invalid", 400);
        }

        if (!isUrlValid(urlParam)) {
            logger.error("URL requested for storage is not valid/allowed");
            return generateHttpRawError("Provided URL is invalid or not allowed", 400);
        }

        if (ttlParam <= 0 || ttlParam > UrlShortenerConfig.MAX_TTL_SECONDS) {
            logger.error("TTL parameter out of range");
            return generateHttpRawError("Provided TTL parameter is out of range", 400);
        }

        // With validation finished, actually generate a user token and a persistent storage item for V1

        Pair<String,V1UrlInfoBean> tokenAndBean;
        try {
            tokenAndBean = v1Processor.generateTokenAndBean(urlParam, ttlParam);
        } catch (RuntimeException e) {
            logger.error("Exception occurred while generating URL token", e);
            return generateHttpRawError("Unknown error while generating URL", 500);
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
        return generateHttpOkay(shortUrl);

    }

    private APIGatewayProxyResponseEvent enactV1RedirectRequest(APIGatewayProxyRequestEvent request, Context context,
                                                                String[] subpathSegments) {

        if (!request.getHttpMethod().equals("GET")) {
            logger.error("Got bad HTTP method, expecting GET");
            return generateHttpRawError("This resource only accepts GET requests", 405);
        }

        if (subpathSegments.length == 0) {
            logger.error("No subpath params provided for V1 redirect path");
            return generateHttpError("Sorry, this URL is invalid. Ensure you are using the correct link.",
                    "missing token path param", 400);
        }

        String token = subpathSegments[0];
        String id;
        try {
            id = v1Processor.extractIdFromToken(token);
        } catch (InvalidTokenException e) {
            logger.error("Failed to get ID from user-supplied token", e);
            return generateHttpError("Sorry, this URL is invalid. Ensure you are using the correct link.",
                    e.getMessage(), 400);
        }

        V1UrlInfoBean urlInfoBean = v1table.getItem(Key.builder().partitionValue(1).sortValue(id).build());
        if (urlInfoBean == null) {
            logger.error("No DDB item found with schema version 1 and ID {}", id);
            return generateHttpError("Sorry, this URL does not exist. It may be invalid or may have been deleted."
                    + " Try getting a new link from wherever you got this one.",
                    "null table lookup on id="+id, 404);
        }
        logger.info("Retrieved URL info bean for id={}:\n{}", id, urlInfoBean);

        String fullUrl;
        try {
            fullUrl = v1Processor.extractFullUrlFromTokenAndBean(token, urlInfoBean);
        } catch (InvalidTokenException e) {
            logger.error("Failed to extract URL for id="+id, e);
            return generateHttpError("Sorry, this URL is invalid. Ensure you are using the correct link.",
                    "bad url extract on id="+id+": "+e.getMessage(), 400);
        } catch (UrlRevokedException e) {
            logger.error("Extracted URL for ID="+id+" is revoked", e);
            return generateHttpError("Sorry, this URL has expired or been revoked."
                    + " Try getting a new link from wherever you got this one.",
                    "revocation on id="+id+": "+e.getMessage(), 403);
        }
        logger.info("ID '{}' URL is '{}'", id, fullUrl);

        if (!isUrlValid(fullUrl)) {
            logger.error("Stored URL is somehow invalid. Should never occur due to validation on store ('{}')", fullUrl);
            return generateHttpError("Sorry, this URL has been revoked."
                    + " Try getting a new link from wherever you got this one.",
                    "bad apex domain for id="+id, 500);
        }

        return generateHttpFound(fullUrl);

    }

    private boolean isUrlValid(String url) {

        if (url.length() > UrlShortenerConfig.MAX_URL_LENGTH) {
            logger.warn("URL size ({}) exceeds shortener limit of {}",
                    url.length(), UrlShortenerConfig.MAX_URL_LENGTH);
            return false;
        }

        Matcher m = basicValidUrlPattern.matcher(url);
        if (!m.matches()) {
            logger.warn("URL does not match regex ({})", url);
            return false;
        }

        String schema = m.group("schema");
        String domain = m.group("domain");
        logger.info("Parsed URL fields schema={} domain={} for URL '{}'",
                schema, domain, url);

        if (!schema.equals("https")) {
            logger.warn("URL validation failure: not HTTPS");
            return false;
        }

        String[] requestedLabels = domain.split("\\.");
        if (Arrays.stream(requestedLabels).anyMatch(String::isEmpty)) {
            logger.warn("URL validation failure: bad label separation");
            return false;
        }

        if (getAllowedApexDomains().stream().noneMatch(allowedDomain ->
                domain.endsWith("."+allowedDomain) || domain.equals(allowedDomain))) {
            logger.warn("URL validation failure: not an allowed apex domain");
            return false;
        }

        return true;

    }

    private List<String> getAllowedApexDomains() {
        List<String> allowedDomains = new ArrayList<>(UrlShortenerConfig.ADDITIONAL_ALLOWED_DOMAINS);
        allowedDomains.add(CommonConfig.SYSTEM_ROOT_DOMAIN_NAME.getValue());
        allowedDomains.add(CommonConfig.APP_ROOT_DOMAIN_NAME.getValue());
        return allowedDomains;
    }

    private APIGatewayProxyResponseEvent generateHttpOkay(String message) {
        return new APIGatewayProxyResponseEvent()
                .withHeaders(noCacheHeaders())
                .withStatusCode(200)
                .withBody(message);
    }

    private APIGatewayProxyResponseEvent generateHttpFound(String location) {
        Map<String,String> baseAndLocationHeaders = new HashMap<>(noCacheHeaders());
        baseAndLocationHeaders.put("Location", location);
        return new APIGatewayProxyResponseEvent()
                .withHeaders(baseAndLocationHeaders)
                .withStatusCode(302);
    }

    private APIGatewayProxyResponseEvent generateHttpError(String message, String cause, Integer code) {
        return generateHttpRawError(message + "\n\n(Error detail: " + cause + ")", code);
    }

    private APIGatewayProxyResponseEvent generateHttpRawError(String message, Integer code) {
        return new APIGatewayProxyResponseEvent()
                .withHeaders(noCacheHeaders())
                .withStatusCode(code)
                .withBody(message);
    }

    private Map<String,String> noCacheHeaders() {
        // Ref: https://stackoverflow.com/questions/49547/how-do-we-control-web-page-caching-across-all-browsers
        return Map.of(
                "Content-Type", "text/plain; charset=UTF-8",
                "Cache-Control", "no-cache, no-store, must-revalidate",
                "Pragma", "no-cache",
                "Expires", "0"
        );
    }

}
