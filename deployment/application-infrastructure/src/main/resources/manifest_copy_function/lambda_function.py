# Currently unused function to implement env manifest copying for use as S3 source artifact
import boto3
from io import BytesIO
import os
import zipfile
import logging

def lambda_handler():

    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)

    account = boto3.client("sts").get_caller_identity()["Account"]
    region = os.environ["AWS_REGION"]
    target_bucket_name = f"serverbot2-pipeline-manifest-copy-{account}-{region}"
    logger.debug(f"Generated bucket name is {target_bucket_name}")

    ssm = boto3.client("ssm")
    manifest_string = ssm.get_parameter(Name="DeploymentEnvironmentManifest")["Parameter"]["Value"]
    # Avoid posting actual content to logs
    logger.debug(f"Got manifest string, length={len(manifest_string)}")

    logger.debug("Creating zip data...")
    zipped_data = BytesIO()
    zip = zipfile.ZipFile(zipped_data, "w")
    zip.writestr("manifest.json", manifest_string)
    zip.close()

    logger.debug("Pushing to S3...")
    s3 = boto3.client("s3")
    s3.put_object(
        Bucket=target_bucket_name,
        Key="manifest.zip",
        Body=zipped_data
    )
