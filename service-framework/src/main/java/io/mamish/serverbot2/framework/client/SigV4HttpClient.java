package io.mamish.serverbot2.framework.client;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.TraceHeader;
import io.mamish.serverbot2.sharedutil.Utils;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SigV4HttpClient {

    private final SdkHttpClient httpClient = UrlConnectionHttpClient.create();
    private final Aws4Signer requestSigner = Aws4Signer.create();

    public SigV4HttpResponse post(String uri, String body, String serviceName, Region signingRegion, AwsCredentials credentials)
            throws IOException {

        String bodyOrEmpty = Optional.ofNullable(body).orElse("");
        ContentStreamProvider bodyProvider = SdkBytes.fromUtf8String(bodyOrEmpty).asContentStreamProvider();

        // Propagate Xray trace ID into request headers if one is found
        Map<String, List<String>> extraHeaders = new HashMap<>(1);
        Utils.ifNotNull(AWSXRay.getTraceEntity(), Entity::getTraceId, traceId -> extraHeaders.put(
                TraceHeader.HEADER_KEY,
                List.of(traceId.toString())
        ));

        SdkHttpFullRequest baseRequest = SdkHttpFullRequest.builder()
                .uri(URI.create(uri))
                .method(SdkHttpMethod.POST)
                .headers(extraHeaders)
                .contentStreamProvider(bodyProvider)
                .build();

        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(credentials)
                .doubleUrlEncode(false)
                .signingName(serviceName)
                .signingRegion(signingRegion)
                .build();

        SdkHttpFullRequest signedRequest = requestSigner.sign(baseRequest, signerParams);

        HttpExecuteResponse response = httpClient.prepareRequest(HttpExecuteRequest.builder()
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
