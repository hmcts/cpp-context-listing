package uk.gov.moj.cpp.listing.query.document.generator.util;

import static java.util.Locale.UK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Sitting;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SittingsSorterTest {

    private SittingsSorter sittingsSorter = new SittingsSorter();
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @BeforeAll
    public static void beforeClass() {
        //Not needed after AM/PM vs am/pm java.util.Locale issue is resolved in EA-11374
        Locale.setDefault(UK);
    }

    @Test
    public void sort() throws IOException {
        final String jsonString = FileUtil.getPayload("sittings.json");

        final Locale locale = Locale.getDefault();

        assertThat("Locale should be 'English (United Kingdom)'", locale.getDisplayName(), is("English (United Kingdom)"));

        final List<Sitting> sittings = objectMapper.readValue(jsonString, new TypeReference<>() {
        });

        sittingsSorter.sort(sittings);

        assertThat(sittings.get(0).getSittingTime(), equalToIgnoringCase("04:30 pm"));
        assertThat(sittings.get(1).getSittingTime(), equalToIgnoringCase("07:30 am"));
        assertThat(sittings.get(2).getSittingTime(), equalToIgnoringCase("03:30 pm"));

        assertThat(sittings.get(1).getHearings().get(0).getHearingDay().getStartTime(), equalToIgnoringCase("08:00 am"));
        assertThat(sittings.get(1).getHearings().get(1).getHearingDay().getStartTime(), equalToIgnoringCase("09:00 am"));
    }
}
