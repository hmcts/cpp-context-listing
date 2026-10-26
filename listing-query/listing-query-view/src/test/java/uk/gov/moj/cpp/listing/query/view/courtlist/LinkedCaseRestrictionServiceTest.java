package uk.gov.moj.cpp.listing.query.view.courtlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.Set;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LinkedCaseRestrictionServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final UUID RESTRICTED_LINKED_CASE_ID = UUID.randomUUID();
    private static final UUID OPEN_LINKED_CASE_ID = UUID.randomUUID();
    private static final UUID UNLISTED_LINKED_CASE_ID = UUID.randomUUID();

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private LinkedCaseRestrictionService service;

    @Test
    public void shouldRemoveARestrictedCaseFromTheLinkedCasesAndKeepTheOthers() throws Exception {
        final JsonObject payload = payloadWithLinkedCases(
                linkedCase(RESTRICTED_LINKED_CASE_ID, "RESTRICTEDURN"),
                linkedCase(OPEN_LINKED_CASE_ID, "OPENURN"),
                linkedCase(UNLISTED_LINKED_CASE_ID, "UNLISTEDURN"));
        when(hearingRepository.findHearingsListingCases(any(Set.class))).thenReturn(List.of(
                hearingListing(RESTRICTED_LINKED_CASE_ID, true),
                hearingListing(OPEN_LINKED_CASE_ID, false)));

        final JsonObject result = service.withoutRestrictedLinkedCases(payload);

        final JsonArray linked = linkedCasesOf(result);
        assertThat(linked, hasSize(2));
        assertThat(urns(linked), contains("OPENURN", "UNLISTEDURN"));
    }

    @Test
    public void shouldLeaveEveryOtherPartOfTheHearingUntouched() throws Exception {
        final JsonObject payload = payloadWithLinkedCases(linkedCase(RESTRICTED_LINKED_CASE_ID, "RESTRICTEDURN"));
        when(hearingRepository.findHearingsListingCases(any(Set.class))).thenReturn(List.of(hearingListing(RESTRICTED_LINKED_CASE_ID, true)));

        final JsonObject result = service.withoutRestrictedLinkedCases(payload);

        final JsonObject hearing = result.getJsonArray("hearings").getJsonObject(0);
        final JsonObject listedCase = hearing.getJsonArray("listedCases").getJsonObject(0);
        assertThat(linkedCasesOf(result), hasSize(0));
        assertThat(hearing.getString("id"), is(payload.getJsonArray("hearings").getJsonObject(0).getString("id")));
        assertThat(listedCase.getString("caseReference"), is("MAINCASE"));
        assertThat(result.getString("other"), is("kept"));
    }

    @Test
    public void shouldAskOnlyAboutTheLinkedCasesNotTheMainCase() throws Exception {
        final JsonObject payload = payloadWithLinkedCases(linkedCase(OPEN_LINKED_CASE_ID, "OPENURN"), linkedCase(RESTRICTED_LINKED_CASE_ID, "RESTRICTEDURN"));
        when(hearingRepository.findHearingsListingCases(any(Set.class))).thenReturn(List.of());

        service.withoutRestrictedLinkedCases(payload);

        final ArgumentCaptor<Set<String>> asked = ArgumentCaptor.forClass(Set.class);
        verify(hearingRepository).findHearingsListingCases(asked.capture());
        assertThat(asked.getValue().size(), is(2));
        assertThat(asked.getValue().contains(OPEN_LINKED_CASE_ID.toString()), is(true));
        assertThat(asked.getValue().contains(RESTRICTED_LINKED_CASE_ID.toString()), is(true));
    }

    @Test
    public void shouldReturnThePayloadItselfWhenNoLinkedCaseIsRestricted() throws Exception {
        final JsonObject payload = payloadWithLinkedCases(linkedCase(OPEN_LINKED_CASE_ID, "OPENURN"));
        when(hearingRepository.findHearingsListingCases(any(Set.class))).thenReturn(List.of(hearingListing(OPEN_LINKED_CASE_ID, false)));

        assertThat(service.withoutRestrictedLinkedCases(payload), is(sameInstance(payload)));
    }

    @Test
    public void shouldKeepLinkedCasesThatNoHearingListsBecauseNoRestrictionIsRecordedForThem() {
        final JsonObject payload = payloadWithLinkedCases(linkedCase(UNLISTED_LINKED_CASE_ID, "UNLISTEDURN"));
        when(hearingRepository.findHearingsListingCases(any(Set.class))).thenReturn(List.of());

        assertThat(service.withoutRestrictedLinkedCases(payload), is(sameInstance(payload)));
    }

    @Test
    public void shouldNotQueryTheViewStoreWhenThereAreNoLinkedCases() {
        final JsonObject withoutLinked = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
                        .add("id", UUID.randomUUID().toString())
                        .add("listedCases", createArrayBuilder().add(createObjectBuilder().add("caseReference", "MAINCASE")))))
                .build();
        final JsonObject noHearings = createObjectBuilder().add("other", "x").build();

        assertThat(service.withoutRestrictedLinkedCases(withoutLinked), is(sameInstance(withoutLinked)));
        assertThat(service.withoutRestrictedLinkedCases(noHearings), is(sameInstance(noHearings)));
        verifyNoInteractions(hearingRepository);
    }

    @Test
    public void shouldIgnoreARestrictedCaseThatWasNotAskedAboutAndAnUnreadableLinkedCaseId() throws Exception {
        final JsonObject payload = payloadWithLinkedCases(
                linkedCase(OPEN_LINKED_CASE_ID, "OPENURN"),
                createObjectBuilder().add("caseId", "not-a-uuid").add("caseUrn", "ODDURN").build());
        when(hearingRepository.findHearingsListingCases(any(Set.class))).thenReturn(List.of(
                hearingListing(UUID.randomUUID(), true)));

        assertThat(service.withoutRestrictedLinkedCases(payload), is(sameInstance(payload)));
    }

    // ---- helpers ------------------------------------------------------------------------------------------------

    private static JsonObject payloadWithLinkedCases(final JsonObject... linkedCases) {
        final var linked = createArrayBuilder();
        for (final JsonObject linkedCase : linkedCases) {
            linked.add(linkedCase);
        }
        final JsonObjectBuilder listedCase = createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("caseReference", "MAINCASE")
                .add("linkedCases", linked);
        return createObjectBuilder()
                .add("other", "kept")
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
                        .add("id", UUID.randomUUID().toString())
                        .add("listedCases", createArrayBuilder().add(listedCase))))
                .build();
    }

    private static JsonObject linkedCase(final UUID caseId, final String urn) {
        return createObjectBuilder().add("caseId", caseId.toString()).add("caseUrn", urn).build();
    }

    private static JsonArray linkedCasesOf(final JsonObject payload) {
        return payload.getJsonArray("hearings").getJsonObject(0).getJsonArray("listedCases").getJsonObject(0).getJsonArray("linkedCases");
    }

    private static List<String> urns(final JsonArray linked) {
        return linked.getValuesAs(JsonObject.class).stream().map(l -> l.getString("caseUrn")).toList();
    }

    /** A stored hearing whose listed case is the given case, with the given restriction. */
    private static Hearing hearingListing(final UUID caseId, final boolean restricted) throws Exception {
        final JsonNode properties = MAPPER.readTree(
                "{\"listedCases\":[{\"id\":\"" + caseId + "\",\"restrictFromCourtList\":" + restricted + "}]}");
        final Hearing hearing = new Hearing();
        hearing.setProperties(properties);
        return hearing;
    }
}
