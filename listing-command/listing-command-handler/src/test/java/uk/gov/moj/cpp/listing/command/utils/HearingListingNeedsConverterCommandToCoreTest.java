package uk.gov.moj.cpp.listing.command.utils;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.listing.commands.HearingListingNeeds;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * LPT-2405 — this converter copies field by field, so a field added to the carrier schema
 * but not copied here is silently dropped between the command API and the aggregate. These
 * tests cover both directions of each inherited field's null guard.
 */
class HearingListingNeedsConverterCommandToCoreTest {

    private final HearingListingNeedsConverterCommandToCore converter = new HearingListingNeedsConverterCommandToCore();

    private HearingListingNeeds.Builder minimalHearing() {
        return hearingListingNeeds()
                .withId(randomUUID())
                .withType(HearingType.hearingType().withId(randomUUID()).withDescription("Trial").build());
    }

    @Test
    void shouldCopyInheritedTierListTypeAndKeyReason() {
        final HearingListingNeeds source = minimalHearing()
                .withTier("TIER_3")
                .withListType("TYPE_1_FIXED")
                .withKeyReason("Vulnerable witness")
                .build();

        final HearingListingNeeds converted = converter.convert(source);

        assertThat(converted.getTier(), is("TIER_3"));
        assertThat(converted.getListType(), is("TYPE_1_FIXED"));
        assertThat(converted.getKeyReason(), is("Vulnerable witness"));
    }

    @Test
    void shouldLeaveInheritedFieldsNullWhenNothingWasInherited() {
        final HearingListingNeeds converted = converter.convert(minimalHearing().build());

        assertThat(converted.getTier(), is(nullValue()));
        assertThat(converted.getListType(), is(nullValue()));
        assertThat(converted.getKeyReason(), is(nullValue()));
    }

    /**
     * A flexible list type carries no key reason, so the partially-populated case must copy
     * what is present without inventing the rest.
     */
    @Test
    void shouldCopyTierAndListTypeWhenKeyReasonIsAbsent() {
        final HearingListingNeeds source = minimalHearing()
                .withTier("TIER_1")
                .withListType("TYPE_2_FLEXIBLE")
                .build();

        final HearingListingNeeds converted = converter.convert(source);

        assertThat(converted.getTier(), is("TIER_1"));
        assertThat(converted.getListType(), is("TYPE_2_FLEXIBLE"));
        assertThat(converted.getKeyReason(), is(nullValue()));
    }

    @Test
    void shouldPreserveTheHearingIdentityWhileCopying() {
        final UUID hearingId = randomUUID();
        final HearingListingNeeds source = hearingListingNeeds()
                .withId(hearingId)
                .withType(HearingType.hearingType().withId(randomUUID()).withDescription("Trial").build())
                .withTier("TIER_7")
                .build();

        final HearingListingNeeds converted = converter.convert(source);

        assertThat(converted.getId(), is(hearingId));
        assertThat(converted.getTier(), is("TIER_7"));
    }
}
