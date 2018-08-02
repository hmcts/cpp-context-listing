package uk.gov.moj.cpp.listing.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;

import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingDate;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.external.HearingConfirmed;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingConfirmedFactoryTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final String TYPE = RandomGenerator.STRING.next();
    private static final Integer ESTIMATED_MINUTES = RandomGenerator.INTEGER.next();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final LocalDate UPDATED_START_DATE = FUTURE_LOCAL_DATE.next();
    private static final LocalTime UPDATED_START_TIME = LocalTime.of(10,0);
    private static final String URN = RandomGenerator.STRING.next();
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");




    @InjectMocks
    private HearingConfirmedFactory hearingConfirmedFactory;



    @Test
    public void shouldCreateAHearingConfirmed() throws Exception {
        //given
        HearingAllocatedForListing hearingAllocated = hearingAllocatedForListing();

        //when
        HearingConfirmed actual = hearingConfirmedFactory.create(hearingAllocated);

        //then
        assertThat(actual.getCaseId(), is(hearingAllocated.getCaseId().toString()));
        assertThat(actual.getUrn(), is(hearingAllocated.getUrn()));
        
        uk.gov.moj.cpp.listing.event.external.Hearing listedHearing = actual.getHearing();
        assertThat(listedHearing.getId(), is(hearingAllocated.getHearingId().toString()));

        assertThat(listedHearing.getHearingDays().get(0).toInstant().toString(),
                is(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC).toInstant().toString()));
        assertThat(listedHearing.getType(), is(hearingAllocated.getType()));
        assertThat(listedHearing.getCaseId(), is(hearingAllocated.getCaseId().toString()));
        assertThat(listedHearing.getCourtCentreId(), is(hearingAllocated.getCourtCentreId().toString()));
        assertThat(listedHearing.getCourtRoomId(), is(hearingAllocated.getCourtRoomId().toString()));
        assertThat(listedHearing.getJudgeId(), is(hearingAllocated.getJudgeId().toString()));

        assertThat(listedHearing.getDefendants().size(), is(1));
        assertThat(listedHearing.getDefendants(), contains(allOf(
                hasProperty("id", is(DEFENDANT_ID.toString())))));

        List<uk.gov.moj.cpp.listing.event.external.Defendant> actualDefendants = listedHearing.getDefendants().stream().limit(1).collect(Collectors.toList());
        assertThat(actualDefendants.get(0).getOffences().size(), is(1));
        assertThat(actualDefendants.get(0).getOffences(), contains(allOf(
                hasProperty("id", is(OFFENCE_ID.toString())))));


    }


    private HearingAllocatedForListing hearingAllocatedForListing() {
        List<DefendantOffenceIds> defendantsOffenceIds = Collections.singletonList(
                DefendantOffenceIds.defendantOffenceIds().withId(DEFENDANT_ID).withOffenceIds(Collections.singletonList(OFFENCE_ID)).build()
        );
        List<ZonedDateTime> hearingDays = Arrays.asList(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC));
        return new HearingAllocatedForListing(CASE_ID, COURT_CENTRE_ID, COURT_ROOM_ID, defendantsOffenceIds, ESTIMATED_MINUTES,
                new HearingDate(UPDATED_START_DATE.toString(), Optional.of(UPDATED_START_TIME.toString())),
                hearingDays,
                HEARING_ID, JUDGE_ID, TYPE,
                URN);
    }


  
}