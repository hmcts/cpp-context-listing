package uk.gov.moj.cpp.listing.command.inbound;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.util.List;

import jakarta.json.JsonArrayBuilder;
import jakarta.persistence.EntityManager;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Port of the JEE {@code CpsProsecutorUpdatedEventProcessor} (MBD refactor P2.4): finds the case's
 * allocated and unallocated hearings with the query view (JEE: an in-VM call of
 * {@code listing.allocated.and.unallocated.hearings}, a viewstore read, no REST and no user id) and,
 * if there are any, sends update-cps-prosecutor-with-associated-hearings for them.
 *
 * <p>The search loads the hearings into the persistence context before the command takes each
 * hearing's lock (P2.4, {@code HearingStateStore#load}); a hearing changed by a concurrent command in
 * between would then be decided on as first read. The context holds nothing else yet (the dedup row
 * is a JDBC insert), so it is cleared after the search and the command reads each hearing after
 * its lock.
 */
@Component
public class CpsProsecutorUpdatedEventProcessor extends InboundProcessor {

    static final String PUBLIC_PROGRESSION_EVENTS_CPS_PROSECUTOR_UPDATED = "public.progression.events.cps-prosecutor-updated";
    static final String COMMAND_UPDATE_CPS_PROSECUTOR_WITH_ASSOCIATED_HEARINGS = "listing.command.update-cps-prosecutor-with-associated-hearings";

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsProsecutorUpdatedEventProcessor.class);
    private static final String HEARINGS_ID = "hearingIds";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";

    private final ProcessorSender sender;
    private final HearingQueryView hearingQueryView;
    private final EntityManager entityManager;

    public CpsProsecutorUpdatedEventProcessor(final ProcessorQueue processorQueue, final ProcessorSender sender,
                                              final HearingQueryView hearingQueryView, final EntityManager entityManager) {
        super(processorQueue);
        this.sender = sender;
        this.hearingQueryView = hearingQueryView;
        this.entityManager = entityManager;
        on(PUBLIC_PROGRESSION_EVENTS_CPS_PROSECUTOR_UPDATED, this::cpsProsecutorUpdated);
    }

    public void cpsProsecutorUpdated(final Envelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.progression.events.cps-prosecutor-updated event received {}", event.metadata());
        }

        final JsonObject inputPayload = event.payload();
        final JsonObject queryPayload = createObjectBuilder().add("caseId", inputPayload.get(PROSECUTION_CASE_ID)).build();

        final JsonObject queryResponse = hearingQueryView.searchAllocatedAndUnallocatedHearings(queryPayload);

        final List<String> hearingIds = queryResponse.getJsonArray("hearings").stream()
                .map(JsonObject.class::cast)
                .map(h -> h.getString("id"))
                .toList();
        entityManager.clear();

        if (hearingIds.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.info("public.progression.events.cps-prosecutor-updated event received without hearings in listing {}", event.metadata());
            }
        } else {
            final JsonArrayBuilder builder = createArrayBuilder();
            hearingIds.forEach(builder::add);
            final JsonObject outputPayload = createObjectBuilder().add(PROSECUTION_CASE_ID, inputPayload.get(PROSECUTION_CASE_ID))
                    .add("prosecutionAuthorityId", inputPayload.get("prosecutionAuthorityId"))
                    .add("prosecutionAuthorityCode", inputPayload.get("prosecutionAuthorityCode"))
                    .add(HEARINGS_ID, builder.build())
                    .build();
            sender.send(envelopeFrom(metadataFrom(event.metadata())
                    .withName(COMMAND_UPDATE_CPS_PROSECUTOR_WITH_ASSOCIATED_HEARINGS), outputPayload));
        }
    }
}
