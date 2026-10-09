package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.core.courts.CivilOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CAD-1760 AC5-AC8 for applications whose linked civil case is not on the list. Linked cases on the
 * list are CAD-1710's {@code filterExParteOffencesFromHearings}, tested with that converter.
 */
@ExtendWith(MockitoExtension.class)
class ExParteLinkedCaseApplicationFilterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonEnvelope ENVELOPE = mock(JsonEnvelope.class);

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private ExParteLinkedCaseApplicationFilter filter;

    @Test
    void ac5_shouldRemoveApplicationLinkedToExParteCaseListedOnAnotherHearing() throws Exception {
        final UUID linkedCaseId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(List.of(linkedCaseId)))
                .thenReturn(List.of(viewStoreHearing(listedCase(linkedCaseId, false, true))));
        final JsonObject payload = hearingsPayload(applicationHearing(courtApplication(randomUUID(), linkedCaseId)));

        final JsonObject result = filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE);

        assertThat(result.getJsonArray("hearings").isEmpty(), is(true));
        verify(progressionService, never()).findProsecutionCaseByCaseId(any(), any());
    }

    @Test
    void ac5_shouldRemoveApplicationLinkedToExParteCaseKnownOnlyToProgression() {
        final UUID linkedCaseId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(List.of(linkedCaseId))).thenReturn(List.of());
        when(progressionService.findProsecutionCaseByCaseId(ENVELOPE, linkedCaseId.toString())).thenReturn(Optional.of(progressionCase(false, true)));
        final JsonObject payload = hearingsPayload(applicationHearing(courtApplication(randomUUID(), linkedCaseId)));

        final JsonObject result = filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE);

        assertThat(result.getJsonArray("hearings").isEmpty(), is(true));
    }

    @Test
    void ac6_shouldKeepApplicationLinkedToNonExParteCaseListedOnAnotherHearing() throws Exception {
        final UUID linkedCaseId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(List.of(linkedCaseId)))
                .thenReturn(List.of(viewStoreHearing(listedCase(linkedCaseId, false, false))));
        final JsonObject payload = hearingsPayload(applicationHearing(courtApplication(randomUUID(), linkedCaseId)));

        assertThat(filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE), is(sameInstance(payload)));
    }

    @Test
    void ac6_shouldKeepApplicationLinkedToNonExParteCaseKnownOnlyToProgression() {
        final UUID linkedCaseId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(List.of(linkedCaseId))).thenReturn(List.of());
        when(progressionService.findProsecutionCaseByCaseId(ENVELOPE, linkedCaseId.toString())).thenReturn(Optional.of(progressionCase(false, false)));
        final JsonObject payload = hearingsPayload(applicationHearing(courtApplication(randomUUID(), linkedCaseId)));

        assertThat(filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE), is(sameInstance(payload)));
    }

    @Test
    void ac7_shouldRemoveApplicationWhenAnyOfItsLinkedCasesIsExParte() throws Exception {
        final UUID exParteCaseId = randomUUID();
        final UUID nonExParteCaseId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(any())).thenReturn(List.of(
                viewStoreHearing(listedCase(exParteCaseId, true)),
                viewStoreHearing(listedCase(nonExParteCaseId, false))));
        final JsonObject payload = hearingsPayload(applicationHearing(courtApplication(randomUUID(), exParteCaseId, nonExParteCaseId)));

        final JsonObject result = filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE);

        assertThat(result.getJsonArray("hearings").isEmpty(), is(true));
    }

    @Test
    void ac7_shouldAskProgressionOnlyForTheLinkedCaseListingHasNeverListed() throws Exception {
        final UUID caseKnownToListing = randomUUID();
        final UUID caseUnknownToListing = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(any())).thenReturn(List.of(viewStoreHearing(listedCase(caseKnownToListing, false))));
        when(progressionService.findProsecutionCaseByCaseId(ENVELOPE, caseUnknownToListing.toString())).thenReturn(Optional.of(progressionCase(true)));
        final JsonObject payload = hearingsPayload(applicationHearing(courtApplication(randomUUID(), caseKnownToListing, caseUnknownToListing)));

        final JsonObject result = filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE);

        assertThat(result.getJsonArray("hearings").isEmpty(), is(true));
        verify(progressionService, never()).findProsecutionCaseByCaseId(any(), eq(caseKnownToListing.toString()));
    }

    @Test
    void ac8_shouldKeepApplicationWhenNoneOfItsLinkedCasesIsExParte() throws Exception {
        final UUID firstCaseId = randomUUID();
        final UUID secondCaseId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(any())).thenReturn(List.of(
                viewStoreHearing(listedCase(firstCaseId, false)),
                viewStoreHearing(listedCase(secondCaseId, false, false))));
        final JsonObject payload = hearingsPayload(applicationHearing(courtApplication(randomUUID(), firstCaseId, secondCaseId)));

        assertThat(filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE), is(sameInstance(payload)));
    }

    @Test
    void shouldLeaveLinkedCasesOnTheListToCad1710() {
        final UUID caseOnTheList = randomUUID();
        final JsonObject payload = hearingsPayload(
                caseHearing(listedCase(caseOnTheList, false)),
                applicationHearing(courtApplication(randomUUID(), caseOnTheList)));

        assertThat(filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE), is(sameInstance(payload)));
        verify(hearingRepository, never()).findHearingsByListedCaseIds(any());
        verify(progressionService, never()).findProsecutionCaseByCaseId(any(), any());
    }

    @Test
    void shouldKeepApplicationWhenLinkedCaseIsNotFoundInProgression() {
        final UUID linkedCaseId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(List.of(linkedCaseId))).thenReturn(List.of());
        when(progressionService.findProsecutionCaseByCaseId(ENVELOPE, linkedCaseId.toString())).thenReturn(Optional.empty());
        final JsonObject payload = hearingsPayload(applicationHearing(courtApplication(randomUUID(), linkedCaseId)));

        assertThat(filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE), is(sameInstance(payload)));
    }

    @Test
    void shouldRemoveApplicationWhenProgressionLookupFails() {
        final UUID linkedCaseId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(List.of(linkedCaseId))).thenReturn(List.of());
        when(progressionService.findProsecutionCaseByCaseId(ENVELOPE, linkedCaseId.toString())).thenThrow(new IllegalStateException("progression unavailable"));
        final JsonObject payload = hearingsPayload(applicationHearing(courtApplication(randomUUID(), linkedCaseId)));

        final JsonObject result = filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE);

        assertThat(result.getJsonArray("hearings").isEmpty(), is(true));
    }

    @Test
    void shouldKeepOtherApplicationsOnTheSameHearing() throws Exception {
        final UUID exParteCaseId = randomUUID();
        final UUID keptApplicationId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(List.of(exParteCaseId)))
                .thenReturn(List.of(viewStoreHearing(listedCase(exParteCaseId, true))));
        final JsonObject payload = hearingsPayload(applicationHearing(
                courtApplication(randomUUID(), exParteCaseId),
                createObjectBuilder().add("id", keptApplicationId.toString()).build()));

        final JsonObject result = filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE);

        assertThat(result.getJsonArray("hearings").getJsonObject(0).getJsonArray("courtApplications").getValuesAs(JsonObject.class)
                .stream().map(application -> application.getString("id")).toList(), contains(keptApplicationId.toString()));
    }

    @Test
    void shouldKeepStandaloneApplicationWithoutLookingAnythingUp() {
        final JsonObject payload = hearingsPayload(applicationHearing(createObjectBuilder().add("id", randomUUID().toString()).build()));

        assertThat(filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE), is(sameInstance(payload)));
        verify(hearingRepository, never()).findHearingsByListedCaseIds(any());
    }

    @Test
    void shouldKeepOtherPayloadPropertiesWhenFiltering() throws Exception {
        final UUID exParteCaseId = randomUUID();
        when(hearingRepository.findHearingsByListedCaseIds(List.of(exParteCaseId)))
                .thenReturn(List.of(viewStoreHearing(listedCase(exParteCaseId, true))));
        final JsonObject payload = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(applicationHearing(courtApplication(randomUUID(), exParteCaseId))))
                .add("notes", createArrayBuilder().add("note"))
                .build();

        final JsonObject result = filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE);

        assertThat(result.getJsonArray("hearings").isEmpty(), is(true));
        assertThat(result.getJsonArray("notes").getString(0), is("note"));
    }

    @Test
    void shouldReturnPayloadUnchangedWhenItHasNoHearings() {
        final JsonObject payload = createObjectBuilder().add("notes", createArrayBuilder()).build();

        assertThat(filter.removeApplicationsLinkedToExParteCases(payload, ENVELOPE), is(sameInstance(payload)));
    }

    private static JsonObject hearingsPayload(final JsonObject... hearings) {
        final JsonArrayBuilder hearingsBuilder = createArrayBuilder();
        for (final JsonObject hearing : hearings) {
            hearingsBuilder.add(hearing);
        }
        return createObjectBuilder().add("hearings", hearingsBuilder).build();
    }

    private static JsonObject caseHearing(final JsonObject listedCase) {
        return createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("listedCases", createArrayBuilder().add(listedCase))
                .build();
    }

    private static JsonObject applicationHearing(final JsonObject... courtApplications) {
        final JsonArrayBuilder applicationsBuilder = createArrayBuilder();
        for (final JsonObject courtApplication : courtApplications) {
            applicationsBuilder.add(courtApplication);
        }
        return createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("courtApplications", applicationsBuilder)
                .build();
    }

    private static JsonObject listedCase(final UUID caseId, final boolean... offenceExParteFlags) {
        final JsonArrayBuilder offences = createArrayBuilder();
        for (final boolean exParte : offenceExParteFlags) {
            offences.add(createObjectBuilder()
                    .add("id", randomUUID().toString())
                    .add("civilOffence", createObjectBuilder().add("isExParte", exParte)));
        }
        return createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", createArrayBuilder().add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("offences", offences)))
                .build();
    }

    private static JsonObject courtApplication(final UUID applicationId, final UUID... linkedCaseIds) {
        final JsonArrayBuilder linkedCaseIdsBuilder = createArrayBuilder();
        for (final UUID linkedCaseId : linkedCaseIds) {
            linkedCaseIdsBuilder.add(linkedCaseId.toString());
        }
        return createObjectBuilder()
                .add("id", applicationId.toString())
                .add("linkedCaseIds", linkedCaseIdsBuilder)
                .build();
    }

    private static ProsecutionCase progressionCase(final boolean... offenceExParteFlags) {
        final List<Offence> offences = new ArrayList<>();
        for (final boolean exParte : offenceExParteFlags) {
            offences.add(Offence.offence()
                    .withId(randomUUID())
                    .withCivilOffence(CivilOffence.civilOffence().withIsExParte(exParte).build())
                    .build());
        }
        return ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withDefendants(List.of(Defendant.defendant().withId(randomUUID()).withOffences(offences).build()))
                .build();
    }

    private static Hearing viewStoreHearing(final JsonObject listedCase) throws Exception {
        final JsonObject properties = createObjectBuilder().add("listedCases", createArrayBuilder().add(listedCase)).build();
        return new Hearing(randomUUID(), OBJECT_MAPPER.readTree(properties.toString()));
    }
}
