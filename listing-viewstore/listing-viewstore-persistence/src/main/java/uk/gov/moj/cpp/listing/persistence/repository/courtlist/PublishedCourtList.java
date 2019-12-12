package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import static javax.persistence.EnumType.STRING;

import uk.gov.justice.listing.event.PublishCourtListType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Type;

@Entity
@IdClass(PublishedCourtListPrimaryKey.class)
@Table(name = "published_court_list")
public class PublishedCourtList {

    @Id
    @Column(name = "court_centre_id", nullable = false)
    private UUID courtCentreId;

    @Id
    @Enumerated(STRING)
    @Column(name = "publish_court_list_type", nullable = false)
    private PublishCourtListType publishCourtListType;

    @Id
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "court_list_json", columnDefinition = "jsonb", nullable = false)
    @Type( type = "jsonb-node" )
    private JsonNode courtListJson;

    @Column(name = "last_updated", nullable = false)
    private ZonedDateTime lastUpdated;

    @Column(name = "last_exported")
    private ZonedDateTime lastExported;

    public PublishedCourtList() {
    }

    public PublishedCourtList(final UUID courtCentreId,
                              final PublishCourtListType publishCourtListType,
                              final LocalDate startDate,
                              final JsonNode courtListJson,
                              final ZonedDateTime lastUpdated,
                              final ZonedDateTime lastExported) {
        this.courtCentreId = courtCentreId;
        this.publishCourtListType = publishCourtListType;
        this.startDate = startDate;
        this.courtListJson = courtListJson;
        this.lastUpdated = lastUpdated;
        this.lastExported = lastExported;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreId(final UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public PublishCourtListType getPublishCourtListType() {
        return publishCourtListType;
    }

    public void setPublishCourtListType(final PublishCourtListType publishCourtListType) {
        this.publishCourtListType = publishCourtListType;
    }

    public JsonNode getCourtListJson() {
        return courtListJson;
    }

    public void setCourtListJson(final JsonNode courtListJson) {
        this.courtListJson = courtListJson;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(final ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public ZonedDateTime getLastExported() {
        return lastExported;
    }

    public void setLastExported(final ZonedDateTime lastExported) {
        this.lastExported = lastExported;
    }
}
