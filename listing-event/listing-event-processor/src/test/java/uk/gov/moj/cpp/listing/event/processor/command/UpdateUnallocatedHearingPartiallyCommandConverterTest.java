package uk.gov.moj.cpp.listing.event.processor.command;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import uk.gov.justice.listing.events.HearingMarkedForPartialUpdate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateUnallocatedHearingPartiallyCommandConverterTest {

    private UpdateUnallocatedHearingPartiallyCommandConverter updateOffencesForHearingCommandCollectionConverter = new UpdateUnallocatedHearingPartiallyCommandConverter();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @InjectMocks
    private EventBuilder eventBuilder;

    @Test
    public void convertFromDefendantsToBeUpdatedEventToListOfUpdateDefendantsForHearingCommands() throws Exception {
        //given
        HearingMarkedForPartialUpdate hearingMarkedForPartialUpdate = eventBuilder.buildHearingMarkedForPartialUpdate();

        //when
        JsonObject actualList = updateOffencesForHearingCommandCollectionConverter.convertPartialUpdateEventToCommand(hearingMarkedForPartialUpdate);

        //then
        String expected =
                "{\n" +
                        "  \"hearingIdToBeUpdated\": \"298ce137-11bc-41dc-8274-484ca70ce5ee\",\n" +
                        "  \"prosecutionCasesToRemove\": [\n" +
                        "    {\n" +
                        "      \"caseId\": \"8d327d53-7a25-465c-bdcc-1d37cd3cf3b8\",\n" +
                        "      \"defendantsToRemove\": [\n" +
                        "        {\n" +
                        "          \"defendantId\": \"2e6e7bc4-1796-47cd-b56a-947e585884e4\",\n" +
                        "          \"offencesToRemove\": [\n" +
                        "            {\n" +
                        "              \"offenceId\": \"c5d79c65-c256-41bc-9016-23dd178a6dcb\"\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        assertEquals(expected, objectMapper.writeValueAsString(actualList), true);
    }
}