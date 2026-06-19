package uk.gov.moj.cpp.listing.common;

import static java.util.UUID.nameUUIDFromBytes;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NoteUUIDService {


    public UUID getNoteId(final UUID courtRmId, final LocalDate noteDate) {
        final String courtRoomId = courtRmId.toString();
        final String date = noteDate.toString();
        final String listingNotesId = String.format("%s/%s", courtRoomId, date);
        return nameUUIDFromBytes(listingNotesId.getBytes());
    }

    public List<UUID> getNoteId(final List<ListingNotesCollection> notes){
        return notes.stream().map( noteArg -> getNoteId(noteArg.courtRmId, noteArg.noteDate)).collect(Collectors.toList());
    }

    public static class ListingNotesCollection{
        UUID courtRmId;
        LocalDate noteDate;


        public ListingNotesCollection(UUID courtRmId, LocalDate noteDate) {
            this.courtRmId = courtRmId;
            this.noteDate = noteDate;
        }

        public UUID getCourtRmId() {
            return courtRmId;
        }

        public LocalDate getNoteDate() {
            return noteDate;
        }

        public void setCourtRmId(UUID courtRmId) {
            this.courtRmId = courtRmId;
        }

        public void setNoteDate(LocalDate noteDate) {
            this.noteDate = noteDate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o){
                return true;
            }
            if (o == null || getClass() != o.getClass()){
                return false;
            }
            final ListingNotesCollection that = (ListingNotesCollection) o;
            return Objects.equals(courtRmId, that.courtRmId) &&
                    Objects.equals(noteDate, that.noteDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(courtRmId, noteDate);
        }
    }
}

