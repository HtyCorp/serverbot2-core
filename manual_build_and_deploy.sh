#!/bin/sh

set -e

cd ~/environment/serverbot2/serverbot2-core

npm install -g aws-cdk

mvn clean install -Dmaven.test.skip=true

if [ ! -d relay-docker ]
then
    mkdir relay-docker
fi
cp discord-relay/target/discord-relay-1.0-SNAPSHOT-jar-with-dependencies.jar relay-docker/discord-relay.jar
cp application-infrastructure/src/main/resources/RelayDockerfile relay-docker/Dockerfile

cd application-infrastructure
cdk synth
DEPLOY="cdk deploy --require-approval=never"

# Phase 1: common resources
echo "Running phase 1 deployment..."
$DEPLOY CommonResources &
wait

# Phase 2: passive services
echo "Running phase 2 deployment..."
$DEPLOY AppInstanceResources &
$DEPLOY GameMetadataService &
$DEPLOY NetworkSecurityService &
$DEPLOY ResourceReaper &
wait

# Phase 3: intermediate services depending on passive services
echo "Running phase 3 deployment..."
$DEPLOY IpAuthorizerApi &
$DEPLOY WorkflowService &
wait 

# Phase 4: Command service depending on all previous services
echo "Running phase 4 deployment..."
$DEPLOY CommandService &
wait

# Phase 5: Discord relay relying on Command service
echo "Running phase 5 deployment..."
$DEPLOY DiscordRelay &
wait

echo "Done!"