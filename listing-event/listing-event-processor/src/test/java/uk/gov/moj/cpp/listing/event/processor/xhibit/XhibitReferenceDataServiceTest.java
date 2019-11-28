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
    public void shouldGetJudge() throws Exception {

        String title = "Mr";
        String firstName = "James";
        String lastName = "May";

        final JsonEnvelope responseEnvelope =
                envelopeFrom(
                        metadataWithDefaults()
                                .withName(REFERENCEDATA_QUERY_JUDGE),
                        createObjectBuilder()
                                .add("title", title)
                                .add("firstName", firstName)
                                .add("lastName", lastName)
                                .build());

        when(requester.request(any(Envelope.class))).thenReturn(responseEnvelope);

        final UUID judgeId = randomUUID();
        JsonObject judge = xhibitReferenceDataService.getJudge(inputEnvelope, judgeId);

        verify(requester).request(requestCaptor.capture());
        assertThat(judge.getString("title"), equalTo(title));
        assertThat(judge.getString("firstName"), equalTo(firstName));
        assertThat(judge.getString("lastName"), equalTo(lastName));

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
                                                .add("courtroomId", Integer.toString(expectedCourtRoomNumber))
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
