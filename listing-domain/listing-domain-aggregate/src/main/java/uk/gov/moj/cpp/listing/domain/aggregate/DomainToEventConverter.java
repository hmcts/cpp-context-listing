package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.listing.events.Hearing.hearing;

import uk.gov.justice.listing.events.BailStatus;
import uk.gov.justice.listing.events.BaseDefendant;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.SimpleOffence;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S3655", "squid:S1118"})
public class DomainToEventConverter {

    public static List<Hearing> createHearingsFrom(final List<uk.gov.moj.cpp.listing.domain.Hearing> sourceHearings) {
        return sourceHearings.stream()
                .map(h -> createHearingFrom(h))
                .collect(toList());
    }

    public static BaseDefendant createBaseDefendantFrom(final uk.gov.moj.cpp.listing.domain.Defendant sourceDefendant) {

        return BaseDefendant.baseDefendant()
                .withId(getUUIDOrNull(sourceDefendant.getId()))
                .withPersonId(getUUIDOrNull(sourceDefendant.getPersonId()))
                .withFirstName(sourceDefendant.getFirstName())
                .withLastName(sourceDefendant.getLastName())
                .withDateOfBirth(getLocalDateOrNull(sourceDefendant.getDateOfBirth()))
                .withBailStatus(getBailStatus(sourceDefendant.getBailStatus()))
                .withCustodyTimeLimit(ofNullable(getLocalDateOrNull(sourceDefendant.getCustodyTimeLimit())))
                .withDefenceOrganisation(sourceDefendant.getDefenceOrganisation())
                .build();
    }

    public static List<Defendant> createDefendantsFrom(final List<uk.gov.moj.cpp.listing.domain.Defendant> sourceDefendants) {
        return sourceDefendants.stream()
                .map(d -> createDefendantFrom(d))
                .collect(toList());
    }

    public static Defendant createDefendantFrom(final uk.gov.moj.cpp.listing.domain.Defendant sourceDefendant) {

        return Defendant.defendant()
                .withId(getUUIDOrNull(sourceDefendant.getId()))
                .withPersonId(getUUIDOrNull(sourceDefendant.getPersonId()))
                .withFirstName(sourceDefendant.getFirstName())
                .withLastName(sourceDefendant.getLastName())
                .withDateOfBirth(getLocalDateOrNull(sourceDefendant.getDateOfBirth()))
                .withBailStatus(getBailStatus(sourceDefendant.getBailStatus()))
                .withCustodyTimeLimit(ofNullable(getLocalDateOrNull(sourceDefendant.getCustodyTimeLimit())))
                .withDefenceOrganisation(sourceDefendant.getDefenceOrganisation())
                .withOffences(createOffencesFrom(sourceDefendant.getOffences()))
                .build();
    }

    private static SimpleOffence createSimpleOffenceFrom(uk.gov.moj.cpp.listing.domain.SimpleOffence o) {
        return SimpleOffence.simpleOffence()
                .withId(fromString(o.getId()))
                .withDefendantId(UUID.fromString(o.getDefendantId()))
                .build();
    }

    public static List<SimpleOffence> createDeletedOffencesFrom(CaseSimpleOffences caseSimpleOffences) {
        return caseSimpleOffences.getOffences().stream()
                .map(DomainToEventConverter::createSimpleOffenceFrom)
                .collect(toList());
    }

    public static List<Offence> createOffencesFrom(CaseOffences caseOffences) {
        return caseOffences.getOffences().stream()
                .map(DomainToEventConverter::createOffenceFrom)
                .collect(toList());
   }

    private static Hearing createHearingFrom(final uk.gov.moj.cpp.listing.domain.Hearing source) {
        return hearing()
                .withId(fromString(source.getId()))
                .withCaseId(fromString(source.getCaseId()))
                .withCourtCentreId(fromString(source.getCourtCentreId()))
                .withType(source.getType())
                .withEstimateMinutes(source.getEstimateMinutes())
                .withStartDate(getLocalDateOrNull(source.getStartDate()))
                .withEndDate(source.getEndDate())
                .withStartTime(ofNullable(source.getStartDateTime()))
                .withDefendants(createDefendantsFrom(source.getDefendants()))
                .withJudgeId(getUUIDOrNull(source.getJudgeId()))
                .withCourtRoomId(getUUIDOrNull(source.getCourtRoomId()))
                .build();
    }

    private static List<Offence> createOffencesFrom(final List<uk.gov.moj.cpp.listing.domain.Offence> sourceOffences) {
        return sourceOffences.stream()
                .map(o -> createOffenceFrom(o))
                .collect(toList());
    }

    public static Offence createOffenceFrom(final uk.gov.moj.cpp.listing.domain.Offence sourceOffence) {
        return uk.gov.justice.listing.events.Offence.offence()
                .withId(fromString(sourceOffence.getId()))
                .withOffenceCode(sourceOffence.getOffenceCode())
                .withStartDate(getLocalDateOrNull(sourceOffence.getStartDate()))
                .withEndDate(Optional.ofNullable(getLocalDateOrNull(sourceOffence.getEndDate())))
                .withStatementOfOffence(createStatementOfOffenceFrom(sourceOffence.getStatementOfOffence()))
                .withDefendantId(UUID.fromString(sourceOffence.getDefendantId()))
                .build();
    }

    private static StatementOfOffence createStatementOfOffenceFrom(final uk.gov.moj.cpp.listing.domain.StatementOfOffence sourceStatementOfOffence) {
        return uk.gov.justice.listing.events.StatementOfOffence.statementOfOffence()
                .withTitle(sourceStatementOfOffence.getTitle())
                .withLegislation(sourceStatementOfOffence.getLegislation())
                .build();
    }

    private static UUID getUUIDOrNull(String uuidStr) {
        UUID uuid = null;
        if(uuidStr!=null){
            uuid =  fromString(uuidStr);
        }
        return uuid;
    }

    private static String getLocalDateOrNull(LocalDate custodyTimeLimit) {
        return custodyTimeLimit != null ? custodyTimeLimit.toString() : null;
    }

    private static BailStatus getBailStatus(String status) {
        return status!=null &&
                BailStatus.valueFor(status).isPresent() ?
                BailStatus.valueFor(status).get() : null;
    }
}
