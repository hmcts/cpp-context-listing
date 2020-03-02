package uk.gov.moj.cpp.listing.event.processor.command;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import uk.gov.justice.listing.events.OffencesToBeAdded;
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
public class AddOffencesForHearingCommandCollectionConverterTest {

    private AddOffencesForHearingCommandCollectionConverter  addOffencesForHearingCommandCollectionConverter = new AddOffencesForHearingCommandCollectionConverter();

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
        OffencesToBeAdded event = eventBuilder.buildOffencesToBeAdded();

        //when
        List<AddOffencesForHearingCommand> actualList = addOffencesForHearingCommandCollectionConverter.convert(event);

        //then
        String expected =
                "[\n" +
                        "  {\n" +
                        "    \"offences\": [\n" +
                        "      {\n" +
                        "        \"endDate\": \"2017-08-01\",\n" +
                        "        \"id\": \"0baecac5-222b-402d-9047-84803679edad\",\n" +
                        "        \"offenceCode\": \"OF61131\",\n" +
                        "        \"startDate\": \"2016-06-21\",\n" +
                        "        \"statementOfOffence\": {\n" +
                        "          \"legislation\": \"legislation\",\n" +
                        "          \"title\": \"Wounding with intent\",\n" +
                        "          \"welshLegislation\": \"legislation in welsh\",\n" +
                        "          \"welshTitle\": \"Wounding with intent in welsh\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"hearingId\": \"0baecac5-222b-402d-9047-84803679edaf\"\n" +
                        "  }\n" +
                        "]\n";

        assertEquals(expected, objectMapper.writeValueAsString(actualList), true);
    }
}