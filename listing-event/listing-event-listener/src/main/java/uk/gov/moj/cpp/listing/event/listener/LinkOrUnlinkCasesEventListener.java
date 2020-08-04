package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.LinkedCases;
import uk.gov.justice.listing.events.LinkedCasesUpdated;
import uk.gov.justice.listing.events.LinkedToCases;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;

@ServiceComponent(Component.EVENT_LISTENER)
public class LinkOrUnlinkCasesEventListener {

    private static final String LISTED_CASES_FIELD = "listedCases";

    private static final TypeReference<List<ListedCase>> LISTED_CASE_TYPE = new TypeReference<List<ListedCase>>() {
    };
    public static final String LINK = "LINK";
    public static final String UNLINK = "UNLINK";

    @Inject
    private HearingRepository hearingRepository;

    @Handles("listing.events.linked-cases-updated")
    public void handleLinkedCasesUpdated(final Envelope<LinkedCasesUpdated> event) {
        final LinkedCasesUpdated payload = event.payload();
        final UUID hearingId = payload.getHearingId();
        final UUID prosecutionCaseId = payload.getCaseId();
        final List<LinkedToCases> linkedToCases = payload.getLinkedToCases();
        final String linkActionType = payload.getLinkActionType();

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, LISTED_CASE_TYPE, getLinkedToCasesAddFunction(linkActionType, prosecutionCaseId, linkedToCases))
                .save();
    }

    private Function<List<ListedCase>, List<ListedCase>> getLinkedToCasesAddFunction(final String linkActionType, final UUID caseId, final List<LinkedToCases> linkedToCases) {
        return cases -> getUpdatedListedCase(linkActionType, caseId, linkedToCases, cases);
    }

    private List<ListedCase> getUpdatedListedCase(final String linkActionType, final UUID caseId, final List<LinkedToCases> linkedToCases, final List<ListedCase> cases) {
        final List<ListedCase> listedCases = new ArrayList<>(cases);
        final Optional<ListedCase> optListedCase = listedCases.stream().filter(caze -> caze.getId().equals(caseId)).findFirst();
        if (!optListedCase.isPresent()){
            return listedCases;
        }

        final ListedCase listedCase = optListedCase.get();

        List<LinkedCases> linkedCases = listedCase.getLinkedCases();
        if(linkedCases == null){
            linkedCases = new ArrayList<>();
        }

        final List<LinkedCases> newLinkedCases;

        switch (linkActionType) {
            case LINK:
                newLinkedCases = addLinkedCases(linkedCases, linkedToCases);
                break;
            case UNLINK:
                newLinkedCases = removeUnlinkedCases(linkedCases, linkedToCases);
                break;
            default:
                throw new IllegalArgumentException("Unsupported linkActionType:" + linkActionType);
        }

        final ListedCase newListedCase = rebuildListedCase(listedCase, newLinkedCases);
        listedCases.remove(listedCase);
        listedCases.add(newListedCase);
        return listedCases;
    }

    private ListedCase rebuildListedCase(final ListedCase listedCase, final List<LinkedCases> linkedCases){
        return ListedCase.listedCase()
                .withCaseIdentifier(listedCase.getCaseIdentifier())
                .withDefendants(listedCase.getDefendants())
                .withId(listedCase.getId())
                .withIsEjected(listedCase.getIsEjected())
                .withRestrictFromCourtList(listedCase.getRestrictFromCourtList())
                .withMarkers(listedCase.getMarkers())
                .withLinkedCases(linkedCases)
                .withShadowListed(listedCase.getShadowListed())
                .build();
    }

    private List<LinkedCases> addLinkedCases(final List<LinkedCases> linkedCases, final List<LinkedToCases> linkedToCases){
        final List<LinkedCases> newLinkedCases = new ArrayList<>();
        newLinkedCases.addAll(linkedCases);
        linkedToCases.stream().forEach(
                ltc -> {
                    final LinkedCases newLinkedCase = getLinkedCases(ltc);
                    if (!newLinkedCases.contains(newLinkedCase)){
                        newLinkedCases.add(newLinkedCase);
                    }
                }
        );
        return newLinkedCases;
    }

    private List<LinkedCases> removeUnlinkedCases(final List<LinkedCases> linkedCases, final List<LinkedToCases> unlinkedFromCases){
        final List<LinkedCases> newLinkedCases = new ArrayList<>();
        for (final LinkedCases lc : linkedCases) {
            boolean toBeRemoved = false;
            for (final LinkedToCases ltc : unlinkedFromCases) {
                if (ltc.getCaseId().equals(lc.getCaseId())) {
                    toBeRemoved = true;
                }
            }
            if (!toBeRemoved) {
                newLinkedCases.add(lc);
            }
        }
        return newLinkedCases;
    }

    private LinkedCases getLinkedCases(LinkedToCases linkedToCases){
        return LinkedCases.linkedCases()
                .withCaseId(linkedToCases.getCaseId())
                .withCaseUrn(linkedToCases.getCaseUrn())
                .build();
    }
}
