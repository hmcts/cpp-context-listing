package uk.gov.moj.cpp.listing.event.converter;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCaseBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.ListingCaseRepository;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingConverterTest {

    private static final int ESTIMATE_MINUTES = 15;
    private static final boolean ALLOCATED = true;

    @InjectMocks
    private HearingConverter hearingConverter;

    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ListingCaseRepository listingCaseRepository;


    @Test
    public void shouldConvertNewCaseToHearing() throws Exception {
        // Given
        when(listingCaseRepository.findBy(anyObject())).thenReturn(null);

        StatementOfOffence statementOfOffence = createStatementOfOffence();
        Offence offence = createOffence(statementOfOffence);
        Defendant defendant = createDefendant(offence);
        Hearing hearingPart = createHearing(defendant);
        CaseSentForListing event = createCaseSentForListing(hearingPart);

        // When
        uk.gov.moj.cpp.listing.persistence.entity.Hearing actual = hearingConverter.convert(event);

        // Then
        assertHearing(actual, hearingPart);
        assertEventCaseDataUsedToCreateHearing(actual, event);

        uk.gov.moj.cpp.listing.persistence.entity.Defendant defendantToTest =
                actual.getDefendants().toArray(new uk.gov.moj.cpp.listing.persistence.entity.Defendant[1])[0];
        assertDefendant(defendantToTest, defendant);

        uk.gov.moj.cpp.listing.persistence.entity.Offence offenceToTest = defendantToTest
                .getOffences().toArray(new uk.gov.moj.cpp.listing.persistence.entity.Offence[1])[0];
        assertOffence(offenceToTest, offence);
    }




    @Test
    public void shouldRetrieveExistingListingCaseAndConvertRestOfCaseDataToHearing() throws Exception {
        // Given
        ListingCase retrievedListingCase = createListingCase();
        given(listingCaseRepository.findBy(anyObject())).willReturn(retrievedListingCase);

        StatementOfOffence statementOfOffence = createStatementOfOffence();
        Offence offence = createOffence(statementOfOffence);
        Defendant defendant = createDefendant(offence);
        Hearing hearingPart = createHearing(defendant);
        CaseSentForListing event = createCaseSentForListing(hearingPart);

        // When
        uk.gov.moj.cpp.listing.persistence.entity.Hearing actual = hearingConverter.convert(event);

        // Then
        assertHearing(actual, hearingPart);
        assertRetrievedListingCaseDataUsedToCreateHearing(actual, retrievedListingCase);

        uk.gov.moj.cpp.listing.persistence.entity.Defendant defendantToTest =
                actual.getDefendants().toArray(new uk.gov.moj.cpp.listing.persistence.entity.Defendant[1])[0];
        assertDefendant(defendantToTest, defendant);


        uk.gov.moj.cpp.listing.persistence.entity.Offence offenceToTest = defendantToTest
                .getOffences().toArray(new uk.gov.moj.cpp.listing.persistence.entity.Offence[1])[0];
        assertOffence(offenceToTest, offence);


    }

    private ListingCase createListingCase() {
        ListingCaseBuilder listingCaseBuilder = new ListingCaseBuilder();
        listingCaseBuilder.setId(randomUUID())
                .setUrn(STRING.next())
                .setSendingCommittalDate(LocalDate.now());

        return listingCaseBuilder.build();
    }

    private CaseSentForListing createCaseSentForListing(final Hearing hearingPart) {
        return new CaseSentForListing(randomUUID().toString(), STRING.next(), LocalDate
                .now(), hearingPart);
    }

    private Defendant createDefendant(final Offence offence) {
        return new Defendant(randomUUID().toString(), randomUUID().toString(),STRING.next(), STRING
                .next(), LocalDate.now(), STRING.next(), STRING.next(), Collections.singletonList
                (offence));
    }

    private StatementOfOffence createStatementOfOffence() {
        return new StatementOfOffence(STRING.next(), STRING.next());
    }

    private Hearing createHearing(final Defendant defendant) {
        return new Hearing(randomUUID().toString(), STRING.next(), STRING.next(), LocalDate.now
                (), ESTIMATE_MINUTES, ALLOCATED, Collections.singletonList(defendant));
    }

    private Offence createOffence(final StatementOfOffence statementOfOffence) {
        return new Offence(randomUUID().toString(), STRING.next(), STRING.next(), LocalDate.now
                (), LocalDate.now(), statementOfOffence);
    }

    private void assertRetrievedListingCaseDataUsedToCreateHearing(final uk.gov.moj.cpp.listing.persistence.entity.Hearing actual,
                                                                   final ListingCase retrievedListingCase) {
        assertThat(actual.getListingCase().getId().toString(), is(retrievedListingCase.getId().toString()));
        assertThat(actual.getListingCase().getSendingCommittalDate(), is(retrievedListingCase.getSendingCommittalDate()));
        assertThat(actual.getListingCase().getUrn(), is(retrievedListingCase.getUrn()));
    }

    private void assertEventCaseDataUsedToCreateHearing(final uk.gov.moj.cpp.listing.persistence.entity.Hearing actual,
                                                        final CaseSentForListing event) {
        assertThat(actual.getListingCase().getId().toString(), is(event.getCaseId()));
        assertThat(actual.getListingCase().getSendingCommittalDate(), is(event.getSendingCommittalDate()));
        assertThat(actual.getListingCase().getUrn(), is(event.getUrn()));
    }

    private void assertHearing(final uk.gov.moj.cpp.listing.persistence.entity.Hearing actual, final Hearing hearingPart) {
        assertThat(actual.getCourtCentreId(), is(hearingPart.getCourtCentreId()));
        assertThat(actual.getEstimateMinutes(), is(hearingPart.getEstimateMinutes()));
        assertThat(actual.getId().toString(), is(hearingPart.getId()));
        assertThat(actual.getStartDateTime(), is(hearingPart.getStartDate()));
        assertThat(actual.getType(), is(hearingPart.getType()));
        assertThat(actual.getAllocated(), is(hearingPart.isAllocated()));
    }


    private void assertDefendant(final uk.gov.moj.cpp.listing.persistence.entity.Defendant actual, final  Defendant defendant) {
        assertThat(actual.getBailStatus(), is(defendant.getBailStatus()));
        assertThat(actual.getDateOfBirth(), is(defendant.getDateOfBirth()));
        assertThat(actual.getDefenceOrganisation(), is(defendant.getDefenceOrganisation()));
        assertThat(actual.getFirstName(), is(defendant.getFirstName()));
        assertThat(actual.getDefendantId().toString(), is(defendant.getId()));
        assertThat(actual.getLastName(), is(defendant.getLastName()));
        assertThat(actual.getPersonId().toString(), is(defendant.getPersonId()));
        assertTrue(! actual.getListingDefendantId().toString().equals(defendant.getId().toString()));
    }

    private void assertOffence(final uk.gov.moj.cpp.listing.persistence.entity.Offence actual, final Offence offence) {
        assertThat(actual.getEndDate(), is(offence.getEndDate()));
        assertThat(actual.getOffenceId().toString(), is(offence.getId()));
        assertThat(actual.getOffenceCode(), is(offence.getOffenceCode()));
        assertThat(actual.getPlea(), is(offence.getPlea()));
        assertThat(actual.getStartDate(), is(offence.getStartDate()));

        assertThat(actual.getStatementOfOffence().getLegislation(), is(offence
                .getStatementOfOffence().getLegislation()));
        assertThat(actual.getStatementOfOffence().getTitle(), is(offence
                .getStatementOfOffence().getTitle()));
    }

}