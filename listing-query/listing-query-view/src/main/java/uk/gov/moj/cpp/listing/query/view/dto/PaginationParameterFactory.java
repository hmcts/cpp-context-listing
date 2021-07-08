package uk.gov.moj.cpp.listing.query.view.dto;


import javax.json.JsonObject;


public class PaginationParameterFactory {

    private static final Integer FIRST_PAGE_NUMBER = 1;
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_NUMBER = "pageNumber";
    private static final Integer DEFAULT_PAGE_SIZE = 50;

    private PaginationParameterFactory() {
    }

    public static PaginationParameter newPaginationParameter(final JsonObject requestJsonObject) {
        final Integer pageSize = requestJsonObject.getInt(PAGE_SIZE, DEFAULT_PAGE_SIZE);
        final Integer pageNumber = requestJsonObject.getInt(PAGE_NUMBER, FIRST_PAGE_NUMBER);
        final Integer offSet = (pageNumber - 1) * pageSize;
        return new PaginationParameter(pageSize, pageNumber, offSet);
    }
}
