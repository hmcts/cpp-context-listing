package uk.gov.moj.cpp.listing.steps;

public class CivilOffenceData {

    private final Boolean isExParte;

    public CivilOffenceData(Boolean isExParte) {
        this.isExParte = isExParte;
    }

    public Boolean getExParte() {
        return isExParte;
    }
}
