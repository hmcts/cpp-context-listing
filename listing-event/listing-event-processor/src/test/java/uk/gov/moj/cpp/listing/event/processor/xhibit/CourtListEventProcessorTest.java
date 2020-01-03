package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.lang.String.format;
import static java.time.ZonedDateTime.parse;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.common.xhibit.ExportFailedException;
import uk.gov.moj.cpp.listing.common.xhibit.XhibitService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListFileGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.ListingService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersParser;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class CourtListEventProcessorTest {
    private static final String PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED = "listing.event.publish-court-list-produced";
    @Spy
    @InjectMocks
    CourtListEventProcessor courtListEventProcessor;
    @Mock
    private Logger LOGGER;
    @Mock
    private XhibitService xhibitService;
    @Mock
    private PublishCourtListCommandSender publishCourtListCommandSender;
    @Mock
    private PublishCourtListRequestParametersParser publishCourtListRequestParametersParser;
    @Mock
    private CourtListMetadataGenerator courtListMetadataGenerator;
    @Mock
    private CourtListFileGenerator courtListFileGenerator;
    @Mock
    private FileServiceClient fileServiceClient;
    @Mock
    private ListingService listingService;
    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Before
    public void before() {
        setField(this.jsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandlePublishCourtListRequested() throws Exception {

        // Mocked values
        final JsonEnvelope tEnvelope = mock(JsonEnvelope.class);
        final UUID generatedfileId = randomUUID();
        final String mockFileContent = "FILE_CONTENT";
        final PublishCourtListRequestParameters parameters = mock(PublishCourtListRequestParameters.class);
        final CourtListMetadata courtListMetadata = new CourtListMetadata("TESTFILENAME",
                "UNIQUE_ID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));
        final JsonObject courtListData = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list.json");

        when(publishCourtListRequestParametersParser.parse(tEnvelope)).thenReturn(parameters);
        when(courtListMetadataGenerator.generate(tEnvelope, parameters)).thenReturn(courtListMetadata);
        when(courtListFileGenerator.generateXml(tEnvelope, parameters, courtListMetadata)).thenReturn(mockFileContent);
        when(fileServiceClient.store(courtListMetadata, mockFileContent)).thenReturn(generatedfileId);
        when(listingService.getUnpublishedCourtListForCourtCentre(tEnvelope, parameters)).thenReturn(courtListData);

        // Tested method
        courtListEventProcessor.handlePublishCourtListRequested(tEnvelope);

        // Assertions
        assertCourtListPublished(parameters, tEnvelope, courtListData);
        assertCourtListExported(generatedfileId, mockFileContent, parameters, courtListMetadata); // TODO SCSL-280 Remove since export will be triggered by a scheduled task.
    }

    private void assertCourtListPublished(final PublishCourtListRequestParameters parameters,
                                          final JsonEnvelope tEnvelope,
                                          final JsonObject courtListData) {

        verify(listingService).getUnpublishedCourtListForCourtCentre(tEnvelope, parameters);
        verify(publishCourtListCommandSender).storePublishedCourtList(parameters, courtListData);
    }

    private void assertCourtListExported(final UUID generatedfileId,
                                         final String mockFileContent,
                                         final PublishCourtListRequestParameters parameters,
                                         final CourtListMetadata courtListMetadata) throws FileServiceException {
        verify(fileServiceClient).store(courtListMetadata, mockFileContent);
        verify(fileServiceClient).store(courtListMetadata, mockFileContent);
        verify(publishCourtListCommandSender).recordCourtListProduced(parameters, generatedfileId, courtListMetadata.getFilename());
    }

    @Test
    public void handleProducedCourtListWhenExportSuccessful() throws ExportFailedException {
        final UUID publishCourtListRequestId = randomUUID();
        final UUID fileId = randomUUID();
        final String fileName = "fileName";
        final JsonObject courtListProducedEvent = createObjectBuilder()
                .add("publishCourtListRequestId", publishCourtListRequestId.toString())
                .add("courtListFileId", fileId.toString())
                .add("courtListFileName", fileName).build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED)
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListProducedEvent);

        courtListEventProcessor.handleProducedCourtList(tEnvelope);

        verify(xhibitService).sendToXhibit(fileId, fileName);
        verify(publishCourtListCommandSender).recordCourtListExportSuccessful(publishCourtListRequestId);
    }

    @Test
    public void handleProducedCourtListWhenExportFailed() throws ExportFailedException {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED)
                .withUserId(randomUUID().toString()).build();
        final UUID publishCourtListRequestId = randomUUID();
        final UUID fileId = randomUUID();
        final String fileName = "fileName";

        ExportFailedException2 expectedException = new ExportFailedException2();
        doThrow(expectedException).when(xhibitService).sendToXhibit(fileId, fileName);
        final JsonObject courtListProducedEvent = createObjectBuilder()
                .add("publishCourtListRequestId", publishCourtListRequestId.toString())
                .add("courtListFileId", fileId.toString())
                .add("courtListFileName", fileName)
                .build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListProducedEvent);
        courtListEventProcessor.handleProducedCourtList(tEnvelope);
        verify(xhibitService).sendToXhibit(fileId, fileName);
        verify(publishCourtListCommandSender).recordCourtListExportFailed(publishCourtListRequestId,
                "Not reachable");
        verify(LOGGER).error(format("Export failed for %s %s %s", fileId, fileName, "Not reachable"), expectedException);
    }

    private class ExportFailedException2 extends ExportFailedException {
        ExportFailedException2() {
            super("Not reachable", new Throwable());
        }
    }
}
