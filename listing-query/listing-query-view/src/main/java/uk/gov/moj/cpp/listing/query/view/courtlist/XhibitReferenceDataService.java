package uk.gov.moj.cpp.listing.query.view.courtlist;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;

import javax.inject.Inject;

public class XhibitReferenceDataService extends CommonXhibitReferenceDataService {

    @Inject
    @ServiceComponent(Component.QUERY_VIEW)
    private Requester requester;

    @Override
    public Requester getRequester() {
        return requester;
    }

}
