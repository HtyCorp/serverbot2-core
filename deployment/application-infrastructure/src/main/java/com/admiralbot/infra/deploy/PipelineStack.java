package com.admiralbot.infra.deploy;

import com.admiralbot.sharedconfig.DeployConfig;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.pipelines.CdkPipeline;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.s3.Bucket;

import java.util.List;

public class PipelineStack extends Stack {

    private final CdkPipeline pipeline;

    public CdkPipeline getPipeline() {
        return pipeline;
    }

    public PipelineStack(Construct parent, String id, StackProps stackProps, String sourceBranch, String pipelineName) {
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

        GitHubSourceAction gitHubSource = GitHubSourceAction.Builder.create()
                .actionName("PullGitHubSource")
                .output(sourceArtifact)
                .oauthToken(SecretValue.secretsManager(DeployConfig.GITHUB_OAUTH_TOKEN_SECRET_NAME))
                .owner(DeployConfig.GITHUB_DEPLOYMENT_SOURCE_OWNER)
                .repo(DeployConfig.GITHUB_DEPLOYMENT_SOURCE_REPO)
                .branch(sourceBranch)
                .build();

        // Shelving this until I can figure out if pipelines lib can allow multiple source artifacts
//        String generatedBucketName = IDUtils.kebab("serverbot2-pipeline-manifest-copy",
//                Util.defaultAccount(), Util.defaultRegion());
//        S3SourceAction s3ManifestSource = S3SourceAction.Builder.create()
//                .actionName("PullS3ManifestFile")
//                .bucket(Bucket.fromBucketName(this, "ManifestCopyBucket", generatedBucketName))
//                .bucketKey("manifest.zip")
//                .output(manifestArtifact)
//                .build();

        BuildEnvironment codeBuildBuildEnvironment = BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.STANDARD_4_0)
                .computeType(ComputeType.LARGE)
                .build();
        PipelineProject codeBuildProject = PipelineProject.Builder.create(this, "CodeBuildProject")
                .environment(codeBuildBuildEnvironment)
                .cache(Cache.bucket(cacheBucket, cacheOptions))
                .build();
        codeBuildProject.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("ssm:GetParameter"))
                .resources(List.of(
                        "arn:aws:ssm:*:*:parameter/"+DeployConfig.DEPLOYMENT_MANIFEST_PARAM_NAME,
                        "arn:aws:ssm:*:*:parameter/"+DeployConfig.DEV_ENVIRONMENT_PARAM_NAME
                )).build());

        CodeBuildAction codeBuildAction = CodeBuildAction.Builder.create()
                .project(codeBuildProject)
                .actionName("BuildAndSynthProject")
                .input(sourceArtifact)
                .outputs(List.of(assemblyArtifact))
                .build();

        pipeline = CdkPipeline.Builder.create(this, "DeploymentPipeline")
                .pipelineName(pipelineName)
                .sourceAction(gitHubSource)
                .synthAction(codeBuildAction)
                .cloudAssemblyArtifact(assemblyArtifact)
                .build();

    }

}
