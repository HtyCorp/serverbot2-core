#!/bin/sh
cd ~/environment/serverbot2/serverbot2-core
cd application-infrastructure
echo "Destroying all stacks except CommonResources..."
cdk destroy AppInstanceResources CommandService DiscordRelay GameMetadataService NetSecService ResourceReaper WorkflowService IpAuthService
echo "Done!"