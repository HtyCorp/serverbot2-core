package io.mamish.serverbot2.appinfra;

import software.amazon.awscdk.services.lambda.Code;

public class Util {

    static Code mavenJarAsset(String module) {
        String rootPath = System.getenv("CODEBUILD_SRC_DIR");
        String jarPath = String.join("/", rootPath, module, "target", (module+".jar"));
        return Code.fromAsset(jarPath);
    }

}
