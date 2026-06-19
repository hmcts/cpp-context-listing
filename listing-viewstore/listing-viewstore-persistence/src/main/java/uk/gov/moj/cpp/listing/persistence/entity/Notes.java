package uk.gov.moj.cpp.listing.persistence.entity;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import static uk.gov.justice.services.common.converter.LocalDates.to;

@Entity
@Table(name = "listing_notes")
public class Notes {

    @Id
    private UUID id;

    @Column(name = "court_room_id")
    private UUID courtRoomId;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "note")
    private String note;

    public Notes() {
    }

    public Notes(UUID id, UUID courtRoomId, LocalDate date, String note) {
        this.id = id;
        this.courtRoomId = courtRoomId;
        this.date = date;
        this.note = note;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCourtRoomId(UUID courtRoomId) {
        this.courtRoomId = courtRoomId;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return "{ \"courtRoomId\":\"" + courtRoomId +"\""+
                ", \"date\":\"" + to(date)+"\"}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Notes notes = (Notes) o;
        return id.equals(notes.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
