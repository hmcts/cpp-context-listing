package uk.gov.moj.cpp.listing.common.service;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.listing.domain.exception.DataValidationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1118", "squid:S1312", "squid:S2629"})
final class CourtSchedulerJsonGetQuerySupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtSchedulerJsonGetQuerySupport.class);

    private static final String CJS_CPP_UID = "CJSCPPUID";

    private CourtSchedulerJsonGetQuerySupport() {
    }

    static CloseableHttpResponse execute(final HttpRequestBase httpRequest) throws IOException {
        return HttpClientBuilder
                .create()
                .build()
                .execute(httpRequest);
    }

    static boolean isOk(final HttpResponse httpResponse) {
        return httpResponse.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode();
    }

    static Response executeQuery(
            final String baseUri,
            final SystemUserProvider systemUserProvider,
            final StringToJsonObjectConverter stringToJsonObjectConverter,
            final String urlPath,
            final String acceptHeader,
            final Map<String, String> params,
            final String logContextLabel) {
        if (LOGGER.isInfoEnabled() && Objects.nonNull(params)) {
            params.forEach((key, value) ->
                    LOGGER.info("{} in {} with params '{}-{}'", acceptHeader, logContextLabel, key, value));
        }

        if (params == null) {
            throw new DataValidationException("Params for search %s is null ....".formatted(acceptHeader));
        }

        try {
            final URIBuilder uriBuilder = new URIBuilder(baseUri + urlPath);
            params.forEach(uriBuilder::addParameter);
            final HttpGet httpGet = new HttpGet(uriBuilder.build());
            httpGet.addHeader(ACCEPT, acceptHeader);
            httpGet.addHeader(CJS_CPP_UID, getUserId(systemUserProvider).toString());

            final HttpResponse httpResponse = execute(httpGet);

            if (isOk(httpResponse)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Retrieve {} successfully", acceptHeader);
                }
                return Response
                        .status(Response.Status.fromStatusCode(httpResponse.getStatusLine().getStatusCode()))
                        .entity(stringToJsonObjectConverter.convert(EntityUtils.toString(httpResponse.getEntity())))
                        .build();
            }
            LOGGER.error("Retrieve {} failed with status code:{}", acceptHeader,
                    httpResponse.getStatusLine().getStatusCode());
            return Response
                    .status(Response.Status.fromStatusCode(httpResponse.getStatusLine().getStatusCode()))
                    .entity(EntityUtils.toString(httpResponse.getEntity()))
                    .build();
        } catch (URISyntaxException | IOException ex) {
            LOGGER.error("Exception thrown on trying to Retrieving %s".formatted(acceptHeader), ex);
            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(ex.getMessage())
                    .build();
        }
    }

    private static UUID getUserId(final SystemUserProvider systemUserProvider) {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new IllegalStateException("contextSystemUserId missing!!!"));
    }
}
