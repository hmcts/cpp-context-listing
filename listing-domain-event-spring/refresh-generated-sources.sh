#!/usr/bin/env bash
# Re-vendors the generated listing payload POJOs (ADR 014, ADR 015). Runs the framework's
# pojo-generation-plugin through the Maven modules listing-domain/listing-domain-event and
# listing-event/listing-event-processor (offline by default) and copies their output into
# src/generated/java:
#   - everything listing-domain-event generates, minus the two enums listing-domain-spring owns;
#   - the five uk.gov.justice.listing.courts.events classes listing-event-processor generates for
#     public.listing.court-list-published (CourtLists & co., the JEE public payload shape);
#   - (P2.3.2) the command/payload POJOs the command handler, the event processor and the command
#     API generate (uk.gov.justice.listing.commands/courts, progression/hearing payloads, …), for the
#     Hearing command services: every class not already vendored, first module wins in the order
#     handler, processor, api (the same FQCN can differ slightly between modules, e.g. a String
#     vs a UUID field; the handler's shape is the one the commands were converted with).
# The framework's @Event annotation is stripped (ADR 015: no private events, and
# framework-api-domain must not be on the Spring classpath).
set -euo pipefail
cd "$(dirname "$0")/.."
mvn ${MVN_ARGS:--o} -q -B -pl listing-domain/listing-domain-event,listing-event/listing-event-processor,listing-command/listing-command-handler,listing-command/listing-command-api generate-sources
target=listing-domain-event-spring/src/generated/java
rm -rf "$target"
mkdir -p "$target"
rsync -a listing-domain/listing-domain-event/target/generated-sources/ "$target/"
rm "$target/uk/gov/justice/listing/event/PublishCourtListType.java" "$target/uk/gov/justice/listing/event/PublishStatus.java"
mkdir -p "$target/uk/gov/justice/listing/courts/events"
for class in CaseIdentifier CourtLists CustodialEstablishment Hearings PublishCourtListType; do
  cp "listing-event/listing-event-processor/target/generated-sources/uk/gov/justice/listing/courts/events/${class}.java" \
     "$target/uk/gov/justice/listing/courts/events/"
done
for module in listing-command/listing-command-handler listing-event/listing-event-processor listing-command/listing-command-api; do
  rsync -a --ignore-existing --exclude 'uk/gov/justice/api/' --exclude 'annotations/' "$module/target/generated-sources/" "$target/"
done
grep -rl 'uk.gov.justice.domain.annotation.Event' "$target" | while read -r file; do
  sed -i.bak -e '/^import uk\.gov\.justice\.domain\.annotation\.Event;$/d' -e '/^@Event(".*")$/d' "$file" && rm "${file}.bak"
done
echo "Vendored $(find "$target" -name '*.java' | wc -l | tr -d ' ') classes into $target"
