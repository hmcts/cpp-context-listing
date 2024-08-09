package uk.gov.moj.cpp.listing.command.utils.hearing;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.courts.Defendants;
import uk.gov.justice.listing.courts.Offences;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.moj.cpp.listing.command.utils.ProsecutionCasesBuilder;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendHearingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendHearingUtils.class);

    @Inject
    private ProsecutionCasesBuilder prosecutionCasesBuilder;

    public uk.gov.justice.listing.events.Hearing updateUnallocatedHearing(final uk.gov.justice.listing.events.Hearing unAllocatedHearingStored, final Map<UUID, Map<UUID, List<UUID>>> requestCaseMap) {
        unAllocatedHearingStored.getListedCases().forEach(listedCase -> { //iterate through persisted cases
            if (requestCaseMap.containsKey(listedCase.getId())) { // if persisted case is in the request, iterate it's defendants

                listedCase.getDefendants().forEach(listedDefendant -> {
                    if (requestCaseMap.get(listedCase.getId()).containsKey(listedDefendant.getId())) { // if persisted defendant is in the request, iterate through it's offences

                        //remove the offence from persisted hearing, if offence is in the request
                        final List<uk.gov.justice.listing.events.Offence> newOffenceList = listedDefendant.getOffences().stream().filter(offence -> !requestCaseMap.get(listedCase.getId()).get(listedDefendant.getId()).contains(offence.getId())).collect(toList());
                        listedDefendant.getOffences().clear();
                        listedDefendant.getOffences().addAll(newOffenceList);
                    }
                });
                //remove the defendant if all of it's offences is removed
                final List<uk.gov.justice.listing.events.Defendant> newDefendantsList = listedCase.getDefendants().stream().filter(listedDefendant -> !listedDefendant.getOffences().isEmpty()).collect(toList());
                listedCase.getDefendants().clear();
                listedCase.getDefendants().addAll(newDefendantsList);

            }

        });
        //remove the case if all of it's defendants removed
        final List<ListedCase> newListedCaseList = unAllocatedHearingStored.getListedCases().stream().filter(listedCase -> !listedCase.getDefendants().isEmpty()).collect(toList());
        unAllocatedHearingStored.getListedCases().clear();
        unAllocatedHearingStored.getListedCases().addAll(newListedCaseList);

        LOGGER.info("Hearing updated partially: {}", unAllocatedHearingStored);

        return unAllocatedHearingStored;
    }

    public List<ListedCase> extractCasesToMove(final List<ListedCase> listedCasesToAllocate, final Map<UUID, Map<UUID, List<UUID>>> requestCaseMap) {
        listedCasesToAllocate.forEach(listedCase -> { //iterate through cases
            if (requestCaseMap.containsKey(listedCase.getId())) { // if case is in the request, iterate it's defendants

                listedCase.getDefendants().forEach(listedDefendant -> {
                    if (requestCaseMap.get(listedCase.getId()).containsKey(listedDefendant.getId())) { // if defendant is in the request, iterate through it's offences

                        //remove the offence from unAllocatedHearingNew, if offence is not in the request
                        final List<uk.gov.justice.listing.events.Offence> newOffenceList = listedDefendant.getOffences().stream().filter(offence -> requestCaseMap.get(listedCase.getId()).get(listedDefendant.getId()).contains(offence.getId())).collect(toList());
                        listedDefendant.getOffences().clear();
                        listedDefendant.getOffences().addAll(newOffenceList);
                    }
                });
                //remove the defendant if all of it's offences is removed
                final List<uk.gov.justice.listing.events.Defendant> newDefendantsList = listedCase.getDefendants().stream().filter(listedDefendant -> !listedDefendant.getOffences().isEmpty() && requestCaseMap.get(listedCase.getId()).containsKey(listedDefendant.getId())).collect(toList());
                listedCase.getDefendants().clear();
                listedCase.getDefendants().addAll(newDefendantsList);

            }

        });
        //remove the case if all of it's defendants removed
        final List<ListedCase> newListedCaseList = listedCasesToAllocate.stream().filter(listedCase -> !listedCase.getDefendants().isEmpty() && requestCaseMap.containsKey(listedCase.getId())).collect(toList());

        LOGGER.info("Cases to allocate to new hearing: {}", newListedCaseList);

        return newListedCaseList;
    }

    @SuppressWarnings("squid:S3655")
    public Map<UUID, Map<UUID, List<UUID>>> buildRequestedCaseDefendantOffenceMap(final List<ProsecutionCases> prosecutionCases, final UUID hearingId) {
        final Map<UUID, Map<UUID, List<UUID>>> requestCaseMap = new HashMap<>();
        for (final ProsecutionCases prosecutionCase : prosecutionCases) {
            final Map<UUID, List<UUID>> defendantOffencesMap = new HashMap<>();
            for (final Defendants defendant : prosecutionCase.getDefendants()) {
                if (nonNull(defendant.getDefendantId())) {
                    defendantOffencesMap.put(defendant.getDefendantId(), defendant.getOffences().stream().map(Offences::getOffenceId).collect(toList()));
                    requestCaseMap.put(prosecutionCase.getCaseId(), defendantOffencesMap);
                }
            }
        }
        LOGGER.info("Map of <case<defendants,List<offences>>> created from hearing {} in the request: {}", hearingId, requestCaseMap);
        return requestCaseMap;
    }

    public Map<UUID, Map<UUID, List<UUID>>> buildPersistedCaseDefendantOffenceMap(final uk.gov.justice.listing.events.Hearing unAllocatedHearingStored) {
        final Map<UUID, Map<UUID, List<UUID>>> persistedCasesMap = new HashMap<>();
        for (final ListedCase listedCase : unAllocatedHearingStored.getListedCases()) {
            final Map<UUID, List<UUID>> defendantOffencesMap = new HashMap<>();
            for (final uk.gov.justice.listing.events.Defendant defendant : listedCase.getDefendants()) {
                defendantOffencesMap.put(defendant.getId(), defendant.getOffences().stream().map(uk.gov.justice.listing.events.Offence::getId).collect(toList()));
                persistedCasesMap.put(listedCase.getId(), defendantOffencesMap);
            }
        }
        LOGGER.info("Map of <case<defendants,List<offences>>> created from hearing {} in the aggregate: {}", unAllocatedHearingStored.getId(), persistedCasesMap);
        return persistedCasesMap;
    }

    public Stream<Object> createPartiallyAllocationEventForUpdateHearing(final Hearing hearing, final UUID hearingId,
                                                                         final Map<UUID, Map<UUID, List<UUID>>> unallocatedHearingRequestCaseMap,
                                                                         final Map<UUID, Map<UUID, List<UUID>>> persistedUnallocatedHearingCasesMap,
                                                                         HearingUpdateOperationType operationType
    ) {
        if (HearingUpdateOperationType.FULL_ALLOCATION.equals(operationType) || HearingUpdateOperationType.UNALLOCATED_NO_OFFENCE_CHANGE.equals(operationType)) {
            return Stream.empty();
        }
        removeSelectedCaseDefendantsOffencesFromStored(persistedUnallocatedHearingCasesMap, unallocatedHearingRequestCaseMap);
        final List<uk.gov.justice.listing.events.ProsecutionCases> prosecutionCasesToBeRemovedFromHearing = prosecutionCasesBuilder.buildEventProsecutionCase(unallocatedHearingRequestCaseMap);
        return hearing.updateUnallocatedHearingPartially(hearingId, prosecutionCasesToBeRemovedFromHearing);

    }


    public void removeSelectedCaseDefendantsOffencesFromStored(final Map<UUID, Map<UUID, List<UUID>>> persistedUnallocatedHearingCasesMap, final Map<UUID, Map<UUID, List<UUID>>> unallocatedHearingRequestCaseMap) {
        final List<UUID> selectedOffences = new ArrayList<>();

        unallocatedHearingRequestCaseMap.forEach((key, value) -> selectedOffences.addAll(extractAllOffencesInDefendant(value)));

        removeSelectedCases(persistedUnallocatedHearingCasesMap, selectedOffences);
        removeSelectedDefendants(persistedUnallocatedHearingCasesMap, selectedOffences);
        removeSelectedOffences(persistedUnallocatedHearingCasesMap, selectedOffences);

    }

    @SuppressWarnings({"squid:S107"})
    public HearingUpdateOperationType getOperationType(final UUID hearingId,
                                                       final uk.gov.justice.listing.events.Hearing storedHearing,
                                                       final List<ProsecutionCases> prosecutionCases,
                                                       final Map<UUID, Map<UUID, List<UUID>>> unallocatedHearingRequestCaseMap,
                                                       final Map<UUID, Map<UUID, List<UUID>>> persistedUnallocatedHearingCasesMap,
                                                       final List<NonDefaultDay> nonDefaultDays,
                                                       final UUID selectedCourtRoomId,
                                                       final LocalDate weekCommencingStartDate) {
        HearingUpdateOperationType operationType = HearingUpdateOperationType.UNALLOCATED_NO_OFFENCE_CHANGE;

        if (prosecutionCases == null || CollectionUtils.isEmpty(prosecutionCases)) {
            return operationType;
        }

        final List<UUID> requestOffences = new ArrayList<>();
        final List<UUID> persistedOffences = new ArrayList<>();

        //build <case, <defendant, <offences>>> map of the persisted unallocated hearing
        persistedUnallocatedHearingCasesMap.putAll(buildPersistedCaseDefendantOffenceMap(storedHearing));

        //build <case, <defendant, <offences>>> map of the unallocated hearing in request
        unallocatedHearingRequestCaseMap.putAll(buildRequestedCaseDefendantOffenceMap(prosecutionCases, hearingId));


        unallocatedHearingRequestCaseMap.forEach((key, value) -> requestOffences.addAll(extractAllOffencesInDefendant(value)));
        persistedUnallocatedHearingCasesMap.forEach((key, value) -> persistedOffences.addAll(extractAllOffencesInDefendant(value)));

        if (Maps.difference(persistedUnallocatedHearingCasesMap, unallocatedHearingRequestCaseMap).areEqual()) {
            return checkNonDefaultDaysAndCourtRoom(nonDefaultDays, selectedCourtRoomId);
        }

        if (persistedOffences.containsAll(requestOffences) && requestOffences.size() < persistedOffences.size()) {

            if (isNull(selectedCourtRoomId) || nonNull(weekCommencingStartDate)) {
                operationType = HearingUpdateOperationType.SPLIT;
                return operationType;
            }
            operationType = CollectionUtils.isNotEmpty(nonDefaultDays) && Boolean.TRUE.equals(storedHearing.getAllocated()) ? HearingUpdateOperationType.SPLIT : HearingUpdateOperationType.PARTIAL_ALLOCATION;
            return operationType;
        }
        return operationType;
    }

    private HearingUpdateOperationType checkNonDefaultDaysAndCourtRoom(final List<NonDefaultDay> nonDefaultDays, final UUID selectedCourtRoomId) {
        HearingUpdateOperationType operationType;
        operationType = CollectionUtils.isNotEmpty(nonDefaultDays) && nonNull(selectedCourtRoomId) ? HearingUpdateOperationType.FULL_ALLOCATION : HearingUpdateOperationType.UNALLOCATED_NO_OFFENCE_CHANGE;
        return operationType;
    }

    private void removeSelectedOffences(final Map<UUID, Map<UUID, List<UUID>>> persistedUnallocatedHearingCasesMap, final List<UUID> selectedOffences) {
        persistedUnallocatedHearingCasesMap.forEach((key, value) -> value.values()
                .forEach(d -> d.removeIf(selectedOffences::contains)));
    }

    private void removeSelectedCases(final Map<UUID, Map<UUID, List<UUID>>> persistedUnallocatedHearingCasesMap, final List<UUID> selectedOffences) {
        persistedUnallocatedHearingCasesMap.entrySet()
                .removeIf(p -> selectedOffences.containsAll(extractAllOffencesInDefendant(p.getValue()))
                );
    }

    private void removeSelectedDefendants(final Map<UUID, Map<UUID, List<UUID>>> persistedUnallocatedHearingCasesMap, final List<UUID> selectedOffences) {
        persistedUnallocatedHearingCasesMap.forEach((key, value) -> value.entrySet()
                .removeIf(d -> selectedOffences.containsAll(d.getValue())
                ));
    }

    private List<UUID> extractAllOffencesInDefendant(final Map<UUID, List<UUID>> defendantMap) {
        final List<UUID> offences = new ArrayList<>();
        defendantMap.values().forEach(offences::addAll);
        return offences;
    }
}
