package uk.gov.moj.cpp.listing.event.processor.command;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class deleteOffencesForHearingCommandCollectionConverterTest {

    private DeleteOffencesForHearingCommandCollectionConverter  deleteOffencesForHearingCommandCollectionConverter = new DeleteOffencesForHearingCommandCollectionConverter();

    @Spy
    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @InjectMocks
    EventBuilder eventBuilder;

    @Test
    public void convertFromDefendantsToBeUpdatedEventToListOfUpdateDefendantsForHearingCommands() throws Exception {
        //given
        OffencesToBeDeleted event = eventBuilder.buildOffencesToBeDeleted();

        //when
        List<DeleteOffencesForHearingCommand> actualList = deleteOffencesForHearingCommandCollectionConverter.convert(event);

        //then
        String expected =
                "[\n" +
                "  {\n" +
                "    \"offences\": [\n" +
                "      {\n" +
                "        \"id\": \"0baecac5-222b-402d-9047-84803679edad\",\n" +
                "        \"defendantId\": \"bd9f602d-428e-4aec-adee-91cc45d71ebf\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"hearingId\": \"0baecac5-222b-402d-9047-84803679edaf\"\n" +
                "  }\n" +
                "]\n";
        
        assertEquals(expected, objectMapper.writeValueAsString(actualList), true);
    }
}