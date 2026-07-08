package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("hearings/download-hearing-csv-report")
public interface QueryApiHearingsDownloadHearingCsvReportResource {
  @GET
  @Produces("text/csv")
  Response getHearingsDownloadHearingCsvReport(@QueryParam("courtCentreId") String courtCentreId, @QueryParam("startDate") String startDate, @QueryParam("numberOfWeeks") String numberOfWeeks, @HeaderParam(HeaderConstants.USER_ID) UUID userId);
}
