#!/usr/bin/env bash
# run-it-10x.sh — prove the listing IT suite is robust: 10 clean runs, 0 failures each.
#
# Per repo CLAUDE.md, one "run" is the 3-step unit of work:
#     docker rm -f $(docker ps -aq --filter name=cpp- --filter name=wildfly-to-hap)
#     mvn clean
#     ./runIntegrationTests.sh
#
# Behaviour:
#   - EXECUTION errors (non-zero exit, failsafe failures/errors, missing summary, broker death)
#     STOP the harness: the failure is reported and the script WAITS for manual resolution
#     (interactive prompt: retry the same run or abort). In a non-interactive session it exits 1
#     immediately. LOG errors (unexpected server.log ERROR/WARN lines) do NOT stop the harness —
#     they are reported per run and aggregated in the final summary.
#   - On first execution the Wildfly server.log is DELETED; before every run it is truncated and
#     after every run its content (= that run's slice) is appended to an aggregated copy:
#         $OUTDIR/server-log-aggregate.log   (one "#### RUN n" header per slice)
#   - Unexpected server.log errors come from the in-run detector
#     (ServerLogTestMarkerExtension -> listing-integration-test/target/unexpected-server-errors.txt,
#     @ExpectedServerErrors windows and known framework-race floor already filtered) and are shown
#     on each RUN line and under the final "RESULT: x/y green" summary.
#
# A run counts toward "X/10 green" only if it is a true PASS (exit 0, failures=0, errors=0).
# Rerun-masked flakes (failsafe-green only thanks to reruns) are surfaced separately and still
# fail the final robustness gate.
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
UNEXPECTED_FILE="listing-integration-test/target/unexpected-server-errors.txt"
TS="$(date +%Y%m%d-%H%M%S)"
OUTDIR="/tmp/it-10x-$TS"
mkdir -p "$OUTDIR"
REPORT="$OUTDIR/results.log"
AGGREGATE="$OUTDIR/server-log-aggregate.log"
STATUS="/tmp/it-10x-STATUS.txt"   # stable path for live tailing

log() { echo "$@" | tee -a "$REPORT"; }

# Echo the full in-run detector block (SERVER.LOG UNEXPECTED ERROR SUMMARY) into the harness
# output so it is visible between runs without opening run-N.out.
log_unexpected_block() {
  if [[ -f "$UNEXPECTED_FILE" ]]; then
    sed 's/^/    /' "$UNEXPECTED_FILE" | tee -a "$REPORT"
  else
    log "    (no unexpected-server-errors.txt — in-run detector absent: run died before the summary,"
    log "     or this branch lacks ServerLogTestMarkerExtension)"
  fi
}

log "================================================================"
log "run-it-10x.sh  repo=$REPO_ROOT  branch=$(git branch --show-current)"
log "             head=$(git rev-parse --short HEAD)  RUNS=$RUNS  HARDEN=$HARDEN"
log "             outdir=$OUTDIR  started=$(date '+%F %T %Z')"
log "             server.log aggregate: $AGGREGATE"
log "================================================================"

# First execution: delete the shared Wildfly server.log so the aggregate starts from zero.
rm -f "$SERVER_LOG" 2>/dev/null || true

pass=0 ; attempts=0 ; exec_failures=0 ; total_flakes=0
declare -a FLAKY_TESTS=()
declare -a FLAKE_TESTS=()
declare -a UNEXPECTED_ALL=()

free_gb() { awk '/MemAvailable/ {printf "%d", $2/1024/1024}' /proc/meminfo 2>/dev/null || echo 99; }

# Append the current server.log (this run's slice) to the aggregated copy.
append_aggregate() {
  local run_no="$1" run_status="$2"
  {
    echo ""
    echo "##############################################################"
    echo "#### RUN $run_no  ($(date '+%F %T'))  status=$run_status"
    echo "##############################################################"
    cat "$SERVER_LOG" 2>/dev/null || echo "(server.log unreadable)"
  } >> "$AGGREGATE"
}

# Execution failure -> block until a human resolves it (or exit 1 when non-interactive).
await_manual_resolution() {
  local run_no="$1" ; local reason="$2"
  log ""
  log "!!!! EXECUTION FAILURE on run $run_no: $reason"
  log "!!!! Inspect: $OUTDIR/run-$run_no.out   server.log slice: $AGGREGATE (RUN $run_no)"
  if [[ -t 0 ]]; then
    local ans=""
    read -rp ">>>> Manual resolution required. Press ENTER to retry run $run_no, or q+ENTER to abort: " ans
    if [[ "$ans" == "q" || "$ans" == "Q" ]]; then
      log "Aborted by operator after execution failure on run $run_no."
      exit 1
    fi
    log ">>>> Retrying run $run_no after manual resolution."
  else
    log "!!!! Non-interactive session — failing the entire harness (exit 1)."
    exit 1
  fi
}

i=1
while [[ "$i" -le "$RUNS" ]]; do
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
  # Clean per-run failsafe summary + truncate the shared server.log so this run's slice is isolated
  # (the previous slice is already in the aggregate).
  rm -f "$SUMMARY_GLOB" 2>/dev/null
  : > "$SERVER_LOG" 2>/dev/null || true

  # Step 3: run the suite.
  attempts=$((attempts+1))
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

  # Unexpected server.log errors from the in-run detector (@ExpectedServerErrors windows and the
  # framework-race floor are already filtered by ServerLogTestMarkerExtension).
  unexpected="?" ; unexpected_tests=""
  if [[ -f "$UNEXPECTED_FILE" ]]; then
    if grep -qa 'server.log CLEAN' "$UNEXPECTED_FILE"; then
      unexpected=0
    else
      unexpected_tests=$(grep -aE '^  [A-Za-z0-9_]+(IT|Scenario)\.[A-Za-z0-9_]+' "$UNEXPECTED_FILE" | sed 's/^  //' | sort -u | paste -sd, -)
      unexpected=$(grep -acE '^  [A-Za-z0-9_]+(IT|Scenario)\.[A-Za-z0-9_]+' "$UNEXPECTED_FILE")
      cp "$UNEXPECTED_FILE" "$OUTDIR/run-$i.unexpected-errors.txt" 2>/dev/null || true
    fi
  fi

  # broker death signature (classification only — every execution failure now stops the harness).
  broker_dead=0
  docker ps -a --filter "name=cpp-artemis" --format '{{.Status}}' 2>/dev/null | grep -qiE 'Exited' && broker_dead=1
  grep -qaE 'AMQ219019|Session is closed' "$OUTDIR/run-$i.out" 2>/dev/null && broker_dead=1

  failing_names=$(grep -aE '<<< (FAILURE|ERROR)|Tests run.*Failures: [1-9]' "$OUTDIR/run-$i.out" 2>/dev/null \
                  | grep -aoE '[A-Za-z0-9_]+IT(\.[A-Za-z0-9_]+)?' | sort -u | paste -sd, - )

  if [[ "$rc" -eq 0 && "${errs:-1}" == "0" && "${fails:-1}" == "0" ]]; then
    if [[ "$flakes" -gt 0 ]]; then
      status="PASS*flaky" ; pass=$((pass+1)) ; total_flakes=$((total_flakes+flakes))
      [[ -n "$flaky_this" ]] && FLAKE_TESTS+=("$flaky_this")
    else
      status="PASS" ; pass=$((pass+1))
    fi

    append_aggregate "$i" "$status"

    line=$(printf 'RUN %2d  %-12s completed=%s failures=%s errors=%s flakes=%s unexpected_log_errors=%s  %ss%s' \
           "$i" "$status" "${comp:-?}" "${fails:-?}" "${errs:-?}" "$flakes" "$unexpected" "$dur" \
           "${flaky_this:+ [rerun-masked: $flaky_this]}")
    log "$line"
    log_unexpected_block
    if [[ "$unexpected" != "0" && "$unexpected" != "?" && -n "$unexpected_tests" ]]; then
      log "        unexpected server.log errors in: $unexpected_tests"
      UNEXPECTED_ALL+=("RUN$i: $unexpected_tests")
    fi
    printf '%s\n' "$line" > "$STATUS"
    i=$((i+1))
  else
    exec_failures=$((exec_failures+1))
    [[ -n "$failing_names" ]] && FLAKY_TESTS+=("$failing_names")
    reason="exit=$rc failures=${fails:-?} errors=${errs:-?}"
    [[ "$broker_dead" == "1" ]] && reason="$reason (broker death signature)"
    [[ ! -f "$SUMMARY_GLOB" ]] && reason="$reason (no failsafe summary — run died early)"
    [[ -n "$failing_names" ]] && reason="$reason failing=[$failing_names]"

    append_aggregate "$i" "FAIL"

    line=$(printf 'RUN %2d  %-12s completed=%s failures=%s errors=%s flakes=%s unexpected_log_errors=%s  %ss  %s' \
           "$i" "EXEC-FAIL" "${comp:-?}" "${fails:-?}" "${errs:-?}" "$flakes" "$unexpected" "$dur" "$failing_names")
    log "$line"
    log_unexpected_block
    printf '%s\n' "$line" > "$STATUS"

    cp "$OUTDIR/run-$i.out" "$OUTDIR/FAIL-run-$i.out" 2>/dev/null || true
    await_manual_resolution "$i" "$reason"
    # retry the same run number (i unchanged)
  fi
done

log ""
log "================================================================"
log "RESULT: $pass/$RUNS green   (attempts=$attempts, execution failures manually resolved=$exec_failures, rerun-masked flakes=$total_flakes)"
if [[ ${#UNEXPECTED_ALL[@]} -gt 0 ]]; then
  log "Unexpected server.log errors across runs (per-run details in $OUTDIR/run-*.unexpected-errors.txt):"
  printf '%s\n' "${UNEXPECTED_ALL[@]}" | sed 's/^/  /' | tee -a "$REPORT"
  log "Most-affected tests:"
  printf '%s\n' "${UNEXPECTED_ALL[@]}" | sed 's/^RUN[0-9]*: //' | tr ',' '\n' | sed '/^$/d' | sort | uniq -c | sort -rn | sed 's/^/  /' | tee -a "$REPORT"
else
  log "Unexpected server.log errors across runs: NONE — every window clean or sanctioned by @ExpectedServerErrors."
fi
if [[ ${#FLAKY_TESTS[@]} -gt 0 ]]; then
  log "Hard-failed tests observed (before manual resolution):"
  printf '%s\n' "${FLAKY_TESTS[@]}" | tr ',' '\n' | sed '/^$/d' | sort | uniq -c | sort -rn | sed 's/^/  /' | tee -a "$REPORT"
fi
if [[ ${#FLAKE_TESTS[@]} -gt 0 ]]; then
  log "Rerun-masked flaky tests (failsafe-green but NOT robust):"
  printf '%s\n' "${FLAKE_TESTS[@]}" | tr ',' '\n' | sed '/^$/d' | sort | uniq -c | sort -rn | sed 's/^/  /' | tee -a "$REPORT"
fi
log "Aggregated server.log (all run slices): $AGGREGATE"
log "Report: $REPORT"
log "================================================================"

# Truly robust ONLY when every run is failsafe-green AND no run needed a rerun (0 masked flakes).
[[ "$pass" -eq "$RUNS" && "$total_flakes" -eq 0 ]] && exit 0 || exit 1
