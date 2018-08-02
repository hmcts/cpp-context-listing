package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.listing.domain.*;
import uk.gov.moj.cpp.listing.domain.Hearing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S2789"})
public class EventToDomainConverter {

    private static final boolean UNALLOCATED = false;

    private EventToDomainConverter() {
    }

    public static List<Hearing> createHearingsFrom(final List<uk.gov.justice.listing.events.Hearing> sourceHearings) {
        return sourceHearings.stream()
                .map(h -> createHearingFrom(h))
                .collect(toList());
    }

    private static Hearing createHearingFrom(final uk.gov.justice.listing.events.Hearing sourceHearing) {

        return new uk.gov.moj.cpp.listing.domain.Hearing(
                sourceHearing.getId().toString(),
                sourceHearing.getCaseId().toString(),
                sourceHearing.getCourtCentreId().toString(),
                sourceHearing.getType(),
                getLocalDateOrNull(sourceHearing.getStartDate()),
                sourceHearing.getEndDate(),
                sourceHearing.getEstimateMinutes(),
                getStringOrNull(sourceHearing.getCourtRoomId()),
                getStringOrNull(sourceHearing.getJudgeId()),
                getStringOrNull(sourceHearing.getStartTime()),
                createDefendantsFrom(sourceHearing.getDefendants()),
                UNALLOCATED);
    }

    private static List<Defendant> createDefendantsFrom(final List<uk.gov.justice.listing.events.Defendant> sourceDefendants) {
        return sourceDefendants.stream()
                .map(d -> createDefendantFrom(d))
                .collect(toList());
    }

    private static Defendant createDefendantFrom(final uk.gov.justice.listing.events.Defendant sourceDefendant) {
        final LocalDate custodyTimeLimit = getLocalDateOrNull(sourceDefendant.getCustodyTimeLimit()); // Optional field
        String bailStatus = sourceDefendant.getBailStatus()!=null? sourceDefendant.getBailStatus().toString() : null;
        return new uk.gov.moj.cpp.listing.domain.Defendant(
                sourceDefendant.getId().toString(),
                getStringOrNull(sourceDefendant.getPersonId()),
                sourceDefendant.getFirstName(),
                sourceDefendant.getLastName(),
                getLocalDateOrNull(sourceDefendant.getDateOfBirth()),
                bailStatus,
                custodyTimeLimit,
                sourceDefendant.getDefenceOrganisation(),
                createOffencesFrom(sourceDefendant.getOffences(), sourceDefendant.getId().toString())
        );
    }

    private static List<Offence> createOffencesFrom(
            final List<uk.gov.justice.listing.events.Offence> sourceOffences,
            final String defendantId) {
        return sourceOffences.stream()
                .map(o -> createOffenceFrom(o, defendantId))
                .collect(toList());
    }

    private static Offence createOffenceFrom(final uk.gov.justice.listing.events.Offence sourceOffence, final String defendantId) {
        return Offence.createOffenceBuilder()
                .setId(sourceOffence.getId().toString())
                .setOffenceCode(sourceOffence.getOffenceCode())
                .setStartDate(getLocalDateOrNull(sourceOffence.getStartDate()))
                .setEndDate(getLocalDateOrNull(sourceOffence.getEndDate()))
                .setStatementOfOffence(createStatementOfOffenceFrom(sourceOffence.getStatementOfOffence()))
                .setDefendantId(defendantId).build();
    }

    private static StatementOfOffence createStatementOfOffenceFrom(final uk.gov.justice.listing.events.StatementOfOffence sofSource) {
        return new StatementOfOffence(
                sofSource.getTitle(),
                sofSource.getLegislation()
        );
    }

    private static LocalDate getLocalDateOrNull(String startDate) {
        LocalDate localDate = null;
        if (startDate != null) {
            localDate = LocalDates.from(startDate);
        }
        return localDate;
    }

    private static LocalDate getLocalDateOrNull(final Optional<String> optional) {
        LocalDate localDate = null;
        if (optional!=null && optional.isPresent()) {
            localDate = LocalDate.parse(optional.get());
        }
        return localDate;
    }

    private static String getStringOrNull(final Optional<String> optional) {
        String value = null;
        if (optional!=null && optional.isPresent()) {
            value = optional.get();
        }
        return value;
    }

    private static String getStringOrNull(UUID uuid) {
        String uuidStr = null;
        if(uuid!=null){
           uuidStr = uuid.toString();
        }
        return uuidStr;
    }
}

