package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;


public class HearingBuilder {
    private UUID id;
    private LocalDate startDateTime;
    private Integer estimateMinutes;
    private String type;
    private String courtCentreId;
    private ListingCase listingCase;
    private Boolean allocated;
    private Set<Defendant> defendants = new LinkedHashSet<>();

    public HearingBuilder setId(final UUID id) {
        this.id = id;
        return this;
    }

    public HearingBuilder setStartDateTime(final LocalDate startDateTime) {
        this.startDateTime = startDateTime;
        return this;
    }

    public HearingBuilder setEstimateMinutes(final Integer estimateMinutes) {
        this.estimateMinutes = estimateMinutes;
        return this;
    }

    public HearingBuilder setType(final String type) {
        this.type = type;
        return this;
    }

    public HearingBuilder setCourtCentreId(final String courtCentreId) {
        this.courtCentreId = courtCentreId;
        return this;
    }

    public HearingBuilder setListingCase(final ListingCase listingCase) {
        this.listingCase = listingCase;
        return this;
    }

    public HearingBuilder setAllocated(final Boolean allocated) {
        this.allocated = allocated;
        return this;
    }

    public HearingBuilder setDefendants(final Set<Defendant> defendants) {
        this.defendants = defendants;
        return this;
    }

    public Hearing build() {
        return new Hearing(id, listingCase, allocated, defendants,
                new Hearing.HearingDetails(startDateTime,estimateMinutes,type,courtCentreId));
    }
}    