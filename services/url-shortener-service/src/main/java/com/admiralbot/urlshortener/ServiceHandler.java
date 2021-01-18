package com.admiralbot.urlshortener;

import com.admiralbot.discordrelay.model.service.IDiscordService;
import com.admiralbot.discordrelay.model.service.NewMessageRequest;
import com.admiralbot.discordrelay.model.service.NewMessageResponse;
import com.admiralbot.discordrelay.model.service.SimpleEmbed;
import com.admiralbot.framework.client.ApiClient;
import com.admiralbot.framework.exception.server.RequestHandlingException;
import com.admiralbot.framework.exception.server.RequestValidationException;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedconfig.UrlShortenerConfig;
import com.admiralbot.sharedutil.Utils;
import com.admiralbot.urlshortener.model.*;
import com.admiralbot.urlshortener.shortener.UrlShortener;
import com.admiralbot.urlshortener.userprefs.PreferencesService;
import com.admiralbot.urlshortener.userprefs.UserPreferences;
import com.admiralbot.urlshortener.userprefs.WebPushSubscription;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.jose4j.lang.JoseException;
import software.amazon.awssdk.core.SdkBytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceHandler implements IUrlShortener {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Logger logger = LogManager.getLogger(ServiceHandler.class);
    private final Gson gson = new Gson();

    private final Pattern basicValidUrlPattern = Pattern.compile("(?<schema>[a-z]+)://"
            + "(?<domain>[a-zA-Z0-9-.]+)"
            + ".*");

    private final UrlShortener shortener = new UrlShortener();
    private final PreferencesService preferencesService = new PreferencesService();
    private final IDiscordService discordService = ApiClient.http(IDiscordService.class);

    private final PushService pushService;

    public ServiceHandler() {
        KeyPair keyPair;
        try {
            // Key loaded as in webpush lib example: https://github.com/web-push-libs/webpush-java/wiki/VAPID
            InputStream keyPairPemStream = SdkBytes.fromUtf8String(UrlShortenerConfig.PUSH_API_KEY_PAIR.getValue()).asInputStream();
            PEMParser pemParser = new PEMParser(new InputStreamReader(keyPairPemStream));
            PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
            keyPair = new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
        } catch (IOException e) {
            throw new IllegalStateException("Key pair data is invalid", e);
        }
        pushService = new PushService(keyPair);
    }

    @Override
    public DeliverUrlResponse deliverUrl(DeliverUrlRequest request) {

        validateDeliveryRequest(request);

        String user = request.getDiscordUserId();
        Optional<UserPreferences> maybePreferences = preferencesService.getUserPreferences(user);

        DeliveryType deliveryTypeChoice = maybePreferences
                .map(prefs -> {
                    logger.info("Client requested delivery type {} to user ID {}, whose preferences are: " +
                            "pushEnabled={}, automaticWorkflowEnabled={}",
                            request.getPreferredDeliveryType(), user, prefs.isPushEnabled(), prefs.isAutomaticWorkflowEnabled());
                    switch (request.getPreferredDeliveryType()) {
                        case AUTOMATIC_WORKFLOW:
                            if (prefs.isPushEnabled() && prefs.isAutomaticWorkflowEnabled()) {
                                logger.info("Automatic flow allowed");
                                return DeliveryType.AUTOMATIC_WORKFLOW;
                            }
                        case PUSH_NOTIFICATION:
                            if (prefs.isPushEnabled()) {
                                logger.info("Browser notification delivery allowed");
                                return DeliveryType.PUSH_NOTIFICATION;
                            }
                        default:
                            logger.info("Default private message delivery allowed");
                            return DeliveryType.PRIVATE_MESSAGE_LINK;
                    }
                }).orElseGet(() -> {
                    logger.info("Defaulting to private message delivery since no preferences are set");
                    return DeliveryType.PRIVATE_MESSAGE_LINK;
                });

        // Finally action the request

        switch(deliveryTypeChoice) {
            case PRIVATE_MESSAGE_LINK:
                deliverPrivateMessage(request);
                break;
            case PUSH_NOTIFICATION:
                deliverNotification(request, maybePreferences.get().getPushSubscription());
                break;
            case AUTOMATIC_WORKFLOW:
                break;
        }

        return new DeliverUrlResponse(deliveryTypeChoice);

    }

    private void validateDeliveryRequest(DeliverUrlRequest r) {

        if (r.getDiscordUserId() == null || r.getUrl() == null || r.getPreferredDeliveryType() == null
                || r.getLongDisplayText() == null) {
            throw new RequestValidationException("Common required parameters missing");
        }

        if (isInvalidUrl(r.getUrl())) {
            throw new RequestValidationException("Provided URL is invalid or not allowed");
        }

        if (!Utils.inRangeInclusive(r.getTtlSeconds(), 1, UrlShortenerConfig.MAX_TTL_SECONDS)) {
            throw new RequestValidationException("Provided TTL parameter is out of range");
        }

        if (r.getPreferredDeliveryType() == DeliveryType.AUTOMATIC_WORKFLOW
                && (r.getNotificationDisplayText() == null || r.getWorkflowDisplayText() == null)) {
            throw new RequestValidationException("Requested automatic delivery without required display text");
        }

        if (r.getPreferredDeliveryType() == DeliveryType.PUSH_NOTIFICATION && r.getNotificationDisplayText() == null) {
            throw new RequestValidationException("Requested notification delivery without required display text");
        }

    }

    private void deliverPrivateMessage(DeliverUrlRequest request) {

        String shortUrl = shortener.generateShortUrl(request.getUrl(), request.getTtlSeconds());
        logger.info("Delivering via private message as short URL {}", shortUrl);

        String text;
        SimpleEmbed embed;
        if (request.getLongDisplayEmbed() == null) {
            text = request.getLongDisplayText() + "\n\n" + shortUrl;
            embed = null;
        } else {
            text = request.getLongDisplayText();
            embed = request.getLongDisplayEmbed().withUrl(shortUrl);
        }
        NewMessageResponse message = discordService.newMessage(new NewMessageRequest(
                text, null, null, request.getDiscordUserId(), embed
        ));

        logger.info("Delivered to user as private message ID={}", message.getDiscordRealMessageId());

    }

    private void deliverNotification(DeliverUrlRequest request, WebPushSubscription subscription) {
        String shortUrl = shortener.generateShortUrl(request.getUrl(), request.getTtlSeconds());
        logger.info("Delivering via notification as short URL {}", shortUrl);

        doPushNotification(shortUrl, request.getNotificationDisplayText(), false, subscription);
    }

    private void deliverAutomaticNotification(DeliverUrlRequest request, WebPushSubscription subscription) {
        doPushNotification(request.getUrl(), request.getWorkflowDisplayText(), true, subscription);
    }

    private void doPushNotification(String url, String text, boolean fetchDirect, WebPushSubscription subscription) {
        JsonObject payload = new JsonObject();
        payload.addProperty("url", url);
        payload.addProperty("text", text);
        payload.addProperty("fetchDirect", fetchDirect);

        try {
            Notification notification = new Notification(
                    subscription.getPushEndpoint(),
                    subscription.getKeyBase64UrlEncoded(),
                    subscription.getAuthBase64UrlEncoded(),
                    payload.toString()
            );
            pushService.send(notification);
        } catch (Exception e) {
            // Catching 'Exception' is generally bad, but this lib has too many checked exceptions to usefully write
            // handlers for all of them. Better to see how this fails in practice and revisit.
            logger.error("Failed to prepare push notification due to crypto error", e);
            throw new RequestHandlingException("Unexpected error occurred while sending push notification");
        }
    }

    @Override
    public GetFullUrlResponse getFullUrl(GetFullUrlRequest request) {

        if (request.getTokenVersion() != 1) {
            throw new RequestValidationException("This API only supports version 1");
        }
        String token = Objects.requireNonNull(request.getUrlToken());

        String fullUrl = shortener.getFullUrl(1, token);

        if (isInvalidUrl(fullUrl)) {
            logger.error("Stored URL is somehow invalid. Should never occur due to validation on store ('{}')", fullUrl);
            throw new RequestHandlingException("Invalid result URL");
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
