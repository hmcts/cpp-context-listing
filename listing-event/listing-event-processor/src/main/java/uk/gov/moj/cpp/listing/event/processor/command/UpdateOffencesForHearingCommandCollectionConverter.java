package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.listing.domain.LaaReference;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;


public class UpdateOffencesForHearingCommandCollectionConverter implements Converter<OffencesToBeUpdated, List<UpdateOffencesForHearingCommand> > {

    @Inject
    private CommonHearingCommandConverter commonHearingCommandConverter;

    @Override
    public List<UpdateOffencesForHearingCommand>  convert(final OffencesToBeUpdated event) {

        final List <Offence> offences = convertOffences(event.getOffences());
        final UUID caseId = event.getCaseId();
        final UUID defendantId = event.getDefendantId();
        return event.getHearings().stream().map(hearingId ->
                new UpdateOffencesForHearingCommand(offences, hearingId, caseId, defendantId)).collect(toList());
    }

    private List<Offence> convertOffences(List<uk.gov.justice.listing.events.Offence> offences) {
        return offences.stream().map(offence -> {
            final LocalDate endDate = offence.getEndDate().map(LocalDates::from).orElse(null);
            final StatementOfOffence soo = convertStatementOfOffence(offence.getStatementOfOffence());
            final String strEndDate = endDate==null ? null : endDate.toString();
            final Offence.Builder offenceBuilder = Offence.offence()
                    .withEndDate(Optional.ofNullable(strEndDate))
                    .withId(offence.getId())
                    .withOffenceCode(offence.getOffenceCode())
                    .withStartDate(offence.getStartDate())
                    .withStatementOfOffence(soo)
                    .withOffenceWording(offence.getOffenceWording())
                    .withLaaApplnReference(offence.getLaaApplnReference().isPresent()? buildLaaReference(offence.getLaaApplnReference().get()):Optional.empty());

            if (isNotEmpty(offence.getReportingRestrictions())) {
                offenceBuilder.withReportingRestrictions(commonHearingCommandConverter.buildReportingRestrictions(offence.getReportingRestrictions()));
            }

            return offenceBuilder.build();
        }).collect(toList());
    }

    private StatementOfOffence convertStatementOfOffence(uk.gov.justice.listing.events.StatementOfOffence soo) {
        return StatementOfOffence.statementOfOffence()
                .withWelshTitle(soo.getWelshTitle())
                .withWelshLegislation(soo.getWelshLegislation())
                .withTitle(soo.getTitle())
                .withLegislation(soo.getLegislation())
                .build();

    }

    private Optional<LaaReference> buildLaaReference(final uk.gov.justice.listing.events.LaaReference laaReference) {

        return Optional.of(LaaReference.laaReference()
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveEndDate((laaReference.getEffectiveEndDate()))
                .withEffectiveStartDate((laaReference.getEffectiveStartDate()))
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusId(laaReference.getStatusId())
                .build());
    }
}
