package com.admiralbot.framework.client;

import com.admiralbot.framework.exception.ApiException;
import com.admiralbot.framework.exception.ServerExceptionDto;
import com.admiralbot.framework.exception.ServerExceptionParser;
import com.admiralbot.framework.exception.client.ApiClientException;
import com.admiralbot.framework.exception.client.ApiClientIOException;
import com.admiralbot.framework.exception.client.ApiClientParseException;
import com.admiralbot.framework.exception.server.GatewayClientException;
import com.admiralbot.framework.exception.server.GatewayServerException;
import com.admiralbot.framework.exception.server.SerializationException;
import com.admiralbot.framework.modelling.ApiActionDefinition;
import com.admiralbot.framework.modelling.ApiDefinitionSet;
import com.admiralbot.framework.modelling.ApiEndpointInfo;
import com.admiralbot.framework.modelling.ApiHttpMethod;
import com.admiralbot.framework.server.LambdaApiServer;
import com.admiralbot.sharedconfig.ApiConfig;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.*;
import com.admiralbot.sharedutil.sigv4.SigV4HttpClient;
import com.admiralbot.sharedutil.sigv4.SigV4HttpResponse;
import com.google.gson.*;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class ApiClient {

    private static final Gson gson = new Gson();
    private static final SqsRequestResponseClient sqsRequestResponse = new SqsRequestResponseClient();

    private ApiClient() {}

    public static <ModelType> ModelType http(Class<ModelType> modelInterfaceClass) {
        final SigV4HttpClient httpClient = new SigV4HttpClient(AppContext.get());
        return makeProxyInstance(modelInterfaceClass, true, request -> {
            ApiEndpointInfo endpoint = request.getEndpointInfo();
            if (endpoint.httpMethod() != ApiHttpMethod.POST) {
                throw new IllegalArgumentException("Only HTTP POST is supported");
            }
            String uri = "https://"
                    + endpoint.serviceName()
                    + "."
                    + CommonConfig.SERVICES_SYSTEM_SUBDOMAIN
                    + "."
                    + CommonConfig.SYSTEM_ROOT_DOMAIN_NAME.getValue()
                    + endpoint.uriPath();
            try {
                SigV4HttpResponse response = httpClient.post(uri, request.getPayload(), "execute-api");
                if (response.getBody().isEmpty()) {
                    throw new ApiClientParseException("Service sent an empty response");
                }
                return new ServerResponse(response.getBody().get(), response.getStatusCode());
            } catch (IOException e) {
                throw new ApiClientIOException("Unexpected network error while making HTTP call", e);
            }
        });
    }

    public static <ModelType> ModelType lambda(Class<ModelType> modelInterfaceClass, String functionName) {
        LambdaClient lambdaClient = SdkUtils.client(LambdaClient.builder(),
                r -> r.apiCallTimeout(Duration.ofSeconds(ApiConfig.CLIENT_DEFAULT_TIMEOUT)));
        return makeProxyInstance(modelInterfaceClass, false, request -> {
            SdkBytes lambdaPayload = SdkBytes.fromUtf8String(request.getPayload());
            String functionLiveAlias = Joiner.colon(functionName, CommonConfig.LAMBDA_LIVE_ALIAS_NAME);
            InvokeResponse response = lambdaClient.invoke(r -> r.payload(lambdaPayload)
                    .functionName(functionLiveAlias));
            return new ServerResponse(response.payload().asUtf8String(), null);
        });
    }

    public static <ModelType> ModelType localLambda(Class<ModelType> modelInterfaceClass, LambdaApiServer<ModelType> localFunction) {
        final int outputCapacity = 262144; // 256KB
        final Charset UTF8 = StandardCharsets.UTF_8;
        return makeProxyInstance(modelInterfaceClass, false, request -> {
            InputStream inputStream = new ByteArrayInputStream(request.getPayload().getBytes(UTF8));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(outputCapacity);
            try {
                localFunction.handleRequest(inputStream, outputStream, null);
                return new ServerResponse(outputStream.toString(UTF8), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <ModelType> ModelType sqs(Class<ModelType> modelInterfaceClass, String queueName) {
        final String queueUrl = sqsRequestResponse.getQueueUrl(queueName);
        return makeProxyInstance(modelInterfaceClass, false, request -> {
            String sqsResponse = sqsRequestResponse.sendAndReceive(
                    queueUrl,
                    request.getPayload(),
                    ApiConfig.CLIENT_DEFAULT_TIMEOUT,
                    request.getRequestId()
            );
            return new ServerResponse(sqsResponse, null);
        });
    }

    /*
     * Makes a few assumptions that should be documented/validated in models somewhere:
     * 1) Requests have only one (composite) argument.
     * 2) Definition set is not missing any definitions for the interface.
     */
    private static <ModelType> ModelType makeProxyInstance(Class<ModelType> modelInterfaceClass,
               boolean requiresEndpointInfo, Function<ClientRequest,ServerResponse> requestSender) {
        ApiDefinitionSet<ModelType> definitionSet = new ApiDefinitionSet<>(modelInterfaceClass, requiresEndpointInfo);
        @SuppressWarnings("unchecked")
        ModelType modelType = (ModelType) Proxy.newProxyInstance(
                ApiClient.class.getClassLoader(),
                new Class<?>[]{modelInterfaceClass},
                new InvocationHandler() {

                    private final int serial = ThreadLocalRandom.current().nextInt();

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        try {

                            // Handle extra object methods first
                            switch (method.getName()) {
                                case "hashCode":
                                    return serial;
                                case "equals":
                                    return equals(args[0]);
                                case "toString":
                                    return toString();
                            }

                            XrayUtils.beginSubsegment(modelInterfaceClass.getSimpleName() + "Client");

                            Object requestPojo = args[0];
                            ApiActionDefinition apiDefinition = definitionSet.getFromRequestClass(requestPojo.getClass());
                            String requestId = IDUtils.randomUUID();
                            String payload = annotatedJsonPayload(requestPojo, apiDefinition, requestId);

                            ClientRequest clientRequest = new ClientRequest(definitionSet.getEndpointInfo(), payload, requestId);
                            ServerResponse serverResponse = requestSender.apply(clientRequest);

                            JsonObject response;
                            try {
                                response = JsonParser.parseString(serverResponse.getPayload()).getAsJsonObject();
                            } catch (JsonSyntaxException | IllegalArgumentException e) {
                                throw new ApiClientParseException("Server response is not well-formed JSON");
                            }

                            JsonElement content = response.get(ApiConfig.JSON_RESPONSE_CONTENT_KEY);
                            JsonElement error = response.get(ApiConfig.JSON_RESPONSE_ERROR_KEY);
                            JsonElement message = response.get(ApiConfig.JSON_RESPONSE_GATEWAY_MESSAGE_KEY);

                            if (content != null && !content.isJsonNull()) {
                                return (apiDefinition.hasResponseType())
                                        ? gson.fromJson(content, apiDefinition.getResponseDataType())
                                        : null;
                            }

                            // If error provided, generate the exception (basic details only) and throw.
                            if (error != null && !error.isJsonNull()) {
                                ServerExceptionDto info = gson.fromJson(error, ServerExceptionDto.class);
                                throw ServerExceptionParser.fromName(
                                        info.getExceptionTypeName(),
                                        info.getExceptionMessage());
                            }

                            // If other message provided, return it with type based on status code
                            if (message != null && !message.isJsonNull()) {
                                String messageStr = message.getAsString();
                                if (serverResponse.hasStatusCode()) {
                                    int statusCode = serverResponse.getStatusCode();
                                    if (Utils.inRangeInclusive(statusCode, 400, 499)) {
                                        throw new GatewayClientException(statusCode + ": " + messageStr);
                                    } else {
                                        throw new GatewayServerException(statusCode + ": " + messageStr);
                                    }
                                } else {
                                    throw new GatewayServerException(messageStr);
                                }
                            }

                            throw new ApiClientParseException("Server response didn't include any expected keys");

                        } catch (ApiException e) {
                            throw e; // Ensure the following RuntimeException catch doesn't override details
                        } catch (RuntimeException e) {
                            throw new ApiClientException("Unexpected error while making client request", e);
                        } finally {
                            XrayUtils.endSubsegment();
                        }
                    }
                });
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

    private static class ClientRequest {
        private final ApiEndpointInfo endpointInfo;
        private final String payload;
        private final String requestId;

        public ClientRequest(ApiEndpointInfo endpointInfo, String payload, String requestId) {
            this.endpointInfo = endpointInfo;
            this.payload = payload;
            this.requestId = requestId;
        }

        public ApiEndpointInfo getEndpointInfo() {
            return endpointInfo;
        }

        public String getPayload() {
            return payload;
        }

        public String getRequestId() {
            return requestId;
        }
    }

    private static class ServerResponse {
        private final String payload;
        private final Integer statusCode;

        public ServerResponse(String payload, Integer statusCode) {
            this.payload = payload;
            this.statusCode = statusCode;
        }

        public String getPayload() {
            return payload;
        }

        public boolean hasStatusCode() {
            return statusCode != null;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

}
