package uk.gov.moj.cpp.listing.domain.aggregate.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

import uk.gov.moj.cpp.listing.domain.ReportingRestriction;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReportingRestrictionConverterTest {

    private static final UUID RR_ID = UUID.randomUUID();
    private static final UUID RR_JUDICIAL_RESULT_ID = UUID.randomUUID();
    private static final String RR_LABEL = "RR Label 1";
    private static final LocalDate RR_ORDERED_DATE = LocalDate.now();

    @Test
    public void shouldConvertCourtsToDomainObject() {
        final uk.gov.justice.core.courts.ReportingRestriction reportingRestrictionCourts = getCourtsRRObject();

        final ReportingRestriction reportingRestrictionDomain = ReportingRestrictionConverter.courtsToDomain(reportingRestrictionCourts);

        assertNotNull(reportingRestrictionDomain);
        assertThat(reportingRestrictionDomain.getId(), is(reportingRestrictionCourts.getId()));
        assertThat(reportingRestrictionDomain.getJudicialResultId(), is(reportingRestrictionCourts.getJudicialResultId()));
        assertThat(reportingRestrictionDomain.getLabel(), is(reportingRestrictionCourts.getLabel()));
        assertThat(reportingRestrictionDomain.getOrderedDate().get().toString(), is(reportingRestrictionCourts.getOrderedDate().get()));
    }

    @Test
    public void shouldConvertEventsToDomainObject() {
        final uk.gov.justice.listing.events.ReportingRestriction reportingRestrictionEvents = getEventsRRObject();

        final ReportingRestriction reportingRestrictionDomain = ReportingRestrictionConverter.eventsToDomain(reportingRestrictionEvents);

        assertNotNull(reportingRestrictionDomain);
        assertThat(reportingRestrictionDomain.getId(), is(reportingRestrictionEvents.getId()));
        assertThat(reportingRestrictionDomain.getJudicialResultId(), is(reportingRestrictionEvents.getJudicialResultId()));
        assertThat(reportingRestrictionDomain.getLabel(), is(reportingRestrictionEvents.getLabel()));
        assertThat(reportingRestrictionDomain.getOrderedDate().get(), is(reportingRestrictionEvents.getOrderedDate().get()));
    }

    @Test
    public void shouldConvertDomainToEventsObject() {
        final ReportingRestriction reportingRestrictionDomain = getDomainRRObject();

        final uk.gov.justice.listing.events.ReportingRestriction reportingRestrictionEvents = ReportingRestrictionConverter.domainToEvents(reportingRestrictionDomain);

        assertNotNull(reportingRestrictionEvents);
        assertThat(reportingRestrictionEvents.getId(), is(reportingRestrictionDomain.getId()));
        assertThat(reportingRestrictionEvents.getJudicialResultId(), is(reportingRestrictionDomain.getJudicialResultId()));
        assertThat(reportingRestrictionEvents.getLabel(), is(reportingRestrictionDomain.getLabel()));
        assertThat(reportingRestrictionEvents.getOrderedDate().get(), is(reportingRestrictionDomain.getOrderedDate().get()));
    }

    @Test
    public void shouldConvertCourtsToEventsObject() {
        final uk.gov.justice.core.courts.ReportingRestriction reportingRestrictionCourts = getCourtsRRObject();

        final uk.gov.justice.listing.events.ReportingRestriction reportingRestrictionEvents = ReportingRestrictionConverter.courtsToEvents(reportingRestrictionCourts);

        assertNotNull(reportingRestrictionEvents);
        assertThat(reportingRestrictionEvents.getId(), is(reportingRestrictionCourts.getId()));
        assertThat(reportingRestrictionEvents.getJudicialResultId(), is(reportingRestrictionCourts.getJudicialResultId()));
        assertThat(reportingRestrictionEvents.getLabel(), is(reportingRestrictionCourts.getLabel()));
        assertThat(reportingRestrictionEvents.getOrderedDate().get().toString(), is(reportingRestrictionCourts.getOrderedDate().get()));
    }

    @Test
    public void shouldConvertEventsToDomainObjectAsList() {
        final List<uk.gov.justice.listing.events.ReportingRestriction> reportingRestrictionEventsList = getEventsRRObjectAsList();

        final List<ReportingRestriction> reportingRestrictionDomainList = ReportingRestrictionConverter.eventsToDomainAsList(reportingRestrictionEventsList);

        assertNotNull(reportingRestrictionDomainList);
        assertThat(reportingRestrictionDomainList.size(), is(reportingRestrictionEventsList.size()));

        final uk.gov.justice.listing.events.ReportingRestriction reportingRestrictionEvents = reportingRestrictionEventsList.get(0);
        final ReportingRestriction reportingRestrictionDomain = reportingRestrictionDomainList.get(0);

        assertThat(reportingRestrictionDomain.getId(), is(reportingRestrictionEvents.getId()));
        assertThat(reportingRestrictionDomain.getJudicialResultId(), is(reportingRestrictionEvents.getJudicialResultId()));
        assertThat(reportingRestrictionDomain.getLabel(), is(reportingRestrictionEvents.getLabel()));
        assertThat(reportingRestrictionDomain.getOrderedDate().get(), is(reportingRestrictionEvents.getOrderedDate().get()));
    }

    private uk.gov.justice.core.courts.ReportingRestriction getCourtsRRObject() {
        return uk.gov.justice.core.courts.ReportingRestriction.reportingRestriction()
                .withId(RR_ID)
                .withJudicialResultId(RR_JUDICIAL_RESULT_ID)
                .withLabel(RR_LABEL)
                .withOrderedDate(RR_ORDERED_DATE.toString())
                .build();
    }

    private List<uk.gov.justice.listing.events.ReportingRestriction> getEventsRRObjectAsList() {
        return Arrays.asList(getEventsRRObject());
    }

    private uk.gov.justice.listing.events.ReportingRestriction getEventsRRObject() {
        return uk.gov.justice.listing.events.ReportingRestriction.reportingRestriction()
                .withId(RR_ID)
                .withJudicialResultId(RR_JUDICIAL_RESULT_ID)
                .withLabel(RR_LABEL)
                .withOrderedDate(RR_ORDERED_DATE)
                .build();
    }

    private ReportingRestriction getDomainRRObject() {
        return ReportingRestriction.reportingRestriction()
                .withId(RR_ID)
                .withJudicialResultId(Optional.of(RR_JUDICIAL_RESULT_ID))
                .withLabel(RR_LABEL)
                .withOrderedDate(Optional.of(RR_ORDERED_DATE))
                .build();
    }
}
