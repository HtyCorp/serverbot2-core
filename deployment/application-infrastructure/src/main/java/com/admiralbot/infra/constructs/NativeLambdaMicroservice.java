package com.admiralbot.infra.constructs;

import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.infra.util.Util;
import com.admiralbot.sharedconfig.CommonConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.IGrantable;
import software.amazon.awscdk.services.iam.IPrincipal;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.IFunction;
import software.amazon.awscdk.services.lambda.Runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NativeLambdaMicroservice extends Construct implements IGrantable {

    private final Function function;

    public NativeLambdaMicroservice(Stack parent, String id, ApplicationRegionalStage appStage, String serviceModule) {
        super(parent, id);

        Code nativeZipCode = null;
        try {
            Path buildDir = Util.codeBuildPath("services", serviceModule, "target");
            Path zipDir = Files.createDirectories(buildDir.resolve("lambdazip"));
            Files.copy(buildDir.resolve("service-native-image"), zipDir.resolve("bootstrap"),
                    StandardCopyOption.REPLACE_EXISTING);
            nativeZipCode = Code.fromAsset(zipDir.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy native artifact for " + serviceModule, e);
        }

        function = Function.Builder.create(this, "Function")
                .functionName("native-" + serviceModule)
                .code(nativeZipCode)
                .runtime(Runtime.PROVIDED_AL2)
                .memorySize(4096)
                .timeout(Duration.seconds(15))
                .handler("default") // not applicable to native Lambda right now
                .build();

        Permissions.addConfigPathRead(parent, function, CommonConfig.PATH);
    }

    public IFunction getFunction() {
        return function;
    }

    @Override
    public IPrincipal getGrantPrincipal() {
        return function.getGrantPrincipal();
    }

    public IRole getRole() {
        return function.getRole();
    }
}
