package uk.gov.moj.cpp.listing.event.processor.util;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.events.CourtCentreDetails;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.HearingLanguage;
import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.JurisdictionType;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.Type;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingListedToUpdateHearingForListingCommandTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final LocalDate END_DATE = LocalDate.now().plusDays(10);
    private static final UUID HEARING_ID = randomUUID();
    private static final boolean IS_BENCH_CHAIRMAN = true;
    private static final boolean IS_DEPUTY = false;
    private static final UUID JUDICIAL_ID = randomUUID();
    private static final UUID JUDICIAL_ROLE_TYPE_ID = randomUUID();
    private static final String JUDICIARY_TYPE = "judiciaryType";
    private static final String JURISDICTION_TYPE = "CROWN";
    private static final int NON_DEFAULT_DAY_COURT_ROOM_ID = 123566;
    private static final String COURT_SCHEDULE_ID = randomUUID().toString();
    private static final int NON_DEFAULT_DAY_DURATION = 25;
    private static final String OUCODE = "Oucode";
    private static final String SESSION = "session";
    private static final ZonedDateTime NON_DEFAULT_DAY_START_TIME = ZonedDateTime.now().plusDays(1);
    private static final LocalDate NON_SITTING_DAYS1 = LocalDate.now().plusDays(2);
    private static final LocalDate NON_SITTING_DAYS2 = LocalDate.now().plusDays(3);
    private static final LocalDate START_DATE = LocalDate.now();
    private static final String TYPE_DESCRIPTION = "type description";
    private static final UUID TYPE_ID = randomUUID();
    private static final LocalDate WEEK_COMMENCING_END_DATE = LocalDate.now().plusWeeks(2);
    private static final LocalDate WEEK_COMMENCING_START_DATE = LocalDate.now().plusWeeks(1);
    private static final int WEEK_COMMENCING_DURATION_IN_WEEKS = 1;
    private static final int COURT_CENTRE_DETAILS_DURATION = 45;
    private static final String COURT_CENTRE_DETAILS_START_TIME = "12:30";

    @InjectMocks
    HearingListedToUpdateHearingForListingCommand hearingListedToUpdateHearingForListingCommand;

    @Test
    public void shouldConvertToUpdateHearingForListingCommand() {

        final Hearing hearing = buildSampleHearing();

        final UpdateHearingForListingEnriched updateHearingForListingEnriched = hearingListedToUpdateHearingForListingCommand.convert(hearing);

        final uk.gov.justice.listing.commands.CourtCentreDetails courtCentreDetails = updateHearingForListingEnriched.getCourtCentreDetails();
        assertThat(courtCentreDetails.getDefaultDuration(), is(COURT_CENTRE_DETAILS_DURATION));
        assertThat(courtCentreDetails.getDefaultStartTime(), is(LocalTime.parse(COURT_CENTRE_DETAILS_START_TIME)));

        final UpdateHearingForListing updateHearingForListing = updateHearingForListingEnriched.getUpdateHearingForListing();
        assertThat(updateHearingForListing.getCourtCentreId(), is(COURT_CENTRE_ID));
        assertThat(updateHearingForListing.getCourtRoomId(), is(of(COURT_ROOM_ID)));
        assertThat(updateHearingForListing.getEndDate(), is(of(END_DATE)));
        assertThat(updateHearingForListing.getHearingId(), is(HEARING_ID));
        assertThat(updateHearingForListing.getHearingLanguage().name(), is("ENGLISH"));

        final uk.gov.justice.core.courts.JudicialRole judicialRole = updateHearingForListing.getJudiciary().get(0);
        assertThat(judicialRole.getIsBenchChairman(), is(of(IS_BENCH_CHAIRMAN)));
        assertThat(judicialRole.getIsDeputy(), is(of(IS_DEPUTY)));
        assertThat(judicialRole.getJudicialId(), is(JUDICIAL_ID));
        assertThat(judicialRole.getJudicialRoleType().getJudicialRoleTypeId(), is(of(JUDICIAL_ROLE_TYPE_ID)));
        assertThat(judicialRole.getJudicialRoleType().getJudiciaryType(), is(JUDICIARY_TYPE));

        assertThat(updateHearingForListing.getJurisdictionType().name(), is(JURISDICTION_TYPE));

        final uk.gov.justice.listing.commands.NonDefaultDay nonDefaultDay = updateHearingForListing.getNonDefaultDays().get(0);
        assertThat(nonDefaultDay.getCourtRoomId(), is(of(NON_DEFAULT_DAY_COURT_ROOM_ID)));
        assertThat(nonDefaultDay.getCourtScheduleId(), is(of(COURT_SCHEDULE_ID)));
        assertThat(nonDefaultDay.getDuration(), is(of(NON_DEFAULT_DAY_DURATION)));
        assertThat(nonDefaultDay.getOucode(), is(of(OUCODE)));
        assertThat(nonDefaultDay.getSession(), is(of(SESSION)));
        assertThat(nonDefaultDay.getStartTime(), is(NON_DEFAULT_DAY_START_TIME));

        final List<LocalDate> nonSittingDays = updateHearingForListing.getNonSittingDays();
        assertThat(nonSittingDays.get(0), is(NON_SITTING_DAYS1));
        assertThat(nonSittingDays.get(1), is(NON_SITTING_DAYS2));

        assertThat(updateHearingForListing.getStartDate(), is(of(START_DATE)));

        final HearingType type = updateHearingForListing.getType();
        assertThat(type.getId(), is(TYPE_ID));
        assertThat(type.getDescription(), is(TYPE_DESCRIPTION));

        assertThat(updateHearingForListing.getWeekCommencingDurationInWeeks(), is(of(WEEK_COMMENCING_DURATION_IN_WEEKS)));
        assertThat(updateHearingForListing.getWeekCommencingEndDate(), is(of(WEEK_COMMENCING_END_DATE)));
        assertThat(updateHearingForListing.getWeekCommencingStartDate(), is(of(WEEK_COMMENCING_START_DATE)));
    }

    private Hearing buildSampleHearing() {
        return Hearing.hearing()
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(of(COURT_ROOM_ID))
                .withEndDate(of(END_DATE))
                .withId(HEARING_ID)
                .withHearingLanguage(HearingLanguage.valueOf("ENGLISH"))
                .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                        .withIsBenchChairman(of(IS_BENCH_CHAIRMAN))
                        .withIsDeputy(of(IS_DEPUTY))
                        .withJudicialId(JUDICIAL_ID)
                        .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                                .withJudicialRoleTypeId(of(JUDICIAL_ROLE_TYPE_ID))
                                .withJudiciaryType(JUDICIARY_TYPE)
                                .build())
                        .build()))
                .withJurisdictionType(JurisdictionType.valueOf(JURISDICTION_TYPE))
                .withNonDefaultDays(Arrays.asList(NonDefaultDay.nonDefaultDay()
                        .withCourtRoomId(of(NON_DEFAULT_DAY_COURT_ROOM_ID))
                        .withCourtScheduleId(of(COURT_SCHEDULE_ID))
                        .withDuration(of(NON_DEFAULT_DAY_DURATION))
                        .withOucode(of(OUCODE))
                        .withSession(of(SESSION))
                        .withStartTime(NON_DEFAULT_DAY_START_TIME)
                        .build()))
                .withStartDate(of(START_DATE))
                .withNonSittingDays(Arrays.asList(NON_SITTING_DAYS1, NON_SITTING_DAYS2))
                .withType(Type.type()
                        .withId(TYPE_ID)
                        .withDescription(TYPE_DESCRIPTION)
                        .build())
                .withWeekCommencingEndDate(of(WEEK_COMMENCING_END_DATE))
                .withWeekCommencingStartDate(of(WEEK_COMMENCING_START_DATE))
                .withWeekCommencingDurationInWeeks(of(WEEK_COMMENCING_DURATION_IN_WEEKS))
                .withCourtCentreDetails(of(CourtCentreDetails.courtCentreDetails()
                        .withDefaultDuration(COURT_CENTRE_DETAILS_DURATION)
                        .withDefaultStartTime(LocalTime.parse(COURT_CENTRE_DETAILS_START_TIME))
                        .build()))
                .build();
    }
}

