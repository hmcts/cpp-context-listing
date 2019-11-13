package uk.gov.moj.cpp.listing.domain.xhibit;

public class CourtLocation {

    private String crestCourtSiteId;    // e.g. 433 Also known as crestCourtId, crestCode
    private String courtFullName;   // e.g. LIVERPOOL
    private String courtShortName;  // e.g. LIVER
    private String courtSiteCode;   // e.g. A
    private String courtType;       // e.g CROWN_COURT

    public CourtLocation(final String crestCourtSiteId, final String courtFullName, final String courtShortName, final String courtSiteCode, final String courtType) {
        this.crestCourtSiteId = crestCourtSiteId;
        this.courtFullName = courtFullName;
        this.courtShortName = courtShortName;
        this.courtSiteCode = courtSiteCode;
        this.courtType = courtType;
    }

    public String getCrestCourtSiteId() {
        return crestCourtSiteId;
    }

    public String getCourtFullName() {
        return courtFullName;
    }

    public String getCourtShortName() {
        return courtShortName;
    }

    public String getCourtSiteCode() {
        return courtSiteCode;
    }

    public String getCourtType() {
        return courtType;
    }
}
