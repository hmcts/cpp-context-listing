package uk.gov.moj.cpp.listing.event.processor;

import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.moj.cpp.listing.event.external.Defendant;
import uk.gov.moj.cpp.listing.event.external.Hearing;
import uk.gov.moj.cpp.listing.event.external.HearingConfirmed;
import uk.gov.moj.cpp.listing.event.external.Offence;

import java.util.List;
import java.util.stream.Collectors;

public class HearingConfirmedFactory {


    public HearingConfirmed create(final HearingAllocatedForListing hearingAllocated) {

        final Hearing externalHearing = new Hearing(hearingAllocated.getHearingId().toString(), hearingAllocated.getType(), hearingAllocated.getCaseId().toString(),
                hearingAllocated.getCourtCentreId().toString(), hearingAllocated.getCourtRoomId().toString(), hearingAllocated.getJudgeId().toString(),
                hearingAllocated.getHearingDays(), getDefendants(hearingAllocated.getDefendantsOffenceIds()));

        return new HearingConfirmed(hearingAllocated.getCaseId().toString(), hearingAllocated.getUrn(), externalHearing);
    }


    private List<Defendant> getDefendants(final List<DefendantOffenceIds> defendantsOffenceIds) {
        return defendantsOffenceIds.stream()
                .map(d -> new Defendant(d.getId().toString(), d.getOffenceIds().stream().map(id -> new Offence(id.toString())).collect(Collectors.toList())))
                .collect(Collectors.toList());
    }


}
