# Bonita UIPath Connector

## Project Overview

This project is a Bonita connector that integrates with UiPath Orchestrator to automate RPA (Robotic Process Automation) workflows from Bonita BPM processes.

- **GroupId:** `org.bonitasoft.connectors`
- **ArtifactId:** `bonita-connector-uipath`
- **Current version:** `2.3.1-SNAPSHOT`
- **Java version:** 11 (compiled), 17 (CI build)
- **License:** GPL v2.0
- **Bonita Engine version:** 7.14.0

The connector ships three distinct operations:
1. **Add Queue Item** (`uipath-add-queueItem`) — adds an item to a UiPath queue
2. **Start Job** (`uipath-startjob`) — starts a UiPath process job
3. **Get Job** (`uipath-getjob`) — retrieves the status/result of a running job

## Build Commands

```bash
# Full build with tests and SonarCloud analysis
./mvnw -B -ntp clean verify sonar:sonar

# Build and run tests only (no Sonar)
./mvnw -B -ntp clean verify

# Skip tests (not recommended)
./mvnw -B -ntp clean package -DskipTests

# Run tests only
./mvnw -B -ntp test

# Deploy/release (requires GPG key and deploy profile)
./mvnw -B -ntp deploy -P deploy
```

Build output artifacts are placed in `target/`:
- `*.jar` — compiled connector JAR
- `*.zip` — connector assembly bundles (one per connector type + an "all" bundle)

## Architecture

### Source layout

```
src/
  main/
    java/org/bonitasoft/engine/connector/uipath/
      UIPathConnector.java          # Abstract base: auth, HTTP client setup (Retrofit + OkHttp)
      UIPathStartJobsConnector.java # Starts UiPath jobs
      UIPathGetJobConnector.java    # Polls/retrieves job status
      UIPathAddToQueueConnector.java# Adds items to a UiPath queue
      UIPathService.java            # Retrofit interface for UiPath Orchestrator REST API
      model/                        # POJOs: Job, Release, Robot, QueueItem, etc.
      converters/                   # Custom Jackson converter for wrapped API responses
    resources/                      # i18n property files (en, fr, es) + icons
    resources-filtered/             # Connector definition/impl descriptors (Maven-filtered)
  assembly/                         # Maven Assembly descriptors for packaging
  script/                           # Groovy script to generate dependency vars for assembly
  test/
    java/...                        # JUnit 5 + Mockito + WireMock tests per connector
    resources/__files/              # WireMock mock response JSON fixtures
```

### Key dependencies

| Library | Purpose |
|---|---|
| `bonita-common` (provided) | Bonita connector API (`AbstractConnector`) |
| `retrofit2` + `converter-jackson` | Typed HTTP client for UiPath REST API |
| `okhttp3:logging-interceptor` | HTTP request/response debug logging |
| `lombok` (provided) | Boilerplate reduction (getters/setters/builders) |

### Authentication modes

`UIPathConnector` supports two deployment modes controlled by the `cloud` boolean input:

- **On-premise / classic:** authenticates via username + password against a tenant URL, retrieves a bearer token from the `result` field.
- **Cloud:** two sub-modes selected by `cloudAuthType`:
  - `"Client credentials (Oauth)"` — uses `client_id` + `client_secret` to fetch an OAuth2 token.
  - `"Token (Bearer)"` — accepts a pre-generated bearer token directly.

The HTTP client is built with Retrofit 3 over OkHttp. A custom `WrappedAttributeConverter` unwraps UiPath's `value`-wrapped list responses.

## Testing

Tests use **JUnit 5**, **Mockito**, and **WireMock** (HTTP mock server).

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw -pl . -Dtest=UIPathStartJobsConnectorTest test
```

Test classes mirror the connector classes:
- `UIPathConnectorTest` — base connector logic (auth, validation)
- `UIPathStartJobsConnectorTest` — start-job flow
- `UIPathGetJobConnectorTest` — get-job flow
- `UIPathAddToQueueConnectorTest` — queue item flow

WireMock stubs and JSON fixtures live in `src/test/resources/__files/`. Code coverage is measured by JaCoCo and reported to SonarCloud.

## Commit Format

Commits must follow the **Conventional Commits** pattern enforced by the `commit-message-check` workflow:

```
type(category): description
```

Examples:
```
feat(startjob): add runtimeType input parameter support
fix(auth): handle empty scope in client credentials flow
chore(ci): add Claude Code review workflow
refactor(connector): extract token header builder
test(queue): add WireMock test for priority enum
```

Common types: `feat`, `fix`, `chore`, `refactor`, `test`, `docs`, `ci`, `build`.

## Release Process

Releases are triggered manually via the **Release** GitHub Actions workflow (`release.yml`):

1. Go to **Actions → Release → Run workflow**.
2. Enter the release version (e.g., `2.3.1`).
3. The reusable workflow `bonitasoft/github-workflows/_reusable_release_connector.yml@main` handles:
   - Removing the `-SNAPSHOT` suffix from `pom.xml`
   - Building and signing artifacts (GPG via `deploy` profile)
   - Publishing to Maven Central via the Sonatype Central plugin
   - Creating a GitHub release and tag
   - Bumping to the next `-SNAPSHOT` version

Secrets required: `KSM_CONFIG` (Keeper Secrets Manager), `ORGANIZATION_ANTHROPIC_API_KEY` (for Claude CI).
