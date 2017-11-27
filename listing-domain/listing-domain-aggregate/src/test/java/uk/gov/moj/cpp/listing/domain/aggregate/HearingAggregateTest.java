package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.listing.domain.aggregate.utils.DomainBuilder.buildDefendant;

import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.event.UnallocatedHearingListed;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HearingAggregateTest {

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String COURT_CENTRE = "Liverpool";
    private static final LocalDate START_DATE = LocalDate.parse("2018-06-01");
    private static final int ESTIMATE_MINUTES = 7200;
    private static final String TYPE = "TRIAL";

    private HearingAggregate hearingAggregate;

    @Before
    public void setUp() {
        hearingAggregate = new HearingAggregate();
    }
    

    @Test
    public void shouldEmitUnallocatedHearingListedEventWhenListed() throws Exception {
        //given
        Defendant defendant = buildDefendant();

        //when
        List<Object> events = hearingAggregate.list(HEARING_ID.toString(), TYPE, START_DATE, ESTIMATE_MINUTES,
                CASE_ID.toString(), COURT_CENTRE, Arrays.asList(defendant)).collect(toList());

        //then
        assertThat(events.size(), is(1));

        Object object = events.get(0);
        assertThat(object.getClass() , is(equalTo(UnallocatedHearingListed.class)));
        
    }

    @Test
    public void shouldEmitNoEventWhenAlreadyListed() throws Exception {
        //given
        Defendant defendant = buildDefendant();

        hearingAggregate.list(HEARING_ID.toString(), TYPE, START_DATE, ESTIMATE_MINUTES,
                CASE_ID.toString(), COURT_CENTRE, Arrays.asList(defendant)).collect(toList());

        //when
        List<Object> events = hearingAggregate.list(HEARING_ID.toString(), TYPE, START_DATE, ESTIMATE_MINUTES,
                CASE_ID.toString(), COURT_CENTRE, Arrays.asList(defendant)).collect(toList());

        //then
        assertThat(events.size(), is(0));
    }

    @After
    public void teardown() {
        try {
            // ensure aggregate is serializable
            SerializationUtils.serialize(hearingAggregate);
        } catch (SerializationException e) {
            fail("Aggregate should be serializable");
        }
    }

}