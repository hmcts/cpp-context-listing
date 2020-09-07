package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper.VideoLinkTextFormatter.getFormattedVideoLinkText;

import org.junit.Test;

public class VideoLinkTextFormatterTest {

    @Test
    public void shouldReturnEmptyVideoLinkDetails() {
        assertThat(getFormattedVideoLinkText(false, null), nullValue());
    }

    @Test
    public void shouldReturnJustVideoAsVideoLinkDetailsWhenNoVideoLinkIsSetAndVideoLinkBooleanIsSetTrue() {
        assertThat(getFormattedVideoLinkText(true, null), is("Video Link:"));
    }

    @Test
    public void shouldReturnJustVideoAsVideoLinkDetailsWhenNEmptyVideoLinkIsSetAndVideoLinkBooleanIsSetTrue() {
        assertThat(getFormattedVideoLinkText(true, " "), is("Video Link:"));
    }

    @Test
    public void shouldReturnJustActualDetailsAsVideoLinkDetailsWhenNoVideoLinkIsSetAndVideoLinkBooleanIsSetTrue() {
        assertThat(getFormattedVideoLinkText(true, "Pre Cof"), is("Video Link:Pre Cof"));
    }

}