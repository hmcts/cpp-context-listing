package uk.gov.moj.cpp.listing.common;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.common.converter.LocalDates.from;

@RunWith(MockitoJUnitRunner.class)
public class NoteUUIDServiceTest {

    @InjectMocks
    NoteUUIDService uuidService;

    private UUID courtRoomId;
    private LocalDate courtSessionDate;

    @Before
    public void setUp() {
        courtRoomId = UUID.randomUUID();
        courtSessionDate = LocalDate.now();
    }

    @Test
    public void shouldReturnSameUuidForSameInput() {
        final UUID courtListId = uuidService.getNoteId(courtRoomId, courtSessionDate);
        assertThat(courtListId, is(notNullValue()));

        final UUID courtListId2 = uuidService.getNoteId(courtRoomId, courtSessionDate);

        assertThat(courtListId2, is(courtListId));
    }

    @Test
    public void shouldReturnDifferentUuidForDifferentInputs() {
        final UUID courtListId = uuidService.getNoteId(courtRoomId, courtSessionDate);
        assertThat(courtListId, is(notNullValue()));

        final UUID courtListId2 = uuidService.getNoteId(UUID.randomUUID(), courtSessionDate);
        assertThat(courtListId2, is(not(courtListId)));
    }

    @Test
    public void shouldReturnSameUuidListForSameInputs() {
        List<UUID> expectedList = Lists.newArrayList(fromString("329d8d2a-9b93-3a40-813d-8ee12017fb28"), fromString("07d805cc-192f-3378-968c-6a25dd331123"), fromString("5294a6cd-b64e-366f-a181-00ea00021cd0"));

        List<NoteUUIDService.ListingNotesCollection> notes = Lists.newArrayList(new NoteUUIDService.ListingNotesCollection(fromString("119d2a2e-10a2-46ac-aa8a-b1aea3389639"),from("2020-09-03")),
                new NoteUUIDService.ListingNotesCollection(fromString("5ebe60c8-e831-41dc-8442-d94df3982139"),from("2020-09-03")),
                new NoteUUIDService.ListingNotesCollection(fromString("02d596c1-0d7a-4567-bff3-1f1c8eed90cd"),from("2020-09-03")));

        final List<UUID> notesIds = uuidService.getNoteId(notes);
        assertThat(notesIds, is(notNullValue()));
        assertThat(expectedList, is(notesIds));
    }
}
