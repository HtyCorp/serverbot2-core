package io.mamish.serverbot2.deployinfra;

import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedconfig.DeploymentConfig;
import software.amazon.awscdk.appdelivery.PipelineDeployStackAction;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
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

import java.util.List;

public class DeploymentStack extends Stack {
    public DeploymentStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public DeploymentStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

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
        Artifact synthDeploymentInfra = Artifact.artifact("cdk_deploy_assembly");
        Artifact synthAppInfra = Artifact.artifact("cdk_app_assembly");

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
                .outputs(List.of(s3JarFiles, synthDeploymentInfra, synthAppInfra))
                .build();
        pipeline.addStage(StageOptions.builder()
                .stageName("BuildAll")
                .actions(List.of(codeBuildAction))
                .build());

        // CDK self-update/deploy action for this pipeline

        PipelineDeployStackAction updateSelfAction = PipelineDeployStackAction.Builder.create()
                .stack(this)
                .input(synthDeploymentInfra)
                .adminPermissions(true)
                .build();
        pipeline.addStage(StageOptions.builder()
                .stageName("SelfUpdateDeploymentInfra")
                .actions(List.of(updateSelfAction))
                .build());

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

        // CDK deploy action for application assembly

        PipelineDeployStackAction updateApplicationAction = PipelineDeployStackAction.Builder.create()
                .stack(this)
                .input(synthAppInfra)
                .adminPermissions(true)
                .build();
        pipeline.addStage(StageOptions.builder()
                .stageName("UpdateApplicationInfra")
                .actions(List.of(updateApplicationAction))
                .build());

    }

}
