package uk.gov.moj.cpp.listing.persistence.persistence;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(CdiTestRunner.class)
public class HearingRepositoryTest extends BaseTransactionalTest {

    private static final Boolean UNALLOCATED = false;
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID OTHER_COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final Integer ESTIMATED_MINUTES = INTEGER.next();
    private static final String TYPE = STRING.next();
    private static final LocalDate START_DATE = PAST_LOCAL_DATE.next();
    private static final LocalDate END_DATE = PAST_LOCAL_DATE.next();


    private static final Boolean ALLOCATED = BOOLEAN.next();

    @Inject
    private HearingRepository hearingRepository;

    @Test
    public void shouldFindHearingById() {
        final Hearing actualHearing = saveHearing(COURT_CENTRE_ID, UNALLOCATED);

        final Hearing expectedHearing = hearingRepository.findBy(actualHearing.getId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedHearing, actualHearing));
    }

    @Test
    public void shouldFindHearingsByAllocatedAndCourtCentreId() {
        final Hearing expectedHearingToBeReturned = saveHearing(COURT_CENTRE_ID, UNALLOCATED);
        final Hearing expectedHearingNotToBeReturned = saveHearing(OTHER_COURT_CENTRE_ID, ALLOCATED);

        final List<Hearing> actualHearings = hearingRepository.findByAllocatedAndCourtCentreId(UNALLOCATED, COURT_CENTRE_ID);

        assertThat(actualHearings.size(), is(1));
        assertThat(actualHearings.get(0).getAllocated(), is(UNALLOCATED));
        assertThat(actualHearings.get(0).getCourtCentreId(), is(COURT_CENTRE_ID));
    }

    @Test
    public void shouldReturnEmptyHearingsByAllocatedAndCourtCentreIdNotFound() {
        final Hearing expectedHearingToBeReturned = saveHearing(COURT_CENTRE_ID, UNALLOCATED);
        final Hearing expectedHearingNotToBeReturned = saveHearing(OTHER_COURT_CENTRE_ID, ALLOCATED);

        final List<Hearing> actualHearings = hearingRepository.findByAllocatedAndCourtCentreId(UNALLOCATED, UUID.randomUUID());

        assertThat(actualHearings.size(), is(0));
    }

    @Test
    public void shouldShouldUpdateCourtRoomIdForHearing() {
        //given
        final Hearing hearing = saveHearing(COURT_CENTRE_ID, UNALLOCATED);

        //when
        hearingRepository.updateCourtRoomId(COURT_ROOM_ID, hearing.getId());
        hearingRepository.refresh(hearing);

        //then
        final Hearing actualHearing = hearingRepository.findBy(hearing.getId());
        assertThat(actualHearing.getCourtRoomId(), is(COURT_ROOM_ID));
    }

    @Test
    public void shouldShouldUpdateJudgeIdForHearing() {
        //given
        final Hearing hearing = saveHearing(COURT_CENTRE_ID, UNALLOCATED);

        //when
        hearingRepository.updateJudgeId(JUDGE_ID, hearing.getId());
        hearingRepository.refresh(hearing);

        //then
        final Hearing actualHearing = hearingRepository.findBy(hearing.getId());
        assertThat(actualHearing.getJudgeId(), is(JUDGE_ID));
    }

    @Test
    public void shouldShouldUpdateStartDateForHearing() {
        //given
        final Hearing hearing = saveHearing(COURT_CENTRE_ID, UNALLOCATED);

        //when
        hearingRepository.updateStartDate(START_DATE, hearing.getId());
        hearingRepository.refresh(hearing);

        //then
        final Hearing actualHearing = hearingRepository.findBy(hearing.getId());
        assertThat(actualHearing.getStartDate(), is(START_DATE));
    }

    @Test
    public void shouldShouldUpdateEndDateForHearing() {
        //given
        final Hearing hearing = saveHearing(COURT_CENTRE_ID, UNALLOCATED);

        //when
        hearingRepository.updateEndDate(END_DATE, hearing.getId());
        hearingRepository.refresh(hearing);

        //then
        final Hearing actualHearing = hearingRepository.findBy(hearing.getId());
        assertThat(actualHearing.getEndDate(), is(END_DATE));
    }


    @Test
    public void shouldShouldUpdateTypeForHearing() {
        //given
        final Hearing hearing = saveHearing(COURT_CENTRE_ID, UNALLOCATED);

        //when
        hearingRepository.updateType(TYPE, hearing.getId());
        hearingRepository.refresh(hearing);

        //then
        final Hearing actualHearing = hearingRepository.findBy(hearing.getId());
        assertThat(actualHearing.getType(), is(TYPE));
    }

    @Test
    public void shouldShouldUpdateAllocatedForHearing() {
        //given
        final Hearing hearing = saveHearing(COURT_CENTRE_ID, UNALLOCATED);

        //when
        hearingRepository.updateAllocated(ALLOCATED, hearing.getId());
        hearingRepository.refresh(hearing);

        //then
        final Hearing actualHearing = hearingRepository.findBy(hearing.getId());
        assertThat(actualHearing.getAllocated(), is(ALLOCATED));
    }

    private Hearing saveHearing(final UUID courtCentreId, final boolean allocated) {
        final Hearing hearing = createHearing(courtCentreId, allocated);

        hearingRepository.save(hearing);

        return hearing;
    }

    private Hearing createHearing(final UUID courtCentreId, final boolean allocated) {
        return new HearingBuilder()
                .setId(UUID.randomUUID())
                .setAllocated(allocated)
                .setCourtCentreId(courtCentreId)
                .build();
    }

}