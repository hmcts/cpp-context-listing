package uk.gov.moj.cpp.listing.steps.data;


public class CourtRoomData {

    private final String id;
    private final String courtCentre;
    private final String courtRoomName;

    public CourtRoomData(final String id,
                         final String courtCentre,
                         final String courtRoomName) {
        this.id = id;
        this.courtCentre = courtCentre;
        this.courtRoomName = courtRoomName;
    }

    public String getId() { return id; }
    public String getCourtCentre() { return courtCentre; }
    public String getCourtRoomName() { return courtRoomName; }
}
