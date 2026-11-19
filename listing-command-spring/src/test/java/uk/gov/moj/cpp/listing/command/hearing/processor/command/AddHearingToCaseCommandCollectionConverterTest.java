package uk.gov.moj.cpp.listing.command.hearing.processor.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.command.hearing.processor.utils.EventBuilder;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddHearingToCaseCommandCollectionConverterTest {

    private final AddHearingToCaseCommandCollectionConverter addHearingToCaseCommandCollectionConverter = new AddHearingToCaseCommandCollectionConverter();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @InjectMocks
    EventBuilder eventBuilder;

    @Test
    public void convert() {

        //given
        final HearingListed hearingListed = eventBuilder.buildHearingListed();

        //when
        final List<AddHearingToCaseCommand> actualList = addHearingToCaseCommandCollectionConverter.convert(hearingListed);

        //then
        assertThat(actualList.size(), is(1));
        final AddHearingToCaseCommand actualCommand = actualList.get(0);
        final Hearing listedHearing = hearingListed.getHearing();
        final ListedCase listedCase = listedHearing.getListedCases().get(0);

        assertThat(actualCommand.getCaseId(), is(listedCase.getId()));
        assertThat(actualCommand.getHearingId(), is(listedHearing.getId()));
    }
}