#!/bin/sh

set -e

echo "Destroying all stacks except CommonResources..."

# Note: uses CLI instead of CDK since we don't care about following the status and just want to initiate the deletes
DESTROY="aws cloudformation delete-stack --stack-name"
$DESTROY AppInstanceResources &
$DESTROY CommandService &
$DESTROY DiscordRelay &
$DESTROY GameMetadataService &
$DESTROY NetworkSecurityService &
$DESTROY ResourceReaper &
$DESTROY WorkflowService &
$DESTROY IpAuthorizerApi &

echo "Deletion of all stacks initiated"