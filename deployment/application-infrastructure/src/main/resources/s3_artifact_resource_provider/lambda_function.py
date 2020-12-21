import boto3

def lambda_handler(event, context):
    request_type = event["RequestType"]
    if request_type in ["Create", "Update"]:
        return copy_object(event)
    elif request_type in ["Delete"]:
        return delete_object(event)
    else:
        raise Exception("Unexpected request type: " + request_type)

def copy_object(event):
    s3 = boto3.client("s3")
    props = event["ResourceProperties"]
    s3.copy_object(
        CopySource = {
            "Bucket": props["SourceS3Bucket"],
            "Key": props["SourceS3Key"]
        },
        Bucket = props["TargetS3Bucket"],
        Key = props["TargetS3Key"]
    )

    physical_id = props["TargetS3Bucket"] + "/" + props["TargetS3Key"]
    return {
        "PhysicalResourceId": physical_id,
        "Data": {}
    }

def delete_object(event):
    s3 = boto3.client("s3")
    props = event["ResourceProperties"]
    s3.delete_object(
        Bucket = props["TargetS3Bucket"],
        Key = props["TargetS3Key"]
    )