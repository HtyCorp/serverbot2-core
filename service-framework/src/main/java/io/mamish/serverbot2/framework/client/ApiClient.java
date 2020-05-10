package io.mamish.serverbot2.framework.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mamish.serverbot2.framework.common.ApiActionDefinition;
import io.mamish.serverbot2.framework.common.ApiDefinitionSet;
import io.mamish.serverbot2.framework.common.ApiErrorInfo;
import io.mamish.serverbot2.framework.exception.ApiException;
import io.mamish.serverbot2.framework.exception.SerializationException;
import io.mamish.serverbot2.framework.server.LambdaStandardApiHandler;
import io.mamish.serverbot2.sharedconfig.ApiConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;
import io.mamish.serverbot2.sharedutil.Pair;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.function.Function;

public final class ApiClient {

    private static final Gson gson = new Gson();
    private static final LambdaClient lambdaClient = LambdaClient.builder()
            .overrideConfiguration(r -> r.apiCallTimeout(Duration.ofSeconds(ApiConfig.CUSTOM_API_CLIENT_DEFAULT_TIMEOUT)))
            .build();

    // Don't initialise SQS unless used: creates temporary SQS queue needing cleanup later.
    private static final Object sqsRequestReponseLock = new Object();
    private static SqsRequestResponseClient sqsRequestResponse;

    private ApiClient() {}

    public static <ModelType> ModelType lambda(Class<ModelType> modelInterfaceClass, String functionName) {
        return makeProxyInstance(modelInterfaceClass, payloadAndId -> {
            SdkBytes lambdaPayload = SdkBytes.fromUtf8String(payloadAndId.fst());
            InvokeResponse response = lambdaClient.invoke(r -> r.payload(lambdaPayload)
                    .functionName(functionName));
            return response.payload().asUtf8String();
        });
    }

    public static <ModelType> ModelType localLambda(Class<ModelType> modelInterfaceClass, LambdaStandardApiHandler localFunction) {
        return makeProxyInstance(modelInterfaceClass, payloadAndId -> {
            return localFunction.handleRequest(payloadAndId.fst(), null);
        });
    }

    public static <ModelType> ModelType sqs(Class<ModelType> modelInterfaceClass, String queueName) {

        if (sqsRequestResponse == null) {
            synchronized(sqsRequestReponseLock) {
                if (sqsRequestResponse == null) {
                    sqsRequestResponse = new SqsRequestResponseClient();
                }
            }
        }

        final String queueUrl = sqsRequestResponse.getQueueUrl(queueName);
        return makeProxyInstance(modelInterfaceClass, payloadAndId -> {
            return sqsRequestResponse.sendAndReceive(queueUrl,
                    payloadAndId.fst(),
                    ApiConfig.CUSTOM_API_CLIENT_DEFAULT_TIMEOUT,
                    payloadAndId.snd());
        });

    }

    /*
     * Makes a few assumptions that should be documented/validation in models somewhere:
     * 1) Requests have only one (composite) argument.
     * 2) Definition set is not missing any definitions for the interface.
     */
    public static <ModelType> ModelType makeProxyInstance(Class<ModelType> modelInterfaceClass,
                Function<Pair<String,String>,String> senderReceiver) {
        ApiDefinitionSet<ModelType> definitionSet = new ApiDefinitionSet<>(modelInterfaceClass);
        @SuppressWarnings("unchecked")
        ModelType modelType = (ModelType) Proxy.newProxyInstance(
                ApiClient.class.getClassLoader(),
                new Class<?>[]{modelInterfaceClass},
                ((proxy, method, args) -> {
                    Object request = args[0];
                    ApiActionDefinition apiDefinition = definitionSet.getFromRequestClass(request.getClass());
                    String requestId = IDUtils.randomUUID();
                    String payload = annotatedJsonPayload(request, apiDefinition, requestId);

                    String responseString = senderReceiver.apply(new Pair<>(payload,requestId));

                    JsonObject response = JsonParser.parseString(responseString).getAsJsonObject();
                    JsonElement error = response.get(ApiConfig.JSON_RESPONSE_ERROR_KEY);
                    JsonElement content = response.get(ApiConfig.JSON_RESPONSE_CONTENT_KEY);

                    // If error provided, generate the exception (basic details only) and throw.
                    if (error != null) {
                        ApiErrorInfo info = gson.fromJson(error, ApiErrorInfo.class);
                        ApiException deserialisedException = ApiErrorTransformer.fromName(
                                info.getExceptionTypeName(),
                                info.getExceptionMessage());
                        throw deserialisedException;
                    }
                    // Otherwise, parse the content and return as expected response type.
                    else {
                        if (apiDefinition.hasResponseType()) {
                            return gson.fromJson(content, apiDefinition.getResponseDataType());
                        } else {
                            return null;
                        }
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
