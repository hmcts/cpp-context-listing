package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.listing.domain.ReportingRestriction;

import java.util.ArrayList;
import java.util.List;

public class CommonHearingCommandConverter {

    protected List<ReportingRestriction> buildReportingRestrictions(final List<uk.gov.justice.listing.events.ReportingRestriction> reportingRestrictions) {

        final List<ReportingRestriction> reportingRestrictionDomainList = new ArrayList<>();
        reportingRestrictions.forEach(reportingRestriction ->
                reportingRestrictionDomainList.add(ReportingRestriction.reportingRestriction()
                        .withId(reportingRestriction.getId())
                        .withJudicialResultId(ofNullable(reportingRestriction.getJudicialResultId()))
                        .withLabel(reportingRestriction.getLabel())
                        .withOrderedDate(ofNullable(reportingRestriction.getOrderedDate()))
                        .build())
        );

        return reportingRestrictionDomainList;
    }
}
