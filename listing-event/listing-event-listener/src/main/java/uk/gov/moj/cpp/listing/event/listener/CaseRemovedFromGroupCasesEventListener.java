package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.listing.events.CaseRemovedFromGroupCases;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.node.ObjectNode;

@ServiceComponent(EVENT_LISTENER)
public class CaseRemovedFromGroupCasesEventListener {

    private static final String NUMBER_OF_GROUP_CASES = "numberOfGroupCases";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingSearchSyncService hearingSearchSyncService;

    @Transactional
    @Handles("listing.events.case-removed-from-group-cases")
    public void caseRemovedFromGroupCases(final JsonEnvelope envelope) {
        final CaseRemovedFromGroupCases caseRemovedFromGroupCases =
                jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), CaseRemovedFromGroupCases.class);

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