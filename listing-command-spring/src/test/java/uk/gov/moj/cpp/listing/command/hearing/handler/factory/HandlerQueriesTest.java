package uk.gov.moj.cpp.listing.command.hearing.handler.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.command.hearing.handler.service.HearingService;
import uk.gov.moj.cpp.listing.command.hearing.handler.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The handler's reference-data and viewstore reads (ported factories/services, MBD refactor P2.3.2). */
@ExtendWith(MockitoExtension.class)
class HandlerQueriesTest {

    @Mock
    private CppQueryClient cppQueryClient;
    @Mock
    private HearingQueryView hearingQueryView;

    @Test
    void hearingTypesAreQueriedAsAdminAndMappedToDefaultDurations() {
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.all-hearing-types"), any())).thenReturn(Optional.of(Json.createObjectBuilder()
                .add("hearingTypes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("id", "t1").add("defaultDurationMin", 30))
                        .add(Json.createObjectBuilder().add("id", "t2").add("defaultDurationMin", 0)))
                .build()));

        final Map<String, Integer> map = new HearingTypeFactory(new ReferenceDataService(cppQueryClient)).getHearingTypesIdDurationMap();

        assertThat(map).containsExactlyInAnyOrderEntriesOf(Map.of("t1", 30, "t2", 0));
    }

    @Test
    void courtCentreIsQueriedAsAdminByIdAndCourtRoomNumberIsLookedUp() {
        final UUID courtCentreId = UUID.randomUUID();
        final JsonObject courtCentre = Json.createObjectBuilder().add("oucode", "B01")
                .add("courtrooms", Json.createArrayBuilder().add(Json.createObjectBuilder().add("id", "room-1").add("courtroomId", 7)))
                .build();
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.courtroom"), any())).thenReturn(Optional.of(courtCentre));

        final CourtCentreFactory factory = new CourtCentreFactory(new ReferenceDataService(cppQueryClient));

        assertThat(factory.getOrganisationUnit(courtCentreId)).isEqualTo(courtCentre);
        final ArgumentCaptor<JsonObject> params = ArgumentCaptor.forClass(JsonObject.class);
        verify(cppQueryClient).requestAsAdmin(eq("referencedata.query.courtroom"), params.capture());
        assertThat(params.getValue().getString("id")).isEqualTo(courtCentreId.toString());
        assertThat(factory.getCourtRoomNumber(courtCentre, "room-1")).contains(7);
        assertThat(factory.getCourtRoomNumber(courtCentre, "room-2")).isEmpty();
    }

    @Test
    void noReferenceDataResponseFailsTheCommand() {
        when(cppQueryClient.requestAsAdmin(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ReferenceDataService(cppQueryClient).getCourtCentreById(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void storedHearingIsReadFromTheViewstoreQueryAndConverted() {
        final UUID hearingId = UUID.randomUUID();
        when(hearingQueryView.getHearingById(any())).thenReturn(Json.createObjectBuilder()
                .add("id", hearingId.toString()).add("allocated", false).build());

        final uk.gov.moj.cpp.listing.persistence.repository.HearingRepository hearingRepository =
                org.mockito.Mockito.mock(uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.class);
        when(hearingRepository.findBy(hearingId)).thenReturn(new uk.gov.moj.cpp.listing.persistence.entity.Hearing());

        final uk.gov.justice.listing.events.Hearing hearing = new HearingFactory(new HearingService(hearingQueryView, hearingRepository)).getHearingById(hearingId);

        assertThat(hearing.getId()).isEqualTo(hearingId);
        assertThat(hearing.getAllocated()).isFalse();
        final ArgumentCaptor<JsonObject> query = ArgumentCaptor.forClass(JsonObject.class);
        verify(hearingQueryView).getHearingById(query.capture());
        assertThat(query.getValue().getString("id")).isEqualTo(hearingId.toString());
    }

    @Test
    void aMissingHearingFailsWithoutCallingTheTransactionalQueryView() {
        final UUID hearingId = UUID.randomUUID();
        final uk.gov.moj.cpp.listing.persistence.repository.HearingRepository hearingRepository =
                org.mockito.Mockito.mock(uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.class);

        assertThatThrownBy(() -> new HearingService(hearingQueryView, hearingRepository).getHearingById(hearingId))
                .isInstanceOf(uk.gov.moj.cpp.listing.query.view.exception.QueryNotFoundException.class);
        org.mockito.Mockito.verifyNoInteractions(hearingQueryView);
    }
}
