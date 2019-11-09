package uk.gov.moj.cpp.listing.domain.xhibit;

public class CourtLocation {

    private String crestCourtSiteId;    // e.g. 433 Also known as crestCourtId, crestCode
    private String courtFullName;   // e.g. LIVERPOOL
    private String courtShortName;  // e.g. LIVER
    private String courtSiteCode;   // e.g. A

    public CourtLocation(final String crestCourtSiteId, final String courtFullName, final String courtShortName, final String courtSiteCode) {
        this.crestCourtSiteId = crestCourtSiteId;
        this.courtFullName = courtFullName;
        this.courtShortName = courtShortName;
        this.courtSiteCode = courtSiteCode;
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
}
