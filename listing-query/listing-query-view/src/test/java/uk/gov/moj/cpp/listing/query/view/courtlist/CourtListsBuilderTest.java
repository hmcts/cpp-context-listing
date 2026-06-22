package uk.gov.moj.cpp.listing.query.view.courtlist;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.CourtRoomMapping;
import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.FlatHearing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtListsBuilderTest {

    private static final String CREST_CODE = "A";

    @Mock
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    private UUID courtCentreId;

    @BeforeEach
    void setUp() {
        courtCentreId = UUID.randomUUID();
        final JsonObject courtSite = Json.createObjectBuilder()
                .add("crestCourtSiteCode", CREST_CODE)
                .build();
        when(commonXhibitReferenceDataService.getCrestCourtSitesForCrownCourtCentre(courtCentreId))
                .thenReturn(List.of(courtSite));
    }

    @Test
    void shouldAssignHearingToCourtSiteWhenCourtRoomIdIsPresent() {
        // Given — a court room that maps to CREST_CODE
        final UUID courtRoomId = UUID.randomUUID();
        final CourtRoomMapping mapping = new CourtRoomMapping.Builder()
                .withCrestCourtSiteCode(CREST_CODE)
                .build();
        when(commonXhibitReferenceDataService.getCourtRoom(courtCentreId, courtRoomId))
                .thenReturn(Optional.of(mapping));

        // FlatHearing with a present Optional<UUID> courtRoomId
        // exercises: getCourtRoomId().map(UUID::toString) path of the changed log line
        final FlatHearing flatHearing = new FlatHearing(
                LocalDate.now(), null, Optional.of(courtRoomId), null, false);

        final CourtListsBuilder builder = CourtListsBuilder.forCourtCentre(commonXhibitReferenceDataService)
                .prepareEmptyCourtSiteHearings(courtCentreId);

        // When / Then
        assertDoesNotThrow(() ->
                builder.assignHearingsToCourtSitesUsingCourtRoom(courtCentreId, List.of(flatHearing)));
    }

    @Test
    void shouldAssignHearingToDefaultCourtSiteWhenCourtRoomIdIsAbsent() {
        // Given — no courtRoomId, falls back to default
        when(commonXhibitReferenceDataService.getDefaultCrestCourtSiteCode(courtCentreId))
                .thenReturn(CREST_CODE);

        // FlatHearing with an empty Optional<UUID> courtRoomId
        // exercises: getCourtRoomId().orElse("No Value") path of the changed log line
        final FlatHearing flatHearing = new FlatHearing(
                LocalDate.now(), null, Optional.empty(), null, false);

        final CourtListsBuilder builder = CourtListsBuilder.forCourtCentre(commonXhibitReferenceDataService)
                .prepareEmptyCourtSiteHearings(courtCentreId);

        // When / Then
        assertDoesNotThrow(() ->
                builder.assignHearingsToCourtSitesUsingCourtRoom(courtCentreId, List.of(flatHearing)));
    }

    @Test
    void shouldFallBackToDefaultCourtSiteWhenCourtRoomHasNoMapping() {
        // Given — courtRoomId present but no mapping found
        final UUID unmappedRoomId = UUID.randomUUID();
        when(commonXhibitReferenceDataService.getCourtRoom(courtCentreId, unmappedRoomId))
                .thenReturn(Optional.empty());
        when(commonXhibitReferenceDataService.getDefaultCrestCourtSiteCode(courtCentreId))
                .thenReturn(CREST_CODE);

        final FlatHearing flatHearing = new FlatHearing(
                LocalDate.now(), null, Optional.of(unmappedRoomId), null, false);

        final CourtListsBuilder builder = CourtListsBuilder.forCourtCentre(commonXhibitReferenceDataService)
                .prepareEmptyCourtSiteHearings(courtCentreId);

        // When / Then
        assertDoesNotThrow(() ->
                builder.assignHearingsToCourtSitesUsingCourtRoom(courtCentreId, List.of(flatHearing)));
    }
}
