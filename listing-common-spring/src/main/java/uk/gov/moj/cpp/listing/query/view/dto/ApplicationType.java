package uk.gov.moj.cpp.listing.query.view.dto;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public enum ApplicationType {

    PL84506("PL84506", "Application to withhold sensitive information during pre-charge bail application"),
    PL84505("PL84505", "Application for the subsequent extension of pre-charge bail period"),
    PL84501("PL84501", "Application for a warrant for further detention"),
    PL84502("PL84502", "Application for an extension of warrant for further detention"),
    PL84504("PL84504", "Application for an initial extension of pre-charge bail period");

    private String applicationTypeCode;
    private String title;

    ApplicationType(final String applicationTypeCode, final String title) {
        this.applicationTypeCode = applicationTypeCode;
        this.title = title;
    }

    public static String getApplicationTypeTitle(final String applicationTypeCode) {
        final ApplicationType[] applicationTypes = ApplicationType.values();
        for (final ApplicationType applicationType : applicationTypes) {
            if (applicationType.getApplicationTypeCode().equals(applicationTypeCode)) {
                return applicationType.getTitle();
            }
        }
        return EMPTY;
    }

    public String getApplicationTypeCode() {
        return applicationTypeCode;
    }

    public String getTitle() {
        return title;
    }
}
