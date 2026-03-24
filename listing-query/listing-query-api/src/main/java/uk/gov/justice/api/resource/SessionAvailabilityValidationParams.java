package uk.gov.justice.api.resource;

import javax.json.JsonArray;

public record SessionAvailabilityValidationParams(
        JsonArray courtScheduleIdList,
        Integer duration,
        Integer consecutiveDays
) {}
