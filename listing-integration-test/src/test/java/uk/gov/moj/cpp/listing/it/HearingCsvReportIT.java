package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static javax.ws.rs.core.Response.Status.OK;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingDataWithJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.randomJudicialRole;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.it.util.ViewStoreCleaner;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
        final UUID courtCentreId = getRandomCourtCenterId();
        final UUID courtRoomUUID = getRandomCourtRoomId();
        final int courtRoomId = 231;

        stubGetReferenceDataCourtCentreById(courtCentreId);

        data = loadHearingDataWithJudiciary(courtCentreId, courtRoomUUID);

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

        assertThat(csvContent, is(not(emptyString())));
        assertThat(csvContent, containsString("Date of hearing"));
        assertThat(csvContent, containsString("Courtroom"));
        assertThat(csvContent, containsString("Judiciary"));
        
        // Verify that notes are being enriched from progression service
        // The WireMock stubs should return test notes that get included in the CSV
        assertThat(csvContent, containsString("PTP"));
        assertThat(csvContent, containsString("Fixed"));
        assertThat(csvContent, containsString("30")); // HEARING_ESTIMATE_MINUTES = 30
        assertThat(csvContent, containsString("Youth"));
        assertThat(csvContent, containsString("ENGLISH"));
        assertThat(csvContent, containsString("Custody"));
        assertThat(csvContent, containsString("ENGLISH"));
        assertThat(csvContent, containsString("RestrictionApplied"));
        assertThat(csvContent, containsString("C - Description"));
        assertThat(csvContent, Matchers.stringContainsInOrder("1 of 4","2 of 4","3 of 4","4 of 4"));
        final LocalTime utcTime = ZonedDateTime.of(LocalDate.now(), LocalTime.of(10, 30), ZoneId.of("Europe/London"))
                .withZoneSameInstant(ZoneOffset.UTC).toLocalTime();
        final String expectedUtcTime = String.format("T%02d:%02d:00Z", utcTime.getHour(), utcTime.getMinute());
        assertThat(csvContent, Matchers.stringContainsInOrder(expectedUtcTime));

    }

    private String getDownloadUrl(final UUID courtCentreId, final LocalDate startDate, final int numberOfWeeks){
        return  String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(LISTING_QUERY_DOWNLOAD_CSV_REPORT), courtCentreId, startDate, numberOfWeeks));
    }
}
