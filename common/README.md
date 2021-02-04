## About

These packages contain various utilities and configuration data used by other packages and services.

### `service-framework`

A client/server framework to simplify implementing microservices, deploying them and making requests to them. Mainly supports HTTP APIs on ECS as the most common use case, using the Spark microframework.

APIs are modelled as plain Java interfaces with associated metadata. Clients and servers are generated at runtime using reflection, and APIs are deployed using the associated endpoint metadata.

### `shared-configuration`

Intended to store configuration constants needed by multiple services/modules.

Uses an abstract `ConfigValue` type with various storage/lookup options: SSM Parameter Store, Secrets Manager, environment variables or system propeties.

Really should be refactored to move most of these parameters to their respective service packages. 

### `shared-util`

Various utilities for logging and ID generation, and minor helpers like a basic pair type.

Simplifies configuring the 'context' of a service (e.g. whether it runs on Lambda, ECS or EC2), to configure credentials and clients in a unified way.

### `test-utilities`

Mostly unused since the project does not have consistent test coverage. Contains a reflective equals option to simplify testing without writing equals() and hashCode() boilerplate.