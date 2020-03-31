package uk.gov.moj.cpp.listing.event.processor.xhibit;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;

import javax.inject.Inject;

public class XhibitReferenceDataService extends CommonXhibitReferenceDataService {

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Override
    public Requester getRequester() {
        return requester;
    }

}
