#!/bin/sh

cd ~/environment/serverbot2/serverbot2-core

npm install -g aws-cdk

mvn clean install

if [[ ! -d relay-docker ]]
then
    mkdir relay-docker
fi
cp discord-relay/target/discord-relay.jar relay-docker/discord-relay.jar
cp application-infrastructure/src/main/resources/RelayDockerfile relay-docker/Dockerfile

cd application-infrastructure
cdk synth
DEPLOY="cdk deploy --require-approval=never"
$DEPLOY CommonResources
$DEPLOY AppInstanceResources GameMetadataService NetSecService ResourceReaper
$DEPLOY IpAuthService WorkflowService
$DEPLOY CommandService
$DEPLOY DiscordRelay

echo "Done!"