package io.mamish.serverbot2.commandlambda;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mamish.serverbot2.sharedconfig.CommandLambdaConfig;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sts.StsClient;
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

        final int durationSeconds = (int) Duration.ofHours(CommandLambdaConfig.TERMINAL_SESSION_ROLE_DURATION_HOURS).getSeconds();
        Credentials roleCredentials = stsClient.assumeRole(r -> r.roleArn(SESSION_ROLE_ARN)
                .roleSessionName(sessionName)
                .durationSeconds(durationSeconds)
                .policy(singleInstanceSessionPolicy)
        ).credentials();

        JsonObject sessionObject = new JsonObject();
        sessionObject.addProperty("sessionId", roleCredentials.accessKeyId());
        sessionObject.addProperty("sessionKey", roleCredentials.secretAccessKey());
        sessionObject.addProperty("sessionToken", roleCredentials.sessionToken());
        String sessionString = URLEncoder.encode(sessionObject.toString(), StandardCharsets.UTF_8);

        String getSigninTokenUrl = "https://signin.aws.amazon.com/federation"
                + "?Action=getSigninToken"
                + "&SessionDuration=" + durationSeconds
                + "&SessionType=json"
                + "&Session=" + sessionString;

        HttpRequest getSigninToken = HttpRequest.newBuilder().GET().uri(URI.create(getSigninTokenUrl)).build();
        String tokenResponse = httpClient.send(getSigninToken, HttpResponse.BodyHandlers.ofString()).body();
        String signinToken = JsonParser.parseString(tokenResponse).getAsJsonObject().get("SigninToken").getAsString();

        // TODO: Set up some simple static page explain how to re-auth
        String issuer = "https://www.notarealwebsitedontclickthis.com";
        String region = System.getenv("AWS_REGION");
        String destination = String.format("https://%s.console.aws.amazon.com/systems-manager/session-manager/%s?region=%s",
                region, instanceId, region);

        return "https://signin.aws.amazon.com/federation"
                + "?Action=login"
                + "&Issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8)
                + "&Destination=" + URLEncoder.encode(destination, StandardCharsets.UTF_8)
                + "&SigninToken=" + signinToken;

    }

}
