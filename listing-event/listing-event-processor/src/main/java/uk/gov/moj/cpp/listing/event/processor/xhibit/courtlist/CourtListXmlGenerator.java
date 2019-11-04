package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class CourtListXmlGenerator {

    @SuppressWarnings("squid:S1172")
    public InputStream generateCourtListXml(final PublishCourtListRequestParameters parameters) {
        // TODO Implement SCSL-27
        return new ByteArrayInputStream("DUMMY CONTENT".getBytes());
    }
}
