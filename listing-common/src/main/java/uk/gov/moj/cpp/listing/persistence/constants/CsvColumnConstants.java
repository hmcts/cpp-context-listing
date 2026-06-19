package uk.gov.moj.cpp.listing.persistence.constants;

/**
 * Constants for CSV column names used in hearing CSV reports
 */
public final class CsvColumnConstants {
    
    // Private constructor to prevent instantiation
    private CsvColumnConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // CSV Column Names
    public static final String DATE_OF_HEARING = "Date of hearing";
    public static final String FIXED_WEEK_COMMENCING = "Fixed/week commencing";
    public static final String COURTROOM = "Courtroom";
    public static final String JUDICIARY = "Judiciary";
    public static final String TIME = "Time";
    public static final String HEARING_TYPE = "Hearing type";
    public static final String DURATION = "Duration";
    public static final String DAY = "Day";
    public static final String URN_S = "URN/s";
    public static final String DEFENDANT_NAMES = "Defendant Names";
    public static final String DEFT_FLAG = "Deft flag";
    public static final String OFFENCES = "Offences";
    public static final String PUBLIC_LIST_NOTE = "Public list note";
    public static final String LANGUAGE = "Language";
    public static final String VIDEO_HEARING = "Video Hearing";
    public static final String CUSTODY_STATUS = "Custody status";
    public static final String MULTI_DAY_HEARING_DETAILS = "Multi-day hearing details";
    public static final String PINNED_NOTES = "Pinned notes";
    public static final String UNPINNED_NOTES = "Unpinned notes";
    public static final String MARKERS = "Markers";
    public static final String REPORTING_RESTRICTION = "Reporting Restriction";
}
