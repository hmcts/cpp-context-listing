package uk.gov.moj.cpp.listing.query.view.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import uk.gov.moj.cpp.listing.common.NoteUUIDService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.NotesRepository;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.common.converter.LocalDates.from;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.EARLIEST_SEARCH_DATE;

@SuppressWarnings({"squid:S1612"})
@ApplicationScoped
public class NotesService {

    private static final String ALLOCATED_QUERY_PARAMETER = "allocated";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String START_DATE = "startDate";
    private static final String HEARING_DAYS = "hearingDays";
    private static final String HEARING_DATE = "hearingDate";

    @Inject
    private NoteUUIDService noteUUIDService;

    @Inject
    private NotesRepository notesRepository;

    public List<Notes> findNotes(List<NoteUUIDService.ListingNotesCollection> notes){
        if(notes.isEmpty()){
            return new ArrayList<>();
        }
        final List<UUID> ids = noteUUIDService.getNoteId(notes.stream().distinct().collect(Collectors.toList()));
        return notesRepository.findNotes(ids);
    }

    public List<Notes> findNotes(boolean allocated, String courtRoomId, String startDate, List<Hearing> hearings) {
        final List<NoteUUIDService.ListingNotesCollection> notesArg = hearings.stream().map(h -> h.getProperties()).
                filter(p -> p.get(ALLOCATED_QUERY_PARAMETER).asBoolean()).
                flatMap(n -> createNoteListCollection(n)).
                collect(Collectors.toList());
        final List<Notes> notes;
        if(allocated && courtRoomId != null && startDate != null && !startDate.equals(EARLIEST_SEARCH_DATE)){
            notesArg.add(new NoteUUIDService.ListingNotesCollection(fromString(courtRoomId), from(startDate)));
        }
        if( notesArg.isEmpty()) {
            notes = new ArrayList<>();
        }else{
            notes = findNotes(notesArg);
        }
        return notes;
    }

    private Stream<NoteUUIDService.ListingNotesCollection> createNoteListCollection(JsonNode node ){

        if (node.get(HEARING_DAYS) == null || node.get(HEARING_DAYS).findValues(HEARING_DATE).isEmpty() ) {
            return Stream.of(new NoteUUIDService.ListingNotesCollection(fromString(node.get(COURT_ROOM_ID).textValue()), from(node.get(START_DATE).textValue())));
        }else {
            return node.get(HEARING_DAYS).findValues(HEARING_DATE).stream().
                    map(n -> new NoteUUIDService.ListingNotesCollection(fromString(node.get(COURT_ROOM_ID).textValue()), from(n.textValue())));
        }
    }
}
