package uk.gov.moj.cpp.listing.command.hearing.rules.rules;

import java.time.LocalDate;

public class HearingEndDateRule {

    private HearingEndDateRule() {}

    public static LocalDate apply(LocalDate endDate, LocalDate startDate) {
        return endDate != null ? endDate: startDate;
    }
}