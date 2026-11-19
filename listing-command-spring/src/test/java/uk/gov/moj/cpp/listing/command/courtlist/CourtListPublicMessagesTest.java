package uk.gov.moj.cpp.listing.command.courtlist;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.command.support.HearingObjectsListingToCoreConverter;
import uk.gov.moj.cpp.listing.common.domain.DomainEvent;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataCache;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;
import uk.gov.moj.cpp.listing.xhibit.courtlist.PublishCourtListRequestParameters;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.junit.jupiter.api.Test;

/**
 * Ports the public-message cases of the JEE {@code PublishCourtListCommandSenderTest}: the payloads
 * of {@code public.listing.court-daily-list} and {@code public.listing.court-list-published}.
 */
class CourtListPublicMessagesTest {

    private static final ZonedDateTime REQUESTED_TIME = ZonedDateTime.parse("2018-01-02T13:04:05+00:00[UTC]");

    private final ProgressionService progressionService = mock(ProgressionService.class);
    private final CourtListPublicMessages messages = new CourtListPublicMessages(
            new HearingObjectsListingToCoreConverter(mock(ReferenceDataCache.class)), progressionService);

    private final UUID courtCentreId = randomUUID();
    private final UUID courtListId = randomUUID();
    private final UUID userId = randomUUID();
    private final LocalDate startDate = LocalDate.of(2023, 2, 20);

    @Test
    void dailyListIsPublishedForDraftAndFinalWithTheEscapedXml() {
        for (final PublishCourtListType type : new PublishCourtListType[]{PublishCourtListType.DRAFT, PublishCourtListType.FINAL}) {
            final DomainEvent event = messages.courtDailyList(parameters(type, true), "<cs:DailyList a=\"1\">&</cs:DailyList>").orElseThrow();

            assertThat(event.name()).isEqualTo("public.listing.court-daily-list");
            assertThat(event.streamId()).isEqualTo(courtListId);
            assertThat(event.payload()).isEqualTo(Json.createObjectBuilder()
                    .add("courtListId", courtListId.toString())
                    .add("courtCentreId", courtCentreId.toString())
                    .add("dailyListDocument", "&lt;cs:DailyList a=&quot;1&quot;&gt;&amp;&lt;/cs:DailyList&gt;")
                    .add("publishCourtListType", type.name())
                    .build());
        }
    }

    @Test
    void noDailyListForWarnedAndFirmLists() {
        assertThat(messages.courtDailyList(parameters(PublishCourtListType.WARN, true), "<x/>")).isEmpty();
        assertThat(messages.courtDailyList(parameters(PublishCourtListType.FIRM, true), "<x/>")).isEmpty();
    }

    @Test
    void courtListPublishedMapsDefendantsToCoreDefendants() {
        when(progressionService.caseExistsByCaseUrn(eq(userId), anyString()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        final DomainEvent event = messages.courtListPublished(parameters(PublishCourtListType.FIRM, true),
                payload("listing.event.court-list-export-requested-warn-courtListJson.json"), userId).orElseThrow();

        assertThat(event.name()).isEqualTo("public.listing.court-list-published");
        final JsonObject payload = (JsonObject) event.payload();
        assertCourtListPublished(payload, 4);
        assertThat(payload.getString("requestedTime")).isEqualTo("2018-01-02T13:04:05.000Z");
        assertThat(payload.getBoolean("weekCommencing")).isTrue();
        assertThat(payload.getBoolean("sendNotificationToParties")).isTrue();
        assertThat(payload.getString("endDate")).isEqualTo(startDate.plusDays(5).toString());
        final JsonObject defendant = payload.getJsonArray("courtLists").getJsonObject(0).getJsonArray("hearings").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.containsKey("prosecutionCaseId")).isTrue();
        assertThat(defendant.containsKey("masterDefendantId")).isTrue();
    }

    @Test
    void courtListPublishedHasMultipleDefendants() {
        when(progressionService.caseExistsByCaseUrn(any(), anyString()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        final JsonObject payload = (JsonObject) messages.courtListPublished(parameters(PublishCourtListType.FIRM, true),
                payload("listing.event.court-list-export-requested-warn-courtListJson-multiple.json"), userId).orElseThrow().payload();

        assertCourtListPublished(payload, 6);
    }

    @Test
    void extraHearingDataIsDroppedByThePublicShape() {
        when(progressionService.caseExistsByCaseUrn(any(), anyString()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        final JsonObject payload = (JsonObject) messages.courtListPublished(parameters(PublishCourtListType.FIRM, true),
                payload("listing.event.court-list-export-requested-warn-courtListJson-extraHearingData.json"), userId).orElseThrow().payload();

        assertThat(payload.getJsonArray("courtLists")).isNotEmpty();
        payload.getJsonArray("courtLists").getValuesAs(JsonObject.class).forEach(courtList ->
                courtList.getJsonArray("hearings").getValuesAs(JsonObject.class).forEach(hearing ->
                        assertThat(hearing.keySet()).isSubsetOf("startTime", "endTime", "hearingType", "restrictFromCourtList", "weekCommencing",
                                "hasVideoLink", "committingCourtCentreId", "caseIdentifier", "defendants")));
    }

    @Test
    void applicationHearingsArePublishedWithoutDefendantsAndWithoutAProgressionLookup() {
        final JsonObject payload = (JsonObject) messages.courtListPublished(parameters(PublishCourtListType.FIRM, true),
                payload("listing.event.court-list-export-requested-warn-courtListJson-applicationDefendantWithProsecutionCaseId.json"), userId)
                .orElseThrow().payload();

        final JsonObject applicationHearing = payload.getJsonArray("courtLists").getJsonObject(0).getJsonArray("hearings").getJsonObject(0);
        assertThat(applicationHearing.containsKey("defendants")).isFalse();
        assertThat(applicationHearing.getString("startTime")).isNotNull();
        verify(progressionService, never()).caseExistsByCaseUrn(any(), anyString());
    }

    @Test
    void theDefendantsOwnProsecutionCaseIdWinsOverTheCaseUrnLookup() {
        when(progressionService.caseExistsByCaseUrn(any(), anyString()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        final JsonObject payload = (JsonObject) messages.courtListPublished(parameters(PublishCourtListType.FIRM, true),
                payload("listing.event.court-list-export-requested-warn-courtListJson-defendantProsecutionCaseIdTakesPrecedence.json"), userId)
                .orElseThrow().payload();

        final JsonObject defendant = payload.getJsonArray("courtLists").getJsonObject(0).getJsonArray("hearings").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("prosecutionCaseId")).isEqualTo("72b9df0f-4c81-4b17-ad3e-cea90833a882");
    }

    @Test
    void nothingIsPublishedWithoutHearingsOrCourtLists() {
        assertThat(messages.courtListPublished(parameters(PublishCourtListType.FIRM, true),
                payload("listing.event.court-list-export-requested-warn-courtListJson-no-hearings.json"), userId)).isEmpty();
        assertThat(messages.courtListPublished(parameters(PublishCourtListType.FIRM, true),
                payload("listing.event.court-list-export-requested-warn-empty-courtListJson.json"), userId)).isEmpty();
    }

    @Test
    void aMissingSendNotificationToPartiesFailsAsTheJeeBuilderDid() {
        when(progressionService.caseExistsByCaseUrn(any(), anyString()))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("caseId", randomUUID().toString()).build()));

        assertThatThrownBy(() -> messages.courtListPublished(parameters(PublishCourtListType.FIRM, null),
                payload("listing.event.court-list-export-requested-warn-courtListJson.json"), userId))
                .isInstanceOf(NullPointerException.class);
    }

    private PublishCourtListRequestParameters parameters(final PublishCourtListType type, final Boolean sendNotificationToParties) {
        return new PublishCourtListRequestParameters(courtListId, courtCentreId, startDate, startDate.plusDays(5), type, REQUESTED_TIME,
                sendNotificationToParties);
    }

    private void assertCourtListPublished(final JsonObject payload, final int courtLists) {
        assertThat(payload.getString("courtListId")).isEqualTo(courtListId.toString());
        assertThat(payload.getString("courtCentreId")).isEqualTo(courtCentreId.toString());
        assertThat(payload.getString("startDate")).isEqualTo(startDate.toString());
        assertThat(payload.getString("publishCourtListType")).isEqualTo("FIRM");
        assertThat(payload.getJsonArray("courtLists")).hasSize(courtLists);
        payload.getJsonArray("courtLists").getValuesAs(JsonObject.class)
                .forEach(courtList -> assertThat(courtList.getString("sittingDate")).isIn("2023-02-20", "2023-02-21"));
    }

    static JsonObject payload(final String name) {
        try (InputStream in = CourtListPublicMessagesTest.class.getResourceAsStream("/test-data/" + name);
             JsonReader reader = Json.createReader(in)) {
            return reader.readObject();
        } catch (final java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
