package uk.gov.moj.cpp.listing.event.processor.xhibit;

import java.io.ByteArrayInputStream;

public class FileServiceStreamFactory {

    public ByteArrayInputStream buildStream(final String content) {
        return new ByteArrayInputStream(content.getBytes());
    }
}
