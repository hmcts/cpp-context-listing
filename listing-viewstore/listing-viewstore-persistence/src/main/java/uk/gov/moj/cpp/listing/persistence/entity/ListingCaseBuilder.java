package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class ListingCaseBuilder {
    private UUID id;
    private String urn;
    private LocalDate sendingCommittalDate;
    private Set<Defendant> defendants = new LinkedHashSet<>();

    public ListingCaseBuilder setId(final UUID id) {
        this.id = id;
        return this;
    }

    public ListingCaseBuilder setUrn(final String urn) {
        this.urn = urn;
        return this;
    }

    public ListingCaseBuilder setSendingCommittalDate(final LocalDate sendingCommittalDate) {
        this.sendingCommittalDate = sendingCommittalDate;
        return this;
    }

    public ListingCaseBuilder setDefendants(final Set<Defendant> defendants) {
        this.defendants = defendants;
        return this;
    }

    public ListingCase build() {
        return new ListingCase(id, urn, sendingCommittalDate, defendants);
    }
}