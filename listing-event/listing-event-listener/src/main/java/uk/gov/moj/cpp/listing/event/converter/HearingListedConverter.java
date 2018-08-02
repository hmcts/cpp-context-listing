package uk.gov.moj.cpp.listing.event.converter;

import static java.time.LocalDate.parse;
import static java.util.stream.Collectors.toSet;

import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeDefendantId;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeDefendantIdBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeOffenceId;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeOffenceIdBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.DefendantBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.Offence;
import uk.gov.moj.cpp.listing.persistence.entity.OffenceBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.StatementOfOffence;
import uk.gov.moj.cpp.listing.persistence.entity.StatementOfOffenceBuilder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S3655"})
public class HearingListedConverter implements Converter<HearingListed, Hearing> {

    @Override
    public Hearing convert(final HearingListed event) {

        final Hearing hearing =  buildHearing(
                event.getHearingId(),
                event.getCourtCentreId(),
                event.getType(),
                event.getStartDate(),
                event.getEstimateMinutes(),
                event.getCaseId(),
                event.getCourtRoomId(),
                event.getJudgeId(),
                event.getStartTimes(),
                event.getEndDate()
        );

        final Set<Defendant> defendants = buildDefendants(event.getDefendants(), hearing);
        hearing.getDefendants().addAll(defendants);
        return hearing;
    }

    Hearing buildHearing(UUID hearingId, UUID courtCentreId, String type, LocalDate startDate,
                         Integer estimateMins, UUID listingCaseId, UUID courtRoomId, UUID judgeId,
                         List<ZonedDateTime> startTimes, LocalDate endDate) {
        final HearingBuilder hearingBuilder = new HearingBuilder();

        hearingBuilder.setId(hearingId);
        hearingBuilder.setCourtCentreId(courtCentreId);
        hearingBuilder.setType(type);
        hearingBuilder.setStartDate(startDate);
        hearingBuilder.setEstimateMinutes(estimateMins);
        hearingBuilder.setListingCaseId(listingCaseId);
        hearingBuilder.setAllocated(false);
        hearingBuilder.setCourtRoomId(courtRoomId);
        hearingBuilder.setJudgeId(judgeId);
        hearingBuilder.setStartTimes(startTimes);
        hearingBuilder.setEndDate(endDate);

        return hearingBuilder.build();
    }


    Set<Defendant> buildDefendants(final List<uk.gov.justice.listing.events.Defendant> defendantsPartOfEvent,
                                   final Hearing hearing) {
        if (defendantsPartOfEvent == null) {
            return Collections.emptySet();
        }
        return defendantsPartOfEvent.stream()
                .map(defendant -> this.buildDefendant(defendant, hearing))
                .collect(toSet());
    }

    Defendant buildDefendant(final uk.gov.justice.listing.events.Defendant defendantPartOfEvent,
                             final Hearing hearing) {
        final CompositeDefendantId compositeDefendantId = getCompositeDefendantId(defendantPartOfEvent, hearing);
        final DefendantBuilder defendantBuilder = new DefendantBuilder();
        final Set<Offence> offences = buildOffences(hearing.getId(), defendantPartOfEvent.getOffences());

        defendantBuilder.setCompositeDefendantId(compositeDefendantId);
        defendantBuilder.setPersonId((defendantPartOfEvent.getPersonId()));
        defendantBuilder.setFirstName(defendantPartOfEvent.getFirstName());
        defendantBuilder.setLastName(defendantPartOfEvent.getLastName());
        defendantBuilder.setBailStatus(defendantPartOfEvent.getBailStatus().toString());
        defendantBuilder.setDateOfBirth(parse(defendantPartOfEvent.getDateOfBirth()));
        if(defendantPartOfEvent.getCustodyTimeLimit().isPresent()) {
            defendantBuilder.setCustodyTimeLimit(parse(defendantPartOfEvent.getCustodyTimeLimit().get()));
        }
        defendantBuilder.setDefenceOrganisation(defendantPartOfEvent.getDefenceOrganisation());
        defendantBuilder.setOffences(offences);
        defendantBuilder.setHearing(hearing);

        return defendantBuilder.build();
    }

    private CompositeDefendantId getCompositeDefendantId(uk.gov.justice.listing.events.Defendant defendantPartOfEvent, Hearing hearing) {
        return new CompositeDefendantIdBuilder()
                    .setDefendantId(defendantPartOfEvent.getId())
                    .setHearingId(hearing.getId())
                    .build();
    }

    private Set<Offence> buildOffences(final UUID hearingId, final List<uk.gov.justice.listing.events.Offence> offencesPartOfEvent) {
        if (offencesPartOfEvent == null) {
            return Collections.emptySet();
        }
        return offencesPartOfEvent.stream()
                .map(o -> buildOffence(hearingId, o))
                .collect(toSet());
    }

    private Offence buildOffence(final UUID hearingId, final uk.gov.justice.listing.events.Offence offencePartOfPayload) {
        final OffenceBuilder offenceBuilder = new OffenceBuilder();
        final StatementOfOffence statementOfOffence = buildStatementOfOffence
                (offencePartOfPayload.getStatementOfOffence());
        final CompositeOffenceId compositeOffenceId = createCompositeOffenceId(hearingId, offencePartOfPayload);

        offenceBuilder.setId(compositeOffenceId);
        offenceBuilder.setOffenceCode(offencePartOfPayload.getOffenceCode());
        offenceBuilder.setStartDate(parse(offencePartOfPayload.getStartDate()));
        offencePartOfPayload.getEndDate().ifPresent(endDate -> offenceBuilder.setEndDate(parse(endDate)));
        offenceBuilder.setStatementOfOffence(statementOfOffence);

        return offenceBuilder.build();
    }

    private CompositeOffenceId createCompositeOffenceId(final UUID hearingId, final uk.gov.justice.listing.events.Offence offencePartOfPayload) {
        return new CompositeOffenceIdBuilder()
                    .setHearingId(hearingId)
                    .setDefendantId(offencePartOfPayload.getDefendantId())
                    .setOffenceId(offencePartOfPayload.getId())
                    .build();
    }

    private StatementOfOffence buildStatementOfOffence(final uk.gov.justice.listing.events.StatementOfOffence statementOfOffencePartOfEvent) {
        final StatementOfOffenceBuilder statementOfOffenceBuilder = new StatementOfOffenceBuilder();

        statementOfOffenceBuilder.setLegislation(statementOfOffencePartOfEvent.getLegislation());
        statementOfOffenceBuilder.setTitle(statementOfOffencePartOfEvent.getTitle());

        return statementOfOffenceBuilder.build();
    }


}


