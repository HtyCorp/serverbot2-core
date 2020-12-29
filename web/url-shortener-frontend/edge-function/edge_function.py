import json
import requests
from aws_requests_auth.aws_auth import AWSRequestsAuth

def lambda_handler(event, context):
    request = event['Records'][0]['cf']['request']

    # TODO