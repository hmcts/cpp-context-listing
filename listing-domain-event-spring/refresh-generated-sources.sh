#!/usr/bin/env bash
# Re-vendors the generated listing domain event POJOs (ADR 014). Runs the framework's
# pojo-generation-plugin through the Maven module listing-domain/listing-domain-event (offline by
# default) and copies its output into src/generated/java, minus the two enums listing-domain-spring owns.
# Review the diff: a changed serialVersionUID breaks the aggregate snapshots (ADR 011).
set -euo pipefail
cd "$(dirname "$0")/.."
mvn ${MVN_ARGS:--o} -q -B -pl listing-domain/listing-domain-event generate-sources
target=listing-domain-event-spring/src/generated/java
rm -rf "$target"
mkdir -p "$target"
rsync -a listing-domain/listing-domain-event/target/generated-sources/ "$target/"
rm "$target/uk/gov/justice/listing/event/PublishCourtListType.java" "$target/uk/gov/justice/listing/event/PublishStatus.java"
echo "Vendored $(find "$target" -name '*.java' | wc -l | tr -d ' ') classes into $target"
