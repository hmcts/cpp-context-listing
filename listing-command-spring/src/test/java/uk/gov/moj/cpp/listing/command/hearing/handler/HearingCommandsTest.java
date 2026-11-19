package uk.gov.moj.cpp.listing.command.hearing.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.listing.command.hearing.flow.InternalCommandPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link HearingCommands}: each command runs the ported handler method; the internal ones are reachable by name. */
@ExtendWith(MockitoExtension.class)
class HearingCommandsTest {

    @Mock
    private HearingCommandHandler handler;

    private final Envelope command = new Envelope(Metadata.builder().withId(UUID.randomUUID()).withName("x").build(), JsonValue.EMPTY_JSON_OBJECT);

    @Test
    void theInternalCommandsTheProcessorSendsAreRegisteredByName() {
        final HearingCommands commands = new HearingCommands(handler, new ProcessorQueue());
        final Map<String, Consumer<Envelope>> registered = new HashMap<>();
        commands.register(new InternalCommandPort.Registry() {
            @Override
            public InternalCommandPort.Registry on(final String name, final Consumer<Envelope> h) {
                registered.put(name, h);
                return this;
            }
        });

        assertThat(registered).containsOnlyKeys(
                "listing.command.hearing-vacate-trial",
                "listing.command.remove-partially-merged-offences-from-original-hearing",
                "listing.command.mark-hearing-as-deleted",
                "listing.command.add-cases-to-hearing",
                "listing.command.update-court-application-for-hearings",
                "listing.command.add-court-application-for-hearing",
                "listing.command.handler.modify-hearing-counsel",
                "listing.command.cancel-hearing-days",
                "listing.command.update-cps-prosecutor-with-associated-hearings",
                "listing.command.set-hearing-resulted-status",
                "listing.command.delete-court-application-hearing",
                "listing.command.change-next-hearing-day",
                "listing.command.remove-selected-offences-from-existing-hearing",
                "listing.command.remove-offences-from-existing-hearing",
                "listing.command.mark-hearing-as-duplicate",
                "listing.command.list-next-hearing",
                "listing.command.list-unscheduled-next-hearing",
                "listing.command.delete-seeded-hearing",
                "listing.command.update-defendants-for-hearing",
                "listing.command.update-offences-for-hearing",
                "listing.command.delete-offences-for-hearing",
                "listing.command.add-offences-for-hearing",
                "listing.command.update-case-markers-to-hearing",
                "listing.command.update-linked-case-in-hearing",
                "listing.command.add-defendants-to-court-proceedings-for-hearing",
                "listing.command.eject-case",
                "listing.command.eject-application",
                "listing.command.update-defendant-legalaid-status-for-hearing",
                "listing.command.update-defendant-court-proceedings");

        registered.get("listing.command.mark-hearing-as-deleted").accept(command);
        verify(handler).handleMarkHearingAsDeleted(command);
        registered.get("listing.command.change-next-hearing-day").accept(command);
        verify(handler).changeNextHearingDay(command);
        registered.get("listing.command.list-next-hearing").accept(command);
        verify(handler).listNextCourtHearing(command);
        registered.get("listing.command.update-defendant-court-proceedings").accept(command);
        verify(handler).updateDefendantCourtProceedings(command);
    }

    @Test
    void eachCommandDelegatesToThePortedHandlerMethod() {
        final HearingCommands commands = new HearingCommands(handler, new ProcessorQueue());

        commands.listCourtHearingEnriched(command);
        commands.listUnscheduledCourtHearingEnriched(command);
        commands.updateHearingForListingEnriched(command);
        commands.updateHearingsForListingEnriched(command);
        commands.vacateTrialEnriched(command);
        commands.moveHearingToPastDateEnriched(command);
        commands.changeCourtRoomForMultidayHearingEnriched(command);
        commands.extendHearingForHearingEnriched(command);
        commands.changeJudiciaryForHearings(command);
        commands.sequenceHearings(command);
        commands.restrictCourtList(command);
        commands.updateHearingDayCourtSchedule(command);
        commands.deleteHearing(command);
        commands.updateHearingAddCaseBdf(command);
        commands.migrateCrownHearingsToCourtschedules(command);
        commands.markUnallocatedHearingAsDuplicate(command);
        commands.markHearingAsDuplicate(command);
        commands.listUnscheduledNextHearing(command);
        commands.deleteSeededHearing(command);
        commands.updateDefendantsForHearing(command);
        commands.updateOffencesForHearing(command);
        commands.deleteOffencesForHearing(command);
        commands.addOffencesForHearing(command);
        commands.updateCaseMarkersToHearing(command);
        commands.updateLinkedCaseInHearing(command);
        commands.addDefendantsToCourtProceedingsForHearing(command);
        commands.ejectCase(command);
        commands.ejectApplication(command);
        commands.updateDefendantLegalaidStatusForHearing(command);

        verify(handler).listCourtHearing(command);
        verify(handler).handleListUnscheduledCourtHearing(command);
        verify(handler).updateHearingForListing(command);
        verify(handler).updateHearingsForListing(command);
        verify(handler).vacateTrial(command);
        verify(handler).moveHearingToPastDate(command);
        verify(handler).changeCourtRoomForMultidayHearing(command);
        verify(handler).extendHearingForHearing(command);
        verify(handler).changeJudiciaryForHearings(command);
        verify(handler).sequenceHearings(command);
        verify(handler).restrictFromCourtList(command);
        verify(handler).updateHearingDayCourtSchedule(command);
        verify(handler).deleteHearing(command);
        verify(handler).updateHearingAddCaseBdf(command);
        verify(handler).migrateCrownHearingsToCourtSchedules(command);
        verify(handler).markUnallocatedHearingAsDuplicate(command);
        verify(handler).markHearingAsDuplicate(command);
        verify(handler).handleListUnscheduledNextHearing(command);
        verify(handler).deleteSeededHearing(command);
        verify(handler).updateDefendantsForHearing(command);
        verify(handler).updateOffencesForHearing(command);
        verify(handler).deleteOffencesForHearing(command);
        verify(handler).addOffencesForHearing(command);
        verify(handler).updateCaseMarkersToHearing(command);
        verify(handler).updateLinkedCaseInHearing(command);
        verify(handler).addDefendantsForCourtProceedingsForHearing(command);
        verify(handler).ejectCase(command);
        verify(handler).ejectApplication(command);
        verify(handler).updateDefendantLegalAidStatusForHearing(command);
    }
}
