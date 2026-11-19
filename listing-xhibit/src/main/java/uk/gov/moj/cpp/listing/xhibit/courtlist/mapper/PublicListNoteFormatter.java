package uk.gov.moj.cpp.listing.xhibit.courtlist.mapper;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public interface PublicListNoteFormatter {

    static String getFormattedPublicListNote(final String publicListNote) {
           return isNotBlank(publicListNote) ? publicListNote : null;

    }
}
