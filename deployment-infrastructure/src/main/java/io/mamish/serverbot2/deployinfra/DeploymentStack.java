package io.mamish.serverbot2.deployinfra;

import io.mamish.serverbot2.sharedconfig.DeploymentConfig;
import software.amazon.awscdk.appdelivery.PipelineDeployStackAction;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigateway.Stage;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.*;
import software.amazon.awscdk.services.codepipeline.actions.Action;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.Bucket;

import java.util.List;

public class DeploymentStack extends Stack {
    public DeploymentStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public DeploymentStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Bucket artifactBucket = Bucket.Builder.create(this, "ArtifactBucket")
                .bucketName(DeploymentConfig.ARTIFACT_BUCKET_NAME)
                .build();

        Artifact sourceOutput = Artifact.artifact("source-output");
        Artifact jarFiles = Artifact.artifact("jar-files");
        Artifact synthDeploymentInfra = Artifact.artifact("synth-deployment-infra");
        Artifact synthAppInfra = Artifact.artifact("synth-app-infra");
        
        GitHubSourceAction gitHubSourceAction = GitHubSourceAction.Builder.create()
                .actionName("GitHubTestReleaseBranch")
                .branch(DeploymentConfig.GITHUB_DEPLOYMENT_SOURCE_BRANCH)
                .owner(DeploymentConfig.GITHUB_DEPLOYMENT_SOURCE_OWNER)
                .repo(DeploymentConfig.GITHUB_DEPLOYMENT_SOURCE_REPO)
                .oauthToken(SecretValue.secretsManager(DeploymentConfig.GITHUB_OAUTH_TOKEN_SECRET_NAME))
                .output(sourceOutput)
                .build();
        StageProps githubSourceStage = StageProps.builder()
                .stageName("TestSource")
                .actions(List.of(gitHubSourceAction))
                .build();

        BuildEnvironment codeBuildBuildEnvironment = BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.STANDARD_4_0)
                .computeType(ComputeType.MEDIUM)
                .build();
        PipelineProject codeBuildProject = PipelineProject.Builder.create(this, "CodeBuildProject")
                .environment(codeBuildBuildEnvironment)
                .build();
        CodeBuildAction codeBuildAction = CodeBuildAction.Builder.create()
                .project(codeBuildProject)
                .actionName("BuildJarsAndCDKSynth")
                .input(sourceOutput)
                .outputs(List.of(jarFiles, synthDeploymentInfra, synthAppInfra))
                .build();
        StageProps codeBuildStage = StageProps.builder()
                .stageName("BuildAll")
                .actions(List.of(codeBuildAction))
                .build();

        PipelineDeployStackAction updateSelfAction = PipelineDeployStackAction.Builder.create()
                .stack(this)
                .input(synthDeploymentInfra)
                .adminPermissions(true)
                .build();
        StageProps updateSelfStage = StageProps.builder()
                .stageName("SelfUpdateDeploymentInfra")
                .actions(List.of(updateSelfAction))
                .build();

        PipelineDeployStackAction updateApplicationAction = PipelineDeployStackAction.Builder.create()
                .stack(this)
                .input(synthAppInfra)
                .adminPermissions(true)
                .build();
        StageProps updateApplicationStage = StageProps.builder()
                .stageName("UpdateApplicationInfra")
                .actions(List.of(updateApplicationAction))
                .build();

        Pipeline pipeline = Pipeline.Builder.create(this, "Pipeline")
                .artifactBucket(artifactBucket)
                .restartExecutionOnUpdate(true)
                .stages(List.of(githubSourceStage,codeBuildStage,updateSelfStage))
                .build();

    }

    private Role pipelineRole() {
        List<IManagedPolicy> pipelineRolePolicies = List.of(
                ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess"),
                ManagedPolicy.fromAwsManagedPolicyName("AWSLambdaFullAccess"),
                ManagedPolicy.fromAwsManagedPolicyName("AWSCodePipelineFullAccess")
        );

        return Role.Builder.create(this, "PipelineRole")
                .assumedBy(new ServicePrincipal("codepipeline.amazonaws.com"))
                .managedPolicies(pipelineRolePolicies)
                .build();
    }

}
