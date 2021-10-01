package com.admiralbot.sharedconfig;

import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.sharedutil.sigv4.SigV4HttpClient;
import com.admiralbot.sharedutil.sigv4.SigV4HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A tiny API client to fetch SSM parameters and Secrets Manager secrets.
 * By implementing this we avoid importing the SSM and Secrets Manager SDKs which removes a lot of needless artifact
 * storage (at least 4MB for SSM models).
 */
public class MicroAwsClient {

    private static final Logger log = LoggerFactory.getLogger(MicroAwsClient.class);
    private static final Gson GSON = new Gson();

    private static volatile SigV4HttpClient client = null;

    private static SigV4HttpClient getClient() {
        if (client == null) {
            synchronized (MicroAwsClient.class) {
                if (client == null) {
                    log.info("Lazy instantiating SigV4 client");
                    client = new SigV4HttpClient(AppContext.get());
                }
            }
        }
        return client;
    }

    public static String ssmGetParameter(String parameterName) {
        return fetch(parameterName, "ssm", "AmazonSSM.GetParameter",
                "Name", List.of("Parameter", "Value"));
    }

    public static String secretsManagerGetSecretValue(String secretId) {
        return fetch(secretId, "secretsmanager", "secretsmanager.GetSecretValue",
                "SecretId", List.of("SecretString"));
    }

    private static String fetch(String id, String service, String serviceTarget, String requestKey,
                                List<String> responseKeyPath) {
        log.info("Invoking {}/{} with request {}={}", service, serviceTarget, requestKey, id);
        try {
            String regionalUri = String.format("https://%s.%s.amazonaws.com/",
                    service, AppContext.get().getRegion().toString().toLowerCase());
            String requestJson = String.format("{\"%s\":\"%s\"}", requestKey, id);
            SigV4HttpResponse response = getClient().post(regionalUri, requestJson, service, Map.of(
                    "Accept-Encoding", "identity",
                    "Content-Type", "application/x-amz-json-1.1",
                    "X-Amz-Target", serviceTarget
            ));
            String parameterBody = response.getBody().orElseThrow();
            JsonObject parameterJson = JsonParser.parseString(parameterBody).getAsJsonObject();
            return jsonPathGetString(parameterJson, responseKeyPath);
        } catch (IOException e) {
            throw new RuntimeException("Value fetch on service <" + service + "> failed", e);
        }
    }

    private static String jsonPathGetString(JsonObject jsonObject, List<String> path) {
        JsonElement result = jsonObject;
        for (String pathElement: path) {
            result = result.getAsJsonObject().get(pathElement);
        }
        return result.getAsString();
    }

}
