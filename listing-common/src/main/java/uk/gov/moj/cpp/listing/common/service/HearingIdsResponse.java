package uk.gov.moj.cpp.listing.common.service;

import java.util.List;
import java.util.StringJoiner;

public class HearingIdsResponse{

    private final long pageCount;
    private final List<IdResponse> uuids;
    private final long results;

    public HearingIdsResponse(final List<IdResponse> uuids, final long results, final long pageCount) {
        this.uuids = uuids;
        this.results = results;
        this.pageCount = pageCount;
    }

    public long getPageCount() {
        return pageCount;
    }

    public List<IdResponse> getUuids() {
        return uuids;
    }

    public long getResults() {
        return results;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HearingIdsResponse.class.getSimpleName() + "[", "]")
                .add("pageCount=" + getPageCount())
                .add("uuids=" + getUuids())
                .add("results=" + getResults())
                .toString();
    }
}
