package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.AllocatedHearingDeleted;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing;
import uk.gov.justice.listing.events.UnallocatedHearingDeleted;
import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;


import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class HearingMarkedAsDuplicateEventListener implements ListenerPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingMarkedAsDuplicateEventListener.class);

    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_DELETED = "listing.events.allocated-hearing-deleted";
    private static final String PRIVATE_EVENT_UNALLOCATED_HEARING_DELETED = "listing.events.unallocated-hearing-deleted";


    private HearingRepository hearingRepository;
    private HearingSearchSyncService hearingSearchSyncService;
    public HearingMarkedAsDuplicateEventListener(final HearingRepository hearingRepository,  final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(HearingMarkedAsDuplicate.class, this::hearingMarkedAsDuplicate)
                .on(AllocatedHearingDeleted.class, this::handleAllocatedHearingDeleted)
                .on(UnallocatedHearingDeleted.class, this::handleUnallocatedHearingDeleted)
                .on(OffencesRemovedFromHearing.class, this::hearingUnAllocatedForListingV2)
                .on(OffencesRemovedFromExistingAllocatedHearing.class, this::removeOffencesFromExistingAllocatedHearing)
                .on(OffencesRemovedFromExistingUnallocatedHearing.class, this::removeOffencesFromExistingUnallocatedHearing);
    }

    public void hearingMarkedAsDuplicate(final RaisedEvent<HearingMarkedAsDuplicate> event) {
        final UUID hearingId = event.payload().getHearingId();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.events.hearing-marked-as-duplicate received. hearingId: {} ", hearingId);
        }

        deleteHearing(hearingId);

    }
    public void handleAllocatedHearingDeleted(final RaisedEvent<AllocatedHearingDeleted> event) {
        final UUID hearingId = event.payload().getHearingId();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Event {} hearingId: {} ", PRIVATE_EVENT_ALLOCATED_HEARING_DELETED, hearingId);
        }

        deleteHearing(hearingId);
    }
    public void handleUnallocatedHearingDeleted(final RaisedEvent<UnallocatedHearingDeleted> event) {
        final UUID hearingId = event.payload().getHearingId();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Event {} hearingId: {} ", PRIVATE_EVENT_UNALLOCATED_HEARING_DELETED, hearingId);
        }

        deleteHearing(hearingId);

    }

    /**
     * CourtRoom id fields needs to be clean up like allocated field when the hearing is
     * unallocated. Manage Hearing links in the UI  appears regarding whether courtRoomId field  is
     * null or not null in both hearing and hearingDays.
     */
    public void hearingUnAllocatedForListingV2(final RaisedEvent<OffencesRemovedFromHearing> event) {
        final UUID hearingId = event.payload().getHearingId();
        final UUID seedingHearingId = event.payload().getSeedingHearingId();
        final List<UUID> seedCaseIds = event.payload().getCaseIdsSeededByOnlySeedingHearingId();
        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.events.offences-removed-from-hearing. hearingId: {} ", hearingId);
        }

        if(isNull(hearingRepository.findBy(hearingId))){
            return;
        }
        using(hearingRepository)
                .find(hearingId)
                .putSubList("listedCases", typeRef, getListedCaseWithRemoveDeletedOffencesFunction(seedingHearingId, seedCaseIds))
                .save();
        hearingSearchSyncService.sync(hearingId);

    }
    public void removeOffencesFromExistingAllocatedHearing(final RaisedEvent<OffencesRemovedFromExistingAllocatedHearing> event) {

        final UUID hearingId = event.payload().getHearingId();
        final List<UUID> offenceIds = event.payload().getOffenceIds();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.events.offences-removed-from-existing-allocated-hearing with hearing id: {} and offence ids: {}", hearingId, offenceIds);
        }

        removeOffencesFromHearing(hearingId, offenceIds);

    }
    public void removeOffencesFromExistingUnallocatedHearing(final RaisedEvent<OffencesRemovedFromExistingUnallocatedHearing> event) {

        final UUID hearingId = event.payload().getHearingId();
        final List<UUID> offenceIds = event.payload().getOffenceIds();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.events.offences-removed-from-existing-unallocated-hearing with hearing id: {} and offence ids: {}", hearingId, offenceIds);
        }

        removeOffencesFromHearing(hearingId, offenceIds);

    }

    private void removeOffencesFromHearing(final UUID hearingId, final List<UUID> offenceIds) {

        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };
        if(isNull(hearingRepository.findBy(hearingId))){
            return;
        }
        using(hearingRepository)
                .find(hearingId)
                .putSubList("listedCases", typeRef, getListedCaseWithRemoveDeletedOffencesFunction(offenceIds))
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    private void deleteHearing(final UUID hearingId) {
        final Hearing hearing = hearingRepository.findBy(hearingId);
        if (nonNull(hearing)) {
            hearingRepository.remove(hearing);
        }
    }

    private Function<List<HearingDay>, List<HearingDay>> getHearingDaysWithRemoveCourtRoomIdFunction() {
        return this::getHearingDaysWithRemoveCourtRoomId;
    }

    private Function<List<ListedCase>, List<ListedCase>> getListedCaseWithRemoveDeletedOffencesFunction(final UUID seedingHearingId, final List<UUID> seedCaseIds) {
        return cases -> getListedCaseWithRemoveDeletedOffences(seedingHearingId, seedCaseIds, cases);
    }

    private Function<List<ListedCase>, List<ListedCase>> getListedCaseWithRemoveDeletedOffencesFunction(final List<UUID> offenceIds) {
        return cases -> getListedCaseWithRemoveDeletedOffences(offenceIds, cases);
    }


    private List<HearingDay> getHearingDaysWithRemoveCourtRoomId(final List<HearingDay> hearingDays) {
        return new ArrayList<>(hearingDays.stream()
                .map(hearingDay -> HearingDay.hearingDay().withValuesFrom(hearingDay).withCourtRoomId(null).build())
                .collect(toList()));

    }

    private List<ListedCase> getListedCaseWithRemoveDeletedOffences(final UUID seedingHearingId, final List<UUID> seedCaseIds, final List<ListedCase> cases) {
        final List<ListedCase> listedCases = new ArrayList<>(cases.stream().filter(listedCase -> !seedCaseIds.contains(listedCase.getId())).collect(toList()));
        listedCases.forEach(listedCase -> listedCase.getDefendants()
                .forEach(defendant -> defendant.getOffences()
                        .removeIf(offence -> nonNull(offence.getSeedingHearing()) && offence.getSeedingHearing().getSeedingHearingId().equals(seedingHearingId))));

        listedCases.forEach(listedCase -> listedCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));

        return listedCases;

    }

    private List<ListedCase> getListedCaseWithRemoveDeletedOffences(final List<UUID> offenceIds, final List<ListedCase> cases) {
        final List<ListedCase> listedCases = cases.stream().collect(toList());
        listedCases.forEach(listedCase -> listedCase.getDefendants()
                .forEach(defendant -> defendant.getOffences()
                        .removeIf(offence -> offenceIds.contains(offence.getId()))));

        listedCases.forEach(listedCase -> listedCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
        listedCases.removeIf(listedCase -> listedCase.getDefendants().isEmpty());

        return listedCases;

    }

}
