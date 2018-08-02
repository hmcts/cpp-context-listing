package uk.gov.moj.cpp.listing.event.converter;

import static javax.json.Json.createObjectBuilder;

import java.io.IOException;
import java.time.LocalDate;
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

public class NonSittingDaysJsonConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonSittingDaysJsonConverter.class);
    private static final String NON_SITTING_DAYS = "nonSittingDays";

    public List<LocalDate> convertNonSittingDaysFrom(String jsonNonSittingDays) {
        List<LocalDate> localDates = Collections.emptyList();
        try {
            ObjectMapper mapper = new ObjectMapper();

            Map<String, List<String>> map = mapper.readValue(jsonNonSittingDays, new TypeReference<Map<String, List<String>>>(){});
            List<String> nonSittingDaysAsStrings = map.get(NON_SITTING_DAYS);
            if(nonSittingDaysAsStrings!=null && !nonSittingDaysAsStrings.isEmpty()) {
                localDates = nonSittingDaysAsStrings.stream().map(st -> LocalDate.parse(st)).collect(Collectors.toList());
            }

        } catch ( IOException e) {
            LOGGER.error("Cannot parse jsonNonSittingDays: {} and exception is: {}", jsonNonSittingDays, e);
        }
        return localDates;

    }

    public String convertNonSittingDaysTo(List<LocalDate> nsd) {

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        nsd.forEach(d -> arrayBuilder.add(d.toString()));
        final JsonObjectBuilder nsdJsonStringBuilder = createObjectBuilder();

        nsdJsonStringBuilder.add(NON_SITTING_DAYS, arrayBuilder.build());
        return nsdJsonStringBuilder.build().toString();

    }

}
