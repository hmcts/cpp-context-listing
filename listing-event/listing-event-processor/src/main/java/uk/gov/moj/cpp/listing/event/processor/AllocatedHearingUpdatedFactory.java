package uk.gov.moj.cpp.listing.event.processor;

import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.moj.cpp.listing.event.external.BaseHearing;
import uk.gov.moj.cpp.listing.event.external.HearingUpdated;

public class AllocatedHearingUpdatedFactory {


    public HearingUpdated create(final AllocatedHearingUpdatedForListing hearingAllocated) {

        final BaseHearing externalHearing =
                new BaseHearing(
                        hearingAllocated.getHearingId().toString(),
                        hearingAllocated.getType(),
                        hearingAllocated.getCourtRoomId().toString(),
                        hearingAllocated.getJudgeId().toString(),
                        hearingAllocated.getHearingDays(),
                        hearingAllocated.getCourtCentreId().toString());

        return new HearingUpdated(externalHearing);
    }

}
