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

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class CaseRemovedFromGroupCasesEventListener {

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

        hearingSearchSyncService.syncEntity(hearing, nonNull(newGroupMasterCase) ?
                asList(removedCase, newGroupMasterCase) : asList(removedCase));
    }
}