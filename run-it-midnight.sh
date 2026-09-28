#!/usr/bin/env bash
# run-it-midnight.sh — prove the listing IT suite is midnight-safe WITHOUT waiting for real midnight.
#
# Freezes ItClock (listing-integration-test/.../it/util/ItClock.java) at a set of hazardous instants
# via -Dit.clock, which the listing-integration-test failsafe profile forwards into the forked IT JVM
# as the it.clock system property. Each anchor exercises a specific date hazard; the headline one is the
# 00:00-01:00 BST band where a Europe/London "today" and a UTC "today" land on different calendar days.
#
# Pre-Phase-2 (no ItClock) this script REPRODUCES the bug (expect FAIL at the BST band anchors) — that is
# itself a valid baseline. With ItClock + the -Duser.timezone=UTC failsafe pin in place, every anchor must
# pass. Per repo CLAUDE.md, the ONLY sanctioned way to run the ITs is ./runIntegrationTests.sh.
#
# Note: if ./runIntegrationTests.sh ignores -Dit.test on this stack, each anchor runs the FULL suite under
# the frozen clock instead of the date-sensitive subset — slower, but still a valid midnight-safety proof.
set -u

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_ROOT"

# Date-sensitive subset (verified to exist on team/ccsph2n). Comma-separated for -Dit.test.
SUBSET='PublishCourtListIT,WeekCommencingHearingIT,ListCourtWeekCommencingHearingIT,RangeSearchQueryForCourtCalendarIT,HearingCsvReportIT,ExhibitScenarioIT,HearingIT,EjectCaseCourtSlotsManagementIT,HearingDaysIT,RangeSearchQueryForMagistratesIT,ListingNoteIT'

# Sunday 2026-06-14 -> Monday 2026-06-15 so the midnight crossing also crosses a weekend
# (multi-day skip-weekend / next-working-day logic). 2026-06-12 is a Friday.
ANCHORS=(
  "2026-06-14T23:30:00+01:00[Europe/London]"   # just BEFORE midnight, BST, Sun
  "2026-06-15T00:00:30+01:00[Europe/London]"   # just AFTER midnight, BST, Mon
  "2026-06-15T00:30:00+01:00[Europe/London]"   # MID-BAND, BST   <-- headline case
  "2026-01-15T00:30:00Z"                        # GMT/winter control (offset 0)
  "2026-06-12T00:30:00+01:00[Europe/London]"   # Fri 00:30 -> next-working-day = Mon
)

green=0
for a in "${ANCHORS[@]}"; do
  echo "==================== anchor: $a ===================="
  docker rm -f $(docker ps -aq --filter "name=cpp-" --filter "name=wildfly-to-hap") 2>/dev/null
  mvn -q clean
  IT_CLOCK="$a" ./runIntegrationTests.sh -Dit.clock="$a" -Dit.test="$SUBSET"
  if [[ $? -eq 0 ]]; then
    green=$((green + 1)); echo "PASS  $a"
  else
    echo "FAIL  $a"
  fi
done

echo "===================================================="
echo "MIDNIGHT RESULT: $green/${#ANCHORS[@]} anchors green"
[[ "$green" -eq "${#ANCHORS[@]}" ]] && exit 0 || exit 1
