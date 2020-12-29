import json
import requests
from aws_requests_auth.aws_auth import AWSRequestsAuth
from os import environ
from uuid import uuid4

def lambda_handler(event, context):
    request = event['Records'][0]['cf']['request']

    path_segments = request['uri'].split('/') # CF team really should have called this 'path', not 'uri'

    if len(path_segments) <= 2:
        return {
            'status': '400',
            'statusDescription': 'Bad Request',
            'body': '400 Bad Request'
        }
    # Remove leading empty param (path always begins with a '/')
    path_segments.pop(0)

    # Should parameterize region and host somehow...
    auth = AWSRequestsAuth(
        aws_access_key=environ['AWS_ACCESS_KEY_ID'],
        aws_secret_access_key=environ['AWS_SECRET_ACCESS_KEY'],
        aws_token=environ['AWS_SESSION_TOKEN'],
        aws_service="execute-api",
        aws_host="urlshortener.services.admiralbot.com",
        aws_region="ap-southeast-2"
    )
    json_string_body = json.dumps({
        'xApiTarget': 'GetFullUrl',
        'xRequestId': str(uuid4()),
        'tokenVersion': int(path_segments[0]),
        'urlToken': path_segments[1]
    })
    url_response_json = requests.post(
        "https://urlshortener.services.admiralbot.com/",
        data=json_string_body,
        auth=auth
    ).json()

    return {
        'status': '302',
        'statusDescription': 'Found',
        'headers': {
            'location': [{
                'key': 'Location',
                'value': url_response_json['fullUrl']
            }]
        }
    }
