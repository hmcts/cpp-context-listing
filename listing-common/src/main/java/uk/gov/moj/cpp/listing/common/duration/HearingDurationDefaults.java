package uk.gov.moj.cpp.listing.common.duration;

import static java.util.Objects.isNull;

import java.util.Map;

// Single source of truth for the "never 0 / never null" rule on hearing duration.
// Called from both command-api enrichment (list-court-hearing, list-next-hearings-v2, update)
// and command-handler unscheduled/split paths that bypass the enrichment orchestrator.
public final class HearingDurationDefaults {

    public static final int DEFAULT_MIN = 20;

    private HearingDurationDefaults() {
    }

    public static Integer resolveHearingTypeDuration(final String hearingTypeId,
                                                     final Map<String, Integer> hearingTypesIdDurationMap) {
        if (isNull(hearingTypesIdDurationMap) || hearingTypesIdDurationMap.isEmpty() || isNull(hearingTypeId)) {
            return DEFAULT_MIN;
        }
        final Integer duration = hearingTypesIdDurationMap.getOrDefault(hearingTypeId, DEFAULT_MIN);
        return coerceToValidDuration(duration);
    }

    public static Integer coerceToValidDuration(final Integer estimatedMinutes) {
        if (estimatedMinutes == null || estimatedMinutes == 0 || estimatedMinutes == 1) {
            return DEFAULT_MIN;
        }
        return estimatedMinutes;
    }
}
