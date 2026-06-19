package uk.gov.moj.cpp.listing.event.util;

import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.event.util.ReportingRestrictionHelper.dedupReportingRestrictions;
import static uk.gov.moj.cpp.listing.event.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;

import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.ReportingRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class ReportingRestrictionHelperTest {

    @Test
    public void testDedupAllReportingRestrictions(){
        final UUID judicialResultId = UUID.randomUUID();
        final List<ReportingRestriction> reportingRestrictions = new ArrayList<>();
        final ReportingRestriction reportingRestriction = ReportingRestriction.reportingRestriction().withJudicialResultId(judicialResultId).withLabel("label").build();
        reportingRestrictions.add(reportingRestriction);
        final Offence offence = Offence.offence().withReportingRestrictions(reportingRestrictions).build();
        final List<Offence> updatedOffences = new ArrayList<>();
        updatedOffences.add(offence);
        final List<Offence> resultOffences = dedupAllReportingRestrictions(updatedOffences);
        assertThat(resultOffences.size(), is(1));
        assertThat(resultOffences.get(0).getReportingRestrictions().get(0).getLabel(), is("label"));
        assertThat(resultOffences.get(0).getReportingRestrictions().get(0).getJudicialResultId(), is(judicialResultId));
    }

    @Test
    public void testDedupAllReportingRestrictionsForHearing(){
        final UUID judicialResultId = UUID.randomUUID();
        final List<ReportingRestriction> reportingRestrictions = new ArrayList<>();
        final ReportingRestriction reportingRestriction = ReportingRestriction.reportingRestriction().withJudicialResultId(judicialResultId).withLabel("label").build();
        reportingRestrictions.add(reportingRestriction);
        final Offence offence = Offence.offence().withReportingRestrictions(reportingRestrictions).build();
        final List<Offence> updatedOffences = new ArrayList<>();
        updatedOffences.add(offence);
        final List<ListedCase> listedCases = new ArrayList<>();
        final List<Defendant> defendants = new ArrayList<>();
        final Defendant defendant = Defendant.defendant().withOffences(updatedOffences).build();
        defendants.add(defendant);
        final ListedCase listedCase = ListedCase.listedCase().withDefendants(defendants).build();
        listedCases.add(listedCase);
        final Hearing hearing = Hearing.hearing().withListedCases(listedCases).build();
        final Hearing result = dedupAllReportingRestrictions(hearing);
        assertThat(result.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictions().size(), is(1));
        assertThat(result.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictions().get(0).getLabel(), is("label"));
        assertThat(result.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictions().get(0).getJudicialResultId(), is(judicialResultId));
    }

    @Test
    public void testDedupReportingRestrictionsWithDifferentLabelAndDate() {
        final List<ReportingRestriction> input = of(newRR("A", LocalDate.now()), newRR("B", LocalDate.now()));
        final List<ReportingRestriction> actual = dedupReportingRestrictions(input);
        assertThat(actual, is(input));
    }

    @Test
    public void testDedupReportingRestrictionsWithSameLabelAndDate() {
        final List<ReportingRestriction> input = of(newRR("A", LocalDate.now()), newRR("A", LocalDate.now()));
        final List<ReportingRestriction> actual = dedupReportingRestrictions(input);
        assertThat(actual, is(of(input.get(0))));
    }

    @Test
    public void testDedupReportingRestrictionsWithSameLabelAndDifferentDateRetainsOldest() {
        final List<ReportingRestriction> input = of(newRR("A", LocalDate.now()), newRR("A", LocalDate.now().minusDays(7)), newRR("A", LocalDate.now().plusDays(7)));
        final List<ReportingRestriction> actual = dedupReportingRestrictions(input);
        assertThat(actual, is(of(input.get(1))));
    }

    @Test
    public void testDedupReportingRestrictionsWithSameLabelAndDifferentResultId() {
        final List<ReportingRestriction> input = of(newRR(UUID.randomUUID(), "A", LocalDate.now()), newRR(UUID.randomUUID(), "A", LocalDate.now()));
        final List<ReportingRestriction> actual = dedupReportingRestrictions(input);
        assertThat(actual, is(input));
    }

    private ReportingRestriction newRR(String label, LocalDate date) {
        return new ReportingRestriction(UUID.randomUUID(), null, label, date);
    }

    private ReportingRestriction newRR(UUID resultId, String label, LocalDate date) {
        return new ReportingRestriction(UUID.randomUUID(), resultId, label, date);
    }
}