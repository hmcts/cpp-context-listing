package uk.gov.moj.cpp.listing.domain.aggregate;

import org.junit.Test;
import uk.gov.justice.listing.events.BaseDefendant;
import uk.gov.moj.cpp.listing.domain.*;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.utils.DomainBuilder;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.listing.domain.aggregate.utils.DomainBuilder.buildHearings;

public class DomainToEventConverterTest {

    @Test
    public void shouldConvertHearingDomainToHearingEvents() {
        //given
        List<Hearing> domainHearings = buildHearings();
        Hearing expectedDomainHearing = domainHearings.get(0);
        Defendant expectedDomainDefendant = expectedDomainHearing.getDefendants().get(0);
        Offence expectedDomainOffence = expectedDomainDefendant.getOffences().get(0);
        StatementOfOffence expectedDomainStatementOfOffence = expectedDomainOffence.getStatementOfOffence();

        //when
        List<uk.gov.justice.listing.events.Hearing> eventHearings = DomainToEventConverter.createHearingsFrom(domainHearings);

        //then
        assertThat(eventHearings.size(), is(1));
        uk.gov.justice.listing.events.Hearing eventHearing = eventHearings.get(0);
        assertThat(eventHearing.getId().toString(), is(expectedDomainHearing.getId()));
        assertThat(eventHearing.getEstimateMinutes(), is(expectedDomainHearing.getEstimateMinutes()));
        assertThat(eventHearing.getType(), is(expectedDomainHearing.getType()));
        assertThat(eventHearing.getCaseId().toString(), is(expectedDomainHearing.getCaseId()));
        assertThat(eventHearing.getCourtCentreId().toString(), is(expectedDomainHearing.getCourtCentreId()));

        assertThat(eventHearing.getDefendants().size(), is(1));
        uk.gov.justice.listing.events.Defendant eventDefendant = eventHearing.getDefendants().get(0);

        assertDefendant(eventDefendant, expectedDomainDefendant);

        assertThat(eventDefendant.getOffences().size(), is(1));
        uk.gov.justice.listing.events.Offence eventOffence = eventDefendant.getOffences().get(0);

        assertOffence(eventOffence, expectedDomainOffence);

        uk.gov.justice.listing.events.StatementOfOffence eventStatementOfOffence = eventOffence.getStatementOfOffence();
        assertStatementOfOffence(eventStatementOfOffence, expectedDomainStatementOfOffence);

    }

    @Test
    public void shouldConvertDefendantDomainToBaseDefendantEvent() {
        //given
        Defendant domainDefendant = DomainBuilder.buildDefendant();

        //when
        BaseDefendant eventDefendant = DomainToEventConverter.createBaseDefendantFrom(domainDefendant);

        //then
        assertBaseDefendant(eventDefendant, domainDefendant);
    }

    @Test
    public void shouldConvertDefendantDomainTODefendantEvent() {
        //given
        Defendant domainHearing = DomainBuilder.buildDefendant();

        //when
        uk.gov.justice.listing.events.Defendant eventDefendant = DomainToEventConverter.createDefendantFrom(domainHearing);

        //then
        assertDefendant(eventDefendant, domainHearing);
    }

    @Test
    public void shouldConvertCaseSimpleOffencesDomainToSimpleOffencesEvent() {
        //given
        CaseSimpleOffences caseSimpleOffences = DomainBuilder.buildCaseSimpleOffences();

        //when
        List<uk.gov.justice.listing.events.SimpleOffence> simpleOffences = DomainToEventConverter.createDeletedOffencesFrom(caseSimpleOffences);

        //then
        assertSimpleOffence(simpleOffences.get(0), caseSimpleOffences.getOffences().get(0));
    }

    @Test
    public void shouldConvertCaseOffencesDomainToOffenceEvent() {
        //given
        CaseOffences caseOffences = DomainBuilder.buildCaseOffences();

        //when
        List<uk.gov.justice.listing.events.Offence> offences = DomainToEventConverter.createOffencesFrom(caseOffences);

        //then
        assertOffence(offences.get(0), caseOffences.getOffences().get(0));
    }

    private void assertStatementOfOffence(uk.gov.justice.listing.events.StatementOfOffence eventStatementOfOffence, StatementOfOffence expectedDomainStatementOfOffence) {
        assertThat(eventStatementOfOffence.getLegislation(), is(expectedDomainStatementOfOffence.getLegislation()));
        assertThat(eventStatementOfOffence.getTitle(), is(expectedDomainStatementOfOffence.getTitle()));
    }

    private void assertSimpleOffence(uk.gov.justice.listing.events.SimpleOffence eventOffence, SimpleOffence expectedDomainOffence) {
        assertThat(eventOffence.getId().toString(), is(expectedDomainOffence.getId()));
        assertThat(eventOffence.getDefendantId().toString(), is(expectedDomainOffence.getDefendantId()));
    }

    private void assertOffence(uk.gov.justice.listing.events.Offence eventOffence, Offence expectedDomainOffence) {
        assertThat(eventOffence.getId().toString(), is(expectedDomainOffence.getId()));
        assertThat(eventOffence.getStartDate(), is(expectedDomainOffence.getStartDate().toString()));
        assertThat(eventOffence.getEndDate(), is(Optional.ofNullable(expectedDomainOffence.getEndDate().toString())));
        assertThat(eventOffence.getOffenceCode(), is(expectedDomainOffence.getOffenceCode()));
        assertThat(eventOffence.getDefendantId().toString(), is(expectedDomainOffence.getDefendantId()));
    }

    private void assertDefendant(uk.gov.justice.listing.events.Defendant eventDefendant, Defendant expectedDomainDefendant) {
        assertThat(eventDefendant.getId().toString(), is(expectedDomainDefendant.getId()));
        assertThat(eventDefendant.getPersonId().toString(), is(expectedDomainDefendant.getPersonId()));
        assertThat(eventDefendant.getFirstName(), is(expectedDomainDefendant.getFirstName()));
        assertThat(eventDefendant.getLastName(), is(expectedDomainDefendant.getLastName()));

        assertThat(eventDefendant.getBailStatus().toString(), is(expectedDomainDefendant.getBailStatus()));
        assertThat(eventDefendant.getCustodyTimeLimit().get(), is(expectedDomainDefendant.getCustodyTimeLimit().toString()));
        assertThat(eventDefendant.getDateOfBirth(), is(expectedDomainDefendant.getDateOfBirth().toString()));
    }

    private void assertBaseDefendant(uk.gov.justice.listing.events.BaseDefendant eventDefendant, Defendant expectedDomainDefendant) {
        assertThat(eventDefendant.getId().toString(), is(expectedDomainDefendant.getId()));
        assertThat(eventDefendant.getPersonId().toString(), is(expectedDomainDefendant.getPersonId()));
        assertThat(eventDefendant.getFirstName(), is(expectedDomainDefendant.getFirstName()));
        assertThat(eventDefendant.getLastName(), is(expectedDomainDefendant.getLastName()));

        assertThat(eventDefendant.getBailStatus().toString(), is(expectedDomainDefendant.getBailStatus()));
        assertThat(eventDefendant.getCustodyTimeLimit().get(), is(expectedDomainDefendant.getCustodyTimeLimit().toString()));
        assertThat(eventDefendant.getDateOfBirth(), is(expectedDomainDefendant.getDateOfBirth().toString()));
    }
}
