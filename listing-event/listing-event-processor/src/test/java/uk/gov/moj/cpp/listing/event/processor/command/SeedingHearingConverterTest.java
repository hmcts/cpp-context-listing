package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.events.SeedingHearing;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

public class SeedingHearingConverterTest {

    private static UUID SEEDING_HEARING_ID = randomUUID();
    private static String SITTING_DAY = LocalDate.now().toString();

    @Test
    public void shouldConvertSeedingHearing() {
        final uk.gov.moj.cpp.listing.domain.SeedingHearing seedingHearing = SeedingHearingConverter.convertSeedingHearing(buildSeedingHearing()).get();

        assertThat(seedingHearing.getSeedingHearingId(), is(SEEDING_HEARING_ID));
        assertThat(seedingHearing.getSittingDay(), is(SITTING_DAY));
        assertThat(seedingHearing.getJurisdictionType(), is(uk.gov.moj.cpp.listing.domain.JurisdictionType.CROWN));
    }

    @Test
    public void shouldConvertSeedingHearingForCoreDomain() {
        final uk.gov.justice.core.courts.SeedingHearing seedingHearing = SeedingHearingConverter.convertSeedingHearingForCoreDomain(buildSeedingHearing()).get();

        assertThat(seedingHearing.getSeedingHearingId(), is(SEEDING_HEARING_ID));
        assertThat(seedingHearing.getSittingDay(), is(SITTING_DAY));
        assertThat(seedingHearing.getJurisdictionType(), is(uk.gov.justice.core.courts.JurisdictionType.CROWN));
    }

    private SeedingHearing buildSeedingHearing() {
        return SeedingHearing.seedingHearing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withSeedingHearingId(SEEDING_HEARING_ID)
                .withSittingDay(SITTING_DAY)
                .build();
    }
}
