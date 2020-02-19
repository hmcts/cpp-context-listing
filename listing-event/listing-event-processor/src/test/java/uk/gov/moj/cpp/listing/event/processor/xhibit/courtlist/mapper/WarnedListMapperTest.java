package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WarnedListMapperTest {

    private static final String HEARING_TYPE_CODE = "ABC";
    @Mock
    CourtServicesMapper courtServicesMapper;

    @InjectMocks
    private WarnedListMapper warnedListMapper;

    @Before
    public void before() {
        when(courtServicesMapper.getHearingTypeForHearing(UUID.fromString("5ae4c090-0f70-4694-b4fc-707633d2b430"))).thenReturn(HEARING_TYPE_CODE);
    }

    @Test
    public void shouldGetXhibitHearingType() {
        final JsonObject courtListJson = givenPayload("/xhibit/mock-data/hearingTypesData.json");
        final String actual = warnedListMapper.getXhibitHearingType(courtListJson);
        assertThat(actual, is(HEARING_TYPE_CODE));
    }

    @Test
    public void shouldGetXhibitHearingTypeEmpty() {
        final JsonObject courtListJson = givenPayload("/xhibit/mock-data/hearingTypesEmptyData.json");
        final String actual = warnedListMapper.getXhibitHearingType(courtListJson);
        assertThat(actual, is(nullValue()));
    }
}
