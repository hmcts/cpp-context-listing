package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Optional.ofNullable;
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
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingMarkedAsDuplicateEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingMarkedAsDuplicateEventListener.class);

    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_DELETED = "listing.events.allocated-hearing-deleted";
    private static final String PRIVATE_EVENT_UNALLOCATED_HEARING_DELETED = "listing.events.unallocated-hearing-deleted";


    private HearingRepository hearingRepository;

    @Inject
    public HearingMarkedAsDuplicateEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Handles("listing.events.hearing-marked-as-duplicate")
    public void hearingMarkedAsDuplicate(final Envelope<HearingMarkedAsDuplicate> event) {
        final UUID hearingId = event.payload().getHearingId();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.events.hearing-marked-as-duplicate received. hearingId: {} ", hearingId);
        }

        deleteHearing(hearingId);

    }

    @Handles(PRIVATE_EVENT_ALLOCATED_HEARING_DELETED)
    public void handleAllocatedHearingDeleted(final Envelope<AllocatedHearingDeleted> event) {
        final UUID hearingId = event.payload().getHearingId();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Event {} hearingId: {} ", PRIVATE_EVENT_ALLOCATED_HEARING_DELETED, hearingId);
        }

        deleteHearing(hearingId);
    }

    @Handles(PRIVATE_EVENT_UNALLOCATED_HEARING_DELETED)
    public void handleUnallocatedHearingDeleted(final Envelope<UnallocatedHearingDeleted> event) {
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
    @Handles("listing.events.offences-removed-from-hearing")
    public void hearingUnAllocatedForListingV2(final Envelope<OffencesRemovedFromHearing> event) {
        final UUID hearingId = event.payload().getHearingId();
        final UUID seedingHearingId = event.payload().getSeedingHearingId();
        final List<UUID> seedCaseIds = event.payload().getCaseIdsSeededByOnlySeedingHearingId();
        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };
        final TypeReference<List<HearingDay>> typeHearingDayRef = new TypeReference<List<HearingDay>>() {
        };
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.events.offences-removed-from-hearing. hearingId: {} ", hearingId);
        }

        using(hearingRepository)
                .find(hearingId)
                .put("allocated", false)
                .remove("courtRoomId")
                .putSubList("hearingDays", typeHearingDayRef, getHearingDaysWithRemoveCourtRoomIdFunction())
                .putSubList("listedCases", typeRef, getListedCaseWithRemoveDeletedOffencesFunction(seedingHearingId, seedCaseIds))
                .save();

    }

    @Handles("listing.events.offences-removed-from-existing-allocated-hearing")
    public void removeOffencesFromExistingAllocatedHearing(final Envelope<OffencesRemovedFromExistingAllocatedHearing> event) {

        final UUID hearingId = event.payload().getHearingId();
        final List<UUID> offenceIds = event.payload().getOffenceIds();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("listing.events.offences-removed-from-existing-allocated-hearing with hearing id: {} and offence ids: {}", hearingId, offenceIds);
        }

        removeOffencesFromHearing(hearingId, offenceIds);

    }

    @Handles("listing.events.offences-removed-from-existing-unallocated-hearing")
    public void removeOffencesFromExistingUnallocatedHearing(final Envelope<OffencesRemovedFromExistingUnallocatedHearing> event) {

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

        using(hearingRepository)
                .find(hearingId)
                .putSubList("listedCases", typeRef, getListedCaseWithRemoveDeletedOffencesFunction(offenceIds))
                .save();
    }

    private void deleteHearing(final UUID hearingId) {
        final Hearing hearing = hearingRepository.findBy(hearingId);
        if (Objects.nonNull(hearing)) {
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
                .map(hearingDay -> HearingDay.hearingDay().withValuesFrom(hearingDay).withCourtRoomId(ofNullable(null)).build())
                .collect(toList()));

    }

    private List<ListedCase> getListedCaseWithRemoveDeletedOffences(final UUID seedingHearingId, final List<UUID> seedCaseIds, final List<ListedCase> cases) {
        final List<ListedCase> listedCases = new ArrayList<>(cases.stream().filter(listedCase -> !seedCaseIds.contains(listedCase.getId())).collect(toList()));
        listedCases.forEach(listedCase -> listedCase.getDefendants()
                .forEach(defendant -> defendant.getOffences()
                        .removeIf(offence -> offence.getSeedingHearing().isPresent() &&  offence.getSeedingHearing().get().getSeedingHearingId().equals(seedingHearingId))));

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
