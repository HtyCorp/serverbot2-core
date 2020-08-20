package io.mamish.serverbot2.infra.customresource;

import io.mamish.serverbot2.infra.util.Util;
import io.mamish.serverbot2.sharedutil.IDUtils;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.CustomResource;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.customresources.Provider;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.ssm.StringParameter;

import java.util.Map;

// Reference: https://github.com/aws/aws-cdk/blob/master/packages/%40aws-cdk/custom-resources/test/provider-framework/integration-test-fixtures/s3-file.ts

public class S3Artifact extends Construct {

    public S3Artifact(Construct scope, String id, S3ArtifactProps props) {
        super(scope, id);

        String targetKeyRandomSuffix = props.getArtifactKeyPrefix() + "-" + IDUtils.randomIdShort() + props.getArtifactKeySuffix();
        String targetS3Url = IDUtils.slash("s3:/", props.getTargetBucket().getBucketName(), targetKeyRandomSuffix);

        StringParameter urlParameter = StringParameter.Builder.create(this, "UrlParameter")
                .parameterName(props.getArtifactS3UrlParameterName())
                .stringValue(targetS3Url)
                .build();

        String providerCodePath = Util.codeBuildPath("application-infrastructure", "src", "main", "resources",
                "s3_artifact_resource_provider");
        Function providerFunction = Function.Builder.create(this, "Function")
                .runtime(Runtime.PYTHON_3_7)
                .handler("lambda_function.lambda_handler")
                .code(Code.fromAsset(providerCodePath))
                .timeout(Duration.seconds(60))
                .build();
        props.getSourceS3Asset().grantRead(providerFunction);
        props.getTargetBucket().grantWrite(providerFunction);
        Provider provider = Provider.Builder.create(this, "Provider")
                .onEventHandler(providerFunction)
                .build();

        Map<String,Object> propertyMap = Map.of(
                "SourceS3Bucket", props.getSourceS3Asset().getS3BucketName(),
                "SourceS3Key", props.getSourceS3Asset().getS3ObjectKey(),
                "TargetS3Bucket", props.getTargetBucket().getBucketName(),
                "TargetS3Key", targetKeyRandomSuffix
        );
        CustomResource resource = CustomResource.Builder.create(this, "Resource")
                .resourceType("Custom::S3Artifact")
                .serviceToken(provider.getServiceToken())
                .properties(propertyMap)
                .build();
    }

}
