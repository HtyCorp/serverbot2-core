package io.mamish.serverbot2.framework.client;

import com.amazonaws.xray.AWSXRay;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mamish.serverbot2.framework.common.ApiActionDefinition;
import io.mamish.serverbot2.framework.common.ApiDefinitionSet;
import io.mamish.serverbot2.framework.exception.ApiException;
import io.mamish.serverbot2.framework.exception.ServerExceptionDto;
import io.mamish.serverbot2.framework.exception.ServerExceptionParser;
import io.mamish.serverbot2.framework.exception.client.ApiClientException;
import io.mamish.serverbot2.framework.exception.server.SerializationException;
import io.mamish.serverbot2.framework.server.LambdaApiServer;
import io.mamish.serverbot2.sharedconfig.ApiConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.sharedutil.Pair;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

public final class ApiClient {

    private static final Gson gson = new Gson();
    private static final SqsRequestResponseClient sqsRequestResponse = new SqsRequestResponseClient();
    private static final LambdaClient lambdaClient = LambdaClient.builder()
            .overrideConfiguration(r -> r.apiCallTimeout(Duration.ofSeconds(ApiConfig.CLIENT_DEFAULT_TIMEOUT)))
            .build();



    private ApiClient() {}

    public static <ModelType> ModelType lambda(Class<ModelType> modelInterfaceClass, String functionName) {
        return makeProxyInstance(modelInterfaceClass, payloadAndId -> {
            SdkBytes lambdaPayload = SdkBytes.fromUtf8String(payloadAndId.a());
            InvokeResponse response = lambdaClient.invoke(r -> r.payload(lambdaPayload)
                    .functionName(functionName));
            return response.payload().asUtf8String();
        });
    }

    public static <ModelType> ModelType localLambda(Class<ModelType> modelInterfaceClass, LambdaApiServer<ModelType> localFunction) {
        final int outputCapacity = 262144; // 256KB
        final Charset UTF8 = StandardCharsets.UTF_8;
        return makeProxyInstance(modelInterfaceClass, payloadAndId -> {
            InputStream inputStream = new ByteArrayInputStream(payloadAndId.a().getBytes(UTF8));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(outputCapacity);
            try {
                localFunction.handleRequest(inputStream, outputStream, null);
                return new String(outputStream.toByteArray(), UTF8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <ModelType> ModelType sqs(Class<ModelType> modelInterfaceClass, String queueName) {
        final String queueUrl = sqsRequestResponse.getQueueUrl(queueName);
        return makeProxyInstance(modelInterfaceClass, payloadAndId -> sqsRequestResponse.sendAndReceive(
                queueUrl,
                payloadAndId.a(),
                ApiConfig.CLIENT_DEFAULT_TIMEOUT,
                payloadAndId.b())
        );
    }

    /*
     * Makes a few assumptions that should be documented/validated in models somewhere:
     * 1) Requests have only one (composite) argument.
     * 2) Definition set is not missing any definitions for the interface.
     */
    private static <ModelType> ModelType makeProxyInstance(Class<ModelType> modelInterfaceClass,
                Function<Pair<String,String>,String> senderReceiver) {
        ApiDefinitionSet<ModelType> definitionSet = new ApiDefinitionSet<>(modelInterfaceClass);
        @SuppressWarnings("unchecked")
        ModelType modelType = (ModelType) Proxy.newProxyInstance(
                ApiClient.class.getClassLoader(),
                new Class<?>[]{modelInterfaceClass},
                ((proxy, method, args) -> {
                    try {
                        AWSXRay.beginSubsegment(modelInterfaceClass.getSimpleName()+"Client");

                        Object request = args[0];
                        ApiActionDefinition apiDefinition = definitionSet.getFromRequestClass(request.getClass());
                        String requestId = IDUtils.randomUUID();
                        String payload = annotatedJsonPayload(request, apiDefinition, requestId);

                        String responseString = senderReceiver.apply(new Pair<>(payload, requestId));

                        JsonObject response = JsonParser.parseString(responseString).getAsJsonObject();
                        JsonElement error = response.get(ApiConfig.JSON_RESPONSE_ERROR_KEY);
                        JsonElement content = response.get(ApiConfig.JSON_RESPONSE_CONTENT_KEY);

                        // If error provided, generate the exception (basic details only) and throw.
                        if (error != null && !error.isJsonNull()) {
                            ServerExceptionDto info = gson.fromJson(error, ServerExceptionDto.class);
                            throw ServerExceptionParser.fromName(
                                    info.getExceptionTypeName(),
                                    info.getExceptionMessage());
                        }
                        // Otherwise, parse the content and return as expected response type.
                        Object responseObject = null;
                        if (apiDefinition.hasResponseType()) {
                            responseObject = gson.fromJson(content, apiDefinition.getResponseDataType());
                        }
                        return responseObject;
                    } catch (ApiException e) {
                        throw e;
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        throw new ApiClientException("Unexpected error while making client request", e);
                    } finally {
                        AWSXRay.endSubsegment();
                    }
                })
        );
        return modelType;
    }

    private static String annotatedJsonPayload(Object request, ApiActionDefinition apiDefinition, String requestId) {
        JsonElement jsonTree = gson.toJsonTree(request);
        if (!jsonTree.isJsonObject()) {
            throw new SerializationException("Response object is not a JSON object. Java type is " + request.getClass());
        }
        JsonObject jsonObject = jsonTree.getAsJsonObject();

        String KEY = ApiConfig.JSON_REQUEST_TARGET_KEY;
        String RID = ApiConfig.JSON_REQUEST_ID_KEY;
        if (jsonObject.has(KEY)) {
            throw new SerializationException("Response object already has property " + KEY + " set.");
        }
        if (jsonObject.has(RID)) {
            throw new SerializationException("Response object already has property " + RID + " set.");
        }

        jsonObject.addProperty(KEY, apiDefinition.getName());
        jsonObject.addProperty(RID, requestId);
        return gson.toJson(jsonObject);
    }

}
