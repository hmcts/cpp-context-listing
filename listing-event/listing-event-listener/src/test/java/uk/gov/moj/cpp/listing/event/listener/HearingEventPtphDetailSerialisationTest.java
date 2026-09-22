package uk.gov.moj.cpp.listing.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * {@code HearingEventListener.hearingListed} serialises the whole event hearing into the
 * {@code hearing.properties} jsonb column, and its own test mocks the mapper. This proves
 * with a real mapper that the PTPH fields actually reach that column.
 */
class HearingEventPtphDetailSerialisationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerialisePtphDetailFieldsIntoHearingProperties() {
        final UUID hearingId = randomUUID();
        final uk.gov.justice.listing.events.Hearing hearingEvent = uk.gov.justice.listing.events.Hearing.hearing()
                .withId(hearingId)
                .withTier("TIER_3")
                .withListType("TYPE_1_FIXED")
                .withKeyReason("Vulnerable witness")
                .build();

        final JsonNode properties = mapper.valueToTree(hearingEvent);

        assertThat(properties.get("tier").asText(), is("TIER_3"));
        assertThat(properties.get("listType").asText(), is("TYPE_1_FIXED"));
        assertThat(properties.get("keyReason").asText(), is("Vulnerable witness"));
    }

    /**
     * The generated event type carries no {@code @JsonInclude}, so unset fields are written
     * as explicit JSON nulls rather than omitted — a blank record, which is what the court
     * calendar must read as "nothing inherited".
     */
    @Test
    void shouldWriteNullPtphDetailFieldsWhenNotSet() {
        final uk.gov.justice.listing.events.Hearing hearingEvent = uk.gov.justice.listing.events.Hearing.hearing()
                .withId(randomUUID())
                .build();

        final JsonNode properties = mapper.valueToTree(hearingEvent);

        assertThat(properties.get("tier").isNull(), is(true));
        assertThat(properties.get("listType").isNull(), is(true));
        assertThat(properties.get("keyReason").isNull(), is(true));
    }
}
