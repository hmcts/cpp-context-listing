package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

import uk.gov.justice.listing.events.CaseRemovedFromGroupCases;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;


import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class CaseRemovedFromGroupCasesEventListener implements ListenerPort {

    private static final String NUMBER_OF_GROUP_CASES = "numberOfGroupCases";
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private final HearingRepository hearingRepository;
    private final HearingSearchSyncService hearingSearchSyncService;

    public CaseRemovedFromGroupCasesEventListener(final JsonObjectToObjectConverter jsonObjectToObjectConverter, final HearingRepository hearingRepository, final HearingSearchSyncService hearingSearchSyncService) {
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.hearingRepository = hearingRepository;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .onJson(CaseRemovedFromGroupCases.class, this::caseRemovedFromGroupCases);
    }

    public void caseRemovedFromGroupCases(final Envelope envelope) {
        final CaseRemovedFromGroupCases caseRemovedFromGroupCases =
                jsonObjectToObjectConverter.convert(envelope.payload(), CaseRemovedFromGroupCases.class);

        final Hearing hearing = hearingRepository.findBy(caseRemovedFromGroupCases.getHearingId());
        final ListedCase removedCase = caseRemovedFromGroupCases.getRemovedCase();
        final ListedCase newGroupMasterCase = nonNull(caseRemovedFromGroupCases.getNewGroupMaster()) ? caseRemovedFromGroupCases.getNewGroupMaster() : null;

        updateNumberOfGroupCases(hearing, caseRemovedFromGroupCases.getNumberOfGroupCases());

        hearingSearchSyncService.syncEntity(hearing, nonNull(newGroupMasterCase) ?
                asList(removedCase, newGroupMasterCase) : asList(removedCase));
    }

    private void updateNumberOfGroupCases(final Hearing hearing, final Integer numberOfGroupCases) {
        if (nonNull(numberOfGroupCases)) {
            JsonNodeUpdater.createJsonNodeUpdater((ObjectNode) hearing.getProperties(), hearing::setProperties)
                    .put(NUMBER_OF_GROUP_CASES, numberOfGroupCases)
                    .save();
        }
    }
}