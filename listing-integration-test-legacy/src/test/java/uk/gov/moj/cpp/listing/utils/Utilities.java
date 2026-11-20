package uk.gov.moj.cpp.listing.utils;

public class Utilities {

    public static final int DEFAULT_POLL_TIMEOUT_IN_SEC = 120;
    public static final int DEFAULT_POLL_TIMEOUT_IN_MILLIS = DEFAULT_POLL_TIMEOUT_IN_SEC * 1000;
    public static final int DEFAULT_NOT_HAPPENED_TIMEOUT_IN_MILLIS = DEFAULT_POLL_TIMEOUT_IN_MILLIS / 6;

    // MBD P1h: dead listenForPrivateEvent / EventListener (private listing.event topic consumer, no callers) removed

    /**
     * todo this needs to be refactored for each usage with correct polling conditions, it's just placeholder for now
     */
    @SuppressWarnings("java:S2925")
    public static void sleepToBeRefactored(){
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
