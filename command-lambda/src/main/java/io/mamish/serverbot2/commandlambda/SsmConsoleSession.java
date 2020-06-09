package io.mamish.serverbot2.commandlambda;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mamish.serverbot2.sharedconfig.CommandLambdaConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SsmConsoleSession {

    private static final String SESSION_ROLE_ARN = CommandLambdaConfig.TERMINAL_SESSION_ROLE_ARN.getValue();
    // Would be nice if we could reuse the STS client's URL HTTP client, but no good sources on whether it's possible
    private static final HttpClient httpClient = HttpClient.newBuilder().build();
    private static final StsClient stsClient = StsClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .build();

    private final Logger logger = LogManager.getLogger(SsmConsoleSession.class);

    private final String instanceId;
    private final String sessionName;

    public SsmConsoleSession(String instanceId, String sessionName) {
        this.instanceId = instanceId;
        this.sessionName = sessionName;
    }

    String getSessionUrl() throws IOException, InterruptedException {

        // TODO: Come up with a better standard way to write JSON objects - this sucks

        String singleInstanceSessionPolicy = String.join("\n",
                "{",
                "    \"Version\": \"2012-10-17\"",
                "    \"Statement\": [{",
                "        \"Effect\": \"Allow\"",
                "        \"Action\": \"ssm:StartSession\"",
                "        \"Resource\": \"arn:aws:ec2:*:*:instance/" + instanceId + "\"",
                "    }]",
                "}"
        );

        logger.debug("Dumping session policy JSON:\n" + singleInstanceSessionPolicy);
        
        AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(r -> r.roleArn(SESSION_ROLE_ARN)
                .roleSessionName(sessionName)
                .policy(singleInstanceSessionPolicy));
        Credentials roleCredentials = assumeRoleResponse.credentials();

        logger.debug("Got key ID " + roleCredentials.accessKeyId()
                + " for role session " + assumeRoleResponse.assumedRoleUser().arn());

        JsonObject sessionObject = new JsonObject();
        sessionObject.addProperty("sessionId", roleCredentials.accessKeyId());
        sessionObject.addProperty("sessionKey", roleCredentials.secretAccessKey());
        sessionObject.addProperty("sessionToken", roleCredentials.sessionToken());
        String sessionStringJson = sessionObject.toString();
        String sessionStringEncoded = URLEncoder.encode(sessionStringJson, StandardCharsets.UTF_8);

        logger.debug("Dumping obfuscated session string:\n"
                + sessionStringJson.replace(roleCredentials.secretAccessKey(), "***REDACTED***"));

        final int sessionDurationSeconds = (int) Duration.ofHours(CommandLambdaConfig.TERMINAL_SESSION_ROLE_DURATION_HOURS).getSeconds();
        String getSigninTokenUrl = "https://signin.aws.amazon.com/federation"
                + "?Action=getSigninToken"
                + "&SessionDuration=" + sessionDurationSeconds
                + "&SessionType=json"
                + "&Session=" + sessionStringEncoded;

        HttpRequest getSigninToken = HttpRequest.newBuilder().GET().uri(URI.create(getSigninTokenUrl)).build();
        String tokenResponse = httpClient.send(getSigninToken, HttpResponse.BodyHandlers.ofString()).body();
        String signinToken = JsonParser.parseString(tokenResponse).getAsJsonObject().get("SigninToken").getAsString();

        // TODO: Set up some simple static page explain how to re-auth
        String issuer = "https://www.notarealwebsitedontclickthis.com";
        String region = System.getenv("AWS_REGION");
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

}
