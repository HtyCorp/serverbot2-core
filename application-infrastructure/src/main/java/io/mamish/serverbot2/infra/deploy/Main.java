package io.mamish.serverbot2.infra.deploy;

import software.amazon.awscdk.core.App;

public class Main {

    public static void main(String[] args) {
        App app = new App();
        PipelineStack pipelineStack = new PipelineStack(app, "DeploymentPipelineStack");

        // Insert application stages here


        app.synth();
    }

}
