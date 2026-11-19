package uk.gov.moj.cpp.listing.command.hearing.handler;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.CreateNextHearing;
import uk.gov.justice.listing.courts.DeleteNextHearings;
import uk.gov.justice.listing.courts.DeletePreviousHearingsAndCreateNextHearing;
import uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched;
import uk.gov.justice.listing.courts.UpdateExistingHearing;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.HearingListingNeedsConverterCommandToCore;
import uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRules;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.PtphDetail;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The JEE command-handler methods for the SeedHearingAggregate (MBD refactor P2.3.3, ADR 015),
 * ported as verbatim as possible from {@code ListNextHearingCommandHandler},
 * {@code UnscheduledListingCommandHandler}, {@code DeletePreviousHearingsAndCreateNextHearingHandler}
 * and {@code UpdateExistingHearingCommandHandler}: the seed rules ({@link SeedHearingRules}) run on
 * their kept state through {@link HearingFlow}. Called by {@link NextHearingCommands}.
 *
 * <p>The courtscheduler slot release ({@code HearingSlotsService.delete}, an HTTP DELETE) of
 * list-next-hearings-enriched-v2 and delete-next-hearings runs, as in JEE, inside the decision,
 * before the events: a failing release fails the command (and rolls the transaction back), but the
 * releases already made are not undone, as before.
 */
@Component
@SuppressWarnings({"squid:S1188", "squid:S2629"})
public class SeedHearingCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeedHearingCommandHandler.class);

    @Autowired
    private HearingFlow hearingFlow;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    @Autowired
    private HearingListingNeedsConverterCommandToCore hearingListingNeedsConverterCommandToCore;

    @Autowired
    private HearingSlotsService hearingSlotsService;

    /** {@code listing.command.list-next-hearings-enriched-v2} (ListNextHearingCommandHandler). */
    @SuppressWarnings("squid:S3655")
    public void listNextHearings(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-next-hearings-enriched' received with payload {}", command.payload());
        }

        final JsonObject payload = command.payload();
        final ListNextHearingsEnrichedV2 listNextHearingsEnriched = jsonObjectConverter.convert(payload, ListNextHearingsEnrichedV2.class);
        final SeedingHearing seedingHearing = listNextHearingsEnriched.getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final String hearingDay = seedingHearing.getSittingDay();
        final List<uk.gov.justice.listing.commands.HearingListingNeeds> hearings = listNextHearingsEnriched.getListNextHearings().getHearings().stream()
                .map(hearingListingNeedsConverterCommandToCore::convert).collect(toList());
        final String adjournedFromDate = listNextHearingsEnriched.getAdjournedFromDate();
        final List<CourtCentreDefaults> courtCentresDetails = listNextHearingsEnriched.getCourtCentresDetails().stream()
                .map(courtCentreDetails -> CourtCentreDefaults.courtCentreDefaults()
                        .withCourtCentreId(courtCentreDetails.getId())
                        .withDefaultDuration(courtCentreDetails.getDefaultDuration())
                        .withDefaultStartTime(courtCentreDetails.getDefaultStartTime())
                        .build())
                .toList();
        final List<UUID> shadowListedOffences = listNextHearingsEnriched.getListNextHearings().getShadowListedOffences();
        updateSeedAggregateEventStream(command, StringUtils.EMPTY, seedingHearingId, (SeedHearingRules seedHearingAggregate) ->
                seedHearingAggregate.requestNextHearings(hearings, hearingDay, courtCentresDetails, ofNullable(adjournedFromDate), shadowListedOffences));
    }

    /** {@code listing.command.delete-next-hearings} (ListNextHearingCommandHandler). */
    public void deleteNextHearings(final Envelope command) {
        final JsonObject payload = command.payload();
        final DeleteNextHearings deleteNextHearings = jsonObjectConverter.convert(payload, DeleteNextHearings.class);
        final SeedingHearing seedingHearing = deleteNextHearings.getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final String hearingDay = seedingHearing.getSittingDay();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'deleting next hearings for hearingId: {} hearingDay: {}", seedingHearingId, hearingDay);
        }

        updateSeedAggregateEventStream(command, seedingHearing.getSittingDay(), seedingHearingId, (SeedHearingRules seedHearingAggregate) ->
                seedHearingAggregate.deleteNextHearings(seedingHearingId, hearingDay));
    }

    /** {@code listing.command.list-unscheduled-next-hearings-enriched} (UnscheduledListingCommandHandler). */
    public void handleListUnscheduledNextHearings(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.list-unscheduled-next-hearings-enriched' listCourtHearing: {}", command.payload());
        }

        final ListUnscheduledNextHearingsEnriched listUnscheduledNextHearingsEnriched = jsonObjectConverter.convert(command.payload(), ListUnscheduledNextHearingsEnriched.class);

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeeds = listUnscheduledNextHearingsEnriched.getHearings();
        final List<CourtCentreDefaults> courtCentresDetails = listUnscheduledNextHearingsEnriched.getCourtCentresDetails().stream()
                .map(courtCentreDetails -> CourtCentreDefaults.courtCentreDefaults()
                        .withCourtCentreId(courtCentreDetails.getId())
                        .withDefaultDuration(courtCentreDetails.getDefaultDuration())
                        .withDefaultStartTime(courtCentreDetails.getDefaultStartTime())
                        .build())
                .collect(toList());

        final SeedingHearing seedingHearing = listUnscheduledNextHearingsEnriched.getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final String hearingDay = seedingHearing.getSittingDay();

        final Map<UUID, PtphDetail> ptphDetailsByHearingId = ptphDetailsByHearingId(listUnscheduledNextHearingsEnriched.getPtphDetails());

        updateSeedHearingEventStream(command, seedingHearingId, (SeedHearingRules seedHearingAggregate) ->
                seedHearingAggregate.requestNextUnscheduledHearings(unscheduledListingNeeds, hearingDay, courtCentresDetails, ptphDetailsByHearingId));
    }

    /** The enriched command's details, keyed by next-hearing id (only the Crown Court trials have one). */
    private Map<UUID, PtphDetail> ptphDetailsByHearingId(final List<uk.gov.justice.listing.commands.HearingPtphDetail> ptphDetails) {
        if (isNull(ptphDetails)) {
            return emptyMap();
        }
        return ptphDetails.stream()
                .collect(toMap(uk.gov.justice.listing.commands.HearingPtphDetail::getHearingId,
                        detail -> new PtphDetail(detail.getTier(), detail.getListType(), detail.getKeyReason())));
    }

    /** {@code listing.command.delete-previous-hearings-and-create-next-hearing} (DeletePreviousHearingsAndCreateNextHearingHandler). */
    public void deletePreviousHearingsAndCreateNextHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.delete-previous-hearings-and-create-next-hearing' received with payload {}", command.payload());
        }
        final DeletePreviousHearingsAndCreateNextHearing deletePreviousHearingsAndCreateNextHearing =
                jsonObjectConverter.convert(command.payload(), DeletePreviousHearingsAndCreateNextHearing.class);
        final SeedingHearing seedingHearing = deletePreviousHearingsAndCreateNextHearing.getDeletePreviousHearings().getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final String hearingDay = seedingHearing.getSittingDay();
        final CreateNextHearing createNextHearing = deletePreviousHearingsAndCreateNextHearing.getCreateNextHearing();
        final uk.gov.justice.listing.events.CreateNextHearing createNextHearingEvent = uk.gov.justice.listing.events.CreateNextHearing.createNextHearing()
                .withCommittingCourt(createNextHearing.getCommittingCourt())
                .withHearing(createNextHearing.getHearing())
                .withPreviousBookingReferencesWithCourtScheduleIds(createNextHearing.getPreviousBookingReferencesWithCourtScheduleIds())
                .withSeedingHearing(createNextHearing.getSeedingHearing())
                .withShadowListedOffences(createNextHearing.getShadowListedOffences())
                .build();

        updateSeedHearingEventStream(command, seedingHearingId, (SeedHearingRules seedHearingAggregate) ->
                seedHearingAggregate.deletePreviousHearingsAndCreateNextHearing(seedingHearingId, hearingDay, createNextHearingEvent));
    }

    /** {@code listing.command.update-existing-hearing} (UpdateExistingHearingCommandHandler). */
    @SuppressWarnings("squid:S3655")
    public void updateExistingHearing(final Envelope command) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'listing.command.update-existing-hearing' received with payload {}", command.payload());
        }

        final JsonObject payload = command.payload();
        final UpdateExistingHearing updateExistingHearing = jsonObjectConverter.convert(payload, UpdateExistingHearing.class);
        final SeedingHearing seedingHearing = updateExistingHearing.getSeedingHearing();
        final UUID seedingHearingId = seedingHearing.getSeedingHearingId();
        final UUID existingHearingHearingId = updateExistingHearing.getHearingId();
        final List<ProsecutionCase> prosecutionCases = updateExistingHearing.getProsecutionCases();
        final List<UUID> shadowListedOffences = updateExistingHearing.getShadowListedOffences();
        final String hearingDay = seedingHearing.getSittingDay();

        // LPT-2405: tier / list type resolved in the command API from the stored hearing
        final PtphDetail ptphDetail = buildPtphDetail(updateExistingHearing.getTier(),
                updateExistingHearing.getListType(), updateExistingHearing.getKeyReason());

        updateSeedHearingEventStream(command, seedingHearingId, (SeedHearingRules seedHearingAggregate) ->
                seedHearingAggregate.requestUpdateExistingHearing(seedingHearingId, existingHearingHearingId, hearingDay, prosecutionCases, shadowListedOffences, ptphDetail));
    }

    private PtphDetail buildPtphDetail(final String tier, final String listType, final String keyReason) {
        if (isNull(tier) && isNull(listType) && isNull(keyReason)) {
            return null;
        }
        return new PtphDetail(tier, listType, keyReason);
    }

    /** ListNextHearingCommandHandler's: releases the slots of the next hearings seeded for {@code sittingDay}, then decides. */
    private void updateSeedAggregateEventStream(final Envelope command, final String sittingDay, final UUID seedHearingId,
                                                final Function<SeedHearingRules, Stream<Object>> aggregatorFunction) {
        hearingFlow.updateSeedHearing(command.metadata(), seedHearingId, seedHearingAggregate -> {
            final List<DeleteNextHearingRequested> deleteNextHearingRequestedList =
                    seedHearingAggregate.retrieveDeleteHearingsList(seedHearingId, sittingDay);

            for (final DeleteNextHearingRequested deleteNextHearingRequested : deleteNextHearingRequestedList) {
                hearingSlotsService.delete(deleteNextHearingRequested.getHearingId());
            }
            return aggregatorFunction.apply(seedHearingAggregate);
        });
    }

    private void updateSeedHearingEventStream(final Envelope command, final UUID seedHearingId,
                                              final Function<SeedHearingRules, Stream<Object>> aggregatorFunction) {
        hearingFlow.updateSeedHearing(command.metadata(), seedHearingId, aggregatorFunction);
    }
}
