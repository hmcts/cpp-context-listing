package uk.gov.moj.cpp.listing.domain.transformation.judicialresult.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.domain.transformation.judicialresult.domain.EventToTransform.CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.judicialresult.domain.EventToTransform.CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.judicialresult.domain.EventToTransform.isEventToTransform;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class EventToTransformTest {

    @DataProvider
    public static Object[][] validEventToMatch() {
        return new Object[][]{
                {CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED.getEventName()},
                {CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED.getEventName()}
        };
    }

    @Test
    @UseDataProvider("validEventToMatch")
    public void shouldReturnTrueIfEventNameIsAMatch(final String eventName) {
        assertThat(isEventToTransform(eventName), is(true));
    }

    @Test
    public void shouldReturnFalseIfEventNameIsNotAMatch() {
        assertThat(isEventToTransform(STRING.next()), is(false));
    }
}