package io.mamish.serverbot2.infra.deploy;

import io.mamish.serverbot2.sharedconfig.DeployConfig;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.pipelines.CdkPipeline;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.List;

public class PipelineStack extends Stack {

    public PipelineStack(Construct parent, String id) {
        super(parent, id);

        Artifact sourceArtifact = Artifact.artifact("github_source");
        Artifact appDaemonJarArtifact = Artifact.artifact("app_daemon_jar");
        Artifact assemblyArtifact = Artifact.artifact("cloud_assembly");

        GitHubSourceAction gitHubSource = GitHubSourceAction.Builder.create()
                .actionName("GitHubRepoSource")
                .output(sourceArtifact)
                .oauthToken(SecretValue.secretsManager(DeployConfig.GITHUB_OAUTH_TOKEN_SECRET_NAME))
                .owner(DeployConfig.GITHUB_DEPLOYMENT_SOURCE_OWNER)
                .repo(DeployConfig.GITHUB_DEPLOYMENT_SOURCE_REPO)
                .branch(DeployConfig.GITHUB_DEPLOYMENT_SOURCE_BRANCH)
                .build();

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
                .actionName("BuildAndSynth")
                .input(sourceArtifact)
                .outputs(List.of(appDaemonJarArtifact, assemblyArtifact))
                .build();

        CdkPipeline pipeline = CdkPipeline.Builder.create(this, "DeploymentPipeline")
                .pipelineName("CDKDeploymentPipeline")
                .sourceAction(gitHubSource)
                .synthAction(codeBuildAction)
                .cloudAssemblyArtifact(assemblyArtifact)
                .build();

    }

}
