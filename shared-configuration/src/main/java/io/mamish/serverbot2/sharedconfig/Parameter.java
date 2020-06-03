package io.mamish.serverbot2.sharedconfig;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.function.Function;

/**
 * A deferred configuration value that fetches the latest SSM parameter with the given name.
 */
public class Parameter extends ConfigValue {

    private static final SsmClient ssmClient = SsmClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .build();
    private static final Function<String,String> fetcher =  n -> ssmClient.getParameter(r -> r.name(n)).parameter().value();

    public Parameter(String path, String name) {
        // SSM params have a '/' at the start of their path, unlike SM secrets
        super("/" + path + "/" + name, fetcher);
    }

}
