package uk.gov.moj.cpp.listing.event.processor.command;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddOffencesForHearingCommandCollectionConverterTest {

    private AddOffencesForHearingCommandCollectionConverter  addOffencesForHearingCommandCollectionConverter = new AddOffencesForHearingCommandCollectionConverter();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

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
                        "        \"count\": 1,\n" +
                        "        \"orderIndex\": 0,\n" +
                        "        \"statementOfOffence\": {\n" +
                        "          \"legislation\": \"legislation\",\n" +
                        "          \"title\": \"Wounding with intent\",\n" +
                        "          \"welshLegislation\": \"legislation in welsh\",\n" +
                        "          \"welshTitle\": \"Wounding with intent in welsh\"\n" +
                        "        },\n" +
                        "        \"seedingHearing\": {\n" +
                        "          \"seedingHearingId\": \"97834953-3f0e-4290-9cef-9e6c0a218469\",\n" +
                        "          \"jurisdictionType\": \"CROWN\",\n" +
                        "          \"sittingDay\": \"2016-06-22\"\n" +
                        "        },\n" +
                        "       \"laaApplnReference\": {\n" +
                        "           \"applicationReference\": \"AB746921\", \n" +
                        "           \"effectiveEndDate\": \"2010-09-01\", \n" +
                        "           \"effectiveStartDate\": \"2011-09-01\", \n" +
                        "           \"statusCode\": \"FPTP\", \n" +
                        "           \"statusDate\": \"2010-12-01\", \n" +
                        "           \"statusDescription\": \"Further Plea & Trial Preparation\", \n" +
                        "           \"statusId\": \"7e2f843e-d639-40b3-8611-8015f3a18612\", \n" +
                        "         }\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"hearingId\": \"0baecac5-222b-402d-9047-84803679edaf\"\n" +
                        "  }\n" +
                        "]\n";

        assertEquals(expected, objectMapper.writeValueAsString(actualList), true);
    }
}