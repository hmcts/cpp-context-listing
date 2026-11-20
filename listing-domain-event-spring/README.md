# listing-domain-event-spring

Listing payload DTOs (ADR 014, ADR 015): the classes under `src/generated/java` are the output of
the CJS framework's `pojo-generation-plugin` as the former Maven modules ran it
(`listing-domain-event`, plus the payload/command classes of `listing-event-processor`,
`listing-command-handler` and `listing-command-api`), with the framework's `@Event` annotation
stripped so that `framework-api-domain` is not on the classpath (§12, D32).

**These sources are now owned here.** The Maven modules and the `refresh-generated-sources.sh`
script that re-vendored them were deleted at P2.6 (MBD refactor); there is no generator in the
Gradle build (jsonschema2pojo produces different classes). Edit the classes directly when a payload
changes, keeping the generated style (`@JsonCreator` constructors, builders), and add or adjust the
unit tests that pin the JSON shape. The source JSON schemas are in git history (commit
`b932de14b`: `listing-event/listing-event-listener/src/yaml/json/schema` for listing-domain-event,
and the `src/raml`/`src/yaml` schemas of the other three modules).

`PublishCourtListType` and `PublishStatus` (package `uk.gov.justice.listing.event`) are not here:
`listing-domain-spring` owns them.
