package uk.gov.moj.cpp.listing.domain.xhibit;

public class CourtLocation {

    private String crestCourtId;        // e.g. 433 - CREST identifier for the crown court (aka parent court) of the court site
    private String crestCourtSiteId;    // e.g. 433 - CREST identifier for a court site (aka court house, court centre)
    private String courtName;           // e.g. LIVERPOOL
    private String courtShortName;      // e.g. LIVER
    private String courtSiteName;       // e.g. LIVERPOOL
    private String courtSiteCode;       // e.g. A
    private String courtType;           // e.g CROWN_COURT

    public CourtLocation(final String crestCourtId,
                         final String crestCourtSiteId,
                         final String courtName,
                         final String courtShortName,
                         final String courtSiteName,
                         final String courtSiteCode,
                         final String courtType) {
        this.crestCourtId = crestCourtId;
        this.crestCourtSiteId = crestCourtSiteId;
        this.courtName = courtName;
        this.courtShortName = courtShortName;
        this.courtSiteName = courtSiteName;
        this.courtSiteCode = courtSiteCode;
        this.courtType = courtType;
    }

    public String getCrestCourtId() {
        return crestCourtId;
    }

    public String getCrestCourtSiteId() {
        return crestCourtSiteId;
    }

    public String getCourtName() {
        return courtName;
    }

    public String getCourtSiteName() {
        return courtSiteName;
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

    @Override
    public String toString() {
        return "CourtLocation{" +
                "crestCourtId='" + crestCourtId + '\'' +
                ", crestCourtSiteId='" + crestCourtSiteId + '\'' +
                ", courtName='" + courtName + '\'' +
                ", courtShortName='" + courtShortName + '\'' +
                ", courtSiteName='" + courtSiteName + '\'' +
                ", courtSiteCode='" + courtSiteCode + '\'' +
                ", courtType='" + courtType + '\'' +
                '}';
    }
}
