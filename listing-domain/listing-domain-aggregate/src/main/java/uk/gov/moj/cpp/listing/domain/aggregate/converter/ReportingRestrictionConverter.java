package uk.gov.moj.cpp.listing.domain.aggregate.converter;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.listing.domain.ReportingRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ReportingRestrictionConverter {

    private ReportingRestrictionConverter() {}

    public static ReportingRestriction courtsToDomain(final uk.gov.justice.core.courts.ReportingRestriction reportingRestriction) {
        return ReportingRestriction.reportingRestriction()
                .withId(reportingRestriction.getId())
                .withLabel(reportingRestriction.getLabel())
                .withJudicialResultId(ofNullable(reportingRestriction.getJudicialResultId()))
                .withOrderedDate(ofNullable(getOrderedDate(reportingRestriction.getOrderedDate())))
                .build();
    }

    public static List<ReportingRestriction> eventsToDomainAsList(final List<uk.gov.justice.listing.events.ReportingRestriction> reportingRestrictions) {

        final List<ReportingRestriction> reportingRestrictionDomainList = new ArrayList<>();
        reportingRestrictions.forEach(reportingRestriction ->
                reportingRestrictionDomainList.add(eventsToDomain(reportingRestriction))
        );

        return reportingRestrictionDomainList;
    }

    public static ReportingRestriction eventsToDomain(final uk.gov.justice.listing.events.ReportingRestriction reportingRestriction) {
        return ReportingRestriction.reportingRestriction()
                .withId(reportingRestriction.getId())
                .withJudicialResultId(ofNullable(reportingRestriction.getJudicialResultId()))
                .withLabel(reportingRestriction.getLabel())
                .withOrderedDate(ofNullable(reportingRestriction.getOrderedDate()))
                .build();
    }

    public static uk.gov.justice.listing.events.ReportingRestriction courtsToEvents(final uk.gov.justice.core.courts.ReportingRestriction reportingRestriction) {
        return uk.gov.justice.listing.events.ReportingRestriction.reportingRestriction()
                .withId(reportingRestriction.getId())
                .withLabel(reportingRestriction.getLabel())
                .withJudicialResultId(reportingRestriction.getJudicialResultId())
                .withOrderedDate(getOrderedDate(reportingRestriction.getOrderedDate()))
                .build();
    }

    public static uk.gov.justice.listing.events.ReportingRestriction domainToEvents(final ReportingRestriction reportingRestriction) {
        return uk.gov.justice.listing.events.ReportingRestriction.reportingRestriction()
                .withId(reportingRestriction.getId())
                .withJudicialResultId(reportingRestriction.getJudicialResultId().orElse(null))
                .withLabel(reportingRestriction.getLabel())
                .withOrderedDate(reportingRestriction.getOrderedDate().orElse(null))
                .build();
    }

    private static LocalDate getOrderedDate(final String orderedDate) {
        return !StringUtils.isEmpty(orderedDate) ? LocalDate.parse(orderedDate) : null;
    }

}
