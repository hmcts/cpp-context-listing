package uk.gov.moj.cpp.listing.query.view.hearing;

import java.io.Serializable;

public class OffenceSummary  implements Serializable {

    private final String offenceId;
    private final String defendantId;
    private final String title;

    public OffenceSummary(final String offenceId, final String defendantId, final String title) {
        this.offenceId = offenceId;
        this.defendantId = defendantId;
        this.title = title;
    }

    public String getOffenceId() {
        return offenceId;
    }

    public String getDefendantId() {
        return defendantId;
    }

    public String getTitle() {
        return title;
    }
}
