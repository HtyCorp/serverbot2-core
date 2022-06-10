package com.admiralbot.infra.constructs;

import com.admiralbot.infra.deploy.ApplicationRegionalStage;
import com.admiralbot.infra.util.ManagedPolicies;
import com.admiralbot.infra.util.Permissions;
import com.admiralbot.infra.util.Util;
import com.admiralbot.sharedconfig.CommonConfig;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.IGrantable;
import software.amazon.awscdk.services.iam.IPrincipal;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;

import java.nio.file.Path;
import java.util.Objects;

public class NativeLambdaMicroservice extends Construct implements IGrantable {

    private final Function function;

    public NativeLambdaMicroservice(Stack parent, String id, ApplicationRegionalStage appStage, String serviceModule) {
        super(parent, id);

        // Plugin configuration for native-maven-image sets these artifacts up for us in the right place
        Path nativeZipDir = Util.codeBuildPath("services", serviceModule, "target", "deployzip");
        Code nativeZipCode = Code.fromAsset(nativeZipDir.toAbsolutePath().toString());

        function = Function.Builder.create(this, "Function")
                .functionName("native-" + serviceModule)
                .code(nativeZipCode)
                .runtime(Runtime.PROVIDED_AL2)
                .memorySize(3008)
                .timeout(Duration.seconds(15))
                .handler("default") // Not applicable to native Lambda right now
                .tracing(Tracing.PASS_THROUGH) // Should be set to ACTIVE once HTTP APIs support Xray integration
                .build();

        Permissions.addConfigPathRead(parent, function, CommonConfig.PATH);
        Permissions.addManagedPoliciesToRole(Objects.requireNonNull(function.getRole()),
                ManagedPolicies.XRAY_DAEMON_WRITE_ACCESS);
    }

    public IFunction getFunction() {
        return function;
    }

    @Override
    public @NotNull IPrincipal getGrantPrincipal() {
        return function.getGrantPrincipal();
    }

    public IRole getRole() {
        return function.getRole();
    }
}
