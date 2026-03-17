package uk.gov.justice.api.resource;

public record SessionAvailabilityValidationParams(
        String panel,
        String sessionStartDate,
        String sessionEndDate,
        String hearingStartTime,
        String oucodeL2Code,
        String ouCode,
        String courtRoomId,
        String courtRoomNumber,
        String businessType,
        String courtSession,
        Boolean isSlotBased,
        Boolean showOverbookedSlots,
        String pageSize,
        String pageNumber,
        Integer availableDurationMins,
        String status,
        Integer consecutiveDays,
        Boolean isWeekCommencing
) {}
