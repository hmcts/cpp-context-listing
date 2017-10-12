package uk.gov.moj.cpp.listing.query.view.hearing;

import java.io.Serializable;

public class OffenceSummary  implements Serializable {

    private final String id;
    private final String title;

    public OffenceSummary(final String id, final String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
