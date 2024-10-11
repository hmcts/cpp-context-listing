package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.JsonObject;
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
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.command.AddHearingToCaseCommandFromHearingAddedToCaseConverter;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class UpdateExistingHearingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateExistingHearingEventProcessor.class);
    private static final String PUBLIC_EVENT_CASES_ADDED_FOR_UPDATED_RELATED_HEARING = "public.events.listing.cases-added-for-updated-related-hearing";
    private static final String PRIVATE_EVENT_UPDATE_EXISTING_HEARING_REQUESTED = "listing.events.update-existing-hearing-requested";
    private static final String PRIVATE_EVENT_CASES_ADDED_TO_HEARING = "listing.event.cases-added-to-hearing";

    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    private static final String COMMAND_ADD_CASES_TO_HEARING = "listing.command.add-cases-to-hearing";
    private static final String COMMAND_PAYLOAD_DEBUG_STRING = "Sending '{}' command with payload {}";
    private static final String COMMAND_ADD_HEARING_TO_CASE = "listing.command.add-hearing-to-case";



    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private AddHearingToCaseCommandFromHearingAddedToCaseConverter addHearingToCaseCommandFromHearingAddedToCaseConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Handles(PRIVATE_EVENT_UPDATE_EXISTING_HEARING_REQUESTED)
    public void handleUpdateExistingHearingRequestedEvent(final JsonEnvelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_UPDATE_EXISTING_HEARING_REQUESTED, envelope.toObfuscatedDebugString());
        }

        final UpdateExistingHearingRequested event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateExistingHearingRequested.class);
        final AddCasesToHearing addCasesForHearing = AddCasesToHearing.addCasesToHearing()
                 .withHearingId(event.getHearingId())
                 .withProsecutionCases(event.getProsecutionCases())
                 .withShadowListedOffences(event.getShadowListedOffences())
                 .withSeedingHearingId(event.getSeedingHearingId())
                 .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_CASES_TO_HEARING), objectToJsonObjectConverter.convert(addCasesForHearing)));

    }

    @SuppressWarnings("squid:S3655")
    @Handles(PRIVATE_EVENT_CASES_ADDED_TO_HEARING)
    public void handleCasesAddedToHearingEvent(final JsonEnvelope envelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_CASES_ADDED_TO_HEARING, envelope.toObfuscatedDebugString());
        }

        final CasesAddedToHearing casesAddedToHearing = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CasesAddedToHearing.class);

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

    @Handles("public.progression.related-hearing-updated-for-adhoc-hearing")
    public void handleRelatedHearingUpdatedForAdhocHearing(final JsonEnvelope envelope){
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, "public.progression.related-hearing-updated-for-adhoc-hearing", envelope.toObfuscatedDebugString());
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_CASES_TO_HEARING), envelope.payloadAsJsonObject()));
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
