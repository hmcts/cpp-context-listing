package uk.gov.moj.cpp.listing.command.utils;

import static java.time.LocalDate.now;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.core.courts.HearingLanguage.ENGLISH;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NonDefaultDayDurationBuilderTest {

    private static final int HALF_DAY_SESSION = 180;

    private static final String OUCODE = "BAOKOO";

    private static final ZonedDateTime NDD1_START_TIME = ZonedDateTime.now();

    private static final ZonedDateTime NDD2_START_TIME = ZonedDateTime.now().plusDays(1);

    private final NonDefaultDayDurationBuilder builder = new NonDefaultDayDurationBuilder();

    @Test
    public void shouldTestHearingWithAllDaySessionBooking() {
        final NonDefaultDay allDaySession = getAllDaySession();

        final UpdateHearingForListing updateHearingForListing = newUpdateHearingForListing(asList(allDaySession));

        final UpdateHearingForListing newUpdateHearingForListing = builder.buildNewUpdateHearingForListingWithNewNonDefaultDays(updateHearingForListing, Arrays.asList(allDaySession));

        assertThat(updateHearingForListing, equalTo(newUpdateHearingForListing));
    }

    @Test
    public void shouldTestHearingWithCountBasedSessionBooking() {
        final NonDefaultDay countBasedSession = getCountBasedSingleSession();

        final UpdateHearingForListing updateHearingForListing = newUpdateHearingForListing(asList(countBasedSession));

        final UpdateHearingForListing newUpdateHearingForListing = builder.buildNewUpdateHearingForListingWithNewNonDefaultDays(updateHearingForListing, Arrays.asList(countBasedSession));

        assertThat(updateHearingForListing, equalTo(newUpdateHearingForListing));
    }

    @Test
    public void shouldTestHearingWithHalfDaySessionBooking() {
        final List<NonDefaultDay> nonDefaultDays = getNonDefaultDays(540);

        final UpdateHearingForListing updateHearingForListing = newUpdateHearingForListing(nonDefaultDays);

        final UpdateHearingForListing newUpdateHearingForListing = builder.buildNewUpdateHearingForListingWithNewNonDefaultDays(updateHearingForListing, nonDefaultDays);

        assertHearingContentMatchesOldHearing(updateHearingForListing, newUpdateHearingForListing);

        assertNonDefaultDayValuesAreCorrect(updateHearingForListing.getNonDefaultDays(), newUpdateHearingForListing.getNonDefaultDays());

        assertNonDefaultDayWithCorrectDurationAssigned(newUpdateHearingForListing.getNonDefaultDays(), HALF_DAY_SESSION, HALF_DAY_SESSION);
    }

    @Test
    public void shouldTestHearingWithLessThanHalfDaySessionBooking() {
        final List<NonDefaultDay> nonDefaultDays = getNonDefaultDays(120);

        final UpdateHearingForListing updateHearingForListing = newUpdateHearingForListing(nonDefaultDays);

        final UpdateHearingForListing newUpdateHearingForListing = builder.buildNewUpdateHearingForListingWithNewNonDefaultDays(updateHearingForListing, nonDefaultDays);

        assertHearingContentMatchesOldHearing(updateHearingForListing, newUpdateHearingForListing);

        assertNonDefaultDayValuesAreCorrect(updateHearingForListing.getNonDefaultDays(), newUpdateHearingForListing.getNonDefaultDays());

        assertNonDefaultDayWithCorrectDurationAssigned(newUpdateHearingForListing.getNonDefaultDays(), 120, 120);
    }

    @Test
    public void shouldTestHearingWithRandomDurationMoreThanHalfDaySessionBooking() {
        final List<NonDefaultDay> nonDefaultDays = getNonDefaultDays(210);

        final UpdateHearingForListing updateHearingForListing = newUpdateHearingForListing(nonDefaultDays);

        final UpdateHearingForListing newUpdateHearingForListing = builder.buildNewUpdateHearingForListingWithNewNonDefaultDays(updateHearingForListing, nonDefaultDays);

        assertHearingContentMatchesOldHearing(updateHearingForListing, newUpdateHearingForListing);

        assertNonDefaultDayValuesAreCorrect(updateHearingForListing.getNonDefaultDays(), newUpdateHearingForListing.getNonDefaultDays());

        assertNonDefaultDayWithCorrectDurationAssigned(newUpdateHearingForListing.getNonDefaultDays(), HALF_DAY_SESSION, 30);
    }

    @Test
    public void shouldTestHearingWithRandomDurationGreaterThan2HalfDaysSessionsBooking() {
        final List<NonDefaultDay> nonDefaultDays = getNonDefaultDays(390);

        final UpdateHearingForListing updateHearingForListing = newUpdateHearingForListing(nonDefaultDays);

        final UpdateHearingForListing newUpdateHearingForListing = builder.buildNewUpdateHearingForListingWithNewNonDefaultDays(updateHearingForListing, nonDefaultDays);

        assertHearingContentMatchesOldHearing(updateHearingForListing, newUpdateHearingForListing);

        assertNonDefaultDayValuesAreCorrect(updateHearingForListing.getNonDefaultDays(), newUpdateHearingForListing.getNonDefaultDays());

        assertNonDefaultDayWithCorrectDurationAssigned(newUpdateHearingForListing.getNonDefaultDays(), HALF_DAY_SESSION, HALF_DAY_SESSION);
    }

    private void assertHearingContentMatchesOldHearing(final UpdateHearingForListing originalHearing, final UpdateHearingForListing updatedHearing) {
        assertThat(updatedHearing.getCourtCentreId(), equalTo(originalHearing.getCourtCentreId()));
        assertThat(updatedHearing.getCourtRoomId(), equalTo(originalHearing.getCourtRoomId()));
        assertThat(updatedHearing.getEndDate(), equalTo(originalHearing.getEndDate()));
        assertThat(updatedHearing.getHearingId(), equalTo(originalHearing.getHearingId()));
        assertThat(updatedHearing.getHearingLanguage(), equalTo(originalHearing.getHearingLanguage()));
        assertThat(updatedHearing.getJudiciary(), equalTo(originalHearing.getJudiciary()));
        assertThat(updatedHearing.getJurisdictionType(), equalTo(originalHearing.getJurisdictionType()));
        assertThat(updatedHearing.getNonSittingDays(), equalTo(originalHearing.getNonSittingDays()));
        assertThat(updatedHearing.getStartDate(), equalTo(originalHearing.getStartDate()));
        assertThat(updatedHearing.getType(), equalTo(originalHearing.getType()));
        assertThat(updatedHearing.getWeekCommencingDurationInWeeks(), equalTo(originalHearing.getWeekCommencingDurationInWeeks()));
        assertThat(updatedHearing.getWeekCommencingEndDate(), equalTo(originalHearing.getWeekCommencingEndDate()));
        assertThat(updatedHearing.getWeekCommencingStartDate(), equalTo(originalHearing.getWeekCommencingStartDate()));
    }

    private void assertNonDefaultDayValuesAreCorrect(final List<NonDefaultDay> originalDays, final List<NonDefaultDay> updatedDays) {

        final NonDefaultDay originalNonDefaultDay1 = originalDays.get(0);
        final NonDefaultDay updatedNonDefaultDay1 = updatedDays.get(0);

        final NonDefaultDay originalNonDefaultDay2 = originalDays.get(1);
        final NonDefaultDay updatedNonDefaultDay2 = updatedDays.get(1);

        assertThat(originalNonDefaultDay1.getCourtRoomId(), equalTo(updatedNonDefaultDay1.getCourtRoomId()));
        assertThat(originalNonDefaultDay1.getCourtScheduleId(), equalTo(updatedNonDefaultDay1.getCourtScheduleId()));
        assertThat(originalNonDefaultDay1.getOucode(), equalTo(updatedNonDefaultDay1.getOucode()));
        assertThat(originalNonDefaultDay1.getSession(), equalTo(updatedNonDefaultDay1.getSession()));
        assertThat(originalNonDefaultDay1.getStartTime(), equalTo(updatedNonDefaultDay1.getStartTime()));
        assertThat(originalNonDefaultDay2.getCourtRoomId(), equalTo(updatedNonDefaultDay2.getCourtRoomId()));
        assertThat(originalNonDefaultDay2.getCourtScheduleId(), equalTo(updatedNonDefaultDay2.getCourtScheduleId()));
        assertThat(originalNonDefaultDay2.getOucode(), equalTo(updatedNonDefaultDay2.getOucode()));
        assertThat(originalNonDefaultDay2.getSession(), equalTo(updatedNonDefaultDay2.getSession()));
        assertThat(originalNonDefaultDay2.getStartTime(), equalTo(updatedNonDefaultDay2.getStartTime()));
    }

    private void assertNonDefaultDayWithCorrectDurationAssigned(final List<NonDefaultDay> nondefaultDays, final int firstNonDefaultDayDuration, final int secondNonDefaultDayDuration) {
        final NonDefaultDay firstNonDefaultDay = nondefaultDays.get(0);
        final NonDefaultDay secondNonDefaultDay = nondefaultDays.get(1);

        assertThat(firstNonDefaultDay.getDuration(), notNullValue());
        assertThat(secondNonDefaultDay.getDuration(), notNullValue());

        assertThat(firstNonDefaultDay.getDuration(), equalTo(firstNonDefaultDayDuration));

        assertThat(secondNonDefaultDay.getDuration(), equalTo(secondNonDefaultDayDuration));
    }

    private UpdateHearingForListing newUpdateHearingForListing(final List<NonDefaultDay> nonDefaultDays) {
        final JudicialRole judiciary = new JudicialRole.Builder().withJudicialId(randomUUID()).build();

        return new UpdateHearingForListing.Builder()
                .withCourtCentreId(randomUUID())
                .withCourtRoomId(randomUUID())
                .withEndDate(now())
                .withHearingId(randomUUID())
                .withHearingLanguage(ENGLISH)
                .withJudiciary(asList(judiciary))
                .withJurisdictionType(MAGISTRATES)
                .withNonDefaultDays(nonDefaultDays)
                .withNonSittingDays(asList(now()))
                .withStartDate(getNewStartDate(nonDefaultDays).get())
                .withType(new HearingType("random type", randomUUID(), null))
                .withWeekCommencingDurationInWeeks(1)
                .withWeekCommencingEndDate(now())
                .withWeekCommencingStartDate(now())
                .build();
    }

    private List<NonDefaultDay> getNonDefaultDays(final int duration) {
        final NonDefaultDay.Builder nonDefaultDayBuilder = NonDefaultDay.nonDefaultDay()
                .withDuration(duration)
                .withStartTime(NDD1_START_TIME)
                .withSession("AM")
                .withOucode(OUCODE)
                .withCourtScheduleId("17452")
                .withCourtRoomId(1245);

        final NonDefaultDay nonDefaultDay1 = nonDefaultDayBuilder.build();
        final NonDefaultDay nonDefaultDay2 = nonDefaultDayBuilder
                .withStartTime(NDD2_START_TIME)
                .withCourtScheduleId("555")
                .withSession("PM").build();

        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();

        nonDefaultDays.add(nonDefaultDay1);
        nonDefaultDays.add(nonDefaultDay2);

        return nonDefaultDays;
    }


    private NonDefaultDay getAllDaySession() {
        return NonDefaultDay.nonDefaultDay()
                .withDuration(360)
                .withStartTime(NDD1_START_TIME)
                .withSession("AD")
                .withOucode(OUCODE)
                .withCourtScheduleId("1111")
                .withCourtRoomId(1245).build();

    }

    private NonDefaultDay getCountBasedSingleSession() {
        return NonDefaultDay.nonDefaultDay()
                .withDuration(1)
                .withStartTime(NDD1_START_TIME)
                .withSession("AM")
                .withOucode(OUCODE)
                .withCourtScheduleId("1111")
                .withCourtRoomId(1245).build();
    }

    private Optional<LocalDate> getNewStartDate(final List<NonDefaultDay> nonDefaultDays) {
        return of(nonDefaultDays.
                get(0)
                .getStartTime()
                .toLocalDate());
    }
}
