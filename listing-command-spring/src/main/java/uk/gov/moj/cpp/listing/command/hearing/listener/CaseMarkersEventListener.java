package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static com.google.common.collect.Iterables.find;
import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.NewCaseMarkerUpdated;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;


import com.fasterxml.jackson.core.type.TypeReference;

@Component
public class CaseMarkersEventListener implements ListenerPort {

    private static final String LISTED_CASES_FIELD = "listedCases";

    private static final TypeReference<List<ListedCase>> LISTED_CASE_TYPE = new TypeReference<List<ListedCase>>() {
    };
    private final HearingRepository hearingRepository;
    private final HearingSearchSyncService hearingSearchSyncService;

    public CaseMarkersEventListener(final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(NewCaseMarkerUpdated.class, this::handleCaseMarkersUpdated);
    }

    public void handleCaseMarkersUpdated(final RaisedEvent<NewCaseMarkerUpdated> event) {
        final NewCaseMarkerUpdated payload = event.payload();
        final UUID hearingId = payload.getHearingId();
        final UUID prosecutionCaseId = payload.getCaseId();
        final List<Marker> caseMarkers = payload.getCaseMarkers();

        if(Objects.nonNull(hearingRepository.findBy(hearingId))) {
            using(hearingRepository)
                    .find(hearingId)
                    .putSubList(LISTED_CASES_FIELD, LISTED_CASE_TYPE, getCaseMarkersAddFunction(prosecutionCaseId, caseMarkers))
                    .save();

            hearingSearchSyncService.sync(hearingId);
        }
    }

    private Function<List<ListedCase>, List<ListedCase>> getCaseMarkersAddFunction(final UUID caseId, final List<Marker> caseMarkers) {
        return cases -> getUpdatedListedCase(caseId, caseMarkers, cases);
    }

    private List<ListedCase> getUpdatedListedCase(final UUID caseId,
                                                  final List<Marker> updatedCaseMarkers,
                                                  final List<ListedCase> cases) {
        final List<ListedCase> listedCases = new ArrayList<>(cases);
        final ListedCase listedCase = find(listedCases, caze -> caze.getId().equals(caseId), null);
        if(isNull(listedCase)){
            return listedCases;
        }
        final List<Marker> markers = listedCase.getMarkers();
        if(null != markers) {
            markers.clear();
            markers.addAll(updatedCaseMarkers);
        }
        else {
            final ListedCase newListedCase = ListedCase.listedCase()
                    .withCaseIdentifier(listedCase.getCaseIdentifier())
                    .withIsCivil(listedCase.getIsCivil())
                    .withGroupId(listedCase.getGroupId())
                    .withIsGroupMember(listedCase.getIsGroupMember())
                    .withIsGroupMaster(listedCase.getIsGroupMaster())
                    .withDefendants(listedCase.getDefendants())
                    .withId(listedCase.getId())
                    .withIsEjected(listedCase.getIsEjected())
                    .withRestrictFromCourtList(listedCase.getRestrictFromCourtList())
                    .withMarkers(updatedCaseMarkers)
                    .withShadowListed(listedCase.getShadowListed())
                    .build();
            listedCases.remove(listedCase);
            listedCases.add(newListedCase);
        }

        return listedCases;
    }

}
