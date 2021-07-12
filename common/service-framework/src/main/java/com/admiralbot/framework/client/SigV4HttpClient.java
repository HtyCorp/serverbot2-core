package com.admiralbot.framework.client;

import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.sharedutil.Utils;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.TraceHeader;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.*;

import java.io.IOException;
import java.net.URI;
import java.util.*;

public class SigV4HttpClient {

    private final Aws4Signer requestSigner = Aws4Signer.create();

    private final AppContext appContext;

    private static final Logger logger = LoggerFactory.getLogger(SigV4HttpClient.class);

    public SigV4HttpClient(AppContext appContext) {
        Objects.requireNonNull(appContext, "SigV4HttpClient requires a non-null app context");
        this.appContext = appContext;
    }

    public SigV4HttpResponse post(String uri, String body, String serviceName)
            throws IOException {

        String bodyOrEmpty = Optional.ofNullable(body).orElse("");
        ContentStreamProvider bodyProvider = SdkBytes.fromUtf8String(bodyOrEmpty).asContentStreamProvider();

        // Propagate Xray trace ID into request headers if one is found
        Map<String, List<String>> extraHeaders = new HashMap<>(1);
        Utils.ifNotNull(AWSXRay.getTraceEntity(), Entity::getTraceId, traceId -> {
            logger.debug("Adding discovered Trace ID to HTTP headers");
            extraHeaders.put(TraceHeader.HEADER_KEY, List.of(traceId.toString()));
        });

        SdkHttpFullRequest baseRequest = SdkHttpFullRequest.builder()
                .uri(URI.create(uri))
                .method(SdkHttpMethod.POST)
                .headers(extraHeaders)
                .contentStreamProvider(bodyProvider)
                .build();

        AwsCredentials credentials = appContext.resolveCredentials();
        logger.debug("Signing HTTP request with access key ID {}", credentials.accessKeyId());

        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(credentials)
                .doubleUrlEncode(false)
                .signingName(serviceName)
                .signingRegion(appContext.getRegion())
                .build();

        SdkHttpFullRequest signedRequest = requestSigner.sign(baseRequest, signerParams);

        HttpExecuteResponse response = appContext.getHttpClient().prepareRequest(HttpExecuteRequest.builder()
                .request(signedRequest)
                .contentStreamProvider(bodyProvider)
                .build()
        ).call();

        return new SigV4HttpResponse(
                response.httpResponse().statusCode(),
                response.httpResponse().statusText().orElse(""),
                response.responseBody().map(b -> SdkBytes.fromInputStream(b).asUtf8String())
        );

    }


}
