package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.util.UUID;

public class CourtReferenceData {

    private static final String COURT_ROOM_1_NAME = RandomGenerator.STRING.next();
    private static final String COURT_CENTRE_NAME = RandomGenerator.STRING.next();
    private static final String TITLE = RandomGenerator.STRING.next();
    private static final String FIRST_NAME = RandomGenerator.STRING.next();
    private static final String LAST_NAME = RandomGenerator.STRING.next();

    private UUID courtCentreId;
    private String courtCentreName;
    private UUID courtRoomId;
    private String courtRoomName;
    private Judge judge;

    public CourtReferenceData(final UUID courtCentreId, final String courtCentreName, final UUID courtRoomId, final String courtRoomName, final Judge judge) {
        this.courtCentreId = courtCentreId;
        this.courtCentreName = courtCentreName;
        this.courtRoomId = courtRoomId;
        this.courtRoomName = courtRoomName;
        this.judge = judge;
    }

    public static CourtReferenceData courtReferenceData(final CaseData caseData, final UpdatedHearingData updatedHearingData) {
             return new CourtReferenceData(UUID.fromString(caseData.getHearingData().get(0).getCourtCentreId()), COURT_CENTRE_NAME,
                     updatedHearingData.getCourtRoomId(), COURT_ROOM_1_NAME,
                     new Judge(updatedHearingData.getJudgeId(), TITLE, FIRST_NAME, LAST_NAME));
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public String getCourtRoomName() {
        return courtRoomName;
    }

    public Judge getJudge() {
        return judge;
    }


}
