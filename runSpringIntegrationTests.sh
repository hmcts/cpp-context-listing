#!/usr/bin/env bash

# Runs the Spring Boot integration tests (Gradle module listing-integration-test-spring) against
# the docker-compose IT stack (db, WireMock, Artemis, app): composeUp → tests → composeDown.
#
# Usage: ./runSpringIntegrationTests.sh [nomidnightguard] [-- <extra gradle args>]
#   nomidnightguard - do not wait when 00:00 UTC is near (see waitIfUtcMidnightIsNear below).
#   Anything after `--` is passed to Gradle, e.g. -- --offline --tests '*SmokeIntegrationTest'
#
# This is the thin wrapper §9 describes. Until cut-over (P2.5) ./runIntegrationTests.sh stays the
# legacy WildFly + cpp-developers-docker runner for the JEE app; at P2.5 it is rewritten to call
# this Gradle task (MBD_REFACTOR_PROMPT.md §10.1). Needs Docker (compose v2) and the JDK 25 Gradle
# toolchain; no CPP_DOCKER_DIR.

set -e

MIDNIGHT_GUARD=true
GRADLE_ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    nomidnightguard)
      MIDNIGHT_GUARD=false
      ;;
    --)
      shift
      GRADLE_ARGS=("$@")
      break
      ;;
    *)
      echo "Unknown argument '$1' (supported: nomidnightguard, then -- <gradle args>)."
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
./gradlew :listing-integration-test-spring:test "${GRADLE_ARGS[@]}"
