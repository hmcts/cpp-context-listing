package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import java.util.UUID;

public class CourtListMetadataGenerator {

    @SuppressWarnings("squid:S1172")
    public CourtListMetadata generate(final PublishCourtListRequestParameters parameters) {
        // TODO Implement SCSL-27
        return new CourtListMetadata("DUMMYFILENAME", UUID.randomUUID().toString());
    }
}
