package com.admiralbot.infra;

import com.admiralbot.infra.deploy.*;
import com.admiralbot.infra.util.Util;
import com.admiralbot.sharedconfig.DeployConfig;
import com.admiralbot.sharedconfig.Parameter;
import com.admiralbot.sharedutil.AppContext;
import com.admiralbot.sharedutil.Pair;
import com.admiralbot.sharedutil.XrayUtils;
import com.google.gson.Gson;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.pipelines.CdkPipeline;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        XrayUtils.setInstrumentationEnabled(false);
        AppContext.setDefault();

        Gson gson = new Gson();

        String devEnvironmentJson;
        try {

            Parameter parameter = new Parameter(DeployConfig.DEV_ENVIRONMENT_PARAM_NAME);
            devEnvironmentJson = parameter.getValue();
            ApplicationEnv devEnvironment = gson.fromJson(devEnvironmentJson, ApplicationEnv.class);

            synthesizeDevPipeline(devEnvironment);

        } catch (ParameterNotFoundException e1) {
            System.err.println("No dev environment definition found, trying deployment manifest instead...");

            String deploymentManifestJson;
            try {

                Parameter parameter = new Parameter(DeployConfig.DEPLOYMENT_MANIFEST_PARAM_NAME);
                deploymentManifestJson = parameter.getValue();

                DeploymentManifest deploymentManifest = gson.fromJson(deploymentManifestJson, DeploymentManifest.class);
                synthesizeFullPipeline(deploymentManifest);

            } catch (ParameterNotFoundException e2) {
                throw new RuntimeException("Could not locate any environment definitions from SSM parameters", e2);
            }

        }

    }

    private static void synthesizeDevPipeline(ApplicationEnv appEnv) {

        if (!appEnv.isEnabled()) {
            throw new IllegalStateException("Dev environment is set to disabled.");
        } else if (appEnv.requiresApproval()) {
            throw new IllegalStateException("Dev environment requires approval (this is unsupported).");
        }

        App app = makeAppWithStandardAzIds(List.of(appEnv));

        // Build dev CDK pipeline.
        PipelineStack pipelineStack = new PipelineStack(app, "DeploymentPipelineStack", makeDefaultProps(),
                "ecs-api-migration", "DevEnvironmentPipeline");
        CdkPipeline pipeline = pipelineStack.getPipeline();

        Environment cdkEnv = Environment.builder().account(appEnv.getAccountId()).region(appEnv.getRegion()).build();
        StageProps stageProps = StageProps.builder().env(cdkEnv).build();

        pipeline.addApplicationStage(new ApplicationRegionalStage(app, appEnv.getName(), stageProps, appEnv));

        app.synth();
    }

    private static void synthesizeFullPipeline(DeploymentManifest deploymentManifest) {
        // Load environment manifest and create root app with extra generated context.
        App app = makeAppWithStandardAzIds(deploymentManifest.getEnvironments());

        // Build central CDK pipeline.
        PipelineStack pipelineStack = new PipelineStack(app, "DeploymentPipelineStack", makeDefaultProps(),
                DeployConfig.GITHUB_DEPLOYMENT_SOURCE_BRANCH, "CDKDeploymentPipeline");
        CdkPipeline pipeline = pipelineStack.getPipeline();

        // Add an application stage for every enabled environment in manifest.
        for (ApplicationEnv env: deploymentManifest.getEnvironments()) {
            if (env.isEnabled()) {
                String stageRegionalId = env.getName() + "Deployment";
                Environment stageRegionalEnv = Environment.builder().account(env.getAccountId()).region(env.getRegion()).build();
                StageProps stageRegionalProps = StageProps.builder().env(stageRegionalEnv).build();

                String stageGlobalId = env.getName() + "Global" + makeShortRegionName(env.getRegion());
                Environment stageGlobalEnv = Environment.builder().account(env.getAccountId()).region("us-east-1").build();
                StageProps stageGlobalProps = StageProps.builder().env(stageGlobalEnv).build();

                if (env.requiresApproval()) {
                    // Not a fan of the built-in AddStageOptions approval setup (adds multiple approvals per stage),
                    // so just add a new stage with a single approval.
                    pipeline.addStage(env.getName()+"Approval").addManualApprovalAction();
                }
                pipeline.addApplicationStage(new ApplicationGlobalStage(app, stageGlobalId, stageGlobalProps, env));
                pipeline.addApplicationStage(new ApplicationRegionalStage(app, stageRegionalId, stageRegionalProps, env));
            }
        }

        app.synth();
    }

    private static String makeShortRegionName(String regionName) {
        Matcher m = Pattern.compile("(?<area>[a-z]{2})-(?<dir>[a-z]+)-(?<index>[1-9])").matcher(regionName);
        if (!m.matches()) {
            throw new IllegalArgumentException("Bad region name: " + regionName);
        }

        String shortLowercase = m.group("area").substring(0,2) + m.group("dir").substring(0) + m.group("index");
        return shortLowercase.toUpperCase();
    }

    private static StackProps makeDefaultProps() {
        Environment defaultEnv = Environment.builder()
                .account(Util.defaultAccount())
                .region(Util.defaultRegion())
                .build();
        return StackProps.builder().env(defaultEnv).build();
    }

    private static App makeAppWithStandardAzIds(List<ApplicationEnv> environments) {
        // Pre-populate AZ IDs: pipelines can't get them from context lookups, and they must be added programmatically
        // before any nodes/constructs are added (which actually occurs during App instance construction).
        // Obviously this will fail for regions with less than 3 AZs.
        // Format ref: https://docs.aws.amazon.com/cdk/latest/guide/context.html#context_viewing
        Map<String,Object> envZoneContextMap = environments.stream().map(env -> {
            String account = env.getAccountId();
            String region = env.getRegion();
            String envZonesContextKey = String.format("availability-zones:account=%s:region=%s", account, region);
            List<String> envZoneIdsList = List.of(region+"a", region+"b", region+"c");
            return new Pair<>(envZonesContextKey, envZoneIdsList);
        }).collect(Collectors.toMap(Pair::a,Pair::b));

        return new App(AppProps.builder().context(envZoneContextMap).build());
    }

}
