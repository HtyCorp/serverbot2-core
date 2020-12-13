package io.mamish.serverbot2.commandlambda;

import com.google.gson.JsonObject;
import io.mamish.serverbot2.framework.client.SigV4HttpClient;
import io.mamish.serverbot2.framework.client.SigV4HttpResponse;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.UrlShortenerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.internal.LazyAwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.LazyAwsRegionProvider;
import software.amazon.awssdk.regions.providers.SystemSettingsRegionProvider;

import java.io.IOException;

public class UrlShortenerClient {

    private final Logger logger = LogManager.getLogger(UrlShortenerClient.class);

    private final SigV4HttpClient sigV4HttpClient = new SigV4HttpClient();
    private final AwsCredentialsProvider credentialsProvider = LazyAwsCredentialsProvider.create(EnvironmentVariableCredentialsProvider::create);
    private final AwsRegionProvider regionProvider = new LazyAwsRegionProvider(SystemSettingsRegionProvider::new);

    private static final String ERR_SHORTENER_REQUEST_FAILED = "An error occurred while generating the URL for this request";

    public String getShortenedUrl(String urlToShorten, long ttlSeconds) {

        String shortenerApiUri = "https://"
                + UrlShortenerConfig.SUBDOMAIN
                + "."
                + CommonConfig.SYSTEM_ROOT_DOMAIN_NAME.getValue()
                + "/"
                + UrlShortenerConfig.URL_ADMIN_PATH
                + "/"
                + UrlShortenerConfig.URL_ADMIN_SUBPATH_NEW;

        logger.info("Calling endpoint '{}' to get shortened URL for full URL '{}' with TTL={}",
                shortenerApiUri, urlToShorten, ttlSeconds);

        JsonObject newUrlRequest = new JsonObject();
        newUrlRequest.addProperty(UrlShortenerConfig.URL_ADMIN_SUBPATH_NEW_JSONKEY_URL, urlToShorten);
        newUrlRequest.addProperty(UrlShortenerConfig.URL_ADMIN_SUBPATH_NEW_JSONKEY_TTLSECONDS, ttlSeconds);

        try {
            SigV4HttpResponse response = sigV4HttpClient.post(
                    shortenerApiUri,
                    newUrlRequest.toString(),
                    "execute-api",
                    regionProvider.getRegion(),
                    credentialsProvider.resolveCredentials()
            );
            if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
                logger.error("Non-2xx response ({}) from shortener API, message: '{}'",
                        response.getStatusCode(), response.getBody().orElse(""));
                throw new RequestHandlingException(ERR_SHORTENER_REQUEST_FAILED);
            }
            if (response.getBody().isEmpty()) {
                logger.error("Empty body in shortener API response, expecting string containing new short URL");
                throw new RequestHandlingException(ERR_SHORTENER_REQUEST_FAILED);
            }
            String shortUrl = response.getBody().get();
            logger.info("URL shortener API returned new short URL '{}'", shortUrl);
            return shortUrl;
        } catch (IOException e) {
            logger.error("IO error while calling shortener API", e);
            throw new RequestHandlingException(ERR_SHORTENER_REQUEST_FAILED);
        }
    }

}
