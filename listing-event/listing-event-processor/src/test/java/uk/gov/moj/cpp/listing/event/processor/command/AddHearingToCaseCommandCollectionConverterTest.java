package uk.gov.moj.cpp.listing.event.processor.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(MockitoJUnitRunner.class)
public class AddHearingToCaseCommandCollectionConverterTest {

    private final AddHearingToCaseCommandCollectionConverter addHearingToCaseCommandCollectionConverter = new AddHearingToCaseCommandCollectionConverter();

    @Spy
    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @InjectMocks
    EventBuilder eventBuilder;

    @Test
    public void convert() {

        //given
        HearingListed hearingListed = eventBuilder.buildHearingListed();

        //when
        List<AddHearingToCaseCommand> actualList = addHearingToCaseCommandCollectionConverter.convert(hearingListed);

        //then
        assertThat(actualList.size(),is(1));
        AddHearingToCaseCommand actualCommand = actualList.get(0);
        Hearing listedHearing = hearingListed.getHearing();
        ListedCase listedCase = listedHearing.getListedCases().get(0);

        assertThat(actualCommand.getCaseId(),is(listedCase.getId()));
        assertThat(actualCommand.getHearingId(),is(listedHearing.getId()));
    }
}