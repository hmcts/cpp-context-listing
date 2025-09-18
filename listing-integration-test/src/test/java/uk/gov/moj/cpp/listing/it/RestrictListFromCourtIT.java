package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.buildPublishCourtListCommandPayload;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingDataWithJudiciary;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;
import static uk.gov.moj.cpp.listing.utils.SystemIdMapperStub.stubIdMapperReturningExistingAssociation;

import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.it.util.ViewStoreCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.PublishCourtListSteps;
import uk.gov.moj.cpp.listing.steps.RestrictCourtListSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


@SuppressWarnings({"squid:UnusedPrivateMethod", "squid:S1607"})
class RestrictListFromCourtIT extends AbstractIT {

    @Test
    void shouldRestrictListingCaseFromCourtForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(false);

        final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData);
        restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getRestrictListingFromCourtData(hearingsData));
        restrictCourtListSteps.verifyCaseOrDefendantOrOffenceListingRestrictedInHearing(true, true, false);
    }

    @Test
    void shouldUnRestrictDefendantsAndOffencesFromListingCaseForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(false);

        final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData);
        restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getDefendantsAndOffencesDataToBeUnrestricted(hearingsData));
        restrictCourtListSteps.verifyCaseOrDefendantOrOffenceListingRestrictedInHearing(false, false, false);
    }

    @Test
    void shouldRestrictDefendantsAndOffencesFromListingCaseForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(false);

        final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData);
        restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getDefendantsAndOffencesDataToBeRestricted(hearingsData));
        restrictCourtListSteps.verifyCaseOrDefendantOrOffenceListingRestrictedInHearing(false, true, true);
    }

    @Test
    void shouldRestrictCourtApplicationFromCourtForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData);
        restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getCourtApplicationDataToBeRestricted(hearingsData));
        restrictCourtListSteps.verifyCourtApplicationOrApplicantOrRespondentListingRestrictedInHearing(true, true, false, false);
    }

    @Test
    void shouldRestrictCourtApplicationTypeFromCourtForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData);
        restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getCourtApplicationTypeToBeRestricted(hearingsData));
        restrictCourtListSteps.verifyCourtApplicationOrApplicantOrRespondentListingRestrictedInHearing(false, false, false, true);
    }

    @Test
    void shouldPublishCourtListWithHearingsWithDefendantNameMasking() throws Exception {
        final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
        viewStoreCleaner.cleanViewStoreTables();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");

        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        stubGetReferenceDataCourtCentreById(courtCentreId);

        final HearingsData hearingsData = loadHearingDataWithJudiciary(courtCentreId, courtRoomUUID);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, LocalTime.of(10, 30), "6:30", null, STRING.next()));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData);
        restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getDefendantsAndOffencesDataToBeRestricted(hearingsData));
        restrictCourtListSteps.verifyListingRestrictedInHearing(false, true, true);

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus("EXPORT_SUCCESSFUL", "true");
        publishCourtListSteps.verifyDefendantNameIsMasked();
    }
}
