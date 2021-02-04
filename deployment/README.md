## About

Contains deployment tools, which for now just means CDK code to deploy everything needed for the project.

### `application-infrastructure`

_Uses: CDK, CodePipeline, CodeBuild, CloudFormation_

Defines the infrastructure needed to host the bot, and the deployment pipeline to update it from this repository. Deployment configuration is loaded from SSM Parameter Store in the deployment account.

Uses a few custom CDK constructs to simplify setting up microservices on ECS, since most project services are deployed this way. Deployment uses Spot auto-scaling groups to run these at low cost.

Service binaries are packaged into the CDK cloud assembly for deployment, so code updates are pushed directly through CloudFormation.
