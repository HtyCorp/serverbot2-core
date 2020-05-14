#!/bin/sh
mvn clean install
cp discord-relay/target/discord-relay.jar relay-docker/discord-relay.jar
cp application-infrastructure/src/main/resources/RelayDockerfile relay-docker/DockerFile
(cd deployment-infrastructure/ && cdk synth >/dev/null && cdk deploy --require-approval=never DeploymentStack)
(cd application-infrastructure/ && cdk synth >/dev/null && cdk deploy --require-approval=never '*')