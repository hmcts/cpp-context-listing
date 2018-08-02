package uk.gov.moj.cpp.listing.event.processor.command;

import org.junit.Test;
import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;

public class UpdateDefendantsForHearingCommandCollectionConverterTest {

    private UpdateDefendantsForHearingCommandCollectionConverter  updateDefendantsForHearingCommandCollectionConverter = new UpdateDefendantsForHearingCommandCollectionConverter();

    @Test
    public void convertFromDefendantsToBeUpdatedEventToListOfUpdateDefendantsForHearingCommands() {
        //given
        DefendantsToBeUpdated defendantsToBeUpdated = EventBuilder.buildDefendantsToBeUpdated();

        //when
        List<UpdateDefendantsForHearingCommand> actualList = updateDefendantsForHearingCommandCollectionConverter.convert(defendantsToBeUpdated);

        //then
        assertThat(actualList.size(),is(1));
        UpdateDefendantsForHearingCommand actualCommand = actualList.get(0);
        assertThat(actualCommand.getHearingId(),is(defendantsToBeUpdated.getHearings().get(0)));
        assertThat(actualCommand.getDefendants().size(),is(1));
        Defendant actualDefendant = actualCommand.getDefendants().get(0);
        assertDefendant(actualDefendant, defendantsToBeUpdated);
    }

    private void assertDefendant(final Defendant actualDefendant, DefendantsToBeUpdated sourceEvent) {
        final uk.gov.justice.listing.events.Defendant eventDefendant = sourceEvent.getDefendants().get(0);
        assertThat(actualDefendant.getDateOfBirth(), is(LocalDates.from(eventDefendant.getDateOfBirth())));
        assertThat(actualDefendant.getFirstName(), is(eventDefendant.getFirstName()));
        assertThat(actualDefendant.getLastName(), is(eventDefendant.getLastName()));
        assertThat(actualDefendant.getOffences(), is(empty()));
        assertThat(actualDefendant.getBailStatus(), is(eventDefendant.getBailStatus().toString()));
        assertThat(actualDefendant.getCustodyTimeLimit(), is(LocalDates.from(eventDefendant.getCustodyTimeLimit().get())));
        assertThat(actualDefendant.getDefenceOrganisation(), is(eventDefendant.getDefenceOrganisation()));
        assertThat(actualDefendant.getId(), is(eventDefendant.getId().toString()));
        assertThat(actualDefendant.getDefenceOrganisation(), is(eventDefendant.getDefenceOrganisation()));
        assertThat(actualDefendant.getId(), is(eventDefendant.getId().toString()));
    }
}