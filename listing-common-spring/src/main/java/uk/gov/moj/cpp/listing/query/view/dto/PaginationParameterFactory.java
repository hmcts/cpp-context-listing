package uk.gov.moj.cpp.listing.query.view.dto;


import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import jakarta.json.JsonObject;


@Component
public class PaginationParameterFactory {

    private static final Integer FIRST_PAGE_NUMBER = 1;
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String USE_MAX_MAGE_SIZE = "useMaxPageSize";
    private static final Integer DEFAULT_PAGE_SIZE = 50;

    private final long maxPageSize;

    // experimented in SIT: 8k works, 10k throws err 502
    public PaginationParameterFactory(@Value("${listing.query.rangeSearch.maxPageSize:4000}") final long maxPageSize) {
        this.maxPageSize = maxPageSize;
    }


    public PaginationParameter newPaginationParameter(final JsonObject requestJsonObject) {
        Integer pageSize = requestJsonObject.getInt(PAGE_SIZE, DEFAULT_PAGE_SIZE);
        // There need to be a cap on the page size so that if someone tries to fetch a huge page, it doesn't crash the system

        final Integer pageNumber = requestJsonObject.getInt(PAGE_NUMBER, FIRST_PAGE_NUMBER);
        final boolean useMax = requestJsonObject.getBoolean(USE_MAX_MAGE_SIZE, false);
        if (useMax && maxPageSize > 0) {
            pageSize = (int) maxPageSize;
        }
        final Integer offSet = (pageNumber - 1) * pageSize;
        return new PaginationParameter(pageSize, pageNumber, offSet);
    }

    public long getMaxPageSize() {
        return maxPageSize;
    }
}
