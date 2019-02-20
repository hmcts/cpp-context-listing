package uk.gov.moj.cpp.listing.event.processor.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantsForHearingCommandCollectionConverterTest {

    private UpdateDefendantsForHearingCommandCollectionConverter  updateDefendantsForHearingCommandCollectionConverter = new UpdateDefendantsForHearingCommandCollectionConverter();

    @Spy
    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @InjectMocks
    EventBuilder eventBuilder;

    @Test
    public void convertFromDefendantsToBeUpdatedEventToListOfUpdateDefendantsForHearingCommands() {
        //given
        DefendantsToBeUpdated defendantsToBeUpdated = eventBuilder.buildDefendantsToBeUpdated();

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
        assertThat(actualDefendant.getDateOfBirth(), is(eventDefendant.getDateOfBirth()));
        assertThat(actualDefendant.getFirstName(), is(eventDefendant.getFirstName()));
        assertThat(actualDefendant.getLastName(), is(eventDefendant.getLastName()));
        assertThat(actualDefendant.getOffences(), is(empty()));
        assertThat(actualDefendant.getBailStatus().get().toString(), is(eventDefendant.getBailStatus().get().toString()));
        assertThat(actualDefendant.getCustodyTimeLimit().get(), is(eventDefendant.getCustodyTimeLimit().get()));
        assertThat(actualDefendant.getDefenceOrganisation(), is(eventDefendant.getDefenceOrganisation()));
        assertThat(actualDefendant.getId(), is(eventDefendant.getId()));
        assertThat(actualDefendant.getDefenceOrganisation(), is(eventDefendant.getDefenceOrganisation()));
        assertThat(actualDefendant.getId(), is(eventDefendant.getId()));
    }
}