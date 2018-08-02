package uk.gov.moj.cpp.listing.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;

import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.external.HearingUpdated;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AllocatedHearingUpdatedFactoryTest {

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final String TYPE = RandomGenerator.STRING.next();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ROOM_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final LocalDate UPDATED_START_DATE = FUTURE_LOCAL_DATE.next();
    private static final LocalTime UPDATED_START_TIME = LocalTime.of(10, 0);
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");


    @InjectMocks
    private AllocatedHearingUpdatedFactory allocatedHearingUpdatedFactory;


    @Test
    public void shouldCreateAllocatedHearingUpdated() throws Exception {
        //given
        AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing = allocatedHearingUpdatedForListing();

        //when
        HearingUpdated actual = allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing);

        //then
        uk.gov.moj.cpp.listing.event.external.BaseHearing listedHearing = actual.getHearing();
        assertThat(listedHearing.getId(), is(allocatedHearingUpdatedForListing.getHearingId().toString()));

        assertThat(listedHearing.getHearingDays().get(0).toInstant().toString(),
                is(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC).toInstant().toString()));

        assertThat(listedHearing.getType(), is(allocatedHearingUpdatedForListing.getType()));
        assertThat(listedHearing.getCourtRoomId(), is(allocatedHearingUpdatedForListing.getCourtRoomId().toString()));
        assertThat(listedHearing.getJudgeId(), is(allocatedHearingUpdatedForListing.getJudgeId().toString()));
    }


    private AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing() {
        List<ZonedDateTime> hearingDays = Arrays.asList(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC));
        return new AllocatedHearingUpdatedForListing(COURT_ROOM_ID, COURT_CENTRE_ROOM_ID,
                hearingDays, HEARING_ID, JUDGE_ID, TYPE);
    }


}