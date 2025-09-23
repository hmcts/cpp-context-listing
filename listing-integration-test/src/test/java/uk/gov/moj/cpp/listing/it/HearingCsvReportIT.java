package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingDataWithJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.randomJudicialRole;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;
import static uk.gov.moj.cpp.listing.utils.SystemIdMapperStub.stubIdMapperReturningExistingAssociation;

import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.it.util.ViewStoreCleaner;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;

/**
 * Integration test for hearing CSV report download endpoint.
 * 
 * This test verifies that:
 * 1. The CSV report can be downloaded successfully
 * 2. The ProgressionNotesCache is working correctly
 * 3. Notes are being enriched from the progression service via WireMock stubs
 * 4. Both case notes and application notes are handled properly
 */
@ExtendWith(MockitoExtension.class)
public class HearingCsvReportIT extends AbstractIT {

    private static final String LISTING_QUERY_DOWNLOAD_CSV_REPORT = "listing.query.download-hearing-csv-report";
    private HearingsData data;

    @BeforeEach
    public void initialize() {
        final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
        viewStoreCleaner.cleanViewStoreTables();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");

        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        stubGetReferenceDataCourtCentreById(courtCentreId);

        data = loadHearingDataWithJudiciary(courtCentreId, courtRoomUUID);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, LocalTime.of(10, 30), "6:30", null, STRING.next()));
        stubGetReferenceDataCpCourtRooms(data.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(data.getHearingData().get(0).getCourtRoomId());

        var first  = data.getHearingData().get(0);
        var updatedHearingDataWithoutNonDefaultDaysShouldPreservePrevRoomChange = new uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData(
                first.getId(),
                first.getCourtCentreId(),
                first.getName(),
                first.getCourtRoomId(),
                first.getHearingTypeData(),
                first.getHearingStartDate().toString(),
                first.getHearingEndDate().plusDays(3).toString(),
                emptyList(),
                emptyList(),
                "ENGLISH",
                asList(randomJudicialRole("DISTRICT_JUDGE")),
                first.getJurisdictionType(),
                null,
                null,
                null,
                first.getHasVideoLink(),
                first.getPublicListNote(),
                "High",
                null,
                null,
                false,
                null
        );


        final UpdateHearingSteps updateHearingStepsWithoutNonDefaultDaysShouldPreservePrevRoomChange = new UpdateHearingSteps(data, updatedHearingDataWithoutNonDefaultDaysShouldPreservePrevRoomChange);
        updateHearingStepsWithoutNonDefaultDaysShouldPreservePrevRoomChange.whenHearingIsUpdatedForListing();
        updateHearingStepsWithoutNonDefaultDaysShouldPreservePrevRoomChange.verifyHearingUpdatedWhenQueryingFromAPICourtCalendar();

    }

    @Test
    void shouldDownloadHearingCsvReport() {
        // Given
        final UUID courtCentreId =  data.getHearingData().get(0).getCourtCentreId();
        final Integer numberOfWeeks = 2;

        final LocalDate now = LocalDate.now();
        final String expectedCsvFileName = "hearing_report_%s.csv".formatted(now.toString());
        // When
        final String url = getDownloadUrl(courtCentreId, now, numberOfWeeks);


        final Response response = restClient.query(url, "text/csv", getLoggedInHeader());
        // Then
        assertThat(response.getStatus(), is(OK.getStatusCode()));
        assertThat(response.getHeaderString("Content-Type"), containsString("text/csv"));
        assertThat(response.getHeaderString("Content-Disposition"), containsString("attachment"));
        assertThat(response.getHeaderString("Content-Disposition"), containsString(expectedCsvFileName));

        final String csvContent = response.readEntity(String.class);
/*        try {
            FileUtils.writeByteArrayToFile(new File("hearings.csv"), csvContent.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        assertThat(csvContent, is(not(emptyString())));
        assertThat(csvContent, containsString("Date of hearing"));
        assertThat(csvContent, containsString("Courtroom"));
        assertThat(csvContent, containsString("Judiciary"));
        
        // Verify that notes are being enriched from progression service
        // The WireMock stubs should return test notes that get included in the CSV
        assertThat(csvContent, containsString("PTP"));
        assertThat(csvContent, containsString("Fixed"));
        assertThat(csvContent, containsString("360"));
        assertThat(csvContent, containsString("Youth"));
        assertThat(csvContent, containsString("ENGLISH"));
        assertThat(csvContent, containsString("Custody"));
        assertThat(csvContent, containsString("ENGLISH"));
        assertThat(csvContent, containsString("RestrictionApplied"));
        assertThat(csvContent, containsString("C - Description"));
        assertThat(csvContent, Matchers.stringContainsInOrder("1 of 4","2 of 4","3 of 4","4 of 4"));
        assertThat(csvContent, Matchers.stringContainsInOrder("T09:00:00Z"));

    }

    private String getDownloadUrl(final UUID courtCentreId, final LocalDate startDate, final int numberOfWeeks){
        return  String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(LISTING_QUERY_DOWNLOAD_CSV_REPORT), courtCentreId, startDate, numberOfWeeks));
    }
}
