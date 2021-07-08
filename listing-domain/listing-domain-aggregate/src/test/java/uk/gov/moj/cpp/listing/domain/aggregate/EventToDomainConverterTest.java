package uk.gov.moj.cpp.listing.domain.aggregate;


import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.events.SeedingHearing;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

public class EventToDomainConverterTest {


    @Test
    public void shouldConvertOffenceEventToDomain(){
        final UUID seedingHearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();
        final UUID offenceId = randomUUID();

        final uk.gov.moj.cpp.listing.domain.OffenceIds offence = EventToDomainConverter.buildOffenceIds(uk.gov.justice.listing.events.Offence.offence()
                .withId(offenceId)
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withSeedingHearingId(seedingHearingId)
                        .withSittingDay(sittingDay)
                        .build())
                .build());

        final uk.gov.moj.cpp.listing.domain.SeedingHearing seedingHearing = offence.getSeedingHearing();
        assertThat(seedingHearing.getJurisdictionType(), is(uk.gov.moj.cpp.listing.domain.JurisdictionType.CROWN));
        assertThat(seedingHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(seedingHearing.getSittingDay(), is(sittingDay));

        assertThat(offence.getId(), is(offenceId));
    }
}
