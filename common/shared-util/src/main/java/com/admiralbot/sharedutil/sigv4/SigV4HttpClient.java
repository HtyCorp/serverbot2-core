package com.admiralbot.sharedutil.sigv4;

import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.sharedutil.XrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public SigV4HttpResponse post(String uri, String body, String serviceName) throws IOException {
        return post(uri, body, serviceName, Map.of());
    }

    public SigV4HttpResponse post(String uri, String body, String serviceName, Map<String,String> headers)
            throws IOException {
        return postWithTrace(uri, body, serviceName, headers);
    }

    private SigV4HttpResponse postWithTrace(String uri, String body, String serviceName, Map<String,String> headers)
            throws IOException{
        URI requestUri = URI.create(uri);
        if (!XrayUtils.isInSegment()) {
            return doPost(requestUri, body, serviceName, headers);
        } else {
            try {
                XrayUtils.beginSubsegment(requestUri.getHost());
                return doPost(requestUri, body, serviceName, headers);
            } catch (Exception e) {
                XrayUtils.addSubsegmentException(e);
                throw e;
            } finally {
                XrayUtils.endSubsegment();
            }
        }
    }

    private SigV4HttpResponse doPost(URI uri, String body, String serviceName, Map<String,String> headers)
            throws IOException {

        String bodyOrEmpty = Optional.ofNullable(body).orElse("");
        ContentStreamProvider bodyProvider = SdkBytes.fromUtf8String(bodyOrEmpty).asContentStreamProvider();

        // Propagate Xray trace ID into request headers if one is found
        Map<String,List<String>> extraHeaders = new HashMap<>(headers.size()+2);
        extraHeaders.put("Content-Type", List.of("application/json"));
        headers.forEach((k, v) -> extraHeaders.put(k, List.of(v)));
        String traceHeader = XrayUtils.getTraceHeaderString();
        if (traceHeader == null) {
            logger.warn("HTTP request outside of Xray trace, skipping trace header");
        } else {
            extraHeaders.put(XrayUtils.TRACE_HEADER_HTTP_HEADER_KEY, List.of(traceHeader));
        }

        SdkHttpFullRequest baseRequest = SdkHttpFullRequest.builder()
                .uri(uri)
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
