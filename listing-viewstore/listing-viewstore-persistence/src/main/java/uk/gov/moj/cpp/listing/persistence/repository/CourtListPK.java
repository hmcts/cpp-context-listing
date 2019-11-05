package uk.gov.moj.cpp.listing.persistence.repository;


import static javax.persistence.EnumType.STRING;

import uk.gov.justice.listing.event.PublishCourtListType;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Embeddable
public class CourtListPK implements Serializable {
    private static final long serialVersionUID = 8137449412662L;

    @Column(name = "court_centre_id", nullable = false)
    private UUID courtCentreId;

    @Enumerated(STRING)
    @Column(name = "publish_court_list_type", nullable = false)
    private PublishCourtListType publishCourtListType;

    public CourtListPK() {
    }

    public CourtListPK(final UUID courtCentreId, final PublishCourtListType publishCourtListType) {
        this.courtCentreId = courtCentreId;
        this.publishCourtListType = publishCourtListType;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreId(UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public PublishCourtListType getPublishCourtListType() {
        return publishCourtListType;
    }

    public void setPublishCourtListType(PublishCourtListType publishCourtListType) {
        this.publishCourtListType = publishCourtListType;
    }
}