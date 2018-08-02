package uk.gov.moj.cpp.listing.query.view.hearing;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonSittingDaysJsonConverter {

    private static final String NON_SITTING_DAYS = "nonSittingDays";
    private static final Logger LOGGER = LoggerFactory.getLogger(NonSittingDaysJsonConverter.class);

    public List<LocalDate> convertNonSittingDays(String jsonNonSittingDays) {
        List<LocalDate> localDates = Collections.emptyList();
        if (jsonNonSittingDays != null && !jsonNonSittingDays.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();

                Map<String, List<String>> map = mapper.readValue(jsonNonSittingDays, new TypeReference<Map<String, List<String>>>() {
                });
                List<String> nonSittingDaysAsStrings = map.get(NON_SITTING_DAYS);
                if (nonSittingDaysAsStrings != null && !nonSittingDaysAsStrings.isEmpty()) {
                    localDates = nonSittingDaysAsStrings.stream()
                            .map(st -> LocalDate.parse(st))
                            .collect(Collectors.toList());
                }
            } catch ( IOException e) {
                LOGGER.error("Cannot parse jsonNonSittingDays: {}  and exception is: {}", jsonNonSittingDays, e);
            }
        }
        return localDates;
    }
}
