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

echo "Running fully parallel, single-stage deployment."
echo "Only use this if updating already deployed services!"

# Phase 1: common resources
$DEPLOY CommonResources &

# Phase 2: passive services
$DEPLOY AppInstanceResources &
$DEPLOY GameMetadataService &
$DEPLOY NetworkSecurityService &
$DEPLOY ResourceReaper &

# Phase 3: intermediate services depending on passive services
$DEPLOY IpAuthorizerApi &
$DEPLOY WorkflowService &

# Phase 4: Command service depending on all previous services
$DEPLOY CommandService &

# Phase 5: Discord relay relying on Command service
$DEPLOY DiscordRelay &

wait

echo "Done!"