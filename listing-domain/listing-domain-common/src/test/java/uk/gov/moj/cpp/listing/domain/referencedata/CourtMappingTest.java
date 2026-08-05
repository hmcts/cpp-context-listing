package uk.gov.moj.cpp.listing.domain.referencedata;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class CourtMappingTest {

    @Test
    void shouldBuildCourtMappingWithWelshCrestCourtSiteName() {
        final CourtMapping courtMapping = new CourtMapping.Builder()
                .withCrestCourtSiteName("Cardiff Crown Court")
                .withWelshCrestCourtSiteName("Llys y Goron Caerdydd")
                .build();

        assertThat(courtMapping.getCrestCourtSiteName(), is("Cardiff Crown Court"));
        assertThat(courtMapping.getWelshCrestCourtSiteName(), is("Llys y Goron Caerdydd"));
    }

    @Test
    void shouldBuildCourtMappingWithWelshCrestCourtFullName() {
        final CourtMapping courtMapping = new CourtMapping.Builder()
                .withWelshCrestCourtFullName("Llys y Goron Caerdydd")
                .build();

        assertThat(courtMapping.getWelshCrestCourtFullName(), is("Llys y Goron Caerdydd"));
    }
}
