package uk.gov.moj.cpp.listing.event.listener;

import static com.google.common.collect.Iterables.find;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.NewCaseMarkerUpdated;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;

@ServiceComponent(Component.EVENT_LISTENER)
public class CaseMarkersEventListener {

    private static final String LISTED_CASES_FIELD = "listedCases";

    private static final TypeReference<List<ListedCase>> LISTED_CASE_TYPE = new TypeReference<List<ListedCase>>() {
    };

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingSearchSyncService hearingSearchSyncService;

    @Handles("listing.events.new-case-marker-updated")
    public void handleCaseMarkersUpdated(final Envelope<NewCaseMarkerUpdated> event) {
        final NewCaseMarkerUpdated payload = event.payload();
        final UUID hearingId = payload.getHearingId();
        final UUID prosecutionCaseId = payload.getCaseId();
        final List<Marker> caseMarkers = payload.getCaseMarkers();

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, LISTED_CASE_TYPE, getCaseMarkersAddFunction(prosecutionCaseId, caseMarkers))
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    private Function<List<ListedCase>, List<ListedCase>> getCaseMarkersAddFunction(final UUID caseId, final List<Marker> caseMarkers) {
        return cases -> getUpdatedListedCase(caseId, caseMarkers, cases);
    }

    private List<ListedCase> getUpdatedListedCase(final UUID caseId,
                                                  final List<Marker> updatedCaseMarkers,
                                                  final List<ListedCase> cases) {
        final List<ListedCase> listedCases = new ArrayList<>(cases);
        final ListedCase listedCase = find(listedCases, caze -> caze.getId().equals(caseId));
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
