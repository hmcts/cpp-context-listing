package uk.gov.moj.cpp.listing.event.util;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.ReportingRestriction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ReportingRestrictionHelper {

    private ReportingRestrictionHelper() {

    }

    public static List<Offence> dedupAllReportingRestrictions(final List<Offence> updatedOffences) {
        if (updatedOffences == null) {
            return updatedOffences;
        }
        final ArrayList<Offence> res = new ArrayList<>();
        for (final Offence offence : updatedOffences) {
            res.add(dedupAllReportingRestrictions(offence));
        }
        return res;
    }

    public static Offence dedupAllReportingRestrictions(final Offence offence) {
        if (offence == null) {
            return offence;
        }

        return Offence.offence()
                .withValuesFrom(offence)
                .withReportingRestrictions(dedupReportingRestrictions(offence.getReportingRestrictions())).build();
    }

    public static Hearing dedupAllReportingRestrictions(final Hearing hearing) {
        if (hearing == null) {
            return hearing;
        }

        final List<ListedCase> listedCases = hearing.getListedCases() == null ? null : hearing.getListedCases()
                .stream().
                map(ReportingRestrictionHelper::dedupAllReportingRestrictions)
                .collect(toList());

        return Hearing.hearing().withValuesFrom(hearing)
                .withListedCases(listedCases)
                .build();
    }

    public static ListedCase dedupAllReportingRestrictions(final ListedCase listedCase) {
        if (listedCase == null) {
            return listedCase;
        }

        return ListedCase.listedCase().withValuesFrom(listedCase)
                .withDefendants(
                        listedCase.getDefendants().stream()
                                .map(d -> Defendant.defendant().withValuesFrom(d)
                                        .withOffences(dedupAllReportingRestrictions(d.getOffences()))
                                        .build())
                                .collect(toList())
                )
                .build();
    }

    public static CourtApplication dedupAllReportingRestrictions(final CourtApplication courtApplication) {
        if (courtApplication == null) {
            return courtApplication;
        }

        return CourtApplication.courtApplication().withValuesFrom(courtApplication)
                .withOffences(dedupAllReportingRestrictions(courtApplication.getOffences()))
                .build();
    }

    public static List<ReportingRestriction> dedupReportingRestrictions(final List<uk.gov.justice.listing.events.ReportingRestriction> reportingRestrictions) {
        if (reportingRestrictions == null) {
            return reportingRestrictions;
        }

        final Map<String, ReportingRestriction> res = new LinkedHashMap<>();
        for (final ReportingRestriction current : reportingRestrictions) {
            final String key = getKey(current);
            ReportingRestriction prev = res.get(key);
            if (prev == null) {
                prev = current;
            }

            res.put(key, oldestOf(prev, current));
        }

        return new ArrayList<>(res.values());
    }

    private static String getKey(final ReportingRestriction current) {
        final String uuid = Optional.<UUID>ofNullable(current.getJudicialResultId()).map(UUID::toString).orElse(null);
        return String.format("%s-%s", current.getLabel(), uuid);
    }

    @SuppressWarnings("squid:S3655")
    private static ReportingRestriction oldestOf(final ReportingRestriction reportingRestriction1, final ReportingRestriction reportingRestriction2) {
        if (reportingRestriction1.getOrderedDate()==null) {
            return reportingRestriction2;
        }

        if (reportingRestriction2.getOrderedDate()==null) {
            return reportingRestriction1;
        }

        if (reportingRestriction2.getOrderedDate().isBefore(reportingRestriction1.getOrderedDate())) {
            return reportingRestriction2;
        }

        return reportingRestriction1;
    }
}

