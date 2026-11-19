package uk.gov.moj.cpp.listing.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import uk.gov.moj.cpp.listing.command.hearing.api.HearingCommandApi;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

/** The Hearing command controllers (P2.3.2, P2.3.3): dispatch on Content-Type, RAML mapping names, path parameters, 202. */
class HearingCommandsControllersTest {

    private final HearingCommandApi commandApi = mock(HearingCommandApi.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final CaseCommandsApi cases = new CaseCommandsApi(commandApi, request);
    private final HearingCommandsApi hearings = new HearingCommandsApi(commandApi, request);
    private final CourtScheduleCommandsApi courtSchedules = new CourtScheduleCommandsApi(commandApi, request);
    private final ArgumentCaptor<Envelope> command = ArgumentCaptor.forClass(Envelope.class);

    @Test
    void listCourtHearingIsDispatchedOnContentTypeWithTheCallersMetadata() {
        contentType("listing.command.list-court-hearing");
        request.addHeader("CJSCPPUID", "user-1");

        assertThat(cases.postCases(Map.of("hearings", java.util.List.of())).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        verify(commandApi).handleListCourtHearing(command.capture());
        assertThat(command.getValue().name()).isEqualTo("listing.command.list-court-hearing");
        assertThat(command.getValue().metadata().userId()).contains("user-1");
        assertThat(command.getValue().payload().containsKey("hearings")).isTrue();
    }

    @Test
    void listUnscheduledCourtHearing() {
        contentType("listing.command.list-unscheduled-court-hearing");

        cases.postCases(Map.of());

        verify(commandApi).handleListUnscheduledCourtHearing(command.capture());
        assertThat(command.getValue().name()).isEqualTo("listing.command.list-unscheduled-court-hearing");
    }

    @Test
    void changeJudiciaryAndRestrictCourtListOnSlashHearings() {
        contentType("listing.command.change-judiciary-for-hearings");
        assertThat(hearings.postHearings(Map.of()).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(commandApi).handleChangeJudiciaryForHearings(command.capture());
        assertThat(command.getValue().name()).isEqualTo("listing.command.change-judiciary-for-hearings");

        request.setContentType("application/vnd.listing.command.restrict-court-list+json");
        hearings.postHearings(Map.of());
        verify(commandApi).handleRestrictCourtList(command.capture());
        assertThat(command.getValue().name()).isEqualTo("listing.command.restrict-court-list");
    }

    @Test
    void deletePreviousHearingsAndCreateNextHearingOnSlashHearings() {
        contentType("listing.delete-previous-hearings-and-create-next-hearing");

        assertThat(hearings.postHearings(Map.of()).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        verify(commandApi).handleDeletePreviousHearingsAndCreateNextHearing(command.capture());
        assertThat(command.getValue().name()).isEqualTo("listing.delete-previous-hearings-and-create-next-hearing");
    }

    @Test
    void theHearingIdOfThePathIsMergedIntoThePayload() {
        contentType("listing.command.update-hearing-for-listing");

        assertThat(hearings.postHearing("hearing-1", Map.of("hearingId", "other")).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        verify(commandApi).handleUpdateHearingForListing(command.capture());
        assertThat(command.getValue().payload().getString("hearingId")).isEqualTo("hearing-1");
    }

    @Test
    void eachHearingMediaTypeReachesItsHandler() {
        post("listing.command.extend-hearing-for-hearing");
        verify(commandApi).handleExtendHearingForHearing(command.capture());
        post("listing.command.vacate-trial");
        verify(commandApi).handleVacateTrial(command.capture());
        post("listing.command.move-hearing-to-past-date");
        verify(commandApi).handleMoveHearingToPastDate(command.capture());
        post("listing.command.change-court-room-for-multiday-hearing");
        verify(commandApi).handleChangeCourtRoomForMultidayHearing(command.capture());
        post("listing.update-hearing-add-case-bdf");
        verify(commandApi).handleUpdateHearingAddCaseBdf(command.capture());
        assertThat(command.getAllValues()).extracting(Envelope::name).containsExactly(
                "listing.command.extend-hearing-for-hearing", "listing.command.vacate-trial", "listing.command.move-hearing-to-past-date",
                "listing.command.change-court-room-for-multiday-hearing", "listing.update-hearing-add-case-bdf");
    }

    @Test
    void theP233MediaTypesOnTheHearingPathReachTheirHandlersUnderTheRamlMappingName() {
        post("listing.command.split-hearing");
        verify(commandApi).handleSplitHearing(command.capture());
        post("listing.next-hearings-v2");
        verify(commandApi).listNextHearings(command.capture());
        post("listing.list-unscheduled-next-hearings");
        verify(commandApi).handleListUnscheduledNextCourtHearings(command.capture());
        post("listing.related-hearing");
        verify(commandApi).updateRelatedHearing(command.capture());
        post("listing.delete-next-hearings");
        verify(commandApi).handleDeleteNextHearings(command.capture());
        assertThat(command.getAllValues()).extracting(Envelope::name).containsExactly(
                "listing.command.split-hearing", "listing.list-next-hearings-v2", "listing.list-unscheduled-next-hearings",
                "listing.update-related-hearing", "listing.delete-next-hearings");
        assertThat(command.getAllValues()).allSatisfy(sent -> assertThat(sent.payload().getString("hearingId")).isEqualTo("hearing-1"));
    }

    @Test
    void duplicateUnallocatedHearingNeedsNoBodyAndCarriesOnlyTheHearingId() {
        contentType("listing.duplicate-unallocated-hearing");

        assertThat(hearings.postHearing("hearing-1", null).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        verify(commandApi).handleMarkUnallocatedHearingAsDuplicate(command.capture());
        assertThat(command.getValue().name()).isEqualTo("listing.mark-unallocated-hearing-as-duplicate");
        assertThat(command.getValue().payload()).isEqualTo(jakarta.json.Json.createObjectBuilder().add("hearingId", "hearing-1").build());
    }

    @Test
    void aMissingBodyIsABadRequestForTheOtherMediaTypes() {
        contentType("listing.command.update-hearing-for-listing");

        assertThatThrownBy(() -> hearings.postHearing("hearing-1", null))
                .isInstanceOf(org.springframework.http.converter.HttpMessageNotReadableException.class);
        verifyNoInteractions(commandApi);
    }

    @ParameterizedTest
    @ValueSource(strings = {"listing.command.update-defendants-for-hearing", "listing.command.delete-offences-for-hearing",
            "listing.command.add-offences-for-hearing", "listing.command.update-offences-for-hearing"})
    void theRamlOnlyHearingCommandsArePassedThrough(final String action) {
        post(action);

        verify(commandApi).handlePassThroughHearingCommand(command.capture());
        assertThat(command.getValue().name()).isEqualTo(action);
        assertThat(command.getValue().payload().getString("hearingId")).isEqualTo("hearing-1");
    }

    @Test
    void updateHearingsSequenceAndDelete() {
        assertThat(hearings.postUpdateHearings(Map.of()).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(commandApi).handleUpdateHearingsForListing(command.capture());
        assertThat(hearings.postSequenceHearings(Map.of()).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(commandApi).handleSequenceHearings(command.capture());
        assertThat(hearings.postDeleteHearing("hearing-2", Map.of()).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(commandApi).handleDeleteHearing(command.capture());

        assertThat(command.getAllValues()).extracting(Envelope::name).containsExactly(
                "listing.command.update-hearings-for-listing", "listing.command.sequence-hearings", "listing.delete-hearing");
        assertThat(command.getAllValues().get(2).payload().getString("hearingId")).isEqualTo("hearing-2");
    }

    @Test
    void updateHearingDayCourtScheduleAndMigrateAre202() {
        assertThat(courtSchedules.postUpdateHearingDayCourtSchedule(Map.of()).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(commandApi).handleUpdateHearingDayCourtSchedule(command.capture());
        assertThat(command.getValue().name()).isEqualTo("listing.update-hearing-day-court-schedule");

        assertThat(courtSchedules.postMigrateCrownHearingsToCourtSchedules(Map.of()).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(commandApi).handleMigrateCrownHearingsToCourtSchedules(command.capture());
        assertThat(command.getValue().name()).isEqualTo("listing.migrate-crown-hearings-to-courtschedules");
    }

    private void post(final String action) {
        contentType(action);
        assertThat(hearings.postHearing("hearing-1", Map.of()).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private void contentType(final String action) {
        request.setContentType("application/vnd." + action + "+json");
    }
}
