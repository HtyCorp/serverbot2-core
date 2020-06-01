package io.mamish.serverbot2.infra.deploy;

import io.mamish.serverbot2.infra.app.ServerbotFullApp;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DeploymentConfig;
import software.amazon.awscdk.appdelivery.PipelineDeployStackAction;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.IAction;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.S3DeployAction;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.ssm.StringParameter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DeploymentStack extends Stack {

    public DeploymentStack(ServerbotFullApp app, String id) {
        super(app.cdkApp, id, app.coreStackProps);

        // Top-level pipeline definition to add stages to.

        Bucket artifactBucket = Bucket.Builder.create(this, "ArtifactBucket")
                .build();

        Pipeline pipeline = Pipeline.Builder.create(this, "Pipeline")
                .artifactBucket(artifactBucket)
                .restartExecutionOnUpdate(true)
                .build();

        // GitHub action with source output artifact

        Artifact sourceOutput = Artifact.artifact("source_output");

        GitHubSourceAction gitHubSourceAction = GitHubSourceAction.Builder.create()
                .actionName("GitHubTestReleaseBranch")
                .branch(DeploymentConfig.GITHUB_DEPLOYMENT_SOURCE_BRANCH)
                .owner(DeploymentConfig.GITHUB_DEPLOYMENT_SOURCE_OWNER)
                .repo(DeploymentConfig.GITHUB_DEPLOYMENT_SOURCE_REPO)
                .oauthToken(SecretValue.secretsManager(DeploymentConfig.GITHUB_OAUTH_TOKEN_SECRET_NAME))
                .output(sourceOutput)
                .build();
        pipeline.addStage(StageOptions.builder()
                .stageName("TestSource")
                .actions(List.of(gitHubSourceAction))
                .build());

        // CodeBuild project with JAR and CDK assembly output artifacts

        Artifact s3JarFiles = Artifact.artifact("s3_jar_files");
        Artifact synthAppInfra = Artifact.artifact("cdk_assembly");

        // Should remove this once we have a clearer test methodology where unit tests need no service access
        Role codeBuildRole = Role.Builder.create(this, "CodeBuildAdminRole")
                .assumedBy(new ServicePrincipal("codebuild.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("AdministratorAccess")))
                .build();
        BuildEnvironment codeBuildBuildEnvironment = BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.STANDARD_4_0)
                .computeType(ComputeType.MEDIUM)
                .build();
        PipelineProject codeBuildProject = PipelineProject.Builder.create(this, "CodeBuildProject")
                .environment(codeBuildBuildEnvironment)
                .role(codeBuildRole)
                .build();

        CodeBuildAction codeBuildAction = CodeBuildAction.Builder.create()
                .project(codeBuildProject)
                .actionName("PackageJarsAndSynthCDK")
                .input(sourceOutput)
                .outputs(List.of(s3JarFiles, synthAppInfra))
                .build();
        pipeline.addStage(StageOptions.builder()
                .stageName("BuildAll")
                .actions(List.of(codeBuildAction))
                .build());

        // CDK self-update/deploy action for this pipeline

        addStackDeployStage(pipeline, synthAppInfra, "SelfUpdateDeploymentPipeline",
                this);

        // S3 deploy stage for app daemon (deployed to S3 rather than by any CDK mechanism)

        Bucket binaryArtifactBucket = Bucket.Builder.create(this, "BinaryArtifactBucket")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        StringParameter bucketNameParamInstance = StringParameter.Builder.create(this, "BucketNameParam")
                .parameterName(CommonConfig.S3_DEPLOYED_ARTIFACTS_BUCKET.getName())
                .stringValue(binaryArtifactBucket.getBucketName())
                .build();

        S3DeployAction artifactDeployAction = S3DeployAction.Builder.create()
                .input(s3JarFiles)
                .actionName("DeployJarsToS3")
                .bucket(binaryArtifactBucket)
                .build();
        pipeline.addStage(StageOptions.builder()
                .stageName("S3Deployment")
                .actions(List.of(artifactDeployAction))
                .build());

        // CDK stack deployments

        // Stage 1 app deploy: common infrastructure only
        addStackDeployStage(pipeline, synthAppInfra, "DeployCommonInfra",
                app.commonStack);

        // Stage 2 app deploy: passive services
        addStackDeployStage(pipeline, synthAppInfra, "DeployPassiveServices",
                app.appInstanceShareStack,
                app.gameMetadataStack,
                app.netSecStack,
                app.reaperStack);

        // Stage 3: worker services depending on passive services
        addStackDeployStage(pipeline, synthAppInfra, "DeployWorkerServices",
                app.ipAuthorizerStack,
                app.workflowsStack);

        // Stage 4: command service depending on all other services
        addStackDeployStage(pipeline, synthAppInfra, "DeployCommandService",
                app.commandStack);

        // Stage 5: Discord delay depending on command service
        addStackDeployStage(pipeline, synthAppInfra, "DeployDiscordRelay",
                app.relayStack);

    }

    private void addStackDeployStage(Pipeline pipeline, Artifact synthArtifact, String stageName, Stack... targetStacks) {
        List<IAction> deployActions = Arrays.stream(targetStacks).map(stack -> PipelineDeployStackAction.Builder.create()
                .input(synthArtifact)
                .stack(stack)
                .adminPermissions(true)
                .build()
        ).collect(Collectors.toList());

        pipeline.addStage(StageOptions.builder()
                .stageName(stageName)
                .actions(deployActions)
                .build());
    }

}
