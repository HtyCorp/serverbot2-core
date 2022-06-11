import json
import requests
import logging
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
uncaught_exception_default_message = (500, "Sorry an unexpected error occurred.", "Lambda uncaught")

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, _context):
    try:
        return handle_event(event, _context)
    except:
        logger.exception("Unexpected error running Lambda event handler")
        (code, message, detail) = uncaught_exception_default_message
        return build_error_response(code, message, detail)

def handle_event(event, _context):
    request = event["Records"][0]["cf"]["request"]
    logger.info(f"Request:\n{json.dumps(request)}")

    path_segments = request["uri"].split("/") # CF team really should have called this "path", not "uri"
    if len(path_segments) <= 2:
        return build_error_response(400, "Bad Request", "missing request path segments")
    # Remove leading empty param (path always begins with a "/")
    path_segments.pop(0)
    logger.info(f"Parsed path segments: {path_segments}")

    # Get auth and endpoint params from env vars to contact URL shortener service

    access_key_id = environ["AWS_ACCESS_KEY_ID"]
    secret_access_key = environ["AWS_SECRET_ACCESS_KEY"]
    session_token = environ["AWS_SESSION_TOKEN"]

    custom_headers = request["origin"]["s3"]["customHeaders"]
    url_service_region = custom_headers["x-admiral-env-target-region"][0]["value"]
    url_service_host = custom_headers["x-admiral-env-target-host"][0]["value"]
    url_service_url = custom_headers["x-admiral-env-target-url"][0]["value"]
    client_request_id = str(uuid4())

    logger.info(f"Authenticating request {client_request_id} to service endpoint {url_service_url} "
                f"with key ID {access_key_id}")

    auth = AWSRequestsAuth(
        aws_access_key=access_key_id,
        aws_secret_access_key=secret_access_key,
        aws_token=session_token,
        aws_service="execute-api",
        aws_host=url_service_host,
        aws_region=url_service_region
    )
    json_string_body = json.dumps({
        "xApiTarget": "GetFullUrl",
        "xRequestId": client_request_id,
        "tokenVersion": int(path_segments[0]),
        "urlToken": path_segments[1]
    })
    url_response_http = requests.post(
        url_service_url,
        data=json_string_body,
        auth=auth
    )
    url_response = url_response_http.json()

    # Handle success case
    if (data := url_response.get("response")) is not None:
        return handle_redirect(data["fullUrl"])

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
    try:
        exception_spec = url_response.get("error")
        exception_type = exception_spec.get("exceptionTypeName")
        (code, message) = api_exceptions_to_messages.get(exception_type, api_exception_default_message)
        detail = exception_spec.get("exceptionMessage")
        return build_error_response(code, message, detail)
    except KeyError:
        (code, message) = api_exception_default_message
        detail = "unexpected API JSON format"
    return build_error_response(code, message, detail)

def build_error_response(code, message, detail):
    error_detail = f"(Error detail: {detail})"
    body = error_page_template.replace("${message}", message).replace("${detail}", error_detail)
    return {
        "status": code,
        "body": body
    }