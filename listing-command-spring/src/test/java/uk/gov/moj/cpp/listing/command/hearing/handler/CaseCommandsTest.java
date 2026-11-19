package uk.gov.moj.cpp.listing.command.hearing.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.listing.command.hearing.flow.InternalCommandPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link CaseCommands} and {@link NextHearingCommands}: each command runs the ported handler method; the Case ones are reachable by name. */
@ExtendWith(MockitoExtension.class)
class CaseCommandsTest {

    @Mock
    private CaseCommandHandler handler;

    @Mock
    private SeedHearingCommandHandler seedHandler;

    private final Envelope command = new Envelope(Metadata.builder().withId(UUID.randomUUID()).withName("x").build(), JsonValue.EMPTY_JSON_OBJECT);

    @Test
    void everyCaseAndApplicationCommandIsRegisteredByNameAndDelegates() {
        final Map<String, Consumer<Envelope>> registered = registered(new CaseCommands(handler, new ProcessorQueue()));

        assertThat(registered).containsOnlyKeys(
                "listing.command.update-case-defendant-details",
                "listing.command.update-case-defendant-offences",
                "listing.command.add-court-application-to-hearing",
                "listing.command.update-court-application",
                "listing.command.update-laa-reference-for-application",
                "listing.command.update-case-markers",
                "listing.command.update-linked-cases",
                "listing.command.add-defendants-to-court-proceedings",
                "listing.command.add-hearing-to-case",
                "listing.command.update-hearing-to-case",
                "listing.command.eject-case-or-application",
                "listing.command.update-defendant-legalaid-status",
                "listing.command.update-case-resulted-defendant-proceedings-concluded",
                "listing.command.mark-hearing-as-duplicate-for-case",
                "listing.command.remove-case-from-group-cases");

        registered.values().forEach(h -> h.accept(command));

        verify(handler).updateCaseDefendantDetails(command);
        verify(handler).updateCaseDefendantOffences(command);
        verify(handler).addCourtApplicationToHearing(command);
        verify(handler).updateCourtApplication(command);
        verify(handler).updateLaaReferenceForCourtApplication(command);
        verify(handler).updateCaseMarker(command);
        verify(handler).updateLinkedCases(command);
        verify(handler).addDefendantsToCourtProceedings(command);
        verify(handler).addHearingToCase(command);
        verify(handler).updateHearingToCase(command);
        verify(handler).ejectCaseOrApplication(command);
        verify(handler).updateDefendantLegalAidStatus(command);
        verify(handler).updateDefendantHearingResultedAndCaseResulted(command);
        verify(handler).handleMarkHearingAsDuplicateForCase(command);
        verify(handler).removeCaseFromGroupCases(command);
    }

    @Test
    void nextHearingCommandsDelegateToTheSeedHandler() {
        final NextHearingCommands commands = new NextHearingCommands(seedHandler, new ProcessorQueue());

        commands.listNextHearingsEnrichedV2(command);
        commands.listUnscheduledNextHearingsEnriched(command);
        commands.deleteNextHearings(command);
        commands.deletePreviousHearingsAndCreateNextHearing(command);
        commands.updateExistingHearing(command);

        verify(seedHandler).listNextHearings(command);
        verify(seedHandler).handleListUnscheduledNextHearings(command);
        verify(seedHandler).deleteNextHearings(command);
        verify(seedHandler).deletePreviousHearingsAndCreateNextHearing(command);
        verify(seedHandler).updateExistingHearing(command);
    }

    /**
     * Every internal command of inv-commands.md summary A (sent only by the JEE processor), apart
     * from the court-list ones (CourtListCommands, P2.3.1), has exactly one command service now.
     */
    @Test
    void everyInternalCommandTheProcessorSendsHasACommandService() {
        final Map<String, Consumer<Envelope>> all = new HashMap<>();
        all.putAll(registered(new CaseCommands(handler, new ProcessorQueue())));
        registered(new HearingCommands(org.mockito.Mockito.mock(HearingCommandHandler.class), new ProcessorQueue())).forEach((name, h) -> {
            assertThat(all.put(name, h)).as("two services for %s", name).isNull();
        });

        final List<String> internal = List.of("add-cases-to-hearing", "add-court-application-for-hearing", "add-court-application-to-hearing",
                "add-defendants-to-court-proceedings", "add-defendants-to-court-proceedings-for-hearing", "add-hearing-to-case",
                "add-offences-for-hearing", "cancel-hearing-days", "change-next-hearing-day", "delete-court-application-hearing",
                "delete-offences-for-hearing", "delete-seeded-hearing", "eject-application", "eject-case", "eject-case-or-application",
                "handler.modify-hearing-counsel", "hearing-vacate-trial", "list-next-hearing", "list-unscheduled-next-hearing",
                "mark-hearing-as-deleted", "mark-hearing-as-duplicate", "mark-hearing-as-duplicate-for-case", "remove-case-from-group-cases",
                "remove-offences-from-existing-hearing", "remove-partially-merged-offences-from-original-hearing",
                "remove-selected-offences-from-existing-hearing", "set-hearing-resulted-status", "update-case-defendant-details",
                "update-case-defendant-offences", "update-case-markers", "update-case-markers-to-hearing",
                "update-case-resulted-defendant-proceedings-concluded", "update-court-application", "update-court-application-for-hearings",
                "update-cps-prosecutor-with-associated-hearings", "update-defendant-court-proceedings", "update-defendant-legalaid-status",
                "update-defendant-legalaid-status-for-hearing", "update-defendants-for-hearing", "update-hearing-to-case",
                "update-laa-reference-for-application", "update-linked-case-in-hearing", "update-linked-cases", "update-offences-for-hearing");
        internal.forEach(name -> assertThat(all).containsKey("listing.command." + name));
    }

    private static Map<String, Consumer<Envelope>> registered(final InternalCommandPort port) {
        final Map<String, Consumer<Envelope>> registered = new HashMap<>();
        port.register(new InternalCommandPort.Registry() {
            @Override
            public InternalCommandPort.Registry on(final String name, final Consumer<Envelope> h) {
                registered.put(name, h);
                return this;
            }
        });
        return registered;
    }
}
