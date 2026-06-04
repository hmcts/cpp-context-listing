#!/usr/bin/env bash
# run-it-10x.sh — prove the listing IT suite is robust: 10 clean runs, 0 failures each.
#
# Per repo CLAUDE.md, one "run" is the 3-step unit of work:
#     docker rm -f $(docker ps -aq --filter name=cpp- --filter name=wildfly-to-hap)
#     mvn clean
#     ./runIntegrationTests.sh
#
# This harness runs that unit 10x, DOES NOT stop on first failure (so it measures
# the true flake rate), parses the failsafe summary each time, scans the shared
# Wildfly server.log, and classifies every failure as one of:
#     code-flake        — failures/errors with normal duration, broker alive   (REAL signal)
#     deterministic     — the SAME test fails on a clean run (not flakiness)
#     broker-infra      — Artemis container exited mid-run / AMQ219019 cascade  (HOST artifact, discount)
#
# A run counts toward "X/10 green" only if it is a true PASS (exit 0, failures=0, errors=0).
# Broker-infra failures are reported separately so they neither pass silently nor hide a real flake.
#
# Env (must be set — see ~/.zprofile): CPP_DOCKER_DIR, CPP_ACR_REGISTRY, CPP_ACR_REGISTRY_PATH
# Optional knobs:
#     RUNS=10                  number of iterations
#     HARDEN=1                 1 = memory-gate + cooldown before each run (suppress broker starvation); 0 = plain
#     MIN_FREE_GB=4            memory-gate threshold (HARDEN=1 only)
#     COOLDOWN=30              seconds to wait before each run (HARDEN=1 only)
set -u

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_ROOT"

: "${CPP_DOCKER_DIR:?export CPP_DOCKER_DIR (cpp-developers-docker checkout)}"
: "${CPP_ACR_REGISTRY:?export CPP_ACR_REGISTRY}"
: "${CPP_ACR_REGISTRY_PATH:?export CPP_ACR_REGISTRY_PATH}"

RUNS="${RUNS:-10}"
HARDEN="${HARDEN:-1}"
MIN_FREE_GB="${MIN_FREE_GB:-4}"
COOLDOWN="${COOLDOWN:-30}"

SERVER_LOG="${CPP_DOCKER_DIR%/}/containers/wildfly/log/server.log"
SUMMARY_GLOB="listing-integration-test/target/failsafe-reports/failsafe-summary.xml"
TS="$(date +%Y%m%d-%H%M%S)"
OUTDIR="/tmp/it-10x-$TS"
mkdir -p "$OUTDIR"
REPORT="$OUTDIR/results.log"
STATUS="/tmp/it-10x-STATUS.txt"   # stable path for live tailing

# Known-noise allow-list (plan §8.3): these appear even on a green run.
NOISE='CrownUpdateHearingMultiday|JsonValue.NULL|StreamStatusLockingException'

log() { echo "$@" | tee -a "$REPORT"; }

log "================================================================"
log "run-it-10x.sh  repo=$REPO_ROOT  branch=$(git branch --show-current)"
log "             head=$(git rev-parse --short HEAD)  RUNS=$RUNS  HARDEN=$HARDEN"
log "             outdir=$OUTDIR  started=$(date '+%F %T %Z')"
log "================================================================"

pass=0 ; broker_fails=0 ; code_fails=0 ; total_flakes=0
declare -a FLAKY_TESTS=()
declare -a FLAKE_TESTS=()

free_gb() { awk '/MemAvailable/ {printf "%d", $2/1024/1024}' /proc/meminfo 2>/dev/null || echo 99; }

for i in $(seq 1 "$RUNS"); do
  log ""
  log "================ RUN $i/$RUNS  ($(date '+%T')) ================"

  if [[ "$HARDEN" == "1" ]]; then
    waited=0
    while [[ "$(free_gb)" -lt "$MIN_FREE_GB" && "$waited" -lt 300 ]]; do
      log "  memory-gate: only $(free_gb)GB free (<${MIN_FREE_GB}GB) — waiting…"
      sleep 15 ; waited=$((waited+15))
    done
    [[ "$COOLDOWN" -gt 0 ]] && { log "  cooldown ${COOLDOWN}s (free=$(free_gb)GB)…" ; sleep "$COOLDOWN" ; }
  fi

  # Step 1: force-remove leftover CPP dev containers + the wildfly-to-hap bridge.
  ids="$(docker ps -aq --filter "name=cpp-" --filter "name=wildfly-to-hap" 2>/dev/null)"
  [[ -n "$ids" ]] && docker rm -f $ids >/dev/null 2>&1
  # Step 2: purge target/.
  mvn -q clean >/dev/null 2>&1
  # Clean per-run failsafe summary + truncate the shared server.log for a clean scan.
  rm -f "$SUMMARY_GLOB" 2>/dev/null
  : > "$SERVER_LOG" 2>/dev/null || true

  # Step 3: run the suite.
  start=$(date +%s)
  ./runIntegrationTests.sh > "$OUTDIR/run-$i.out" 2>&1
  rc=$? ; dur=$(( $(date +%s) - start ))

  # Parse failsafe summary.
  errs="?" ; fails="?" ; comp="?"
  if [[ -f "$SUMMARY_GLOB" ]]; then
    # failsafe-summary.xml uses ELEMENT form (<errors>0</errors>); tolerate attribute form too.
    errs=$(grep -oE 'errors="[0-9]+"|<errors>[0-9]+</errors>'         "$SUMMARY_GLOB" | grep -oE '[0-9]+' | head -1)
    fails=$(grep -oE 'failures="[0-9]+"|<failures>[0-9]+</failures>'   "$SUMMARY_GLOB" | grep -oE '[0-9]+' | head -1)
    comp=$(grep -oE 'completed="[0-9]+"|<completed>[0-9]+</completed>' "$SUMMARY_GLOB" | grep -oE '[0-9]+' | head -1)
  fi

  # Rerun-masked flakes: the parent POM enables failsafe reruns, so a test can fail-then-pass and the
  # summary still shows failures=0. A rerun-masked green is NOT robust — surface it explicitly.
  flakes=$(grep -aoE 'Flakes: [0-9]+' "$OUTDIR/run-$i.out" 2>/dev/null | grep -oE '[0-9]+' | tail -1) ; flakes=${flakes:-0}
  flaky_this=$(grep -aoE 'Run [0-9]+: [A-Za-z0-9_]+IT\.[A-Za-z0-9_]+' "$OUTDIR/run-$i.out" 2>/dev/null \
               | grep -aoE '[A-Za-z0-9_]+IT\.[A-Za-z0-9_]+' | sort -u | paste -sd, - )

  # server.log error scan, excluding documented known-noise.
  logerr=$(grep -aiE 'ERROR|Exception' "$SERVER_LOG" 2>/dev/null | grep -vE "$NOISE" | wc -l | tr -d ' ')
  # broker death signature.
  broker_dead=0
  docker ps -a --filter "name=cpp-artemis" --format '{{.Status}}' 2>/dev/null | grep -qiE 'Exited' && broker_dead=1
  grep -qaE 'AMQ219019|Session is closed' "$OUTDIR/run-$i.out" 2>/dev/null && broker_dead=1

  # capture failing test names
  grep -aoE '[A-Za-z0-9_]+IT\.[A-Za-z0-9_]+' "$OUTDIR/run-$i.out" 2>/dev/null \
     | sort -u > "$OUTDIR/run-$i.failtests" || true
  failing_names=$(grep -aE '<<< (FAILURE|ERROR)|Tests run.*Failures: [1-9]' "$OUTDIR/run-$i.out" 2>/dev/null \
                  | grep -aoE '[A-Za-z0-9_]+IT(\.[A-Za-z0-9_]+)?' | sort -u | paste -sd, - )

  if [[ "$rc" -eq 0 && "${errs:-1}" == "0" && "${fails:-1}" == "0" ]]; then
    if [[ "$flakes" -gt 0 ]]; then
      status="PASS*flaky" ; pass=$((pass+1)) ; total_flakes=$((total_flakes+flakes))
      [[ -n "$flaky_this" ]] && FLAKE_TESTS+=("$flaky_this")
    else
      status="PASS" ; pass=$((pass+1))
    fi
  elif [[ "$broker_dead" == "1" || "$dur" -gt 900 ]]; then
    status="FAIL-broker" ; broker_fails=$((broker_fails+1))
    cp "$OUTDIR/run-$i.out" "$OUTDIR/FAIL-broker-$i.out"
  else
    status="FAIL-flake" ; code_fails=$((code_fails+1))
    cp "$OUTDIR/run-$i.out" "$OUTDIR/FAIL-flake-$i.out"
    [[ -n "$failing_names" ]] && FLAKY_TESTS+=("$failing_names")
  fi

  line=$(printf 'RUN %2d  %-12s completed=%s failures=%s errors=%s flakes=%s serverlog_err=%s broker_dead=%s  %ss  %s' \
         "$i" "$status" "${comp:-?}" "${fails:-?}" "${errs:-?}" "$flakes" "$logerr" "$broker_dead" "$dur" "${failing_names}${flaky_this:+ [rerun-masked: $flaky_this]}")
  log "$line"
  printf '%s\n' "$line" > "$STATUS"
done

log ""
log "================================================================"
log "RESULT: $pass/$RUNS green   (code-flake fails=$code_fails, broker-infra fails=$broker_fails, rerun-masked flakes=$total_flakes)"
if [[ ${#FLAKY_TESTS[@]} -gt 0 ]]; then
  log "Hard-failed tests observed:"
  printf '%s\n' "${FLAKY_TESTS[@]}" | tr ',' '\n' | sed '/^$/d' | sort | uniq -c | sort -rn | sed 's/^/  /' | tee -a "$REPORT"
fi
if [[ ${#FLAKE_TESTS[@]} -gt 0 ]]; then
  log "Rerun-masked flaky tests (failsafe-green but NOT robust):"
  printf '%s\n' "${FLAKE_TESTS[@]}" | tr ',' '\n' | sed '/^$/d' | sort | uniq -c | sort -rn | sed 's/^/  /' | tee -a "$REPORT"
fi
log "Report: $REPORT"
log "================================================================"

# Truly robust ONLY when every run is failsafe-green AND no run needed a rerun (0 masked flakes).
[[ "$pass" -eq "$RUNS" && "$total_flakes" -eq 0 ]] && exit 0 || exit 1
