import json
import requests
from aws_requests_auth.aws_auth import AWSRequestsAuth
from os import environ
from uuid import uuid4

error_page_template = """
<html>
  <head>
    <title>
      AdmiralBot Redirector
    </title>
  </head>
  <body>
    <p><h3>${message}</h3></p>
    <p>${detail}</p>
  </body>
</html>
"""

api_exceptions_to_messages = {
    "RequestValidationException": (400, "Sorry, this request is invalid."),
    "NoSuchResourceException": (400, "Sorry, the requested URL does not exist. Trying getting a new URL."),
    "ResourceExpiredException": (400, "Sorry, this URL has expired. Try getting a new URL."),
    "RequestHandlingException": (500, "Sorry, an error occurred while processing your request."),
    "RequestHandlingRuntimeException": (500, "Sorry, an unexpected error occurred while processing your request.")
}
api_exception_default_message = (500, "Sorry, an unexpected error occurred.")

gateway_exception_default_message = "Sorry, an unexpected gateway error occurred."

def lambda_handler(event, context):
    request = event["Records"][0]["cf"]["request"]

    path_segments = request["uri"].split("/") # CF team really should have called this "path", not "uri"

    if len(path_segments) <= 2:
        return build_error_response(400, "Bad Request", "missing request path segments")
    # Remove leading empty param (path always begins with a "/")
    path_segments.pop(0)

    # Get auth and endpoint params from env vars to contact URL shortener service
    auth = AWSRequestsAuth(
        aws_access_key=environ["AWS_ACCESS_KEY_ID"],
        aws_secret_access_key=environ["AWS_SECRET_ACCESS_KEY"],
        aws_token=environ["AWS_SESSION_TOKEN"],
        aws_service="execute-api",
        aws_host=environ["SB2_TARGET_HOST"],
        aws_region=environ["SB2_TARGET_REGION"]
    )
    json_string_body = json.dumps({
        "xApiTarget": "GetFullUrl",
        "xRequestId": str(uuid4()),
        "tokenVersion": int(path_segments[0]),
        "urlToken": path_segments[1]
    })
    url_response_http = requests.post(
        environ["SB2_TARGET_URL"],
        data=json_string_body,
        auth=auth
    )
    url_response = url_response_http.json()

    # Handle success case
    if "fullUrl" in url_response:
        return handle_redirect(url_response["fullUrl"])

    # Handle case where error comes from API Gateway directly
    if "message" in url_response:
        detail_message = f"{url_response_http.status_code}: {url_response['message']}"
        return build_error_response(500, gateway_exception_default_message, detail_message)

    # Handle standard API exceptions
    return handle_error(url_response)

def handle_redirect(full_url):
    return {
        "status": "302",
        "statusDescription": "Found",
        "headers": {
            "location": [{
                "key": "Location",
                "value": full_url
            }]
        }
    }

def handle_error(url_response):
    (code, message) = api_exceptions_to_messages.get(url_response["exceptionTypeName"], api_exception_default_message)
    detail = url_response["exceptionMessage"]
    return build_error_response(code, message, detail)

def build_error_response(code, message, detail):
    error_detail = f"(Error detail: {detail})"
    body = error_page_template.replace("${message}", message).replace("${detail}", error_detail)
    return {
        "status": code,
        "body": body
    }