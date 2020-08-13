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

                String account = env.getAccountId();
                String region = env.getRegion();

                // Inject typical 3-AZ zone IDs since pipelines don't support context lookups to get them dynamically.
                // Obviously this will fail for regions with less than 3 AZs.
                // Format ref: https://docs.aws.amazon.com/cdk/latest/guide/context.html#context_viewing
                String envZonesKey = String.format("availability-zones:account=%s:region=%s", account, region);
                String envZonesList = String.format("[ \"%sa\", \"%sb\", \"%sc\" ]", region, region, region);
                app.getNode().setContext(envZonesKey, envZonesList);

                // Add a new application stage for this environment.
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
