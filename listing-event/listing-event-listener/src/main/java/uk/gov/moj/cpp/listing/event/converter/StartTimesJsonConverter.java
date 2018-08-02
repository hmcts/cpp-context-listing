package uk.gov.moj.cpp.listing.event.converter;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartTimesJsonConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartTimesJsonConverter.class);
    private static final String START_TIMES = "startTimes";

    public List<ZonedDateTime> convertStartTimesFrom(String jsonStartTimes) {
        List<ZonedDateTime> startTimes = Collections.emptyList();
        try {
            final ObjectMapper mapper = new ObjectMapper();

            final Map<String, List<String>> map = mapper.readValue(jsonStartTimes, new TypeReference<Map<String, List<String>>>(){});
            final List<String> startTimesAsStrings = map.get(START_TIMES);
            if(startTimesAsStrings!=null && !startTimesAsStrings.isEmpty()){
                startTimes = startTimesAsStrings.stream().map(st -> ZonedDateTime.parse(st)).collect(Collectors.toList());
            }
        } catch ( IOException e) {
            LOGGER.error("Cannot parse jsonStartTimes: {}  and exception is: {}", jsonStartTimes, e);
        }
        return startTimes;

    }

    public String convertStartTimesTo(List<ZonedDateTime> startTimes) {

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        startTimes.forEach(st -> arrayBuilder.add(ZonedDateTimes.toString(st)));
        final JsonObjectBuilder startTimesJsonStringBuilder = createObjectBuilder();

        startTimesJsonStringBuilder.add(START_TIMES, arrayBuilder.build());
        return startTimesJsonStringBuilder.build().toString();
    }





}
