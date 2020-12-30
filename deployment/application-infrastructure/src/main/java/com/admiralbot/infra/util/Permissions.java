package com.admiralbot.infra.util;

import com.admiralbot.framework.common.ApiEndpointInfo;
import com.admiralbot.sharedconfig.CommonConfig;
import com.admiralbot.sharedutil.Joiner;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Permissions {

    private Permissions() {}

    public static void addConfigPathRead(Stack stack, IGrantable grantee, String... paths) {
        List<String> secretAndParameterArns = Arrays.stream(paths)
                .flatMap(path -> Stream.of(
                        Util.arn(stack, null, null, "ssm", "parameter/"+path+"/*"),
                        Util.arn(stack, null, null, "secretsmanager", "secret:"+path+"/*")
                )).collect(Collectors.toList());

        grantee.getGrantPrincipal().addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(List.of(
                        "ssm:GetParameter",
                        "secretsmanager:GetSecretValue")
                ).resources(secretAndParameterArns)
                .build());
    }

    public static void addExecuteApi(Stack stack, IGrantable grantee, Class<?>... interfaceClasses) {
        addExecuteApi(stack, null, grantee, interfaceClasses);
    }

    public static void addExecuteApi(Stack stack, String region, IGrantable grantee, Class<?>... interfaceClasses) {
        List<String> stageArns = Arrays.stream(interfaceClasses)
                .map(cls -> {
                    ApiEndpointInfo endpointInfo = cls.getAnnotation(ApiEndpointInfo.class);
                    // Resource segment format is "api-id/stage-name/http-method/api-resource"
                    // Full example ARN: "arn:aws:execute-api:us-east-1:*:a123456789/test/POST/mydemoresource/*"
                    String resource = Joiner.slash("*", endpointInfo.serviceName(), endpointInfo.httpMethod().name(), "*");
                    return Util.arn(stack, null, region, "execute-api", resource);
                }).collect(Collectors.toList());

        grantee.getGrantPrincipal().addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(List.of("execute-api:Invoke"))
                .resources(stageArns)
                .build());
    }

    // TODO: Compatibility method for certain APIs since not everything is standardised to a service interface yet.
    public static void addFullExecuteApi(Stack stack, IGrantable grantee) {
        String wildcardArnApisInAccountAndRegion = Util.arn(stack, null, null, "execute-api", "*");
        grantee.getGrantPrincipal().addToPrincipalPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("execute-api:Invoke"))
                .resources(List.of(wildcardArnApisInAccountAndRegion))
                .build());
    }

    public static void addManagedPoliciesToRole(IRole role, IManagedPolicy... policies) {
        Arrays.stream(policies).forEach(role::addManagedPolicy);
    }

    public static void addLambdaInvoke(Stack stack, IGrantable grantee, String... functionNames) {
        List<String> lambdaArns = Arrays.stream(functionNames)
                .map(name -> Joiner.colon("function", name, CommonConfig.LAMBDA_LIVE_ALIAS_NAME))
                .map(resource -> Util.arn(stack, null, null, "lambda", resource))
                .collect(Collectors.toList());

        grantee.getGrantPrincipal().addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(List.of("lambda:InvokeFunction"))
                .resources(lambdaArns)
                .build());
    }
}
