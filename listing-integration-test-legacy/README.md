# listing-integration-test-legacy

The legacy `*IT` suite (the scenarios of the former Maven module `listing-integration-test`) run
against the Spring Boot app on the docker-compose IT stack (MBD refactor §9, D1, D10, ADR 014).
Moved here unchanged at P2.6; only the harness differs (base URI and ports, WireMock in its own
container, the Artemis connection, the viewstore-only database), see ADR 014.

Run it with `./runIntegrationTests.sh legacy` (or `./runIntegrationTests.sh` for the Spring ITs plus
this suite), or directly:

    ./gradlew :listing-integration-test-legacy:legacyIntegrationTest [-PitClasses=ListingNoteIT,HearingIT] [--tests ...]

Payload-based tests: see `README-PAYLOAD-BASED-TESTING.md`.

## Public message contract

`PublicMessageRecorder` (a JUnit Platform listener, harness only) records every message on
`public.event` during the run into `build/legacy-it/public-messages/<name>/`.
`publicMessageContractTest` runs after `legacyIntegrationTest` and validates each recorded
`public.*` payload against the legacy public schema of its name
(`listing-messaging/src/contract`), one test per name of `public-publications-descriptor.yaml`.
Names without a legacy schema, or that the suite never makes the app publish, are reported as
skipped with the reason.

## Owned inputs (no longer generated)

The Maven module and its `refresh-legacy-inputs.sh` script were deleted at P2.6. These two inputs are
now maintained here by hand:

- `src/generated/java`: POJOs the framework's `pojo-generation-plugin` generated from
  `src/test/raml/json/schema` (kept as their source/documentation). Edit the classes directly if a
  schema changes.
- `legacy-it-dependencies.txt`: the former Maven module's resolved test classpath
  (`group:artifact:version[:classifier]`), resolved by Gradle with `transitive=false`. The
  framework/HMCTS test jars (`uk.gov.*`) come from `~/.m2` or the HMCTS Azure Artifacts feed as plain
  artifacts (their POMs inherit from parents Gradle cannot parse offline). Add or bump entries by
  hand; keep the list flat (every transitive dependency listed). These are **test-scope only**:
  none of them reaches the runtime classpath of `listing-api`.
