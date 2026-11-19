package uk.gov.moj.cpp.listing.command.hearing.rules;

import java.util.List;
import java.util.UUID;

/**
 * The JEE {@code Application} aggregate's one field (MBD refactor P2.3.3, ADR 015), kept in
 * {@code application_rules_state.state} ({@code RulesStateStore}): {@code hearingIds}, in insertion
 * order, duplicates kept.
 */
public record ApplicationRulesState(UUID applicationId, List<UUID> hearingIds) {
}
