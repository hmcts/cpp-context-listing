package uk.gov.moj.cpp.listing.command.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class HearingDaysToDomainConverterTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @InjectMocks
    private CommandBuilder commandBuilder;

    private final HearingDaysToDomainConverter target = new HearingDaysToDomainConverter();

    @Test
    public void shouldConvertHearingDayFromCoreDomainToListingDomain() {
        final List<HearingDay> hearingDaysFromCoreDomain = commandBuilder.buildHearingDays();

        final List<uk.gov.justice.listing.events.HearingDay> hearingDaysInListingDomain = target.convert(hearingDaysFromCoreDomain);

        assertThat(hearingDaysInListingDomain, hasSize(3));
        assertThat(LocalDates.to(hearingDaysInListingDomain.get(0).getHearingDate()), is("2020-08-18"));
        assertThat(hearingDaysInListingDomain.get(0).getSequence(), is(0));
        assertThat(hearingDaysInListingDomain.get(0).getDurationMinutes(), is(30));
        assertThat(ZonedDateTimes.toString(hearingDaysInListingDomain.get(0).getStartTime()), is("2020-08-18T01:22:12.381Z"));
        assertThat(hearingDaysInListingDomain.get(0).getIsCancelled().isPresent(), is(false));

        assertThat(LocalDates.to(hearingDaysInListingDomain.get(1).getHearingDate()), is("2020-08-19"));
        assertThat(hearingDaysInListingDomain.get(1).getSequence(), is(1));
        assertThat(hearingDaysInListingDomain.get(1).getDurationMinutes(), is(10));
        assertThat(ZonedDateTimes.toString(hearingDaysInListingDomain.get(1).getStartTime()), is("2020-08-19T01:22:12.381Z"));
        assertThat(hearingDaysInListingDomain.get(1).getIsCancelled().orElse(null), is(false));

        assertThat(LocalDates.to(hearingDaysInListingDomain.get(2).getHearingDate()), is("2020-08-20"));
        assertThat(hearingDaysInListingDomain.get(2).getSequence(), is(1));
        assertThat(hearingDaysInListingDomain.get(2).getDurationMinutes(), is(20));
        assertThat(ZonedDateTimes.toString(hearingDaysInListingDomain.get(2).getStartTime()), is("2020-08-20T02:22:12.381Z"));
        assertThat(hearingDaysInListingDomain.get(2).getIsCancelled().orElse(null), is(true));
    }
}