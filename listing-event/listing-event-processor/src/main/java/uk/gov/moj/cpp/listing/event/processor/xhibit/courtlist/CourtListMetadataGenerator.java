package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;

public class CourtListMetadataGenerator {

    @Inject
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    @Inject
    private Clock clock;

    public CourtListMetadata generate(final PublishCourtListRequestParameters parameters) {

        final ZonedDateTime createdDate = clock.now();

        final String filename = format("%s_%s_%s.xml",
                parameters.getPublishCourtListType().getFilenamePrefix(),
                getCrownCourtCode(parameters.getCourtCentreId()),
                getSendDate(createdDate));

        return new CourtListMetadata(filename, getDocumentUniqueId(), createdDate);
    }

    private String getDocumentUniqueId() {
        return randomUUID().toString();
    }

    private String getSendDate(final ZonedDateTime createdDate) {

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");

        return createdDate.format(formatter);
    }

    private String getCrownCourtCode(final UUID courtCentreId) {
        return commonXhibitReferenceDataService.getCrownCourtDetails(courtCentreId).getCrestCourtId();
    }
}
