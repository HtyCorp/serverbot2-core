package com.admiralbot.appdaemon;

import com.admiralbot.sharedutil.LogUtils;
import com.google.gson.Gson;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// TODO: Might need to update to IMDSv2 (not sure if instance-identity category is restricted in V2 mode)
public class InstanceMetadata {

    private String accountId;
    private String imageId;
    private String instanceId;
    private String instanceType;
    private String privateIp;
    private String region;

    private static final Logger logger = LoggerFactory.getLogger(InstanceMetadata.class);

    private static final Gson gson = new Gson();
    private static final URI identityMetadataUri =
            URI.create("http://169.254.169.254/latest/dynamic/instance-identity/document");
    private static final HttpClient http = HttpClient.newBuilder().build();

    private static volatile InstanceMetadata instance;

    public static InstanceMetadata fetch() {
        if (instance == null) {
            setInstance();
        }
        return instance;
    }

    private static synchronized void setInstance() {
        if (instance == null) {
            try {
                logger.debug("Getting new instance metadata");
                String data = http.send(HttpRequest.newBuilder()
                                .GET()
                                .uri(identityMetadataUri).build(),
                        HttpResponse.BodyHandlers.ofString()
                ).body();
                LogUtils.debugDump(logger, "Returned instance metadata is:", data);
                instance = gson.fromJson(data, InstanceMetadata.class);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
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
