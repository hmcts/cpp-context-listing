package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.with;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.POLL_INTERVAL;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.*;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;

import org.junit.jupiter.api.*;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.listing.it.util.ViewStoreCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.NotesSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.Filter;
import io.restassured.path.json.JsonPath;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.CoreMatchers;

@SuppressWarnings({"squid:S1607"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ListingNoteIT extends AbstractIT {

    private static final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private static final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();
    private static final String PUBLIC_EVENT_SELECTED_NOTE_EDITED = "public.listing.note-edited";
    private static final String PUBLIC_LISTING_CREATED_LISTING_NOTE = "public.listing.created-listing-note";
    private static final String PUBLIC_LISTING_DELETED_LISTING_NOTE = "public.listing.deleted-listing-note";
    private static final int DELAY = 0;
    private static final int TIMEOUT = 5;
    public static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";
    public static final String NOTE_DESCRIPTION = "note description";

    private final NotesSteps notesSteps = new NotesSteps();

    private Connection connection;

    @BeforeAll
    void setUpAll() {
        connection = testJdbcConnectionProvider.getViewStoreConnection("listing");
    }

    @AfterAll
    void tearDownAll() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    @Test
    void shouldCreateNoteForListing() {
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        final LocalDate date = ItClock.today();
        final UUID courtRoomId = getRandomCourtRoomId();
        final JmsMessageConsumerClient messageConsumerClientPublicForCreateNote = newPublicJmsMessageConsumerClientProvider()
                .withEventNames( PUBLIC_LISTING_CREATED_LISTING_NOTE).getMessageConsumerClient();

        notesSteps.createNoteForListing(courtRoomId, date.toString(), NOTE_DESCRIPTION);
        final UUID noteId = verifyNoteExists(courtRoomId, date);
        assertThat(noteId.toString(), CoreMatchers.is(notNullValue()));
        verifyInMessagingQueueForCreateRole(messageConsumerClientPublicForCreateNote, noteId.toString(), courtRoomId.toString(), date, NOTE_DESCRIPTION);
    }

    @Test
    void shouldEditNote() {
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        //Given 1 : A note created using create note command
        UUID noteId = createRandomNote(ItClock.today());

        //Given 2 :  consumer topic with selector "public.listing.note-edited" to capture public event raised
        final JmsMessageConsumerClient messageConsumerClientPublicForEditNote = newPublicJmsMessageConsumerClientProvider()
                .withEventNames( PUBLIC_EVENT_SELECTED_NOTE_EDITED).getMessageConsumerClient();

        //when
        String editedNoteDescription = "edited note description";
        notesSteps.editNoteForListing(noteId, editedNoteDescription);

        //then 1 : verify DB values
        verifyNoteDescriptionModified(noteId, editedNoteDescription);

        //then 2 : verify public event values
        final JsonPath jsonResponse = messageConsumerClientPublicForEditNote.retrieveMessageAsJsonPath().get();
        assertThat(jsonResponse.get("noteId"), is(noteId.toString()));
        assertThat(jsonResponse.get("noteDescription"), is(editedNoteDescription));

    }

    @Test
    void shouldDeleteListingNote() {
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        //Given : A note created using create note command
        final LocalDate hearingDate = ItClock.today();
        final UUID noteId = createRandomNote(hearingDate);
        final JmsMessageConsumerClient  messageConsumerClientPublicForDeleteNote = newPublicJmsMessageConsumerClientProvider()
                .withEventNames( PUBLIC_LISTING_DELETED_LISTING_NOTE).getMessageConsumerClient();
        //When:  delete note
        notesSteps.deleteNoteForListing(noteId);
        // then : verify note deleted
        final UUID noteIdRetrieved = verifyNoteDeleted(noteId, hearingDate);
        assertThat(noteIdRetrieved, is(nullValue()));
        // Then: verify public messaging queue
        JsonPath message = messageConsumerClientPublicForDeleteNote.retrieveMessageAsJsonPath().get();
        assertThat(message.get("noteId"), is(noteId.toString()));
    }

    @Test
    void shouldReturnNotesForAllocatedHearingOnSearchQuery() {

        //Given 1 : Hearing data
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate();
        List<HearingData> hearingData = hearingsData.getHearingData();
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(ItClock.today(), ImmutableMap.of("courtRoomId", hearingData.get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(hearingData.get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ItClock.nowLondon().withHour(10).withMinute(0).withSecond(0).withNano(0));
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        //Given 2 : Note data using courtRoomId and date from hearing data
        notesSteps.createNoteForListing(hearingData.get(0).getCourtRoomId(), hearingData.get(0).getHearingStartDate().toString(), NOTE_DESCRIPTION);
        UUID noteId = verifyNoteExists(hearingData.get(0).getCourtRoomId(), hearingData.get(0).getHearingStartDate());

        List<UUID> noteIds = new ArrayList<>();
        noteIds.add(noteId);

        //When and Then
        verifyHearingDataWithNoteData(noteIds, hearingData);
    }

    @Test
    void shouldNotReturnNotesForAllocatedHearingIfNoteNotExistForThatHearingOnSearchQuery() {

        //Given 1 : Hearing data and no Note data for this hearing
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        List<HearingData> hearingData = hearingsData.getHearingData();
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(ItClock.today(), ImmutableMap.of("courtRoomId", hearingData.get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(hearingData.get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ItClock.nowLondon().withHour(10).withMinute(0).withSecond(0).withNano(0));
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        //When and Then
        verifyHearingDataWithoutNoteData(hearingsData.getHearingData());
    }


    @Test
    void shouldReturnNotesGivenNoHearingExistForCourtRoomIdAndDateOnSearchQuery() {

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        //Given 1 : No Hearing data but Note data exist for given courtRoom and date
        UUID courtRoomId = getRandomCourtRoomId();
        LocalDate startDate = ItClock.today();
        notesSteps.createNoteForListing(courtRoomId, startDate.toString(), NOTE_DESCRIPTION);
        UUID noteId = verifyNoteExists(courtRoomId, startDate);

        //When and Then
        verifyNoHearingDataButWithNoteData(courtRoomId, startDate, noteId);
    }

    @Test
    void shouldNotReturnNotesAndHearingDataOnSearchQueryWithUnallocatedQueryParam() {

        //Given 1 : Hearing data
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        List<HearingData> hearingData = hearingsData.getHearingData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(ItClock.today(), ImmutableMap.of("courtRoomId", hearingData.get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(hearingData.get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ItClock.nowLondon().withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        //Given 2 : Note data using courtRoomId and date from hearing data. noteId is derived
        //server-side from (courtRoomId, hearingDate) -> de-duplicate, or the second create for a
        //shared courtroom/date logs ERROR "Note already exists" in the aggregate and no-ops.
        List<UUID> noteIds = new ArrayList<>();
        hearingData.stream()
                .map(hearingData1 -> java.util.Map.entry(hearingData1.getCourtRoomId(), hearingData1.getHearingStartDate()))
                .distinct()
                .forEach(roomAndDate -> {
                            notesSteps.createNoteForListing(roomAndDate.getKey(), roomAndDate.getValue().toString(), NOTE_DESCRIPTION);
                            noteIds.add(verifyNoteExists(roomAndDate.getKey(), roomAndDate.getValue()));
                        }
                );

        //When and Then
        verifyNoHearingDataAndNoNoteData(hearingData);
    }

    @AfterEach
    void cleanUp() {
        viewStoreCleaner.cleanViewStoreTables("listing_notes","hearing");
    }

    private void verifyNoHearingDataAndNoNoteData(List<HearingData> hearingData) {
        final Filter hearingsArraySizeFilter = getArraySizeFilter("hearings", 0);
        final Filter noteArraySizeFilter = getArraySizeFilter("notes", 0);

        pollWithDefaults(requestParams(getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(false, hearingData.get(0).getCourtRoomId(), hearingData.get(0).getHearingStartDate()),
                MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
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

        pollWithDefaults(requestParams(getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(true, courtRoomId, startDate),
                MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
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

        pollWithDefaults(requestParams(getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(hearingData),
                MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
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

        pollWithDefaults(requestParams(getSearchHearingUrlByCourtRoomIdAndSearchDateAndAllocated(hearingData),
                MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
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
        final UUID courtRoomId = getRandomCourtRoomId();

        notesSteps.createNoteForListing(courtRoomId, date.toString(), NOTE_DESCRIPTION);
        return verifyNoteExists(courtRoomId, date);
    }

    private void verifyInMessagingQueueForCreateRole(JmsMessageConsumerClient messageConsumerClient, String noteId, String courtRoomId, LocalDate date, final String noteDescription) {
        JsonPath message = messageConsumerClient.retrieveMessageAsJsonPath().get();

        assertThat(message.get("id"), is(noteId));
        assertThat(message.get("date"), is(date.toString()));
        assertThat(message.get("courtRoomId"), is(courtRoomId));
        assertThat(message.get("note"), is(noteDescription));
    }

    private void verifyNoteDescriptionModified(final UUID noteId, final String editedNoteDescription) {
        try {
            with().pollDelay(DELAY, MILLISECONDS)
                    .and()
                    .pollInterval(POLL_INTERVAL)
                    .atMost(TIMEOUT, TimeUnit.SECONDS)
                    .until(() -> getNoteById(noteId, editedNoteDescription, connection));
        } catch (ConditionTimeoutException e) {
            throw e;
        }
    }

    private UUID verifyNoteExists(UUID courtRoomId, LocalDate date) {
        AtomicReference<UUID> noteByCourtRoomIdAndDate = new AtomicReference<>();
        with().pollDelay(DELAY, MILLISECONDS)
                .and()
                .pollInterval(POLL_INTERVAL)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    noteByCourtRoomIdAndDate.set(getNoteByCourtRoomIdAndDate(courtRoomId, date, connection));
                    return noteByCourtRoomIdAndDate.get() != null;
                });
        return noteByCourtRoomIdAndDate.get();
    }

    private UUID verifyNoteDeleted(UUID courtRoomId, LocalDate date) {
        with().pollDelay(DELAY, MILLISECONDS)
                .and()
                .pollInterval(POLL_INTERVAL)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getNoteByCourtRoomIdAndDate(courtRoomId, date, connection) == null);
        return null;
    }


    private boolean getNoteById(final UUID noteId, final String editedNoteDescription, final Connection connection) {

        final String queryTemplate = "select id, note from listing_notes where id = ?";
        try (final PreparedStatement statement = connection.prepareStatement(queryTemplate)) {
            ResultSet resultSet;
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


    private static UUID getNoteByCourtRoomIdAndDate(final UUID courtRoomId, final LocalDate date, final Connection connection) {

        UUID noteId = null;
        final String queryTemplate = "select id from listing_notes where court_room_id = ? and  date = ?";
        try (final PreparedStatement statement = connection.prepareStatement(queryTemplate)) {
            ResultSet resultSet;
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
