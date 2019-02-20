package uk.gov.moj.cpp.listing.query.view.hearing;


import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import javax.json.JsonArray;
import java.util.List;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@RunWith(MockitoJUnitRunner.class)
public class HearingJsonListToArrayConverterTest {

    @InjectMocks
    private HearingJsonListToJsonArrayConverter converter;

    @Test
    public void shouldConvertToJsonArray() throws Exception {
        final List<Hearing> hearings = createHearings();

        JsonArray hearingJsonArray = converter.convert(hearings);
        assertThat(hearingJsonArray.toString(), isJson(allOf(
                withJsonPath("$", hasSize(2)),
                withJsonPath("$[0].hello", equalTo("world")),
                withJsonPath("$[1].foo", equalTo("bar"))

        )));
    }

    private List<Hearing> createHearings() {
        final Hearing hearing1 = new Hearing(UUID.randomUUID(), JacksonUtil.toJsonNode("{ \"hello\": \"world\" }"));
        final Hearing hearing2 = new Hearing(UUID.randomUUID(), JacksonUtil.toJsonNode("{ \"foo\": \"bar\" }"));
        return newArrayList(hearing1, hearing2);
    }

}