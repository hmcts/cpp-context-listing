package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper.PublicListNoteFormatter.getFormattedPublicListNote;

import org.junit.Test;

public class PublicListNoteFormatterTest {


    @Test
    public void shouldReturnJustVideoAsPublicListNoteWhenNoPublicListNoteIsSet() {
        assertNull(getFormattedPublicListNote( null));
    }

    @Test
    public void shouldReturnJustVideoAsVideoAsPublicListNotWhenNEmptyPublicListNoteIsSet() {
        assertNull(getFormattedPublicListNote( " "));
    }

    @Test
    public void shouldReturnJustActualDetailsAsPublicListNoteWhenPublicListNoteIsSet() {
        assertThat(getFormattedPublicListNote( "Pre Cof"), is("Pre Cof"));
    }

}