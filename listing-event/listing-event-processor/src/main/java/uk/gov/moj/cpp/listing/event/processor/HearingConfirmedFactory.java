package uk.gov.moj.cpp.listing.event.processor;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.Judge;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.event.HearingAllocatedForListing;
import uk.gov.moj.cpp.listing.event.external.HearingConfirmed;
import uk.gov.moj.cpp.listing.event.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.entity.Offence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

public class HearingConfirmedFactory {

    private static final String FIELD_NAME = "name";
    private static final String COURT_ROOMS = "courtRooms";
    private static final String FIRST_NAME = "firstName";
    private static final String TITLE = "title";
    private static final String LAST_NAME = "lastName";
    private static final String ID = "id";
    static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Inject
    private ReferenceDataService referenceDataService;


    public HearingConfirmed create(final Hearing hearing, final ListingCase listingCase, final HearingAllocatedForListing hearingUpdated, final JsonEnvelope envelope) {
        final String courtCentreName = getCourtCentreName(hearing.getCourtCentreId(), envelope);
        final String courtRoomName = getCourtRoomName(hearing.getCourtCentreId(), UUID.fromString(hearingUpdated.getCourtRoomId()), envelope);
        final Judge judge = getJudge(UUID.fromString(hearingUpdated.getJudgeId()), envelope);
        
        final LocalDateTime startDateTime = LocalDateTime.of(hearingUpdated.getHearingDate().getStartDate(), hearingUpdated.getHearingDate().getStartTime());
        final String formattedStartDateTime = DATE_TIME_FORMAT.format(startDateTime);

        final uk.gov.moj.cpp.listing.event.external.Hearing externalHearing = new uk.gov.moj.cpp.listing.event.external.Hearing(hearing.getId().toString(), hearingUpdated.getType(), hearing.getListingCaseId().toString(),
                hearing.getCourtCentreId().toString(), courtCentreName, hearingUpdated.getCourtRoomId().toString(), courtRoomName, judge,
                formattedStartDateTime, hearingUpdated.getHearingDate().getNotBefore(), hearingUpdated.getEstimateMinutes(), getDefendants(hearing));

        return new HearingConfirmed(hearing.getListingCaseId().toString(), listingCase.getUrn(), externalHearing);
    }


    private List<Defendant> getDefendants(final Hearing hearing) {
        return hearing.getDefendants().stream()
                .map(d -> new uk.gov.moj.cpp.listing.domain.Defendant(d.getDefendantId().toString(), d.getPersonId().toString(), d.getFirstName(), d.getLastName(),
                        d.getDateOfBirth(), d.getBailStatus(), d.getCustodyTimeLimit(), d.getDefenceOrganisation(),
                        getOffences(d.getOffences())))
                .collect(Collectors.toList());
    }

    private List<uk.gov.moj.cpp.listing.domain.Offence> getOffences(final Set<Offence> offences) {
        return offences.stream()
                .map(o -> {
                    final StatementOfOffence statementOfOffence = new StatementOfOffence(o.getStatementOfOffence().getTitle(), o.getStatementOfOffence().getLegislation());
                    return new uk.gov.moj.cpp.listing.domain.Offence(o.getOffenceId().toString(), o.getOffenceCode(),
                            o.getStartDate(), o.getEndDate(), statementOfOffence);
                })
                .collect(Collectors.toList());
    }


    private String getCourtCentreName(final UUID courtCentreId, final JsonEnvelope event) {
        final JsonObject courtCentrePayload = getCourtCentrePayload(courtCentreId, event);
        return courtCentrePayload.getString(FIELD_NAME);
    }


    private String getCourtRoomName(final UUID courtCentreId, final UUID courtRoomId, final JsonEnvelope event) {
        final JsonObject courtCentrePayload = getCourtCentrePayload(courtCentreId, event);
        return courtCentrePayload.getJsonArray(COURT_ROOMS).getValuesAs(JsonObject.class).stream()
                .filter(cr -> courtRoomId.toString().equals(cr.getString(ID)))
                .map(cr -> cr.getString(FIELD_NAME)).findFirst().get();
    }

    private JsonObject getCourtCentrePayload(UUID courtCentreId, JsonEnvelope event) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getCourtCentreById(courtCentreId, event);
        return courtCentreEnvelope.payloadAsJsonObject();
    }

    private Judge getJudge(final UUID judgeId, final JsonEnvelope event) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getJudgeById(judgeId, event);
        final JsonObject judgePayload = courtCentreEnvelope.payloadAsJsonObject();
        final String firstName = judgePayload.getString(FIRST_NAME);
        final String title = judgePayload.getString(TITLE);
        final String lastName = judgePayload.getString(LAST_NAME);

        return new Judge(judgeId.toString(), title, firstName, lastName);
    }

}
