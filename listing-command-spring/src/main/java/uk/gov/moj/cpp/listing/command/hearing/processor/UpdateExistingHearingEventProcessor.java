package uk.gov.moj.cpp.listing.command.hearing.processor;

import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorPort;
import org.springframework.stereotype.Component;
import static java.util.Objects.nonNull;
import static jakarta.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.courts.AddCasesToHearing;
import uk.gov.justice.listing.events.CaseAddedToHearing;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.UpdateExistingHearingRequested;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonValueConverter;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.AddHearingToCaseCommandFromHearingAddedToCaseConverter;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Port of the JEE {@code UpdateExistingHearingEventProcessor} (MBD refactor P2.3.2, ADR 015): its
 * private-event handlers, logic unchanged. The inbound
 * {@code public.progression.related-hearing-updated-for-adhoc-hearing} handler is
 * {@code command.inbound.RelatedHearingUpdatedForAdhocHearingProcessor} (P2.4).
 */
@Component
public class UpdateExistingHearingEventProcessor implements ProcessorPort {

    @Override
    public void register(final EventHandlers processors) {
        processors
                .onJson(uk.gov.justice.listing.events.UpdateExistingHearingRequested.class, this::handleUpdateExistingHearingRequestedEvent)
                .onJson(uk.gov.justice.listing.events.CasesAddedToHearing.class, this::handleCasesAddedToHearingEvent);
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateExistingHearingEventProcessor.class);
    private static final String PUBLIC_EVENT_CASES_ADDED_FOR_UPDATED_RELATED_HEARING = "public.events.listing.cases-added-for-updated-related-hearing";
    private static final String PRIVATE_EVENT_UPDATE_EXISTING_HEARING_REQUESTED = "listing.events.update-existing-hearing-requested";
    private static final String PRIVATE_EVENT_CASES_ADDED_TO_HEARING = "listing.event.cases-added-to-hearing";

    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    private static final String COMMAND_ADD_CASES_TO_HEARING = "listing.command.add-cases-to-hearing";
    private static final String COMMAND_PAYLOAD_DEBUG_STRING = "Sending '{}' command with payload {}";
    private static final String COMMAND_ADD_HEARING_TO_CASE = "listing.command.add-hearing-to-case";



    @Autowired
    private ProcessorSender sender;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Autowired
    private AddHearingToCaseCommandFromHearingAddedToCaseConverter addHearingToCaseCommandFromHearingAddedToCaseConverter;

    private ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter();

    public void handleUpdateExistingHearingRequestedEvent(final Envelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_UPDATE_EXISTING_HEARING_REQUESTED, envelope.metadata());
        }

        final UpdateExistingHearingRequested event = jsonObjectConverter.convert(envelope.payload(), UpdateExistingHearingRequested.class);
        final AddCasesToHearing addCasesForHearing = AddCasesToHearing.addCasesToHearing()
                 .withHearingId(event.getHearingId())
                 .withProsecutionCases(event.getProsecutionCases())
                 .withShadowListedOffences(event.getShadowListedOffences())
                 .withSeedingHearingId(event.getSeedingHearingId())
                 .withTier(event.getTier())
                 .withListType(event.getListType())
                 .withKeyReason(event.getKeyReason())
                 .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_CASES_TO_HEARING), objectToJsonObjectConverter.convert(addCasesForHearing)));

    }

    @SuppressWarnings("squid:S3655")
    public void handleCasesAddedToHearingEvent(final Envelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_CASES_ADDED_TO_HEARING, envelope.metadata());
        }

        final CasesAddedToHearing casesAddedToHearing = jsonObjectConverter.convert(envelope.payload(), CasesAddedToHearing.class);

        if(Optional.ofNullable(casesAddedToHearing.getAddCasesToUnAllocatedHearing()).orElse(false)) {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.cases-added-to-hearing"),
                    buildCasesAddedToHearingPublicEventPayload(casesAddedToHearing)));
        }else if (nonNull(casesAddedToHearing.getSeedingHearingId()) ) {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_CASES_ADDED_FOR_UPDATED_RELATED_HEARING),
                    createObjectBuilder()
                            .add("hearingId", casesAddedToHearing.getHearingId().toString())
                            .add("seedingHearingId", casesAddedToHearing.getSeedingHearingId().toString())
                            .build()
            ));
        }

        final List<AddHearingToCaseCommand> listHearingCommands = addHearingToCaseCommandFromHearingAddedToCaseConverter.convert(casesAddedToHearing);
        listHearingCommands.forEach(
                listHearingCommand -> {
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_HEARING_TO_CASE, listHearingCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_HEARING_TO_CASE),
                            objectToJsonValueConverter.convert(listHearingCommand)));
                }
        );
    }

    private JsonObject buildCasesAddedToHearingPublicEventPayload(final CasesAddedToHearing casesAddedToHearing) {
        final CaseAddedToHearing payload = CaseAddedToHearing.caseAddedToHearing().withExistingHearingId(casesAddedToHearing.getSeedingHearingId())
                .withHearingId(casesAddedToHearing.getHearingId())
                .withConfirmedProsecutionCase(casesAddedToHearing.getUnAllocatedListedCases().stream()
                        .map(listedCase -> ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withId(listedCase.getId())
                                .withDefendants(listedCase.getDefendants().stream().map(defendant -> ConfirmedDefendant.confirmedDefendant()
                                        .withId(defendant.getId())
                                        .withOffences(defendant.getOffences().stream().map(offence -> ConfirmedOffence.confirmedOffence()
                                                .withId(offence.getId())
                                                .build()).collect(Collectors.toList()))
                                        .build()).collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
        return objectToJsonObjectConverter.convert(payload);
    }

}
