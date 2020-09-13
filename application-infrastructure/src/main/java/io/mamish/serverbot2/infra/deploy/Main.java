package io.mamish.serverbot2.infra.deploy;

import com.google.gson.Gson;
import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedconfig.DeployConfig;
import io.mamish.serverbot2.sharedutil.Pair;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.pipelines.CdkPipeline;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        // Load environment manifest and create root app with extra generated context.
        DeploymentManifest manifest = loadDeploymentManifest();
        App app = makeAppWithStandardAzIds(manifest);

        // Build central CDK pipeline.
        PipelineStack pipelineStack = new PipelineStack(app, "DeploymentPipelineStack", makeDefaultProps());
        CdkPipeline pipeline = pipelineStack.getPipeline();

        // Add an application stage for every enabled environment in manifest.
        for (ApplicationEnv env: manifest.getEnvironments()) {
            if (env.isEnabled()) {
                String stageId = env.getName() + "Deployment";
                Environment stageEnv = Environment.builder().account(env.getAccountId()).region(env.getRegion()).build();
                StageProps stageProps = StageProps.builder().env(stageEnv).build();

                if (env.requiresApproval()) {
                    // Not a fan of the built-in AddStageOptions approval setup (adds multiple approvals per stage),
                    // so just add a new stage with a single approval.
                    pipeline.addStage(env.getName()+"Approval").addManualApprovalAction();
                }
                pipeline.addApplicationStage(new ApplicationStage(app, stageId, stageProps, env));
            }
        }

        app.synth();
    }

    private static StackProps makeDefaultProps() {
        Environment defaultEnv = Environment.builder()
                .account(Util.defaultAccount())
                .region(Util.defaultRegion())
                .build();
        return StackProps.builder().env(defaultEnv).build();
    }

    private static DeploymentManifest loadDeploymentManifest() {
        SsmClient ssmClient = SsmClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
        String manifestParamName = DeployConfig.ENVIRONMENT_MANIFEST_PARAM_NAME;
        String manifestString = ssmClient.getParameter(r -> r.name(manifestParamName)).parameter().value();
        return (new Gson()).fromJson(manifestString, DeploymentManifest.class);
    }

    private static App makeAppWithStandardAzIds(DeploymentManifest manifest) {
        // Pre-populate AZ IDs: pipelines can't get them from context lookups, and they must be added programmatically
        // before any nodes/constructs are added (which actually occurs during App instance construction).
        // Obviously this will fail for regions with less than 3 AZs.
        // Format ref: https://docs.aws.amazon.com/cdk/latest/guide/context.html#context_viewing
        Map<String,Object> envZoneContextMap = manifest.getEnvironments().stream().map(env -> {
            String account = env.getAccountId();
            String region = env.getRegion();
            String envZonesContextKey = String.format("availability-zones:account=%s:region=%s", account, region);
            List<String> envZoneIdsList = List.of(region+"a", region+"b", region+"c");
            return new Pair<>(envZonesContextKey, envZoneIdsList);
        }).collect(Collectors.toMap(Pair::a,Pair::b));

        return new App(AppProps.builder().context(envZoneContextMap).build());
    }

}
