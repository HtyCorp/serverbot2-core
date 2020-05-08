package io.mamish.serverbot2.framework.client;

import io.mamish.serverbot2.framework.common.ApiDefinitionSet;
import io.mamish.serverbot2.framework.common.BasicApiDefinition;
import io.mamish.serverbot2.sharedutil.AnnotatedGson;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class ServiceClient {

    private static final AnnotatedGson gson = new AnnotatedGson();
    private static final LambdaClient lambdaClient = LambdaClient.create();
    private static final SqsClient sqsClient = SqsClient.create();

    private ServiceClient() {}

    public static <ModelType> ModelType lambda(Class<ModelType> modelInterfaceClass, String functionName) {
        ApiDefinitionSet<ModelType> definitionSet = new ApiDefinitionSet<>(modelInterfaceClass);
        @SuppressWarnings("unchecked")
        ModelType modelType = (ModelType) Proxy.newProxyInstance(
                ServiceClient.class.getClassLoader(),
                new Class<?>[]{modelInterfaceClass},
                makeLambdaHandler(definitionSet, functionName)
        );
        return modelType;
    }

    /*
     * Makes a few assumptions that should be documented/validation in models somewhere:
     * 1) Requests have only one (composite) argument.
     * 2) Definition set is not missing any definitions for the interface.
     */
    private static InvocationHandler makeLambdaHandler(ApiDefinitionSet<?> definitionSet, String functionName) {
        return (proxy, method, args) -> {
            Object request = args[0];
            BasicApiDefinition apiDefinition = definitionSet.getFromRequestClass(request.getClass());
            SdkBytes requestPayload = SdkBytes.fromUtf8String(gson.toJsonWithTarget(request));
            InvokeResponse response = lambdaClient.invoke(r -> r.payload(requestPayload).functionName(functionName));

            if (apiDefinition.hasResponseType()) {
                String responseString = response.payload().asUtf8String();
                return gson.fromJson(responseString, apiDefinition.getResponseDataType());
            } else {
                return null;
            }
        };
    }

}
