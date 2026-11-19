package uk.gov.moj.cpp.listing.command.hearing.processor.command;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.SeedingHearing;

import java.util.Optional;

@Component
public class SeedingHearingConverter {

    private SeedingHearingConverter() {
    }

    public static  Optional<SeedingHearing> convertSeedingHearing(final uk.gov.justice.listing.events.SeedingHearing seedingHearing) {
            return Optional.of(SeedingHearing.seedingHearing()
                    .withJurisdictionType(JurisdictionType.valueOf(seedingHearing.getJurisdictionType().name()))
                    .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                    .withSittingDay(seedingHearing.getSittingDay())
                    .build());
    }

    public static  Optional<uk.gov.justice.core.courts.SeedingHearing> convertSeedingHearingForCoreDomain(final uk.gov.justice.listing.events.SeedingHearing seedingHearing) {
        return Optional.of(uk.gov.justice.core.courts.SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.valueOf(seedingHearing.getJurisdictionType().name()))
                .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                .withSittingDay(seedingHearing.getSittingDay())
                .build());
    }
}
