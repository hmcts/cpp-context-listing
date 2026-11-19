#!/usr/bin/env bash

# Runs the Spring Boot integration tests (Gradle module listing-integration-test-spring) against
# the docker-compose IT stack (db, WireMock, Artemis, app): composeUp → tests → composeDown.
#
# Usage: ./runSpringIntegrationTests.sh [legacy] [nomidnightguard] [-- <extra gradle args>]
#   legacy          - run the legacy *IT suite (the Maven listing-integration-test scenarios, compiled
#                     by the Gradle module listing-integration-test-legacy) against the Spring stack
#                     instead of the new-style Spring ITs (MBD P2.3.0, §9, ADR 014). Subsets:
#                     -- -PitClasses=ListingNoteIT,HearingIT  or  -- --tests '*ListingNoteIT'.
#                     A red legacy run is expected until every command slice is ported (P2.3.x);
#                     add -- -PlegacyIgnoreFailures to get the report without a failed build.
#   nomidnightguard - do not wait when 00:00 UTC is near (see waitIfUtcMidnightIsNear below).
#   Anything after `--` is passed to Gradle, e.g. -- --offline --tests '*SmokeIntegrationTest'
#
# This is the thin wrapper §9 describes. Until cut-over (P2.5) ./runIntegrationTests.sh stays the
# legacy WildFly + cpp-developers-docker runner for the JEE app; at P2.5 it is rewritten to call
# this Gradle task (MBD_REFACTOR_PROMPT.md §10.1). Needs Docker (compose v2) and the JDK 25 Gradle
# toolchain; no CPP_DOCKER_DIR.

set -e

MIDNIGHT_GUARD=true
TASK=:listing-integration-test-spring:test
GRADLE_ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    nomidnightguard)
      MIDNIGHT_GUARD=false
      ;;
    legacy)
      TASK=:listing-integration-test-legacy:legacyIntegrationTest
      ;;
    --)
      shift
      GRADLE_ARGS=("$@")
      break
      ;;
    *)
      echo "Unknown argument '$1' (supported: legacy, nomidnightguard, then -- <gradle args>)."
      exit 2
      ;;
  esac
  shift
done

# Same guard as runIntegrationTests.sh: runs that cross 00:00 UTC fail because the test JVM and
# the server disagree on 'today'.
waitIfUtcMidnightIsNear() {
  [ "$MIDNIGHT_GUARD" = true ] || return 0
  local guard="${IT_MIDNIGHT_GUARD_SECONDS:-1200}"
  local secs_left=$(( 86400 - $(date -u +%s) % 86400 ))
  if [ "$secs_left" -le "$guard" ]; then
    echo "00:00 UTC is ${secs_left}s away: waiting $(( secs_left + 30 ))s so the IT JVM and the server agree on 'today'"
    sleep $(( secs_left + 30 ))
  fi
}

cd "$(dirname "$0")"
waitIfUtcMidnightIsNear
./gradlew "$TASK" "${GRADLE_ARGS[@]}"
