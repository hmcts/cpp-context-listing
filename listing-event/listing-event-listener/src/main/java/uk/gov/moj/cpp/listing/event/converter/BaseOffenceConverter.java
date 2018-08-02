package uk.gov.moj.cpp.listing.event.converter;

import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.listing.persistence.entity.*;

import java.time.LocalDate;
import java.util.UUID;

import static uk.gov.justice.services.common.converter.LocalDates.*;

public class BaseOffenceConverter implements Converter<OffenceUpdated, BaseOffence> {

    @Override
    public BaseOffence convert(final OffenceUpdated event) {
        final uk.gov.justice.listing.events.Offence offence = event.getOffence();
        final CompositeOffenceId compositeOffenceId = creatCompositeOffenceId(event.getHearingId(), offence);
        final LocalDate endDate = offence.getEndDate().map(LocalDates::from).orElse(null);
        return new BaseOffenceBuilder()
                .setId(compositeOffenceId)
                .setEndDate(endDate)
                .setOffenceCode(offence.getOffenceCode())
                .setStartDate(from(offence.getStartDate()))
                .setStatementOfOffence(createStatementOfOffence(offence.getStatementOfOffence()))
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
