package uk.gov.moj.cpp.listing.query.view.service;

import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.listing.common.NoteUUIDService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.NotesRepository;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

@RunWith(MockitoJUnitRunner.class)
public class NotesServiceTest {
    private static final boolean ALLOCATED = true;
    private static final boolean UNALLOCATED = false;
    private static final String ALLOCATEDSTR = "true";
    private static final String UNALLOCATEDSTR = "false";
    private static final String queryDate = "2020-09-03";
    private static final String queryCourtRoomId = "6e424105-55f4-4e1a-bb9e-6ffbae3f7c17";
    public static final String EARLIEST_SEARCH_DATE = "1900-01-01";

    @Mock
    NotesRepository notesRepository;

    @Spy
    private NoteUUIDService noteUUIDService = new NoteUUIDService();

    @InjectMocks
    NotesService notesService;

    List<Notes> expectedNotes;
    List<NoteUUIDService.ListingNotesCollection> notes;
    List<UUID> expectedIds;

    ArgumentCaptor<List> queryListCaptor = ArgumentCaptor.forClass(List.class);

    @Before
    public void setup(){
        expectedNotes = IntStream.range(0, 2).mapToObj(i-> new Notes(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), STRING.next())).
                collect(Collectors.toList());
        expectedIds =  expectedNotes.stream().map(n->noteUUIDService.getNoteId(n.getCourtRoomId(), n.getDate())).collect(Collectors.toList());
        notes = expectedNotes.stream().map( n -> new NoteUUIDService.ListingNotesCollection(n.getCourtRoomId(), n.getDate())).collect(Collectors.toList());
    }

    @Test
    public void shouldReturnNotesListWhenRecordExists(){
        when(notesRepository.findNotes(expectedIds)).thenReturn(expectedNotes);

        List<Notes> result = notesService.findNotes(notes);

        verify(notesRepository, times(1)).findNotes(expectedIds);
        assertEquals( expectedNotes.toString(), result.toString());
    }

    @Test
    public void shouldReturnEmptyListWhenNoRecordExist(){
        List<UUID> expectedIds =  expectedNotes.stream().map(n->noteUUIDService.getNoteId(n.getCourtRoomId(), n.getDate())).collect(Collectors.toList());
        when(notesRepository.findNotes(expectedIds)).thenReturn(new ArrayList<>());

        List<Notes> result = notesService.findNotes(notes);

        verify(notesRepository, times(1)).findNotes(expectedIds);
        assertEquals( 0, result.size());
    }

    @Test
    public void shouldReturnEmptyListWhenQueryListIsEmpty(){
        List<UUID> expectedIds = new ArrayList<>();

        List<Notes> result = notesService.findNotes(new ArrayList<NoteUUIDService.ListingNotesCollection>());

        verify(notesRepository, times(0)).findNotes(expectedIds);
        assertEquals( 0, result.size());
    }

    @Test
    public void shouldPassDistinctArgumentsToSQL(){
        when(notesRepository.findNotes(expectedIds)).thenReturn(expectedNotes);

        notes.add(new NoteUUIDService.ListingNotesCollection(notes.get(0).getCourtRmId(), notes.get(0).getNoteDate()));
        List<Notes> result = notesService.findNotes(notes);
        verify(notesRepository, times(1)).findNotes(queryListCaptor.capture());
        assertEquals(2, queryListCaptor.getValue().size());
        assertEquals(expectedIds.get(0), queryListCaptor.getValue().get(0));
        assertEquals(expectedIds.get(1), queryListCaptor.getValue().get(1));
    }

    @Test
    public void ShouldReturnEmptyArrayForUnAllocatedHearings(){
        final List<Hearing> hearings = hearingsJson(UNALLOCATEDSTR);
        final List<Notes> result = notesService.findNotes(UNALLOCATED, queryCourtRoomId, queryDate, hearings);
        verify(notesRepository, times(0)).findNotes(any(List.class));
        assertTrue(result.isEmpty());
    }

    @Test
    public void ShouldReturnNotesArrayForAllocatedHearings(){
        final List<Hearing> hearings = hearingsJson(ALLOCATEDSTR);
        when(notesRepository.findNotes(any(List.class))).thenReturn(expectedNotes);
        final List<Notes> result = notesService.findNotes(ALLOCATED, null, null, hearings);
        verify(notesRepository, times(1)).findNotes(queryListCaptor.capture());
        assertEquals(2, result.size());
        assertEquals(2, queryListCaptor.getValue().size());
    }

    @Test
    public void ShouldReturnNotesArrayForAllocatedHearingsNotFailForNullRoomId(){
        final List<Hearing> hearings = hearingsJsonWithNullRoomId(ALLOCATEDSTR);
        when(notesRepository.findNotes(any(List.class))).thenReturn(expectedNotes);
        final List<Notes> result = notesService.findNotes(ALLOCATED, null, null, hearings);
        verify(notesRepository, times(1)).findNotes(queryListCaptor.capture());
        assertEquals(2, result.size());
        assertEquals(1, queryListCaptor.getValue().size());
    }

    @Test
    public void shouldAddQueryParamsToArgListWhenTheyAreExists(){
        final List<Hearing> hearings = hearingsJson(ALLOCATEDSTR);
        when(notesRepository.findNotes(any(List.class))).thenReturn(expectedNotes);
        final List<Notes> result = notesService.findNotes(ALLOCATED, queryCourtRoomId, queryDate, hearings);
        verify(notesRepository, times(1)).findNotes(queryListCaptor.capture());
        assertEquals(2, result.size());
        assertEquals(3, queryListCaptor.getValue().size());
    }

    @Test
    public void shouldNotAddQueryParamsToArgListWhenDateIsEARLIEST_SEARCH_DATE(){
        final List<Hearing> hearings = hearingsJson(ALLOCATEDSTR);
        when(notesRepository.findNotes(any(List.class))).thenReturn(expectedNotes);
        final List<Notes> result = notesService.findNotes(ALLOCATED, queryCourtRoomId, EARLIEST_SEARCH_DATE, hearings);
        verify(notesRepository, times(1)).findNotes(queryListCaptor.capture());
        assertEquals(2, result.size());
        assertEquals(2, queryListCaptor.getValue().size());
    }

    @Test
    public void shouldNotAddQueryParamsToArgListWhenDateIsNull(){
        final List<Hearing> hearings = hearingsJson(ALLOCATEDSTR);
        when(notesRepository.findNotes(any(List.class))).thenReturn(expectedNotes);
        final List<Notes> result = notesService.findNotes(ALLOCATED, queryCourtRoomId, null, hearings);
        verify(notesRepository, times(1)).findNotes(queryListCaptor.capture());
        assertEquals(2, result.size());
        assertEquals(2, queryListCaptor.getValue().size());
    }

    @Test
    public void shouldReturn2NotesForHearingDays(){
        final List<Hearing> hearings = hearingsJsonWithHearingDays(ALLOCATEDSTR);
        when(notesRepository.findNotes(any(List.class))).thenReturn(expectedNotes);
        final List<Notes> result = notesService.findNotes(ALLOCATED, null, null, hearings);
        verify(notesRepository, times(1)).findNotes(queryListCaptor.capture());
        assertEquals(2, queryListCaptor.getValue().size());
    }

    private List<Hearing> hearingsJsonWithHearingDays(String allocated) {
        final String testJsonString = "{ \"allocated\":\"" + allocated+ "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"hearingDays\": [ {\"hearingDate\":\"2020-09-03\"},{\"hearingDate\":\"2020-09-04\"}], \"listedCases\" : [{}] }";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        return newArrayList(hearing1);
    }

    private List<Hearing> hearingsJson(String allocated) {
        final String testJsonString = "{ \"allocated\":\"" + allocated+ "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final String testJsonString2 = "{ \"allocated\":\"" + allocated+ "\", \"startDate\": \"2020-09-03\", \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c19\", \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString2));
        return newArrayList(hearing1, hearing2);
    }

    private List<Hearing> hearingsJsonWithNullRoomId(String allocated) {
        final String testJsonString = "{ \"allocated\":\"" + allocated+ "\", \"startDate\": \"2020-09-03\",  \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final String testJsonString2 = "{ \"allocated\":\"" + allocated+ "\", \"startDate\": \"2020-09-03\",  \"courtRoomId\": \"6e424105-55f4-4e1a-bb9e-6ffbae3f7c18\", \"courtApplications\" : [{}] , \"listedCases\" : [{}] }";
        final Hearing hearing1 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString));
        final Hearing hearing2 = new Hearing(randomUUID(), JacksonUtil.toJsonNode(testJsonString2));
        return newArrayList(hearing1, hearing2);
    }
}
