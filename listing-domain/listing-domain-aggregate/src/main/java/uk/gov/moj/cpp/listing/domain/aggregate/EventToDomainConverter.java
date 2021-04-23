package uk.gov.moj.cpp.listing.domain.aggregate;


import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.SeedingHearing;



@SuppressWarnings({"squid:S3655", "pmd:NullAssignment", "squid:S1067", "squid:S2583"})
public class EventToDomainConverter {

    private EventToDomainConverter() {
    }

    public static  uk.gov.moj.cpp.listing.domain.OffenceIds buildOffenceIds(final uk.gov.justice.listing.events.Offence offence){
        return uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                .withId(offence.getId())
                .withSeedingHearing(offence.getSeedingHearing().isPresent() ? buildSeedingHearing(offence.getSeedingHearing().get()) : null)
                .build();
    }

    private static SeedingHearing buildSeedingHearing(final uk.gov.justice.listing.events.SeedingHearing seedingHearing) {
        return SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                .withSittingDay(seedingHearing.getSittingDay().orElse(null))
                .withJurisdictionType(JurisdictionType.valueOf(seedingHearing.getJurisdictionType().name()))
                .build();
    }

}
