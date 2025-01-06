package uk.gov.moj.cpp.listing.common.service;

import java.util.List;
import java.util.UUID;

public class HearingIdsResponse{

    private final long pageCount;
    private final List<UUID> uuids;
    private final long results;

    public HearingIdsResponse(final List<UUID> uuids, final long results, final long pageCount) {
        this.uuids = uuids;
        this.results = results;
        this.pageCount = pageCount;
    }

    public long getPageCount() {
        return pageCount;
    }

    public List<UUID> getUuids() {
        return uuids;
    }

    public long getResults() {
        return results;
    }
}
