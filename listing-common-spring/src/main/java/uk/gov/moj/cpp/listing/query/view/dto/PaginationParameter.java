package uk.gov.moj.cpp.listing.query.view.dto;

public class PaginationParameter {

    private final Integer pageSize;
    private final Integer pageNumber;
    private final Integer offSet;

    public PaginationParameter(final Integer pageSize, final Integer pageNumber, final Integer offSet) {
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.offSet = offSet;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public Integer getOffSet() {
        return offSet;
    }
}
