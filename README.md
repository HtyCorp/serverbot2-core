# ServerBot2 

###(a.k.a. *AdmiralBot*)

## About

This is a Discord bot that integrates with an AWS account to simplify deploying and managing game/application servers on EC2. Major features:
* Simple create/edit/start/stop/delete commands for servers.
* Dynamic user IP whitelisting to prevent unknown connections and mitigate DDoS. 
* Allows most admin tasks through Discord without a direct AWS login needed, including server SSH access.

\[TODO: insert demo here\]

Plans for future development:
* Short-term: improve existing features, add a few missing features (e.g. automatic volume backups), add more test coverage.
* Long-term: decouple bot and application AWS accounts to allow moving to a multi-tenant architecture.

## Layout

### `agents`

Code for software agents running on application hosts. Currently this only contains `app-daemon` which runs on application hosts and coordinates running applications as part of Step Functions workflows (see `service/workflows-handler`).

### `common`

Contains common utility packages (most importantly the client-server RPC framework `service-framework`) and shared configuration parameters.

### `deployment`

Tools needed to deploy all required AWS infrastructure. Currently contains a single package `application-infrastructure` that defines complete architecture using AWS CDK. Most services run on ECS and are served by API Gateway using IAM authN/authZ.

### `models`

Model interfaces for the various microservices powering the bot. These are defined as plain Java interfaces with some associated metadata.

Model information is used for both client and server sides of RPC, and for deployment.

### `scribble`

Various helpful notes and worklogs added during development.

### `services`

Actual microservice code implementing all the models in `models` directory. Relies heavily on the `common/service-framework` package to abstract out most RPC and server implementation details. 

### `web`

User-facing web components. Currently only contains the frontend/redirector for the URL shortener service, but other more advanced web app components are being created.
