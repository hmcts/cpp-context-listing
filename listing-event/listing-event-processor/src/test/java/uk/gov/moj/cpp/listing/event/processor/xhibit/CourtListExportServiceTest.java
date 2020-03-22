package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.time.ZonedDateTime.parse;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.common.xhibit.XhibitService;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListFileGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class CourtListExportServiceTest {

    @Mock
    private PublishCourtListCommandSender publishCourtListCommandSender;
    @Mock
    private CourtListMetadataGenerator courtListMetadataGenerator;
    @Mock
    private CourtListFileGenerator courtListFileGenerator;
    @Mock
    private XhibitService xhibitService;
    @Mock
    private Logger LOGGER;

    @InjectMocks
    private CourtListExportService courtListExportService;

    @Test
    public void shouldExportCourtList() throws Exception {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final LocalDate startDate = LocalDate.now();
        final String mockFileContent = "FILE_CONTENT";
        final ZonedDateTime requestedTime = parse("2018-01-02T13:04:05+00:00[UTC]");
        final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                courtListId,
                courtCentreId,
                startDate,
                startDate.plusDays(5),
                PublishCourtListType.WARN,
                requestedTime
        );

        final CourtListMetadata courtListMetadata = new CourtListMetadata("TESTFILENAME",
                "UNIQUE_ID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));
        final JsonObject courtListJson = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list.json");

        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.WARN.name())
                .add("startDate", startDate.toString())
                .add("requestedTime", requestedTime.toString())
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("DUMMY")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        when(courtListMetadataGenerator.generate(any(), any())).thenReturn(courtListMetadata);
        when(courtListFileGenerator.generateXml(any(), any(), any(), eq(courtListJson))).thenReturn(mockFileContent);

        // Tested method
        courtListExportService.exportCourtList(tEnvelope, parameters, courtListJson);

        // Assertions
        verify(xhibitService).sendToXhibit(any(InputStream.class), eq(courtListMetadata.getFilename()));
        verify(publishCourtListCommandSender).recordCourtListExportSuccessful(parameters, courtListMetadata.getFilename());
    }
}
