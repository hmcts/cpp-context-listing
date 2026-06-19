package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.multipart.FileInputDetailsFactory;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilder;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.parameter.ParameterType;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Adapter(Component.QUERY_API)
public class DefaultQueryApiHearingsDownloadHearingCsvReportResource implements QueryApiHearingsDownloadHearingCsvReportResource {
  private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DefaultQueryApiHearingsDownloadHearingCsvReportResource.class);

  private static final String HEARING_CSV_QUERY_NAME = "listing.query.download-hearing-csv-report";
  private static final String CSV_FILE_NAME_PREFIX = "hearing_report";
  private static final String CSV_FILE_EXTENSION = ".csv";
  private static final String CSV_MIME_TYPE = "text/csv";


  @Inject
  RestProcessor restProcessor;

  @Inject
  @Named("DefaultQueryApiHearingsDownloadHearingCsvReportResourceActionMapper")
  ActionMapper actionMapper;

  @Inject
  InterceptorChainProcessor interceptorChainProcessor;

  @Context
  HttpHeaders headers;

  @Inject
  FileInputDetailsFactory fileInputDetailsFactory;

  @Inject
  ParameterCollectionBuilderFactory validParameterCollectionBuilderFactory;

  @Inject
  TraceLogger traceLogger;

  @Inject
  private HearingQueryView hearingQueryView;

  @Inject
  HttpTraceLoggerHelper httpTraceLoggerHelper;

  @Override
  public Response getHearingsDownloadHearingCsvReport(final String courtCentreId, final String startDate, final String numberOfWeeks, final UUID userId) {
    final ParameterCollectionBuilder validParameterCollectionBuilder = validParameterCollectionBuilderFactory.create();
    traceLogger.trace(LOGGER, () -> String.format("Received REST request with headers: %s", httpTraceLoggerHelper.toHttpHeaderTrace(headers)));
    validParameterCollectionBuilder.putRequired("courtCentreId", courtCentreId, ParameterType.STRING);
    validParameterCollectionBuilder.putRequired("startDate", startDate, ParameterType.STRING);
    validParameterCollectionBuilder.putOptional("numberOfWeeks", numberOfWeeks, ParameterType.NUMERIC);

    // return restProcessor.process("OkStatusEnvelopePayloadEntityResponseStrategy", interceptorChainProcessor::process, actionMapper.actionOf("getHearingsDownloadHearingCsvReport", "GET", headers), headers, validParameterCollectionBuilder.parameters());

    LOGGER.info("Downloading hearing CSV report for courtCentreId: {}, startDate: {}, numberOfWeeks: {}",
            courtCentreId, startDate, numberOfWeeks);

    final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
    final JsonEnvelope csvQuery = envelopeFrom(
            metadataBuilder()
                    .withId(randomUUID())
                    .withName(HEARING_CSV_QUERY_NAME)
                    .withUserId(userId.toString())
                    .build(),
            builder
                    .add("courtCentreId", courtCentreId)
                    .add("startDate", startDate)
                    .add("numberOfWeeks", numberOfWeeks != null ? numberOfWeeks : "2")
                    .build());

//    interceptorChainProcessor.process(interceptorContextWithInput(csvQuery));

    final String csvContent = hearingQueryView.generateHearingCsvReport(csvQuery);

    if (csvContent != null && !csvContent.isEmpty()) {
      final String csvFileName = CSV_FILE_NAME_PREFIX + "_" + startDate + CSV_FILE_EXTENSION;
      final String disposition = "attachment; filename=\"" + csvFileName + "\"";
      
      final Response.ResponseBuilder responseBuilder = status(OK)
              .entity(new ByteArrayInputStream(csvContent.getBytes()));
      return responseBuilder
              .header(CONTENT_TYPE, CSV_MIME_TYPE)
              .header(CONTENT_DISPOSITION, disposition)
              .build();
    }

    return null;
  }
}
