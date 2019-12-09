package uk.gov.moj.cpp.listing.common.xhibit;

import java.io.InputStream;
import java.util.UUID;

public interface XhibitService {

    void sendToXhibit(final UUID fileId, final String fileName) throws ExportFailedException;

    void sendToXhibit(final InputStream inputStream, final String fileName) throws ExportFailedException;

}
