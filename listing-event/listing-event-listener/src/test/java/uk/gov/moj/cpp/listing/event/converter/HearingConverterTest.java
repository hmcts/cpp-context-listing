package uk.gov.moj.cpp.listing.event.converter;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingConverterTest {

    public static final int ESTIMATE_MINUTES = 15;

    @InjectMocks
    private HearingConverter hearingConverter;

    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldConvertToHearing() throws Exception {

        StatementOfOffence statementOfOffence = new StatementOfOffence(STRING.next(), STRING.next());

        Offence offence = new Offence(randomUUID().toString(), STRING.next(), STRING.next(), LocalDate.now
                (), LocalDate.now(), statementOfOffence);

        Defendant defendant = new Defendant(randomUUID().toString(), randomUUID().toString(),STRING.next(), STRING
                .next(), LocalDate.now(), STRING.next(), STRING.next(), Collections.singletonList
                (offence));

        Hearing hearingPart = new Hearing(randomUUID().toString(), STRING.next(), STRING.next(), LocalDate.now
                (), ESTIMATE_MINUTES);

        CaseSentForListing event = new CaseSentForListing(randomUUID().toString(), STRING.next(), LocalDate
                .now(), Collections.singletonList(defendant), hearingPart);

        uk.gov.moj.cpp.listing.persistence.entity.Hearing hearing = hearingConverter.convert(event);

        assertThat(hearing.getCourtCentreId(), is(hearingPart.getCourtCentreId()));
        assertThat(hearing.getEstimateMinutes(), is(hearingPart.getEstimateMinutes()));
        assertThat(hearing.getId().toString(), is(hearingPart.getId()));
        assertThat(hearing.getStartDateTime(), is(hearingPart.getStartDate()));
        assertThat(hearing.getType(), is(hearingPart.getType()));

        assertThat(hearing.getListingCase().getId().toString(), is(event.getCaseId()));
        assertThat(hearing.getListingCase().getSendingCommittalDate(), is(event.getSendingCommittalDate()));
        assertThat(hearing.getListingCase().getUrn(), is(event.getUrn()));

        uk.gov.moj.cpp.listing.persistence.entity.Defendant defendantToTest =
                hearing.getListingCase().getDefendants().toArray(new uk.gov.moj.cpp.listing.persistence.entity.Defendant[1])[0];
        assertThat(defendantToTest.getBailStatus(), is(defendant.getBailStatus()));
        assertThat(defendantToTest.getDateOfBirth(), is(defendant.getDateOfBirth()));
        assertThat(defendantToTest.getDefenceOrganisation(), is(defendant.getDefenceOrganisation()));
        assertThat(defendantToTest.getFirstName(), is(defendant.getFirstName()));
        assertThat(defendantToTest.getId().toString(), is(defendant.getId()));
        assertThat(defendantToTest.getLastName(), is(defendant.getLastName()));
        assertThat(defendantToTest.getPersonId().toString(), is(defendant.getPersonId()));

        uk.gov.moj.cpp.listing.persistence.entity.Offence offenceToTest = defendantToTest
                .getOffences().toArray(new uk.gov.moj.cpp.listing.persistence.entity.Offence[1])[0];

        assertThat(offenceToTest.getEndDate(), is(offence.getEndDate()));
        assertThat(offenceToTest.getId().toString(), is(offence.getId()));
        assertThat(offenceToTest.getOffenceCode(), is(offence.getOffenceCode()));
        assertThat(offenceToTest.getPlea(), is(offence.getPlea()));
        assertThat(offenceToTest.getStartDate(), is(offence.getStartDate()));

        assertThat(offenceToTest.getStatementOfOffence().getLegislation(), is(offence
                .getStatementOfOffence().getLegislation()));
        assertThat(offenceToTest.getStatementOfOffence().getTitle(), is(offence
                .getStatementOfOffence().getTitle()));
    }

}