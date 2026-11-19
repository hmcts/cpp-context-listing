#!/usr/bin/env sh
# Container entrypoint for the Spring Boot fat jar published by this repo.
# Looks for the jar in /app (when running in the prod container) and falls back
# to ./build/libs (when shelling into the image from a local checkout).
# JSON, like the application's own log lines (context/logging-standards.md: stdout is JSON only).
logmsg() {
    printf '{"timestamp":"%s","message":"%s","logger_name":"startup.sh","level":"%s"}\n' \
        "$(date -u '+%Y-%m-%d %H:%M:%S.000')" "$1" "${2:-INFO}"
}

export LOCALJARFILE=$(ls ./build/libs/*.jar 2>/dev/null | grep -v 'plain' | head -n1)
export DOCKERJARFILE=$(ls /app/*.jar 2>/dev/null | grep -v 'plain' | head -n1)
# $JAVA_OPTS is supplied by the deployment (Helm values -> container env). It is
# intentionally unquoted so each space-separated flag becomes its own argument.
# exec replaces the shell so the JVM is PID 1 and receives SIGTERM directly for
# graceful shutdown.
if [ -f "$DOCKERJARFILE" ]; then
    logmsg "Running docker java jarfile $DOCKERJARFILE"
    exec java $JAVA_OPTS -jar "$DOCKERJARFILE"
elif [ -f "$LOCALJARFILE" ]; then
    logmsg "Running local java jarfile $LOCALJARFILE"
    exec java $JAVA_OPTS -jar "$LOCALJARFILE"
else
    logmsg "No jarfile found. Unable to start application" ERROR
    exit 1
fi
