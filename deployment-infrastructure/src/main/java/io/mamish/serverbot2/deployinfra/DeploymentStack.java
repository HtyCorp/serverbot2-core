package io.mamish.serverbot2.deployinfra;

import io.mamish.serverbot2.sharedconfig.DeploymentConfig;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.Cache;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.IStage;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.Action;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awssdk.services.secretsmanager.model.DeleteResourcePolicyRequest;

import java.util.List;
import java.util.Map;

public class DeploymentStack extends Stack {
    public DeploymentStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public DeploymentStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Bucket artifactBucket = Bucket.Builder.create(this, "ArtifactBucket")
                .bucketName(DeploymentConfig.ARTIFACT_BUCKET_NAME)
                .build();


        Artifact mainArtifact = Artifact.artifact("ServerbotApp");

        GitHubSourceAction gitHubSourceAction = GitHubSourceAction.Builder.create()
                .actionName("GitHubTestReleaseBranch")
                .branch(DeploymentConfig.GITHUB_DEPLOYMENT_SOURCE_BRANCH)
                .owner(DeploymentConfig.GITHUB_DEPLOYMENT_SOURCE_OWNER)
                .repo(DeploymentConfig.GITHUB_DEPLOYMENT_SOURCE_REPO)
                .oauthToken(SecretValue.secretsManager(DeploymentConfig.GITHUB_OAUTH_TOKEN_SECRET_NAME))
                .output(mainArtifact)
                .build();
        StageProps githubSourceStage = StageProps.builder()
                .stageName("GitHubTestReleaseBranch")
                .actions(List.of(gitHubSourceAction))
                .build();

        Project codeBuildProject = Project.Builder.create(this, "CodeBuildProject")
                .

        CodeBuildAction codeBuildAction = CodeBuildAction.Builder.create()
                .actionName("")


        List<StageProps> stages = List.of(
                githubSourceStage
        );

        Pipeline pipeline = Pipeline.Builder.create(this, "Pipeline")
                .artifactBucket(artifactBucket)
                .restartExecutionOnUpdate(true)
                .role(pipelineRole())
                .stages()
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

    private Project codeBuildProject() {
        Project project = Project.Builder.create(this, "CodeBuildProject")
                .cache(Cache.none()) //TODO: S3 caching to save time grabbing Maven dependencies
                .buildSpec(codeBuildBuildSpec())
                .build();
    }

    private BuildSpec codeBuildBuildSpec() {

//        Map<String,Object> envVars = Map.of();
//        Map<String,Object> phases = Map.of();

        return BuildSpec.fromObject(Map.of(
                "version", "0.2"
                //TODO
        ));
    }

}
