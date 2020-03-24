package uk.gov.moj.cpp.listing.it.util;

import static uk.gov.moj.cpp.listing.it.util.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

public class ViewStoreCleaner {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    public void cleanViewStoreTables() {
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME,
                "hearing");
    }
}
