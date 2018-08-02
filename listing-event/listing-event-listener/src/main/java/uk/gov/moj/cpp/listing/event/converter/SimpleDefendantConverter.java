package uk.gov.moj.cpp.listing.event.converter;

import uk.gov.justice.listing.events.BaseDefendant;
import uk.gov.justice.listing.events.DefendantDetailsUpdated;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeDefendantId;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeDefendantIdBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.SimpleDefendant;
import uk.gov.moj.cpp.listing.persistence.entity.SimpleDefendantBuilder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class SimpleDefendantConverter implements Converter<DefendantDetailsUpdated, SimpleDefendant> {
    @Override
    public SimpleDefendant convert(final DefendantDetailsUpdated event) {
        final BaseDefendant defendant = event.getDefendant();
        final CompositeDefendantId compositeDefendantId = createCompositeDefendantId(event.getHearingId(), defendant);
        return new SimpleDefendantBuilder()
                .setCompositeDefendantId(compositeDefendantId)
                .setBailStatus(defendant.getBailStatus().toString())
                .setDateOfBirth(LocalDates.from(defendant.getDateOfBirth()))
                .setDefenceOrganisation(defendant.getDefenceOrganisation())
                .setFirstName(defendant.getFirstName())
                .setLastName(defendant.getLastName())
                .setPersonId(defendant.getPersonId())
                .setCustodyTimeLimit(getLocalDateOrNull(defendant.getCustodyTimeLimit()))
                .build();
    }

    private CompositeDefendantId createCompositeDefendantId(UUID hearingId, BaseDefendant defendant) {
        return new CompositeDefendantIdBuilder()
                    .setHearingId(hearingId)
                    .setDefendantId(defendant.getId())
                    .build();
    }


    private LocalDate getLocalDateOrNull(Optional<String> custodyTimeLimit) {
        return custodyTimeLimit.map(LocalDates::from).orElse(null);
    }

}
