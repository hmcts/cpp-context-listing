package uk.gov.moj.cpp.listing.query.view.hearing;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartTimesJsonConverter {

    private static final String START_TIMES = "startTimes";
    private static final Logger LOGGER = LoggerFactory.getLogger(StartTimesJsonConverter.class);


    public List<ZonedDateTime> convertStartTimesFrom(String jsonStartTimes) {
        List<ZonedDateTime> startTimes = Collections.emptyList();
        if (jsonStartTimes != null && !jsonStartTimes.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();

                Map<String, List<String>> map = mapper.readValue(jsonStartTimes,
                        new TypeReference<Map<String, List<String>>>() {
                        });
                List<String> startTimesAsString = map.get(START_TIMES);
                if (startTimesAsString != null && !startTimesAsString.isEmpty()) {
                    startTimes = startTimesAsString.stream()
                            .map(st -> ZonedDateTime.parse(st))
                            .collect(Collectors.toList());
                }
            } catch (IOException e) {
                LOGGER.error("Cannot parse jsonStartTimes: {}  and exception is: {} ", jsonStartTimes, e);
            }
        }
        return startTimes;
    }
}
