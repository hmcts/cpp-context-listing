package uk.gov.moj.cpp.listing.common.xhibit;

public class ThreadLocalCommonXhibitReferenceDataService {

    private static final ThreadLocal<CommonXhibitReferenceDataService> DELEGATE = new ThreadLocal<>();


    public CommonXhibitReferenceDataService get() {
        CommonXhibitReferenceDataService commonXhibitReferenceDataService = DELEGATE.get();
        if (commonXhibitReferenceDataService != null) {
            return commonXhibitReferenceDataService;
        }
        throw new RuntimeException("commonXhibitReferenceDataService is not instantiated");
    }

    public void set(CommonXhibitReferenceDataService commonXhibitReferenceDataService) {
        DELEGATE.set(commonXhibitReferenceDataService);
    }

    public void unload() {
        DELEGATE.remove();
    }

}
