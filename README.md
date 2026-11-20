# Listing

The CPP Listing context: a Spring Boot 4.1 application (Java 25, Gradle). It replaced the WildFly WAR
on the CJS framework (MBD refactor, branch `mbd-refactor`) as a drop-in replacement: same URLs, media
types, schemas, access rules and public messages. What changed for callers is in
[docs/breaking.md](docs/breaking.md); the per-module rationale is in [docs/REVIEW.md](docs/REVIEW.md).

## Build and run

```
./gradlew build -x :listing-integration-test-spring:test     # compile + unit tests
./gradlew clean build                                        # ... plus the Spring ITs (docker-compose)
./runIntegrationTests.sh                                      # unit tests, Spring ITs, legacy IT suite + public message contract
./runIntegrationTests.sh legacy nounit -- -PitClasses=HearingIT   # one legacy IT class
./gradlew :listing-api:bootJar                                # listing-api/build/libs/cpp-context-listing-<version>.jar
```

The ITs need Docker (compose v2) only: the stack (Postgres 16, WireMock, Artemis, the app) is
defined in `listing-integration-test-spring/docker/docker-compose.integration.yml`. Deployment
changes still to make (pipeline, Helm): [docs/deployment-notes.md](docs/deployment-notes.md).

| Module | Contents |
|---|---|
| `listing-api` | Boot app, OpenAPI spec (`src/main/resources/openapi/listing-api.openapi.yml`), controllers, request validation (RAML schemas in `src/raml`), access-control DRL, config |
| `listing-command-spring` | Command services, rules (former aggregates), inbound `public.*` handlers, court lists |
| `listing-common-spring` | Query services, REST clients, listing notes |
| `listing-messaging` | Envelope, transactional outbox + relay to `public.event`, `@JmsListener`, inbound dedup; `src/contract` = legacy public schemas |
| `listing-domain-spring`, `listing-domain-event-spring` | Domain types and payload DTOs |
| `listing-viewstore-persistence-spring`, `listing-viewstore-liquibase` | JPA entities/repositories, Liquibase changelogs |
| `listing-xhibit` | XHIBIT court-list XML (JAXB from `src/main/xsd`) and WebDAV export |
| `listing-integration-test-spring`, `listing-integration-test-legacy` | Spring ITs; the legacy `*IT` suite on the Spring stack |

The examples below use the legacy local gateway (`localhost:8080`); against a local app use its port and
the `/listing-service` context path for both command and query paths.

## API

Every request requires a `CJSCPPUID` header, with a valid user id. Access control is the
`cp-auth-rules-filter` (`application-authz.yml`, on/off with `LISTING_AUTHZ_ENABLED`); the old WildFly
`utils/accessControl` switch no longer applies.

### Getting a single Hearing by its ID

This endpoint is primarily defined in [listing-query-api.raml](listing-api/src/raml/query/listing-query-api.raml) and served from [listing-api.openapi.yml](listing-api/src/main/resources/openapi/listing-api.openapi.yml)

#### When one exists for that ID 

First, we'll need take the ID from one of the hearings:

``` 
curl -v -H "CJSCPPUID:441a514e-2437-4338-b72a-baa3d2f9d0be" -H  "Accept:application/vnd.listing.search.hearings+json" http://localhost:8080/listing-query-api/query/api/rest/listing/hearings/range-search\?allocated\=true |  jq '.hearings[0] | {id : .id}'
```

``` 
{
  "id": "c9b38f29-0d6e-4b47-be2b-5b2b8247089f"
}
```

Copying that ID, we can get the Hearing ...

``` 
curl -v -H "CJSCPPUID:441a514e-2437-4338-b72a-baa3d2f9d0be" -H "Accept:application/vnd.listing.search.hearing+json" http://localhost:8080/listing-query-api/query/api/rest/listing/hearings/c9b38f29-0d6e-4b47-be2b-5b2b8247089f | jq .
```

``` 
< HTTP/1.1 200 OK
< CPPID: 66c19677-0980-49e9-af03-802d4dcedaba
< X-Powered-By: Undertow/1
< Server: WildFly/10
< Content-Type: application/vnd.listing.search.hearing+json
< Content-Length: 6019
< Date: Tue, 19 Nov 2019 15:49:44 GMT
```

And [here is an example of the response](listing-api/src/raml/query/json/listing.search.hearing.json).

#### When no Hearing exists for that ID

``` 
curl -v -H "CJSCPPUID:441a514e-2437-4338-b72a-baa3d2f9d0be" -H "Accept:application/vnd.listing.search.hearing+json" http://localhost:8080/listing-query-api/query/api/rest/listing/hearings/b471ab7b-f13f-4c4b-9017-c6b2ca24b79e | jq  
```

``` 
< HTTP/1.1 404 Not Found
< X-Powered-By: Undertow/1
< Server: WildFly/10
< Content-Length: 0
< Date: Tue, 19 Nov 2019 15:48:41 GMT
```

#### When the ID is not a valid UUID.

``` 
curl -v -H "CJSCPPUID:441a514e-2437-4338-b72a-baa3d2f9d0be" -H "Accept:application/vnd.listing.search.hearing+json" http://localhost:8080/listing-query-api/query/api/rest/listing/hearings/c9b38f29 | jq   
```

``` 
< HTTP/1.1 400 Bad Request
< X-Powered-By: Undertow/1
< Server: WildFly/10
< Content-Type: application/vnd.listing.search.hearing+json
< Content-Length: 54
< Date: Wed, 27 Nov 2019 11:01:14 GMT

{
  "error": "Please ensure that the id is a valid UUID."
}
```

### Requesting the publication of the next day's Hearing list for all of the Crown Courts

Please note it is the 'final' list for the next weekday.

As well, this can only be called by a 'System' user (presuming that access control is active).

This endpoint is defined in [listing-command-api.raml](listing-api/src/raml/command/listing-command-api.raml)

``` 
curl -X POST \
-i \
-H "CJSCPPUID: ebc12dd6-9497-47bc-b3a0-864511612ae8" \
-H "Content-Type: application/vnd.listing.command.publish-court-lists-for-crown-courts+json" \
-d '{}' \
'http://localhost:8080/listing-service/command/api/rest/listing/publishCourtListsForCrownCourts'
```


``` 
HTTP/1.1 202 Accepted
X-Powered-By: Undertow/1
Server: WildFly/10
Content-Length: 0
Date: Tue, 03 Dec 2019 16:14:28 GMT
```