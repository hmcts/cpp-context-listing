package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public interface VideoLinkTextFormatter {

    static String getFormattedVideoLinkText(final boolean hasVideoLink, final String videoLinkDetails) {
        String resultText = null;
        if (hasVideoLink) {
            resultText = format("Video Link:%s", isNotBlank(videoLinkDetails) ? videoLinkDetails : EMPTY) ;
        }
        return resultText;
    }
}
