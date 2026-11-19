package uk.gov.moj.cpp.listing.xhibit.courtlist;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * File name ({@code <prefix>_<crestCourtCode>_<yyyyMMddHHmmss>.xml}) and document id of an export
 * (port of the JEE {@code CourtListMetadataGenerator}; the framework's {@code UtcClock} becomes a
 * UTC {@link Clock}, truncated to milliseconds as the framework clock was).
 */
@Component
public class CourtListMetadataGenerator {

    private final CommonXhibitReferenceDataService commonXhibitReferenceDataService;
    private final Clock clock;

    public CourtListMetadataGenerator(final CommonXhibitReferenceDataService commonXhibitReferenceDataService, final Clock clock) {
        this.commonXhibitReferenceDataService = commonXhibitReferenceDataService;
        this.clock = clock;
    }

    public CourtListMetadata generate(final PublishCourtListRequestParameters parameters) {

        final ZonedDateTime createdDate = ZonedDateTime.now(clock).withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);

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
