package uk.gov.moj.cpp.listing.query.view.dto;

import java.util.Optional;

public record RangeSearchQueryParams(
    boolean allocated,
    String courtCentreId,
    String courtRoomId,
    String authorityId,
    String hearingTypeId,
    String jurisdictionType,
    String ouCode,
    String startDate,
    String endDate,
    String weekCommencingStartDate,
    String weekCommencingEndDate,
    PaginationParameter paginationParameter,
    boolean noPagination,
    Optional<String> businessType,
    Optional<String> courtSessionOptional,
    Optional<Boolean> possibleDisqualificationOpt ) {

    public RangeSearchQueryParams withWeekCommencingStartDate(String weekCommencingStartDate) {
        return new RangeSearchQueryParams(
            allocated,
            courtCentreId,
            courtRoomId,
            authorityId,
            hearingTypeId,
            jurisdictionType,
            ouCode,
            startDate,
            endDate,
            weekCommencingStartDate,
            weekCommencingEndDate,
            paginationParameter,
            noPagination,
            businessType,
            courtSessionOptional,
            possibleDisqualificationOpt
        );
    }
}