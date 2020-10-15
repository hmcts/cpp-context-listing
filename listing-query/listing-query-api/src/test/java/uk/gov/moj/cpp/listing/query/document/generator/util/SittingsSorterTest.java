package uk.gov.moj.cpp.listing.query.document.generator.util;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.listing.query.api.util.FileUtil;
import uk.gov.moj.cpp.listing.query.document.generator.courtlist.Sitting;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class SittingsSorterTest {

    private SittingsSorter sittingsSorter = new SittingsSorter();

    @Test
    public void sort() throws IOException {
        final String jsonString = FileUtil.getPayload("sittings.json");

        final ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        final List<Sitting> sittings = mapper.readValue(jsonString, new TypeReference<List<Sitting>>(){});

        sittingsSorter.sort(sittings);

        assertThat(sittings.get(0).getSittingTime(), equalTo("04:30 PM"));
        assertThat(sittings.get(1).getSittingTime(), equalTo("07:30 AM"));
        assertThat(sittings.get(2).getSittingTime(), equalTo("03:30 PM"));

        assertThat(sittings.get(1).getHearings().get(0).getHearingDay().getStartTime(), equalTo("08:00 AM"));
        assertThat(sittings.get(1).getHearings().get(1).getHearingDay().getStartTime(), equalTo("09:00 AM"));
    }
}
