package com.admiralbot.infra.deploy;

import com.admiralbot.sharedconfig.DeployConfig;
import com.admiralbot.sharedutil.Joiner;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.pipelines.CdkPipeline;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeStarConnectionsSourceAction;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.s3.Bucket;

import java.util.List;

public class PipelineStack extends Stack {

    private final CdkPipeline pipeline;

    public CdkPipeline getPipeline() {
        return pipeline;
    }

    public PipelineStack(Construct parent, String id, StackProps stackProps, String connectionUuid,
                         String sourceBranch, String pipelineName) {
        super(parent, id, stackProps);

        // Bucket for Maven dependency caching

        Bucket cacheBucket = Bucket.Builder.create(this, "MavenDependencyCacheBucket")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
        BucketCacheOptions cacheOptions = BucketCacheOptions.builder()
                .prefix("archives")
                .build();

        // CodePipeline and CodeBuild

        // Inputs:
        Artifact sourceArtifact = Artifact.artifact("github_source");
        // Artifact manifestArtifact = Artifact.artifact("environment_manifest");

        // Outputs:
        Artifact assemblyArtifact = Artifact.artifact("cloud_assembly");

        String connectionArn = Joiner.colon("arn", "aws", "codestar-connections",
                getRegion(), getAccount(), "connection/" + connectionUuid);
        CodeStarConnectionsSourceAction gitHubConnection = CodeStarConnectionsSourceAction.Builder.create()
                .actionName("GitHubSource")
                .output(sourceArtifact)
                .connectionArn(connectionArn)
                .owner(DeployConfig.GITHUB_DEPLOYMENT_SOURCE_OWNER)
                .repo(DeployConfig.GITHUB_DEPLOYMENT_MASTER_BRANCH)
                .branch(sourceBranch)
                .build();

        BuildEnvironment codeBuildBuildEnvironment = BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.STANDARD_4_0)
                .computeType(ComputeType.LARGE)
                .build();
        PipelineProject codeBuildProject = PipelineProject.Builder.create(this, "CodeBuildProject")
                .environment(codeBuildBuildEnvironment)
                .cache(Cache.bucket(cacheBucket, cacheOptions))
                .build();
        // Allow CodeBuild project to fetch required SSM parameters
        // Note: This should probably be done via the buildspec env configuration instead
        codeBuildProject.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("ssm:GetParameter"))
                .resources(List.of(
                        "arn:aws:ssm:*:*:parameter/"+DeployConfig.DEPLOYMENT_MANIFEST_PARAM_NAME,
                        "arn:aws:ssm:*:*:parameter/"+DeployConfig.DEV_ENVIRONMENT_PARAM_NAME
                )).build());
        // Enable SSM Session Manager connections for CodeBuild project
        codeBuildProject.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "ssmmessages:CreateControlChannel",
                        "ssmmessages:CreateDataChannel",
                        "ssmmessages:OpenControlChannel",
                        "ssmmessages:OpenDataChannel"
                )).resources(List.of("*"))
                .build());

        CodeBuildAction codeBuildAction = CodeBuildAction.Builder.create()
                .project(codeBuildProject)
                .actionName("BuildAndSynthProject")
                .input(sourceArtifact)
                .outputs(List.of(assemblyArtifact))
                .build();

        pipeline = CdkPipeline.Builder.create(this, "DeploymentPipeline")
                .pipelineName(pipelineName)
                .sourceAction(gitHubConnection)
                .synthAction(codeBuildAction)
                .cloudAssemblyArtifact(assemblyArtifact)
                .build();
    }

}
