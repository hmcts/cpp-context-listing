package uk.gov.moj.cpp.listing.it;

import com.jayway.awaitility.core.ConditionTimeoutException;
import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.listing.it.util.ViewStoreCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.NotesSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.awaitility.Awaitility.with;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.LocalDate.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

public class ListingNoteIT extends AbstractIT {

    private static final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private static final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();
    private static final String PUBLIC_EVENT_SELECTED_NOTE_EDITED = "public.listing.note-edited";
    private static final String PUBLIC_LISTING_CREATED_LISTING_NOTE = "public.listing.created-listing-note";
    private static final String PUBLIC_LISTING_DELETED_LISTING_NOTE = "public.listing.deleted-listing-note";
    private static final int DELAY = 600;
    private static final int POLL_INTERVAL = 800;
    private static final int TIMEOUT = 5;
    public static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";
    public static final String NOTE_DESCRIPTION = "note description";


    private MessageConsumer messageConsumerClientPublicForEditNote;
    private MessageConsumer messageConsumerClientPublicForCreateNote;
    private MessageConsumer messageConsumerClientPublicForDeleteNote;

    private NotesSteps notesSteps = new NotesSteps();

    @Test
    public void shouldCreateNoteForListing() {
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        final LocalDate date = now();
        final UUID courtRoomId = randomUUID();
        messageConsumerClientPublicForCreateNote = publicEvents.createConsumer(PUBLIC_LISTING_CREATED_LISTING_NOTE);

        notesSteps.createNoteForListing(courtRoomId, date.toString(), NOTE_DESCRIPTION);
        final UUID noteId = verifyNoteExists(courtRoomId, date);
        Assert.assertThat(noteId.toString(), CoreMatchers.is(notNullValue()));
        verifyInMessagingQueueForCreateRole(noteId.toString(),courtRoomId.toString(),date,NOTE_DESCRIPTION);
    }

    @Test
    public void shouldEditNote() {
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        //Given 1 : A note created using create note command
        UUID noteId = createRandomNote(now());

        //Given 2 :  consumer topic with selector "public.listing.note-edited" to capture public event raised
        messageConsumerClientPublicForEditNote = publicEvents.createConsumer(PUBLIC_EVENT_SELECTED_NOTE_EDITED);

        //when
        String editedNoteDescription = "edited note description";
        notesSteps.editNoteForListing(noteId, editedNoteDescription);

        //then 1 : verify DB values
        verifyNoteDescriptionModified(noteId, editedNoteDescription);

        //then 2 : verify public event values
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(messageConsumerClientPublicForEditNote);
        assertThat(jsonResponse.get("noteId"), is(noteId.toString()));
        assertThat(jsonResponse.get("noteDescription"), is(editedNoteDescription));

    }

    @Test
    public void shouldDeleteListingNote() {
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        //Given : A note created using create note command
        final LocalDate hearingDate = now();
        final UUID noteId = createRandomNote(hearingDate);
        messageConsumerClientPublicForDeleteNote = publicEvents.createConsumer(PUBLIC_LISTING_DELETED_LISTING_NOTE);
        //When:  delete note
        notesSteps.deleteNoteForListing(noteId);
        // then : verify note deleted
        final UUID noteIdRetrieved = verifyNoteExists(noteId, hearingDate);
        assertThat(noteIdRetrieved, is(nullValue()));
        // Then: verify public messaging queue
        JsonPath message = QueueUtil.retrieveMessage(messageConsumerClientPublicForDeleteNote);
        assertThat(message.get("noteId"), is(noteId.toString()));
    }

    @Test
    public void shouldReturnNotesForAllocatedHearingOnSearchQuery() {

        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now());
        //Given 1 : Hearing data
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        //Given 2 : Note data using courtRoomId and date from hearing data
        List<HearingData> hearingData = hearingsData.getHearingData();
        notesSteps.createNoteForListing(hearingData.get(0).getCourtRoomId(), hearingData.get(0).getHearingStartDate().toString(), NOTE_DESCRIPTION);
        UUID noteId = verifyNoteExists(hearingData.get(0).getCourtRoomId(), hearingData.get(0).getHearingStartDate());

        List<UUID> noteIds = new ArrayList<>();
        noteIds.add(noteId);

        //When and Then
        verifyHearingDataWithNoteData(noteIds, hearingData);
    }

    @Test
    public void shouldNotReturnNotesForAllocatedHearingIfNoteNotExistForThatHearingOnSearchQuery() {

        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now());
        //Given 1 : Hearing data and no Note data for this hearing
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        //When and Then
        verifyHearingDataWithoutNoteData(hearingsData.getHearingData());
    }


    @Test
    public void shouldReturnNotesGivenNoHearingExistForCourtRoomIdAndDateOnSearchQuery() {

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);

        //Given 1 : No Hearing data but Note data exist for given courtRoom and date
        UUID courtRoomId = randomUUID();
        LocalDate startDate = now();
        notesSteps.createNoteForListing(courtRoomId, startDate.toString(), NOTE_DESCRIPTION);
        UUID noteId = verifyNoteExists(courtRoomId, startDate);

        //When and Then
        verifyNoHearingDataButWithNoteData(courtRoomId, startDate, noteId);
    }

    @Test
    public void shouldNotReturnNotesAndHearingDataOnSearchQueryWithUnallocatedQueryParam() {

        //Given 1 : Hearing data
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        //Given 2 : Note data using courtRoomId and date from hearing data
        List<HearingData> hearingData = hearingsData.getHearingData();
        List<UUID> noteIds = new ArrayList<>();
        hearingData
                .forEach(hearingData1 -> {
                            notesSteps.createNoteForListing(hearingData1.getCourtRoomId(), hearingData1.getHearingStartDate().toString(), NOTE_DESCRIPTION);
                            noteIds.add(verifyNoteExists(hearingData1.getCourtRoomId(), hearingData1.getHearingStartDate()));
                        }
                );

        //When and Then
        verifyNoHearingDataAndNoNoteData(hearingData);
    }

    @After
    public void cleanUp() throws JMSException {
        if (messageConsumerClientPublicForCreateNote != null)
            messageConsumerClientPublicForCreateNote.close();
        if (messageConsumerClientPublicForEditNote != null)
            messageConsumerClientPublicForEditNote.close();
        if (messageConsumerClientPublicForDeleteNote != null) {
            messageConsumerClientPublicForDeleteNote.close();
        }
        viewStoreCleaner.cleanViewStoreTables("listing_notes");
        viewStoreCleaner.cleanViewStoreTables("hearing");
    }

    private void verifyNoHearingDataAndNoNoteData(List<HearingData> hearingData) {
        final Filter hearingsArraySizeFilter = getArraySizeFilter("hearings", 0);
        final Filter noteArraySizeFilter = getArraySizeFilter("notes", 0);

        poll(requestParams(getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(false, hearingData.get(0).getCourtRoomId(), hearingData.get(0).getHearingStartDate()),
                MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(getFilteredPath("$[?]", hearingsArraySizeFilter)),
                                withJsonPath(getFilteredPath("$[?]", noteArraySizeFilter))
                        )));
    }

    private void verifyNoHearingDataButWithNoteData(UUID courtRoomId, LocalDate startDate, UUID noteId) {
        final Filter hearingsArraySizeFilter = getArraySizeFilter("hearings", 0);
        final Filter noteIdFilter = getFilter("id", noteId.toString());

        poll(requestParams(getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(true, courtRoomId, startDate),
                MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(getFilteredPath("$[?]", hearingsArraySizeFilter)),
                                withJsonPath(getFilteredPath("$.notes[?]", noteIdFilter))
                        )));
    }

    private void verifyHearingDataWithoutNoteData(List<HearingData> hearingData) {
        final Filter hearingIdFilter = getFilter("id", hearingData.get(0).getId().toString());
        final Filter noteArraySizeFilter = getArraySizeFilter("notes", 0);

        poll(requestParams(getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(hearingData),
                MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(getFilteredPath("$.hearings[?]", hearingIdFilter)),
                                withJsonPath(getFilteredPath("$[?]", noteArraySizeFilter))
                        )));
    }

    private void verifyHearingDataWithNoteData(List<UUID> noteIds, List<HearingData> hearingData) {
        final Filter hearingIdFilter = getFilter("id", hearingData.get(0).getId().toString());
        final Filter noteIdFilter = getFilter("id", noteIds.get(0).toString());
        final Filter noteDescriptionFilter = getFilter("note", NOTE_DESCRIPTION);
        final Filter courtRoomIdFilter = getFilter("courtRoomId", hearingData.get(0).getCourtRoomId().toString());

        poll(requestParams(getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(hearingData),
                MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(getFilteredPath("$.hearings[?]", hearingIdFilter)),
                                withJsonPath(getFilteredPath("$.notes[?]", noteIdFilter)),
                                withJsonPath(getFilteredPath("$.notes[?]", noteDescriptionFilter)),
                                withJsonPath(getFilteredPath("$.notes[?]", courtRoomIdFilter))
                        )));

    }

    private String getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(List<HearingData> hearingData) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-room-id.search-date"),
                        ALLOCATED,
                        hearingData.get(0).getCourtRoomId(),
                        hearingData.get(0).getHearingStartDate()));
    }

    private String getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(final boolean allocated, final UUID courtRoomId, final LocalDate startDate) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-room-id.search-date"),
                        allocated,
                        courtRoomId,
                        startDate));
    }

    private Filter getFilter(final String jsonPath, final String jsonValue) {
        return filter(where(jsonPath).is(jsonValue));
    }

    private Filter getArraySizeFilter(final String jsonPath, final int expectedArraySize) {
        return filter(where(jsonPath).size(expectedArraySize));
    }

    private com.jayway.jsonpath.JsonPath getFilteredPath(final String path, final Filter filter) {
        return com.jayway.jsonpath.JsonPath.compile(path, filter);
    }

    private UUID createRandomNote(final LocalDate date) {
        final UUID courtRoomId = randomUUID();

        notesSteps.createNoteForListing(courtRoomId, date.toString(), NOTE_DESCRIPTION);
        final UUID noteId = verifyNoteExists(courtRoomId, date);
        return noteId;
    }

    private void verifyInMessagingQueueForCreateRole(String noteId, String courtRoomId, LocalDate date, final String noteDescription) {
        JsonPath message = QueueUtil.retrieveMessage(messageConsumerClientPublicForCreateNote);

        assertThat(message.get("id"), is(noteId));
        assertThat(message.get("date"), is(date.toString()));
        assertThat(message.get("courtRoomId"), is(courtRoomId));
        assertThat(message.get("note"), is(noteDescription));
    }

    private void verifyNoteDescriptionModified(final UUID noteId, final String editedNoteDescription) {
        try {
            with().pollDelay(DELAY, MILLISECONDS)
                    .and()
                    .pollInterval(POLL_INTERVAL, MILLISECONDS)
                    .await().atMost(TIMEOUT, TimeUnit.SECONDS)
                    .until(() -> getNoteById(noteId, editedNoteDescription));
        } catch (ConditionTimeoutException e) {
            throw e;
        }
    }

    private UUID verifyNoteExists(UUID courtRoomId, LocalDate date) {
        AtomicReference<UUID> noteByCourtRoomIdAndDate = new AtomicReference<>();
        try {
            with().pollDelay(DELAY, MILLISECONDS)
                    .and()
                    .pollInterval(POLL_INTERVAL, MILLISECONDS)
                    .await().atMost(TIMEOUT, TimeUnit.SECONDS)
                    .until(() -> {
                        noteByCourtRoomIdAndDate.set(getNoteByCourtRoomIdAndDate(courtRoomId, date));
                        return noteByCourtRoomIdAndDate.get() != null;
                    });
        } catch (ConditionTimeoutException e) {
            e.printStackTrace();
        }
        return noteByCourtRoomIdAndDate.get();
    }


    private boolean getNoteById(final UUID noteId, final String editedNoteDescription) {

        final String queryTemplate = "select id, note from listing_notes where id = ?";
        try (final Connection sjpEventStoreConnection =
                     testJdbcConnectionProvider.getViewStoreConnection("listing");
             final PreparedStatement statement = sjpEventStoreConnection.prepareStatement(
                     queryTemplate)) {
            ResultSet resultSet = null;
            statement.setObject(1, noteId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                UUID uuid = fromString(resultSet.getString(1));
                String noteDesc = resultSet.getString(2);
                return (uuid != null && noteDesc != null && noteDesc.trim().equals(editedNoteDescription));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    private static UUID getNoteByCourtRoomIdAndDate(final UUID courtRoomId, final LocalDate date) {

        UUID noteId = null;
        final String queryTemplate = "select id from listing_notes where court_room_id = ? and  date = ?";
        try (final Connection sjpEventStoreConnection =
                     testJdbcConnectionProvider.getViewStoreConnection("listing");
             final PreparedStatement statement =
                     sjpEventStoreConnection.prepareStatement(queryTemplate)
        ) {
            ResultSet resultSet = null;
            statement.setObject(1, courtRoomId);
            statement.setDate(2, Date.valueOf(date));
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                noteId = fromString(resultSet.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return noteId;
    }

}
