#!/bin/sh

set -e

cd ~/environment/serverbot2/serverbot2-core
cd application-infrastructure
echo "Destroying all stacks except CommonResources..."

DESTROY="cdk destroy --force" # Disables confirmation prompt
$DESTROY AppInstanceResources &
$DESTROY CommandService &
$DESTROY DiscordRelay &
$DESTROY GameMetadataService &
$DESTROY NetworkSecurityService &
$DESTROY ResourceReaper &
$DESTROY WorkflowService &
$DESTROY IpAuthorizerApi &
wait

echo "All non-common stacks destroyed"