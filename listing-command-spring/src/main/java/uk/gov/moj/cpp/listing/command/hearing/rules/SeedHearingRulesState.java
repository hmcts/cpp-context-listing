package uk.gov.moj.cpp.listing.command.hearing.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The JEE {@code SeedHearingAggregate}'s fields (MBD refactor P2.3.3, ADR 015), kept in
 * {@code seed_hearing_rules_state.state} ({@code RulesStateStore}). The two per-sitting-day maps are
 * kept as entry lists, as the sitting day can be {@code null} (a JSON object cannot have a null key).
 */
public record SeedHearingRulesState(UUID streamId,
                                    List<HearingDayEntry> seededHearingIdsForHearingDay,
                                    List<HearingDayEntry> existingHearingIdsForHearingDay,
                                    List<UUID> previousHearingIds,
                                    List<UUID> currentHearingIds,
                                    UUID seedingHearingId) {

    /** One map entry: the sitting day (may be null) and its hearing ids. */
    public record HearingDayEntry(String hearingDay, List<UUID> hearingIds) {
    }

    static List<HearingDayEntry> entries(final Map<String, Set<UUID>> map) {
        final List<HearingDayEntry> entries = new ArrayList<>();
        map.forEach((day, ids) -> entries.add(new HearingDayEntry(day, new ArrayList<>(ids))));
        return entries;
    }

    static Map<String, Set<UUID>> map(final List<HearingDayEntry> entries) {
        final Map<String, Set<UUID>> map = new HashMap<>();
        if (entries != null) {
            entries.forEach(entry -> map.put(entry.hearingDay(), entry.hearingIds() == null ? new HashSet<>() : new HashSet<>(entry.hearingIds())));
        }
        return map;
    }
}
