package io.mamish.serverbot2.deployinfra;

import software.amazon.awscdk.core.App;

public class DeploymentApp {
    public static void main() {
        App app = new App();
        new DeploymentStack(app, "DeploymentStack");
        app.synth();
    }
}
