package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.time.ZonedDateTime;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class CourtListMetadata {

    private String filename;
    private String documentUniqueId;
    private ZonedDateTime createdDate;

    public CourtListMetadata(final String filename, final String documentUniqueId, final ZonedDateTime createdDate) {
        this.filename = filename;
        this.documentUniqueId = documentUniqueId;
        this.createdDate = createdDate;
    }

    public String getFilename() {
        return filename;
    }

    public String getDocumentUniqueId() {
        return documentUniqueId;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }
}
