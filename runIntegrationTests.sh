#!/usr/bin/env bash

# Runs the integration tests of the Spring Boot listing app against the docker-compose IT stack
# (db, WireMock, Artemis, app; MBD refactor §9). A thin wrapper around the Gradle IT tasks:
#
#   1. unit tests   ./gradlew build -x :listing-integration-test-spring:test           (skip: nounit)
#   2. Spring ITs   ./gradlew :listing-integration-test-spring:test                     (*IntegrationTest / *IT)
#   3. legacy ITs   ./gradlew :listing-integration-test-legacy:legacyIntegrationTest    (the former Maven
#                   listing-integration-test scenarios), followed by publicMessageContractTest (every
#                   public.* message the app published during the run, validated against its legacy schema)
#
# Usage: ./runIntegrationTests.sh [spring|legacy] [durations] [nounit] [nomidnightguard] [-- <gradle args>]
#   spring          - run only the Spring ITs (step 2).
#   legacy          - run only the legacy suite (step 3). Subsets: -- -PitClasses=ListingNoteIT,HearingIT
#                     or -- --tests '*ListingNoteIT'.
#   durations       - legacy suite: ranked per-test durations (TestDurationListener) in
#                     listing-integration-test-legacy/build/legacy-it/target/test-results/test-durations.csv.
#   nounit          - skip step 1 (the IT stack only needs the bootJar).
#   nomidnightguard - do not wait when 00:00 UTC is near (see waitIfUtcMidnightIsNear below).
#   Anything after `--` is passed to every Gradle call, e.g. -- --offline, -- -Pit.clock=2026-06-01T23:30:00Z.
#
# `errorlog` (the WildFly runner's strict server.log mode) is no longer supported: the app logs JSON to
# stdout in its container (captured under listing-integration-test-legacy/build/legacy-it/container-logs),
# not to a server.log the suite can mark and scan.
#
# Needs Docker (compose v2) and the JDK 25 Gradle toolchain; no WildFly and no CPP_DOCKER_DIR. Both
# suites use the same container names and ports, so they run one after the other, never together.
# Exit status: non-zero if any step failed (every selected step still runs).

set -u

# The host must not sleep mid-run: the suite's JMS connections die with the broker's TTL (seen at P2.3.2).
if [ -z "${IT_CAFFEINATED:-}" ] && [ "$(uname -s)" = "Darwin" ] && command -v caffeinate >/dev/null 2>&1; then
  export IT_CAFFEINATED=1
  exec caffeinate -dims "$0" "$@"
fi

RUN_SPRING=true
RUN_LEGACY=true
RUN_UNIT=true
MIDNIGHT_GUARD=true
LEGACY_PROPS=()
GRADLE_ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    spring)
      RUN_LEGACY=false
      ;;
    legacy)
      RUN_SPRING=false
      ;;
    durations)
      LEGACY_PROPS+=("-PitDurations")
      ;;
    nounit)
      RUN_UNIT=false
      ;;
    nomidnightguard)
      MIDNIGHT_GUARD=false
      ;;
    errorlog)
      echo "'errorlog' is not supported on the Spring stack: the app logs JSON to stdout in its container, not to a"
      echo "server.log the suite can scan. Check listing-integration-test-legacy/build/legacy-it/container-logs/ instead."
      exit 2
      ;;
    --)
      shift
      GRADLE_ARGS=("$@")
      break
      ;;
    *)
      echo "Unknown argument '$1' (supported: spring legacy durations nounit nomidnightguard, then -- <gradle args>)."
      exit 2
      ;;
  esac
  shift
done

# Never start an IT JVM within IT_MIDNIGHT_GUARD_SECONDS of 00:00 UTC (01:00 BST in summer).
# ItClock anchors "today" once per IT JVM, but the app's LocalDate.now() rolls over at 00:00 UTC:
# hearings the suite created for "today" then count as past and date-filtered searches stop matching.
# A run that straddled midnight on 2026-10-09 went 24+ tests red. Checked before each suite.
waitIfUtcMidnightIsNear() {
  [ "$MIDNIGHT_GUARD" = true ] || return 0
  local guard="${IT_MIDNIGHT_GUARD_SECONDS:-1200}"
  local secs_left=$(( 86400 - $(date -u +%s) % 86400 ))
  if [ "$secs_left" -le "$guard" ]; then
    echo "00:00 UTC is ${secs_left}s away: waiting $(( secs_left + 30 ))s so the IT JVM and the app agree on 'today'"
    sleep $(( secs_left + 30 ))
  fi
}

gradle() {
  # "${arr[@]+...}" keeps bash 3.2 (macOS) happy with empty arrays under set -u.
  ./gradlew "$@" ${GRADLE_ARGS[@]+"${GRADLE_ARGS[@]}"}
}

cd "$(dirname "$0")"
FAILED=()

if [ "$RUN_UNIT" = true ]; then
  echo "== Unit tests"
  gradle build -x :listing-integration-test-spring:test || FAILED+=("unit tests")
fi

if [ "$RUN_SPRING" = true ]; then
  waitIfUtcMidnightIsNear
  echo "== Spring integration tests (listing-integration-test-spring)"
  gradle :listing-integration-test-spring:test || FAILED+=("Spring ITs")
fi

if [ "$RUN_LEGACY" = true ]; then
  waitIfUtcMidnightIsNear
  echo "== Legacy IT suite (listing-integration-test-legacy) + public message contract"
  gradle :listing-integration-test-legacy:legacyIntegrationTest ${LEGACY_PROPS[@]+"${LEGACY_PROPS[@]}"} \
    || FAILED+=("legacy ITs / public message contract")
fi

if [ ${#FAILED[@]} -gt 0 ]; then
  printf 'FAILED: %s\n' "${FAILED[@]}"
  exit 1
fi
echo "All selected steps passed."
