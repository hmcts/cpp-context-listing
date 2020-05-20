package uk.gov.moj.cpp.listing.it;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.progression.courts.JurisdictionType;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.io.IOException;
import java.util.UUID;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class ExtendHearingIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListCourtHearingSteps.class);

    @Test
    public void shouldExtendHearingForCase() throws IOException {

        final UUID allocatedHearingId = UUID.randomUUID();
        final UUID unAllocatedHearingId = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();
        final String caseUrn = STRING.next();
        final String jurisdictionType = JurisdictionType.CROWN.name();

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(allocatedHearingId, null, caseUrn, masterDefendantId, null, jurisdictionType, jurisdictionType);

        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        try (ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsData(unAllocatedHearingId))) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();

            LOGGER.info("UnAllocated HearingID : {}  -  Allocated HearingId : {} ", unAllocatedHearingId, allocatedHearingId );

            listCourtHearingSteps.verifyHearingIsCreated(allocatedHearingId,1);
            listCourtHearingSteps.extendHearing(unAllocatedHearingId, allocatedHearingId);
            listCourtHearingSteps.verifyHearingConfirmedEventForExtendHearingPublicMQ(allocatedHearingId, unAllocatedHearingId);
            listCourtHearingSteps.verifyHearingUpdatedToCaseInActiveMQ(allocatedHearingId, unAllocatedHearingId);
            listCourtHearingSteps.verifyHearingDeletedInActiveMQ(unAllocatedHearingId);
        }
    }
}
