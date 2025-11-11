package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.listing.domain.CivilOffence;
import uk.gov.moj.cpp.listing.domain.LaaReference;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;


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

    @SuppressWarnings("squid:S1188")
    private List<Offence> convertOffences(List<uk.gov.justice.listing.events.Offence> offences) {
        return offences.stream().map(offence -> {
            final LocalDate endDate = StringUtils.isNotEmpty(offence.getEndDate()) ? LocalDates.from(offence.getEndDate()) : null;
            final StatementOfOffence soo = convertStatementOfOffence(offence.getStatementOfOffence());
            final String strEndDate = endDate==null ? null : endDate.toString();
            final Offence.Builder offenceBuilder = Offence.offence()
                    .withEndDate(ofNullable(strEndDate))
                    .withId(offence.getId())
                    .withOffenceCode(offence.getOffenceCode())
                    .withStartDate(offence.getStartDate())
                    .withCount(offence.getCount())
                    .withIndictmentParticular(offence.getIndictmentParticular())
                    .withOrderIndex(offence.getOrderIndex())
                    .withStatementOfOffence(soo)
                    .withOffenceWording(offence.getOffenceWording())
                    .withSeedingHearing(nonNull(offence.getSeedingHearing()) ? SeedingHearingConverter.convertSeedingHearing(offence.getSeedingHearing()) : empty())
                    .withLaaApplnReference(nonNull(offence.getLaaApplnReference())? buildLaaReference(offence.getLaaApplnReference()): empty());

            if (isNotEmpty(offence.getReportingRestrictions())) {
                offenceBuilder.withReportingRestrictions(commonHearingCommandConverter.buildReportingRestrictions(offence.getReportingRestrictions()));
            }

            if(nonNull(offence.getCivilOffence())){
                offenceBuilder.withCivilOffence(CivilOffence.civilOffence()
                                .withIsExParte(offence.getCivilOffence().getIsExParte())
                        .build());
            }

            return offenceBuilder.build();
        }).collect(toList());
    }

    private StatementOfOffence convertStatementOfOffence(uk.gov.justice.listing.events.StatementOfOffence soo) {
        return StatementOfOffence.statementOfOffence()
                .withWelshTitle(soo.getWelshTitle())
                .withWelshLegislation(ofNullable(soo.getWelshLegislation()))
                .withTitle(soo.getTitle())
                .withLegislation(ofNullable(soo.getLegislation()))
                .build();

    }

    private Optional<LaaReference> buildLaaReference(final uk.gov.justice.listing.events.LaaReference laaReference) {

        return Optional.of(LaaReference.laaReference()
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveEndDate(ofNullable(laaReference.getEffectiveEndDate()))
                .withEffectiveStartDate(ofNullable(laaReference.getEffectiveStartDate()))
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusId(laaReference.getStatusId())
                .build());
    }

}
