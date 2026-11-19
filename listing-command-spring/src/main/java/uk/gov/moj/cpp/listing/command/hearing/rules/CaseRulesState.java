package uk.gov.moj.cpp.listing.command.hearing.rules;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The JEE {@code Case} aggregate's fields (MBD refactor P2.3.3, ADR 015), kept in
 * {@code case_rules_state.state} ({@code RulesStateStore}): {@code hearingIds},
 * {@code currentDefendants} (defendant id → the defendant as last updated) and
 * {@code defendantsToBeUpdated} (the update waiting for the case's first hearing). None of them is
 * in the viewstore.
 */
public record CaseRulesState(UUID caseId, List<UUID> hearingIds, Map<UUID, uk.gov.justice.listing.events.Defendant> currentDefendants,
                             List<uk.gov.justice.listing.events.Defendant> defendantsToBeUpdated) {
}
