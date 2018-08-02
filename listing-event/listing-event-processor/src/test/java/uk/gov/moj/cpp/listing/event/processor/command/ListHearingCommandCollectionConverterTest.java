package uk.gov.moj.cpp.listing.event.processor.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.BAIL_STATUS;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.CASE_ID;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.COURT_CENTRE_ID;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.CUSTODY_TIME_LIMIT;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.DEFENCE_ORGANISATION;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.DEFENDANT_ID;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.DOB;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.ESTIMATE_MINUTES;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.FIRST_NAME;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.HEARING_END_DATE;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.HEARING_ID;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.HEARING_START_DATE;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.LAST_NAME;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.LEGISLATION;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.OFFENCE_CODE;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.OFFENCE_END_DATE;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.OFFENCE_ID;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.OFFENCE_START_DATE;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.PERSON_ID;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.TITLE;
import static uk.gov.moj.cpp.listing.event.utils.EventBuilder.TYPE;

import uk.gov.justice.listing.events.CaseSentForListing;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import java.util.List;

import org.junit.Test;

public class ListHearingCommandCollectionConverterTest {

    private ListHearingCommandCollectionConverter  listHearingCommandCollectionConverter = new ListHearingCommandCollectionConverter();

    @Test
    public void convert() {
        //given
        CaseSentForListing caseSentForListing = EventBuilder.buildCaseSentForListing();

        //when
        List<ListHearingCommand> actualList = listHearingCommandCollectionConverter.convert(caseSentForListing);

        //then
        assertThat(actualList.size(),is(1));
        ListHearingCommand actualCommand = actualList.get(0);
        assertThat(actualCommand.getCaseId(),is(CASE_ID.toString()));
        assertThat(actualCommand.getCourtCentreId(),is(COURT_CENTRE_ID.toString()));
        assertThat(actualCommand.getEstimateMinutes(),is(ESTIMATE_MINUTES));
        assertThat(actualCommand.getHearingId(),is(HEARING_ID.toString()));
        assertThat(actualCommand.getStartDate().toString(),is(HEARING_START_DATE));
        assertThat(actualCommand.getEndDate().toString(),is(HEARING_END_DATE));
        assertThat(actualCommand.getType(),is(TYPE));
        assertThat(actualCommand.getDefendants().size(),is(1));
        uk.gov.moj.cpp.listing.domain.Defendant actualDefendant = actualCommand.getDefendants().get(0);
        assertDefendant(actualDefendant);
        assertThat(actualDefendant.getOffences().size(), is(1));
        assertOffence(actualDefendant);
    }

    private void assertDefendant(final uk.gov.moj.cpp.listing.domain.Defendant actualDefendant) {
        assertThat(actualDefendant.getBailStatus(), is(BAIL_STATUS.toString()));
        assertThat(actualDefendant.getCustodyTimeLimit(), is(CUSTODY_TIME_LIMIT));
        assertThat(actualDefendant.getDefenceOrganisation(), is(DEFENCE_ORGANISATION));
        assertThat(actualDefendant.getId(), is(DEFENDANT_ID.toString()));
        assertThat(actualDefendant.getPersonId(), is(PERSON_ID.toString()));
        assertThat(actualDefendant.getDateOfBirth(), is(DOB));
        assertThat(actualDefendant.getFirstName(), is(FIRST_NAME));
        assertThat(actualDefendant.getLastName(), is(LAST_NAME));
    }

    private void assertOffence(final uk.gov.moj.cpp.listing.domain.Defendant atualDefendant) {
        uk.gov.moj.cpp.listing.domain.Offence actualOffence = atualDefendant.getOffences().get(0);
        assertThat(actualOffence.getEndDate(),is(OFFENCE_END_DATE));
        assertThat(actualOffence.getStartDate(),is(OFFENCE_START_DATE));
        assertThat(actualOffence.getId(),is(OFFENCE_ID.toString()));
        assertThat(actualOffence.getOffenceCode(),is(OFFENCE_CODE));
        assertThat(actualOffence.getStatementOfOffence().getLegislation(),is(LEGISLATION));
        assertThat(actualOffence.getStatementOfOffence().getTitle(),is(TITLE));
    }
}