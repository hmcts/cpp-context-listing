# Runtime image for the Spring Boot listing app (MBD refactor, Phase 2). Copied from
# cpp-context-listing-courtscheduler/Dockerfile (ADR 007).
#
# Used by the ADO `cp-gh-artifact-to-acr` pipeline ("Docker build and push"), which expects the
# Dockerfile at the repo root, downloads the published Spring Boot fat jar from the HMCTS Azure
# Artifacts feed into build/libs/ and runs `docker build` from the repo root. Locally:
#   ./gradlew :listing-api:bootJar && mkdir -p build/libs && cp listing-api/build/libs/*.jar build/libs/
#   docker build -t listing .
# Whether listing moves to that pipeline or stays on azure-pipelines.yaml is open (§15.7); the
# pipeline switch itself is P2.5. The WildFly image (docker/Dockerfile_listing-service) is untouched.
#
# BASE_IMAGE: CI supplies hmcts/apm-services:25-jre, which already trusts the HMCTS CA.
ARG BASE_IMAGE
FROM ${BASE_IMAGE:-eclipse-temurin:25-jre}

# curl is useful for in-pod debugging and for the actuator health probe.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r app && useradd -r -g app app
WORKDIR /app

# startup.sh picks the right (non-plain) jar at runtime. Only startup.sh is copied: docker/ also
# holds the WildFly Dockerfile and scripts.
COPY docker/startup.sh /app/
COPY build/libs/*.jar /app/

USER app
ENTRYPOINT ["/bin/sh","./startup.sh"]
