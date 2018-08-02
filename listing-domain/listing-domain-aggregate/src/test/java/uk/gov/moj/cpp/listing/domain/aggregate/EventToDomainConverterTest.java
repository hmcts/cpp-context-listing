package uk.gov.moj.cpp.listing.domain.aggregate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.listing.domain.aggregate.utils.EventBuilder.buildHearings;

import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;

import java.util.List;

import org.junit.Test;

public class EventToDomainConverterTest {

    @Test
    public void shouldConvertHearingEventToDomain() {
        //given
        List<Hearing> eventHearings = buildHearings();
        Hearing expectedEventHearing = eventHearings.get(0);
        Defendant expectedEventDefendant = expectedEventHearing.getDefendants().get(0);
        Offence expectedEventOffence = expectedEventDefendant.getOffences().get(0);
        StatementOfOffence expectedEventStatementOfOffence = expectedEventOffence.getStatementOfOffence();


        //when
        List<uk.gov.moj.cpp.listing.domain.Hearing> domainHearings =  EventToDomainConverter.createHearingsFrom(eventHearings);

        //then
        assertThat(domainHearings.size(), is(1));
        uk.gov.moj.cpp.listing.domain.Hearing domainHearing = domainHearings.get(0);
        assertThat(domainHearing.getId(), is(expectedEventHearing.getId().toString()));
        assertThat(domainHearing.getEstimateMinutes(), is(expectedEventHearing.getEstimateMinutes()));
        assertThat(domainHearing.getType(), is(expectedEventHearing.getType()));
        assertThat(domainHearing.getCaseId(), is(expectedEventHearing.getCaseId().toString()));
        assertThat(domainHearing.getCourtCentreId(), is(expectedEventHearing.getCourtCentreId().toString()));

        assertThat(domainHearing.getDefendants().size(), is(1));
        uk.gov.moj.cpp.listing.domain.Defendant domainDefendant = domainHearing.getDefendants().get(0);

        assertDefendant(domainDefendant, expectedEventDefendant);

        assertThat(domainDefendant.getOffences().size(), is(1));
        uk.gov.moj.cpp.listing.domain.Offence domainOffence = domainDefendant.getOffences().get(0);

        assertOffence(domainOffence, expectedEventOffence);

        uk.gov.moj.cpp.listing.domain.StatementOfOffence domainStatementOfOffence = domainOffence.getStatementOfOffence();
        assertStatementOfOffence(domainStatementOfOffence, expectedEventStatementOfOffence);

    }

    private void assertStatementOfOffence(uk.gov.moj.cpp.listing.domain.StatementOfOffence domainStatementOfOffence, StatementOfOffence expectedEventStatementOfOffence) {
        assertThat(domainStatementOfOffence.getLegislation(), is(expectedEventStatementOfOffence.getLegislation()));
        assertThat(domainStatementOfOffence.getTitle(), is(expectedEventStatementOfOffence.getTitle()));
    }

    private void assertOffence(uk.gov.moj.cpp.listing.domain.Offence domainOffence, Offence expectedEventOffence) {
        assertThat(domainOffence.getId(), is(expectedEventOffence.getId().toString()));
        assertThat(domainOffence.getStartDate().toString(), is(expectedEventOffence.getStartDate()));
        assertThat(domainOffence.getEndDate().toString(), is(expectedEventOffence.getEndDate().get()));
        assertThat(domainOffence.getOffenceCode(), is(expectedEventOffence.getOffenceCode()));
    }

    private void assertDefendant(uk.gov.moj.cpp.listing.domain.Defendant domainDefendant, Defendant expectedEventDefendant) {
        assertThat(domainDefendant.getId(), is(expectedEventDefendant.getId().toString()));
        assertThat(domainDefendant.getPersonId(), is(expectedEventDefendant.getPersonId().toString()));
        assertThat(domainDefendant.getFirstName(), is(expectedEventDefendant.getFirstName()));
        assertThat(domainDefendant.getLastName(), is(expectedEventDefendant.getLastName()));

        assertThat(domainDefendant.getBailStatus(), is(expectedEventDefendant.getBailStatus().toString()));
        assertThat(domainDefendant.getCustodyTimeLimit().toString(), is(expectedEventDefendant.getCustodyTimeLimit().get()));
        assertThat(domainDefendant.getDateOfBirth().toString(), is(expectedEventDefendant.getDateOfBirth()));
    }
}
