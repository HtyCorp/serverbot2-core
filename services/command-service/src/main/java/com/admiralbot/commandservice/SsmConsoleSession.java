package com.admiralbot.commandservice;

import com.admiralbot.sharedconfig.CommandLambdaConfig;
import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.sharedutil.LogUtils;
import com.admiralbot.sharedutil.SdkUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetFederationTokenResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SsmConsoleSession {

    // Would be nice if we could reuse the STS client's URL HTTP client, but no good sources on whether it's possible
    private static final HttpClient httpClient = HttpClient.newBuilder().build();
    private static final StsClient stsClient = SdkUtils.client(StsClient.builder());

    private final Logger logger = LoggerFactory.getLogger(SsmConsoleSession.class);

    private final String instanceId;
    private final String sessionName;

    public SsmConsoleSession(String instanceId, String sessionName) {
        this.instanceId = instanceId;
        this.sessionName = sessionName;
    }

    public String getSessionUrl() throws IOException, InterruptedException {

        // TODO: Come up with a better way to write JSON - SDKv2 doesn't appear to have policy builders

        JsonObject policyObject = new JsonObject();
        JsonArray statementArray = new JsonArray();
        JsonObject policyStatement = new JsonObject();

        policyObject.addProperty("Version", "2012-10-17");
        policyObject.add("Statement", statementArray);
        statementArray.add(policyStatement);
        policyStatement.addProperty("Effect", "Allow");
        policyStatement.addProperty("Action", "ssm:StartSession");
        policyStatement.addProperty("Resource", "arn:aws:ec2:*:*:instance/" + instanceId);

        String singleInstanceSessionPolicy = policyObject.toString();

        logger.debug("Dumping session policy JSON:\n" + singleInstanceSessionPolicy);

        final int sessionDurationSeconds = (int) CommandLambdaConfig.TERMINAL_SESSION_DURATION.getSeconds();

        // I would probably prefer a role here but console federation fails when the Lambda role is used to chain into
        // any other target role for sessions. IAM user is required.
        // Alternative: assume a target role with this IAM user. No major difference for our case.
        AwsCredentialsProvider federationAccessKeyProvider = getFederationAccessKey();
        GetFederationTokenResponse stsResponse = stsClient.getFederationToken(r -> r.name(sessionName)
                .policy(singleInstanceSessionPolicy)
                .durationSeconds(sessionDurationSeconds)
                .overrideConfiguration(conf -> conf.credentialsProvider(federationAccessKeyProvider)));
        Credentials credentials = stsResponse.credentials();

        logger.debug("Got key ID " + credentials.accessKeyId()
                + " for federated user ARN " + stsResponse.federatedUser().arn());

        JsonObject sessionObject = new JsonObject();
        sessionObject.addProperty("sessionId", credentials.accessKeyId());
        sessionObject.addProperty("sessionKey", credentials.secretAccessKey());
        sessionObject.addProperty("sessionToken", credentials.sessionToken());
        String sessionStringJson = sessionObject.toString();
        String sessionStringEncoded = URLEncoder.encode(sessionStringJson, StandardCharsets.UTF_8);

        LogUtils.debug(logger, () -> "Dumping obfuscated session string:\n"
                + sessionStringJson.replace(credentials.secretAccessKey(), "***REDACTED***"));

        String getSigninTokenUrl = "https://signin.aws.amazon.com/federation"
                + "?Action=getSigninToken"
                + "&SessionDuration=" + sessionDurationSeconds
                + "&SessionType=json"
                + "&Session=" + sessionStringEncoded;

        LogUtils.debug(logger, () -> "Dumping obfuscated URL:\n"
                + getSigninTokenUrl.replace(URLEncoder.encode(credentials.secretAccessKey(), StandardCharsets.UTF_8),
                "REDACTED"));

        HttpRequest getSigninToken = HttpRequest.newBuilder().GET().uri(URI.create(getSigninTokenUrl)).build();
        String tokenResponse = httpClient.send(getSigninToken, HttpResponse.BodyHandlers.ofString()).body();
        String signinToken = JsonParser.parseString(tokenResponse).getAsJsonObject().get("SigninToken").getAsString();

        // TODO: Set up some simple static page explain how to re-auth
        String issuer = "https://www.notarealwebsitedontclickthis.com";
        String region = AppContext.get().getRegion().toString();
        String destination = String.format("https://%s.console.aws.amazon.com/systems-manager/session-manager/%s?region=%s",
                region, instanceId, region);

        String loginUrl = "https://signin.aws.amazon.com/federation"
                + "?Action=login"
                + "&Issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8)
                + "&Destination=" + URLEncoder.encode(destination, StandardCharsets.UTF_8)
                + "&SigninToken=" + signinToken;

        logger.debug("Dumping obfuscated login URL:\n"
                + loginUrl.replace(signinToken, signinToken.substring(0,32) + "***REDACTED***"));

        return loginUrl;

    }

    private AwsCredentialsProvider getFederationAccessKey() {
        String[] parts = CommandLambdaConfig.TERMINAL_FEDERATION_ACCESS_KEY.getValue().split(":");
        String accessKeyId = parts[0];
        String secretAccessKey = parts[1];
        logger.debug("Got federation access key ID " + accessKeyId);
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }
}