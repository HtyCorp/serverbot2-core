package io.mamish.serverbot2.workflow;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// I forgot I was running on Lambda, not EC2, so this whole class is unnecessary. Oops.
public class InstanceMetadata {

    private String accountId;
    private String imageId;
    private String instanceId;
    private String instanceType;
    private String privateIp;
    private String region;

    private static final Gson gson = new Gson();
    private static final URI identityMetadataUri =
            URI.create("http://169.254.169.254/latest/dynamic/instance-identity/document");
    private static final HttpClient http = HttpClient.newBuilder().build();

    public static InstanceMetadata fetch() {
        try {
            String data = http.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(identityMetadataUri).build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
            return gson.fromJson(data, InstanceMetadata.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAccountId() {
        return accountId;
    }

    public String getImageId() {
        return imageId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getPrivateIp() {
        return privateIp;
    }

}
