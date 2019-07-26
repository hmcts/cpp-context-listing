package uk.gov.moj.transformreport;

import uk.gov.justice.v24.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.v24.listing.events.CourtCentreChangedForHearing;
import uk.gov.justice.v24.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.v24.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.v24.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.v24.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.v24.listing.events.EndDateChangedForHearing;
import uk.gov.justice.v24.listing.events.EndDateRemovedFromHearing;
import uk.gov.justice.v24.listing.events.HearingAddedToCase;
import uk.gov.justice.v24.listing.events.HearingAllocatedForListing;
import uk.gov.justice.v24.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.v24.listing.events.HearingDaysSequenced;
import uk.gov.justice.v24.listing.events.HearingLanguageChangedForHearing;
import uk.gov.justice.v24.listing.events.HearingListed;
import uk.gov.justice.v24.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.v24.listing.events.JudiciaryAssignedToHearing;
import uk.gov.justice.v24.listing.events.JudiciaryChangedForHearing;
import uk.gov.justice.v24.listing.events.JudiciaryRemovedFromHearing;
import uk.gov.justice.v24.listing.events.JurisdictionChangedForHearing;
import uk.gov.justice.v24.listing.events.NewDefendantDetailsUpdated;
import uk.gov.justice.v24.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.v24.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.v24.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.v24.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.justice.v24.listing.events.OffenceAdded;
import uk.gov.justice.v24.listing.events.OffenceDeleted;
import uk.gov.justice.v24.listing.events.OffenceUpdated;
import uk.gov.justice.v24.listing.events.OffencesToBeAdded;
import uk.gov.justice.v24.listing.events.OffencesToBeDeleted;
import uk.gov.justice.v24.listing.events.OffencesToBeUpdated;
import uk.gov.justice.v24.listing.events.ReportingRestrictionReasonChangedForHearing;
import uk.gov.justice.v24.listing.events.SequencesResetOnHearingDays;
import uk.gov.justice.v24.listing.events.StartDateChangedForHearing;
import uk.gov.justice.v24.listing.events.TypeChangedForHearing;
import uk.gov.moj.cpp.coredomain.tools.transform.SchemaExploreKt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */

public class Compare {
    public static void main(String args[]) {
        final Map<Class<?>, Class<?>> roots = new LinkedHashMap<>();

        // No transformation
        roots.put(DefendantsToBeUpdated.class,  uk.gov.justice.listing.events.DefendantsToBeUpdated.class);// didn't generge 2.5 pojo
        roots.put(HearingListed.class,  uk.gov.justice.listing.events.HearingListed.class);
        roots.put(AllocatedHearingUpdatedForListing.class,  uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing.class);
        roots.put(CourtCentreChangedForHearing.class,  uk.gov.justice.listing.events.CourtCentreChangedForHearing.class);
        roots.put(CourtRoomAssignedToHearing.class,  uk.gov.justice.listing.events.CourtRoomAssignedToHearing.class);
        roots.put(CourtRoomChangedForHearing.class,  uk.gov.justice.listing.events.CourtRoomChangedForHearing.class);
        roots.put(CourtRoomRemovedFromHearing.class,  uk.gov.justice.listing.events.CourtRoomRemovedFromHearing.class);
        roots.put(EndDateChangedForHearing.class,  uk.gov.justice.listing.events.EndDateChangedForHearing.class);
        roots.put(EndDateRemovedFromHearing.class,  uk.gov.justice.listing.events.EndDateRemovedFromHearing.class);
        roots.put(HearingAddedToCase.class,  uk.gov.justice.listing.events.HearingAddedToCase.class);
        roots.put(HearingAllocatedForListing.class,  uk.gov.justice.listing.events.HearingAllocatedForListing.class);
        roots.put(HearingDaysChangedForHearing.class,  uk.gov.justice.listing.events.HearingDaysChangedForHearing.class);
        roots.put(HearingDaysSequenced.class,  uk.gov.justice.listing.events.HearingDaysSequenced.class);
        roots.put(HearingLanguageChangedForHearing.class,  uk.gov.justice.listing.events.HearingLanguageChangedForHearing.class);
        roots.put(HearingUnallocatedForListing.class,  uk.gov.justice.listing.events.HearingUnallocatedForListing.class);
        roots.put(JudiciaryAssignedToHearing.class,  uk.gov.justice.listing.events.JudiciaryAssignedToHearing.class);
        roots.put(JudiciaryChangedForHearing.class,  uk.gov.justice.listing.events.JudiciaryChangedForHearing.class);
        roots.put(JurisdictionChangedForHearing.class,  uk.gov.justice.listing.events.JurisdictionChangedForHearing.class);
        roots.put(JudiciaryRemovedFromHearing.class,  uk.gov.justice.listing.events.JudiciaryRemovedFromHearing.class);
        roots.put(NewDefendantDetailsUpdated.class,  uk.gov.justice.listing.events.NewDefendantDetailsUpdated.class);
        roots.put(NonDefaultDaysAssignedToHearing.class,  uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing.class);
        roots.put(NonDefaultDaysChangedForHearing.class,  uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing.class);
        roots.put(NonSittingDaysAssignedToHearing.class,  uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing.class);
        roots.put(NonSittingDaysChangedForHearing.class,  uk.gov.justice.listing.events.NonSittingDaysChangedForHearing.class);
        roots.put(OffenceDeleted.class,  uk.gov.justice.listing.events.OffenceDeleted.class);
        roots.put(OffencesToBeDeleted.class,  uk.gov.justice.listing.events.OffencesToBeDeleted.class);
        roots.put(ReportingRestrictionReasonChangedForHearing.class,  uk.gov.justice.listing.events.ReportingRestrictionReasonChangedForHearing.class);
        roots.put(SequencesResetOnHearingDays.class,  uk.gov.justice.listing.events.SequencesResetOnHearingDays.class);
        roots.put(StartDateChangedForHearing.class,  uk.gov.justice.listing.events.StartDateChangedForHearing.class);
        roots.put(TypeChangedForHearing.class,  uk.gov.justice.listing.events.TypeChangedForHearing.class);
        roots.put(OffencesToBeUpdated.class,  uk.gov.justice.listing.events.OffencesToBeUpdated.class);
        roots.put(OffenceAdded.class,  uk.gov.justice.listing.events.OffenceAdded.class);
        roots.put(OffenceUpdated.class,  uk.gov.justice.listing.events.OffenceUpdated.class);
        roots.put(OffencesToBeAdded.class,  uk.gov.justice.listing.events.OffencesToBeAdded.class);

        // transformation required

        SchemaExploreKt.exploreParralel(roots,
                c -> c.getName().contains("uk.gov.justice")
        );
    }
}
