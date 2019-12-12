package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;

public class CourtListMetadataGenerator {

    @Inject
    private XhibitReferenceDataService xhibitReferenceDataService;

    @Inject
    private Clock clock;

    public CourtListMetadata generate(final JsonEnvelope envelope, final PublishCourtListRequestParameters parameters) {

        final ZonedDateTime createdDate = clock.now();

        final String filename = format("%s_%s_%s.xml",
                parameters.getPublishCourtListType().getFilenamePrefix(),
                getCrownCourtCode(envelope, parameters.getCourtCentreId()),
                getSendDate(createdDate));

        return new CourtListMetadata(filename, getDocumentUniqueId(), createdDate);
    }

    private String getDocumentUniqueId() {
        return randomUUID().toString();
    }

    private String getSendDate(final ZonedDateTime createdDate) {

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYMMddHHmmss");

        return createdDate.format(formatter);
    }

    private String getCrownCourtCode(final JsonEnvelope envelope, final UUID courtCentreId) {
        return xhibitReferenceDataService.getCourtDetails(envelope, courtCentreId).getCrestCourtId();
    }
}
