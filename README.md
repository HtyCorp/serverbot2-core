# ServerBot2

## About

This is a Discord bot that integrates with an AWS account to simplify deploying and managing game/application servers on EC2. Major features:
* Simple create/edit/start/stop/delete commands for servers.
* Dynamic user IP whitelisting to prevent unknown connections and mitigate DDoS. 
* Allows most admin tasks through Discord without a direct AWS login needed, including server SSH access.

\[TODO: insert demo here\]

Plans for future development:
* Short-term: improve existing features and ease-of-use, add a few missing features (automatic backups and guest IP whitelisting), and fix lack of test coverage.
* Long-term: decouple bot and game/application AWS accounts, move to a multi-tenant architecture and potentially make this public.

## Modules

### application-infrastructure

_Uses: CDK, CodePipeline, CodeBuild, CloudFormation_

Defines both the infrastructure needed to host the bot, and the deployment pipeline to update it from this repository. Deployment configuration is loaded from SSM Parameter Store.

Service binaries are packaged into the CDK cloud assembly for deployment, so code updates are pushed directly through CloudFormation.

### service-framework

A client/server framework to simplify making requests between microservices, mainly using Lambda (for serverless APIs) or 2-way SQS (for dedicated/provisioned APIs). Does not currently support typical REST/HTTP APIs.

Service APIs are modelled as Java interfaces (see each `*-model` module), which are used to generate client and server instances at runtime using reflection. 

### discord-relay(-model)

_Hosted on: ECS (Fargate_)

An always-online service ECS service with two purposes:
1. Listens to specific Discord channels for anything matching command syntax (e.g. `!start somegame`) and invokes `command-lambda` to handle the command.
2. Provides a simplifed Discord API to microservces, to send messages to users or channels and edit user permissions.

### command-lambda(-model)

_Hosted on: Lambda_

Handles commands submitted by `discord-relay`, usually by invoking the other relevant services. Invokes `workflow-service` for any commands that can't be handled synchronously/quickly.

Directly implements one-click (federated) terminal access to EC2 instances using Session Manager.

### workflow-service(-model)

_Hosted on: Step Functions, Lambda_

Handles asynchronous/long-running workflows, e.g. deploying a new game, setting it up and shutting it down. Communicates with `app-daemon` directly on game/application EC2 instances, and sends/updates messages in Discord to keep users up-to-date on progress.

This is really just a set of Step Functions resources, not a service on its own.

### game-metadata-service(-model)

_Hosted on: Lambda_

Basic CRUD interface to interact with metadata for games/applications, including details on allocated AWS resources (e.g. SQS queues and EC2 instances).

### network-security-service(-model)

_Hosted on: Lambda_

Simplifies management of security groups for games/application EC2 instances. Maintains a standard user IP list and allows instance ports to be opened _only_ to addresses on this list.

Couples with `ip-authorizer` to allow authenticated Discord users to whitelist themselves using their browser.

Additionally, analyzes VPC flow logs to determine idleness/inactivity in game/application instances.

### ip-authorizer

_Hosted on: API Gateway, Lambda_

The public API component of `network-security-service` needed to support IP authorization callback links. These are necessary since user IP addresses can't be directly obtained from Discord.

Captures requester IP addresses and callback tokens and relays them to `network-security-service`. 

### url-shortener

_Hosted on: API Gateway, Lambda_

URLs generated by the bot are often long and ugly, leading to bad UX since Discord highlights them when asking for user confirmation on navigation. This service provides URL shortening to make these less scary-looking.

URLs can be publicly read/accessed, but APIGW IAM authN/authZ is required to create new URLs. For simplicity, both tasks are handled in the same API, with no Java `service-framework` model.

Internally, URLs are stored in encrypted format using data keys encoded in the shortened URL; access to the URL database alone is not enough to view full URLs.  

### app-daemon(-model)

_Hosted on: application/game EC2 instances_

An agent running on game/application EC2 instances to coordinate with running workflows, run game/application processes and handle logging and idle shutdowns.

This is only ever directly invoked by `workflow-service`, and requests are only relevant to the specific instance the request is made  to.

### lambda-warmer

_Hosted on: CloudWatch Events, Lambda_

Periodically invokes critical Lambda functions to ensure they have environments ready, since not doing so can result in severe cold start latency. This has a direct negative UX effect, since the latency affects bot response times in Discord.

This is currently the most reasonable/economic option, compared to migrating to ECS or using Lambda provisioned concurrency.

### resource-reaper

_Hosted on: CloudWatch Events, Lambda_

Periodically runs cleanup tasks for unused resources. Currently only used to clean up abandoned SQS queues created by `service-framework` SQS-based clients for two-way messaging.

### shared-configuration

Intended to store configuration constants needed by multiple services/modules, but also stores some service-specific values which should probably be refactored.

Uses an abstract `ConfigValue` type with various storage/lookup options: SSM Parameter Store, Secrets Manager, environment variables or system propeties. 

### shared-util

Various utilities for logging and ID generation, and minor helpers like a basic pair type.

### test-utilities

Mostly unused since project does not have consistent test coverage. Contains a reflective equals option to simplify testing without writing equals() and hashCode() boilerplate.