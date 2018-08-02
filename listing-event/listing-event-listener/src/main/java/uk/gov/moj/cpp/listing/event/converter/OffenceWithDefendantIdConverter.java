package uk.gov.moj.cpp.listing.event.converter;

import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.persistence.entity.*;

import java.util.UUID;

import static uk.gov.justice.services.common.converter.LocalDates.from;

public class OffenceWithDefendantIdConverter implements Converter<OffenceAdded, OffenceWithDefendantId> {

    @Override
    public OffenceWithDefendantId convert(final OffenceAdded event) {
        final uk.gov.justice.listing.events.Offence offence = event.getOffence();
        final CompositeOffenceId compositeOffenceId = creatCompositeOffenceId(event.getHearingId(), offence);
        return new OffenceWithDefendantIdBuilder()
                .setId(compositeOffenceId)
                .setEndDate(from(offence.getEndDate().orElse(null)))
                .setOffenceCode(offence.getOffenceCode())
                .setStartDate(from(offence.getStartDate()))
                .setMappedDefendantId(offence.getDefendantId())
                .setStatementOfOffence(createStatementOfOffence(offence.getStatementOfOffence()))
                .setMappedHearingId(event.getHearingId())
                .build();
    }

    private CompositeOffenceId creatCompositeOffenceId(UUID hearingId, uk.gov.justice.listing.events.Offence offence) {
        return new CompositeOffenceIdBuilder()
                    .setDefendantId(offence.getDefendantId())
                    .setHearingId(hearingId)
                    .setOffenceId(offence.getId())
                    .build();
    }

    private StatementOfOffence createStatementOfOffence(uk.gov.justice.listing.events.StatementOfOffence sof) {
        return new StatementOfOffenceBuilder()
                .setLegislation(sof.getLegislation())
                .setTitle(sof.getTitle())
                .build();
    }
}
