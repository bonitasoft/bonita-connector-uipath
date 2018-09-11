# UiPath Connector

## Build

__Clone__ or __fork__ this repository, then at the root of the project run:

`./mvnw clean verify`

## Release

Before releasing a new version make sure that the versionning of `.def` files are consistent with the implementation. In order to create a new release of the connector use the [_maven release plugin_](http://maven.apache.org/maven-release/maven-release-plugin/):

`./mvnw release:prepare`

It creates a new git _tag_ and update the current development version. New tags are automatically built on our CI infrastructure.
