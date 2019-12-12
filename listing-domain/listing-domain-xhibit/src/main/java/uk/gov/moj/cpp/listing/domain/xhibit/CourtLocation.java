package uk.gov.moj.cpp.listing.domain.xhibit;

public class CourtLocation {

    private String crestCourtId;     // e.g. 433 - CREST identifier for the crown court (aka parent court) of the court site
    private String crestCourtSiteId; // e.g. 433 - CREST identifier for a court site (aka court house, court centre)
    private String courtFullName;    // e.g. LIVERPOOL
    private String courtShortName;   // e.g. LIVER
    private String courtSiteCode;    // e.g. A
    private String courtType;        // e.g CROWN_COURT

    public CourtLocation(final String crestCourtId,
                         final String crestCourtSiteId,
                         final String courtFullName,
                         final String courtShortName,
                         final String courtSiteCode,
                         final String courtType) {
        this.crestCourtId = crestCourtId;
        this.crestCourtSiteId = crestCourtSiteId;
        this.courtFullName = courtFullName;
        this.courtShortName = courtShortName;
        this.courtSiteCode = courtSiteCode;
        this.courtType = courtType;
    }

    public String getCrestCourtId() {
        return crestCourtId;
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
