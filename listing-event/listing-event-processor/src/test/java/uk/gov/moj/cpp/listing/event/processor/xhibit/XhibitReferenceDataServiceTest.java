package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class XhibitReferenceDataServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(XhibitReferenceDataServiceTest.class);

    private static final String PUBLISH_COURT_LIST_REQUESTED = "listing.event.publish-court-list-requested";
    private static final String REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS = "referencedata.query.cp-xhibit-court-mappings";
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";
    private static final String REFERENCEDATA_QUERY_JUDICIARIES = "referencedata.query.judiciaries";
    private static final String REFERENCEDATA_QUERY_JUDGE = "referencedata.get.judge";
    private static final String REFERENCE_DATA_HEARING_TYPES = "referencedata.query.hearing-types";

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<Envelope> requestCaptor;

    @InjectMocks
    private XhibitReferenceDataService xhibitReferenceDataService;

    private JsonEnvelope inputEnvelope;

    @Before
    public void init() {
        inputEnvelope =
                envelopeFrom(
                        metadataWithDefaults()
                                .withName(PUBLISH_COURT_LIST_REQUESTED),
                        createObjectBuilder()
                                .add("courtCentreId", randomUUID().toString()).build());
    }

    @Test
    public void shouldGetCourtDetails() throws Exception {

        final UUID courtCentreId = randomUUID();
        final String courtId = "432";
        final String courtSiteId = "433";
        final String courtSiteName = "BLACKFRIARS";
        final String courtShortName = "BLF";
        final String courtSiteCode = "B";
        final String courtType = "MAGISTRATE";

        final JsonEnvelope responseEnvelope =
                envelopeFrom(
                        metadataWithDefaults()
                                .withName(REFERENCEDATA_QUERY_XHIBIT_COURT_MAPPINGS),
                        createObjectBuilder()
                                .add("cpXhibitCourtRoomMappings", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("crestCourtId", courtId)
                                                .add("crestCourtSiteId", courtSiteId)
                                                .add("crestCourtSiteName", courtSiteName)
                                                .add("crestCourtShortName", courtShortName)
                                                .add("crestCourtSiteCode", courtSiteCode)
                                                .add("courtType", courtType).build()))
                                .build());

        when(requester.request(any(Envelope.class))).thenReturn(responseEnvelope);

        CourtLocation courtDetails = xhibitReferenceDataService.getCourtDetails(inputEnvelope, courtCentreId);

        verify(requester).request(requestCaptor.capture());

        final JsonObject actualRequestParameters = (JsonObject) requestCaptor.getValue().payload();

        assertEquals(courtDetails.getCrestCourtId(), courtId);
        assertEquals(courtDetails.getCrestCourtSiteId(), courtSiteId);
        assertEquals(courtDetails.getCourtFullName(), courtSiteName);
        assertEquals(courtDetails.getCourtShortName(), courtShortName);
        assertEquals(courtDetails.getCourtSiteCode(), courtSiteCode);
        assertEquals(courtDetails.getCourtType(), courtType);

        assertEquals(courtCentreId.toString(), actualRequestParameters.getString("ouId"));

    }

    @Test
    public void shouldGetJudiciary() throws Exception {

        final String titlePrefix = "Mr";
        final String titleJudiciaryPrefix = "Recorder";

        final JsonEnvelope responseEnvelope =
                envelopeFrom(
                        metadataWithDefaults()
                                .withName(REFERENCEDATA_QUERY_JUDICIARIES),
                        createObjectBuilder()
                                .add("judiciaries", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("titlePrefix", titlePrefix)
                                                .add("titleJudiciaryPrefix", titleJudiciaryPrefix).build()))
                                .build());

        when(requester.request(any(Envelope.class))).thenReturn(responseEnvelope);

        final UUID judiciaryId = randomUUID();
        JsonObject judiciary = xhibitReferenceDataService.getJudiciary(inputEnvelope, judiciaryId);

        verify(requester).request(requestCaptor.capture());
        assertThat(judiciary.getString("titlePrefix"), equalTo(titlePrefix));
        assertThat(judiciary.getString("titleJudiciaryPrefix"), equalTo(titleJudiciaryPrefix));

    }

    @Test
    public void ShouldGetCourtRoomNumber() {

        final int expectedCourtRoomNumber = 432;
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();

        final JsonEnvelope responseEnvelope =
                envelopeFrom(
                        metadataWithDefaults()
                                .withName(REFERENCEDATA_QUERY_COURTROOM),
                        createObjectBuilder()
                                .add("courtrooms", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("courtroomId", expectedCourtRoomNumber)
                                                .add("id", courtRoomId.toString()).build()))
                                .build());

        when(requester.request(any(Envelope.class))).thenReturn(responseEnvelope);

        int actualCourtRoomNumber = xhibitReferenceDataService.getCourtRoomNumber(inputEnvelope, courtCentreId, courtRoomId);

        verify(requester).request(requestCaptor.capture());
        assertEquals(actualCourtRoomNumber, expectedCourtRoomNumber);

    }

    @Test
    public void ShouldGetXhibitHearingType() {

        final UUID cppHearingTypeId = randomUUID();

        final JsonEnvelope responseEnvelope =
                envelopeFrom(
                        metadataWithDefaults()
                                .withName(REFERENCE_DATA_HEARING_TYPES),
                        createObjectBuilder()
                                .add("hearingTypes", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("id", cppHearingTypeId.toString()).build()))
                                .build());

        when(requester.request(any(Envelope.class))).thenReturn(responseEnvelope);

        final JsonObject xhibitHearingType = xhibitReferenceDataService.getXhibitHearingType(inputEnvelope, cppHearingTypeId);
        LOGGER.info("xhibitHearingType = " + xhibitHearingType);

        verify(requester).request(requestCaptor.capture());
        assertThat(xhibitHearingType.getString("id"), equalTo(cppHearingTypeId.toString()));

    }

}
