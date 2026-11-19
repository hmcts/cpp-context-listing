package uk.gov.moj.cpp.listing.command.inbound;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.ApplicationLaaReferenceUpdatedForApplication;
import uk.gov.justice.progression.courts.ApplicationOffencesUpdated;
import uk.gov.justice.progression.courts.HearingExtended;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.messaging.inbound.InboundEventHandler;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * The inbound public-event handlers (MBD refactor P2.4), ported from the JEE processor tests
 * ({@code ListingEventProcessorTest}, {@code CpsProsecutorUpdatedEventProcessorTest},
 * {@code HearingResultedEventProcessorTest}, {@code CaseRemovedFromGroupCasesEventProcessorTest},
 * {@code CourtApplicationDeletedProcessorTest}, {@code HearingCounselEventProcessorTest},
 * {@code UpdateExistingHearingEventProcessorTest}): each event becomes the same internal command(s),
 * with the same payload and the event's metadata under the command name.
 */
class InboundProcessorsTest {

    private final ProcessorQueue processorQueue = new ProcessorQueue();
    private final ProcessorSender sender = mock(ProcessorSender.class);
    private final HearingSlotsService hearingSlotsService = mock(HearingSlotsService.class);
    private final HearingQueryView hearingQueryView = mock(HearingQueryView.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final ObjectToJsonObjectConverter toJson = new ObjectToJsonObjectConverter();
    private final JsonObjectToObjectConverter fromJson = new JsonObjectToObjectConverter();

    private final List<InboundEventHandler> handlers = InboundHandlers.all(processorQueue, sender, hearingSlotsService, hearingQueryView, entityManager);

    static Stream<Arguments> passThrough() {
        return Stream.of(
                Arguments.of("public.progression.case-defendant-changed", "listing.command.update-case-defendant-details"),
                Arguments.of("public.progression.defendant-offences-changed", "listing.command.update-case-defendant-offences"),
                Arguments.of("public.progression.court-application-changed", "listing.command.update-court-application"),
                Arguments.of("public.progression.case-markers-updated", "listing.command.update-case-markers"),
                Arguments.of("public.progression.defendants-added-to-court-proceedings", "listing.command.add-defendants-to-court-proceedings"),
                Arguments.of("public.progression.events.case-or-application-ejected", "listing.command.eject-case-or-application"),
                Arguments.of("public.progression.hearing-resulted-case-updated", "listing.command.update-case-resulted-defendant-proceedings-concluded"),
                Arguments.of("public.hearing.trial-vacated", "listing.command.hearing-vacate-trial"),
                Arguments.of("public.progression.case-linked", "listing.command.update-linked-cases"),
                Arguments.of("public.hearing.hearing-days-cancelled", "listing.command.cancel-hearing-days"),
                Arguments.of("public.hearing.selected-offences-removed-from-existing-hearing", "listing.command.remove-selected-offences-from-existing-hearing"),
                Arguments.of("public.progression.offences-removed-from-existing-allocated-hearing", "listing.command.remove-selected-offences-from-existing-hearing"),
                Arguments.of("public.progression.case-removed-from-group-cases", "listing.command.remove-case-from-group-cases"),
                Arguments.of("public.progression.related-hearing-updated-for-adhoc-hearing", "listing.command.add-cases-to-hearing"));
    }

    @ParameterizedTest
    @MethodSource("passThrough")
    void theEventPayloadIsSentAsTheCommandWithTheEventMetadata(final String eventName, final String commandName) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", randomUUID().toString())
                .add("offenceIds", Json.createArrayBuilder().add(randomUUID().toString()))
                .add("nested", Json.createObjectBuilder().add("numberOfGroupCases", 2))
                .build();
        final Envelope event = event(eventName, payload);

        handle(event);

        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo(commandName);
        assertThat(command.payload()).isEqualTo(payload);
        assertThat(command.metadata()).isEqualTo(Metadata.from(event.metadata()).withName(commandName).build());
    }

    @Test
    void theProgressionAndHearingOffencesRemovedEventsReachTheSameCommand() {
        final JsonObject payload = Json.createObjectBuilder().add("hearingId", randomUUID().toString())
                .add("offenceIds", Json.createArrayBuilder().add(randomUUID().toString())).build();

        handle(event("public.hearing.selected-offences-removed-from-existing-hearing", payload));
        handle(event("public.progression.offences-removed-from-existing-allocated-hearing", payload));

        final List<Envelope> commands = commands(2);
        assertThat(commands.get(1).name()).isEqualTo(commands.get(0).name());
        assertThat(commands.get(1).payload()).isEqualTo(commands.get(0).payload());
    }

    // public.progression.events.hearing-extended (ListingEventProcessorTest)

    @Test
    void hearingExtendedWithACourtApplicationOnlyAddsTheApplication() {
        final CourtApplication application = CourtApplication.courtApplication().withId(randomUUID()).build();
        final JsonObject payload = toJson.convert(HearingExtended.hearingExtended().withHearingId(randomUUID())
                .withCourtApplication(application).withProsecutionCases(new ArrayList<>()).build());

        handle(event("public.progression.events.hearing-extended", payload));

        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.add-court-application-for-hearing");
        assertThat(command.payload()).isEqualTo(payload);
    }

    @Test
    void hearingExtendedWithProsecutionCasesOnlyAddsTheCases() {
        final JsonObject payload = toJson.convert(HearingExtended.hearingExtended().withHearingId(randomUUID())
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase().withId(randomUUID()).build())).build());

        handle(event("public.progression.events.hearing-extended", payload));

        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.add-cases-to-hearing");
        assertThat(command.payload()).isEqualTo(payload);
    }

    @Test
    void hearingExtendedWithBothAddsTheCasesWithoutTheApplicationThenTheApplication() {
        final UUID caseId = randomUUID();
        final CourtApplication application = CourtApplication.courtApplication().withId(randomUUID()).build();
        final JsonObject payload = toJson.convert(HearingExtended.hearingExtended().withHearingId(randomUUID())
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase().withId(caseId).build()))
                .withCourtApplication(application).build());

        handle(event("public.progression.events.hearing-extended", payload));

        final List<Envelope> commands = commands(2);
        assertThat(commands.get(0).name()).isEqualTo("listing.command.add-cases-to-hearing");
        final HearingExtended cases = fromJson.convert(commands.get(0).payload(), HearingExtended.class);
        assertThat(cases.getProsecutionCases()).extracting(ProsecutionCase::getId).containsExactly(caseId);
        assertThat(cases.getCourtApplication()).isNull();
        assertThat(commands.get(0).payload()).doesNotContainKey("courtApplication");
        assertThat(commands.get(1).name()).isEqualTo("listing.command.add-court-application-for-hearing");
        assertThat(commands.get(1).payload()).isEqualTo(payload);
    }

    @Test
    void aBoxWorkHearingExtensionSendsNothing() {
        final JsonObject payload = toJson.convert(HearingExtended.hearingExtended().withHearingId(randomUUID())
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase().withId(randomUUID()).build()))
                .withCourtApplication(CourtApplication.courtApplication().withId(randomUUID()).build())
                .withIsBoxWorkRequest(true).build());

        handle(event("public.progression.events.hearing-extended", payload));

        verify(sender, never()).send(any());
    }

    @Test
    void theCommandsOfOneEventRunBeforeTheirProcessorSteps() {
        final List<String> calls = new ArrayList<>();
        doAnswer(i -> {
            final String name = i.<Envelope>getArgument(0).name();
            processorQueue.command(() -> {
                calls.add("command " + name);
                processorQueue.enqueue(() -> calls.add("step " + name));
            });
            return null;
        }).when(sender).send(any());
        final JsonObject payload = toJson.convert(HearingExtended.hearingExtended().withHearingId(randomUUID())
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase().withId(randomUUID()).build()))
                .withCourtApplication(CourtApplication.courtApplication().withId(randomUUID()).build()).build());

        handle(event("public.progression.events.hearing-extended", payload));

        assertThat(calls).containsExactly(
                "command listing.command.add-cases-to-hearing", "command listing.command.add-court-application-for-hearing",
                "step listing.command.add-cases-to-hearing", "step listing.command.add-court-application-for-hearing");
    }

    // public.progression.defendant-legalaid-status-updated

    @Test
    void legalAidStatusSendsOnlyCaseDefendantAndStatus() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        handle(event("public.progression.defendant-legalaid-status-updated", Json.createObjectBuilder()
                .add("caseId", caseId.toString()).add("defendantId", defendantId.toString())
                .add("legalAidStatus", "Granted").add("other", "dropped").build()));

        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.update-defendant-legalaid-status");
        assertThat(command.payload()).isEqualTo(Json.createObjectBuilder()
                .add("caseId", caseId.toString()).add("defendantId", defendantId.toString()).add("legalAidStatus", "Granted").build());
    }

    // public.progression.application-offences-updated / application-laa-reference-updated-for-application

    @Test
    void applicationOffencesUpdatedIsSentAsThePojoJson() {
        final ApplicationOffencesUpdated pojo = ApplicationOffencesUpdated.applicationOffencesUpdated()
                .withApplicationId(randomUUID()).withOffenceId(randomUUID()).withSubjectId(randomUUID())
                .withLaaReference(LaaReference.laaReference().withStatusId(randomUUID()).build()).build();
        final JsonObject withUnknownField = Json.createObjectBuilder(toJson.convert(pojo)).add("unknown", "dropped").build();

        handle(event("public.progression.application-offences-updated", withUnknownField));

        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.update-laa-reference-for-application");
        assertThat(fromJson.convert(command.payload(), ApplicationOffencesUpdated.class)).isEqualTo(pojo);
        assertThat(command.payload()).isEqualTo(toJson.convert(pojo));
    }

    @Test
    void applicationLaaReferenceUpdatedIsSentAsThePojoJson() {
        final ApplicationLaaReferenceUpdatedForApplication pojo = ApplicationLaaReferenceUpdatedForApplication.applicationLaaReferenceUpdatedForApplication()
                .withApplicationId(randomUUID()).withSubjectId(randomUUID())
                .withLaaReference(LaaReference.laaReference().withStatusId(randomUUID()).build()).build();

        handle(event("public.progression.application-laa-reference-updated-for-application", toJson.convert(pojo)));

        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.update-laa-reference-for-application");
        assertThat(fromJson.convert(command.payload(), ApplicationLaaReferenceUpdatedForApplication.class)).isEqualTo(pojo);
    }

    // public.events.hearing.marked-as-duplicate

    @Test
    void markedAsDuplicateReleasesTheSessionsThenSendsTheHearingAndCaseIds() {
        final UUID hearingId = randomUUID();
        final String case1 = randomUUID().toString();
        final String case2 = randomUUID().toString();

        handle(event("public.events.hearing.marked-as-duplicate", Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("prosecutionCaseIds", Json.createArrayBuilder().add(case1).add(case2))
                .add("defendantIds", Json.createArrayBuilder().add(randomUUID().toString()))
                .add("offenceIds", Json.createArrayBuilder().add(randomUUID().toString()))
                .build()));

        final InOrder order = inOrder(hearingSlotsService, sender);
        order.verify(hearingSlotsService).delete(hearingId);
        order.verify(sender).send(any());
        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.mark-hearing-as-duplicate");
        assertThat(command.payload()).isEqualTo(Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("prosecutionCaseIds", Json.createArrayBuilder().add(case1).add(case2)).build());
    }

    @Test
    void markedAsDuplicateWithoutCasesSendsTheHearingIdOnly() {
        final UUID hearingId = randomUUID();

        handle(event("public.events.hearing.marked-as-duplicate", Json.createObjectBuilder().add("hearingId", hearingId.toString()).build()));

        assertThat(theOnlyCommand().payload()).isEqualTo(Json.createObjectBuilder().add("hearingId", hearingId.toString()).build());
    }

    // public.progression.events.court-application-deleted (CourtApplicationDeletedProcessorTest)

    @Test
    void courtApplicationDeletedSendsTheHearingAndApplicationIds() {
        final String hearingId = randomUUID().toString();
        final String applicationId = randomUUID().toString();

        handle(event("public.progression.events.court-application-deleted", Json.createObjectBuilder()
                .add("hearingId", hearingId).add("applicationId", applicationId).add("other", "dropped").build()));

        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.delete-court-application-hearing");
        assertThat(command.payload()).isEqualTo(Json.createObjectBuilder().add("hearingId", hearingId).add("applicationId", applicationId).build());
    }

    // public.events.hearing.hearing-resulted (HearingResultedEventProcessorTest)

    @Test
    void aResultedHearingThatIsNotSjpIsMarkedResulted() {
        final String hearingId = randomUUID().toString();

        handle(event("public.events.hearing.hearing-resulted", Json.createObjectBuilder()
                .add("hearing", Json.createObjectBuilder().add("id", hearingId).add("isSJPHearing", false)).build()));

        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.set-hearing-resulted-status");
        assertThat(command.payload()).isEqualTo(Json.createObjectBuilder().add("hearingId", hearingId).build());
    }

    @Test
    void aResultedSjpHearingSendsNothing() {
        handle(event("public.events.hearing.hearing-resulted", Json.createObjectBuilder()
                .add("hearing", Json.createObjectBuilder().add("id", randomUUID().toString()).add("isSJPHearing", true)).build()));

        verify(sender, never()).send(any());
    }

    @Test
    void aResultedEventWithoutAHearingFailsAsInJee() {
        assertThatThrownBy(() -> handle(event("public.events.hearing.hearing-resulted", Json.createObjectBuilder().build())))
                .isInstanceOf(NullPointerException.class);
    }

    // public.progression.events.cps-prosecutor-updated (CpsProsecutorUpdatedEventProcessorTest)

    @Test
    void cpsProsecutorUpdatedIsSentForTheCaseHearingsFoundInListing() {
        when(hearingQueryView.searchAllocatedAndUnallocatedHearings(any())).thenReturn(hearings("a8448a33-68ab-4b9b-84c2-59cee4fe36f4", "095d7412-ba76-4a15-942d-566d3aeae7c9"));

        handle(event("public.progression.events.cps-prosecutor-updated", cpsProsecutorUpdated()));

        final ArgumentCaptor<JsonObject> query = ArgumentCaptor.forClass(JsonObject.class);
        verify(hearingQueryView).searchAllocatedAndUnallocatedHearings(query.capture());
        assertThat(query.getValue()).isEqualTo(Json.createObjectBuilder().add("caseId", "34d07e81-9770-4d23-af6f-84f1d7571bd3").build());
        final InOrder order = inOrder(entityManager, sender);
        order.verify(entityManager).clear();
        order.verify(sender).send(any());
        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.update-cps-prosecutor-with-associated-hearings");
        assertThat(command.payload()).isEqualTo(Json.createObjectBuilder()
                .add("prosecutionCaseId", "34d07e81-9770-4d23-af6f-84f1d7571bd3")
                .add("prosecutionAuthorityId", "test prosecutionAuthorityId")
                .add("prosecutionAuthorityCode", "test prosecutionAuthorityCode")
                .add("hearingIds", Json.createArrayBuilder().add("a8448a33-68ab-4b9b-84c2-59cee4fe36f4").add("095d7412-ba76-4a15-942d-566d3aeae7c9"))
                .build());
    }

    @Test
    void cpsProsecutorUpdatedForACaseWithoutHearingsSendsNothing() {
        when(hearingQueryView.searchAllocatedAndUnallocatedHearings(any())).thenReturn(hearings());

        handle(event("public.progression.events.cps-prosecutor-updated", cpsProsecutorUpdated()));

        verify(sender, never()).send(any());
    }

    // public.hearing.*-counsel-* (HearingCounselEventProcessorTest)

    static Stream<Arguments> counsel() {
        return Stream.of(
                Arguments.of("public.hearing.defence-counsel-added", "public.hearing.defence-counsel-added-or-updated.json",
                        "listing.command.handler.modify-hearing-defence-counsel.json", "ACTION", "ADD"),
                Arguments.of("public.hearing.defence-counsel-updated", "public.hearing.defence-counsel-added-or-updated.json",
                        "listing.command.handler.modify-hearing-defence-counsel.json", "ACTION", "UPDATE"),
                Arguments.of("public.hearing.defence-counsel-removed", "public.hearing.counsel-removed.json",
                        "listing.command.handler.modify-hearing-counsel-remove.json", "COUNSEL_TYPE", "DEFENCE"),
                Arguments.of("public.hearing.prosecution-counsel-added", "public.hearing.prosecution-counsel-added-or-updated.json",
                        "listing.command.handler.modify-hearing-prosecution-counsel.json", "ACTION", "ADD"),
                Arguments.of("public.hearing.prosecution-counsel-updated", "public.hearing.prosecution-counsel-added-or-updated.json",
                        "listing.command.handler.modify-hearing-prosecution-counsel.json", "ACTION", "UPDATE"),
                Arguments.of("public.hearing.prosecution-counsel-removed", "public.hearing.counsel-removed.json",
                        "listing.command.handler.modify-hearing-counsel-remove.json", "COUNSEL_TYPE", "PROSECUTION"));
    }

    @ParameterizedTest
    @MethodSource("counsel")
    void counselChangesBecomeModifyHearingCounsel(final String eventName, final String eventFile, final String commandFile,
                                                  final String placeholder, final String value) throws IOException {
        handle(event(eventName, json(resource(eventFile))));

        final Envelope command = theOnlyCommand();
        assertThat(command.name()).isEqualTo("listing.command.handler.modify-hearing-counsel");
        assertThat(command.payload()).isEqualTo(json(resource(commandFile).replace(placeholder, value)));
    }

    private void handle(final Envelope event) {
        handlers.stream().filter(h -> h.handledEvents().contains(event.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no handler for " + event.name()))
                .handle(event);
    }

    private Envelope theOnlyCommand() {
        return commands(1).get(0);
    }

    private List<Envelope> commands(final int count) {
        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender, times(count)).send(captor.capture());
        return captor.getAllValues();
    }

    private static Envelope event(final String name, final JsonObject payload) {
        return new Envelope(Metadata.builder().withId(randomUUID()).withName(name).withUserId("user-1")
                .withCausation(List.of(randomUUID())).withClientCorrelationId("corr-1").withStreamId(randomUUID()).build(), payload);
    }

    private static JsonObject hearings(final String... ids) {
        final var array = Json.createArrayBuilder();
        for (final String id : ids) {
            array.add(Json.createObjectBuilder().add("id", id).add("allocated", true));
        }
        return Json.createObjectBuilder().add("hearings", array).build();
    }

    private static JsonObject cpsProsecutorUpdated() {
        return Json.createObjectBuilder()
                .add("prosecutionCaseId", "34d07e81-9770-4d23-af6f-84f1d7571bd3")
                .add("caseURN", "test Case URN")
                .add("prosecutionAuthorityId", "test prosecutionAuthorityId")
                .add("prosecutionAuthorityReference", "test prosecutionAuthorityReference")
                .add("prosecutionAuthorityCode", "test prosecutionAuthorityCode")
                .add("prosecutionAuthorityName", "test prosecutionAuthorityName")
                .add("address", Json.createObjectBuilder().add("address1", "41 Manhattan House").add("postcode", "MK9 2BQ"))
                .build();
    }

    private static String resource(final String name) throws IOException {
        try (InputStream in = InboundProcessorsTest.class.getResourceAsStream("/test-data/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static JsonObject json(final String text) {
        try (JsonReader reader = Json.createReader(new StringReader(text))) {
            return reader.readObject();
        }
    }
}
