package io.mamish.serverbot2.infra.deploy;

import com.google.gson.Gson;
import io.mamish.serverbot2.sharedconfig.DeployConfig;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.core.StageProps;
import software.amazon.awscdk.pipelines.AddStageOptions;
import software.amazon.awscdk.pipelines.CdkPipeline;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;

public class Main {

    public static void main(String[] args) {

        App app = new App();

        PipelineStack pipelineStack = new PipelineStack(app, "DeploymentPipelineStack", makeDefaultProps());
        CdkPipeline pipeline = pipelineStack.getPipeline();

        DeploymentManifest manifest = loadDeploymentManifest();
        for (ApplicationEnv env: manifest.getEnvironments()) {
            if (env.isEnabled()) {

                String stageId = env.getName() + "Deployment";
                Environment stageEnv = Environment.builder().account(env.getAccountId()).region(env.getRegion()).build();
                StageProps stageProps = StageProps.builder().env(stageEnv).build();
                AddStageOptions stageOptions = AddStageOptions.builder().manualApprovals(env.requiresApproval()).build();

                pipeline.addApplicationStage(new ApplicationStage(app, stageId, stageProps, env), stageOptions);

            }
        }

        app.synth();
    }

    private static StackProps makeDefaultProps() {
        Environment defaultEnv = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();
        return StackProps.builder().env(defaultEnv).build();
    }

    private static DeploymentManifest loadDeploymentManifest() {
        SsmClient ssmClient = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
        String manifestParamName = DeployConfig.ENVIRONMENT_MANIFEST_PARAM_NAME;
        String manifestString = ssmClient.getParameter(r -> r.name(manifestParamName)).parameter().value();
        return (new Gson()).fromJson(manifestString, DeploymentManifest.class);
    }

}
