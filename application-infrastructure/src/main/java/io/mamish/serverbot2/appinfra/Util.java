package io.mamish.serverbot2.appinfra;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

public class Util {

    static Function.Builder standardJavaFunction(Construct parent, String id, String moduleName, String handler) {
        return Function.Builder.create(parent, id)
                .runtime(Runtime.JAVA_11)
                .code(mavenJarAsset(moduleName))
                .handler(handler)
                .memorySize(CommonConfig.STANDARD_LAMBDA_MEMORY);
    }

    static Code mavenJarAsset(String module) {
        String rootPath = System.getenv("CODEBUILD_SRC_DIR");
        String jarPath = String.join("/", rootPath, module, "target", (module+".jar"));
        return Code.fromAsset(jarPath);
    }

}
