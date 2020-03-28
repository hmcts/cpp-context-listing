package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingLanguage;
import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JurisdictionType;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AllocatedHearingUpdatedFactoryTest {
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final String TYPE = RandomGenerator.STRING.next();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID JUDICIAL_ID = UUID.randomUUID();
    private static final LocalDate UPDATED_START_DATE = FUTURE_LOCAL_DATE.next();
    private static final LocalTime UPDATED_START_TIME = LocalTime.of(10, 0);
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId BST = ZoneId.of("Europe/London");
    private static final HearingLanguage HEARING_LANGUAGE = HearingLanguage.ENGLISH;
    private static final JurisdictionType JURISDICTION_TYPE = JurisdictionType.CROWN;
    private static final UUID TYPE_ID = RandomGenerator.UUID.next();
    private static final String REPORTING_RESTRICTION_REASON = RandomGenerator.STRING.next();
    private static final String JUDICIAL_ROLE_TYPE = "MAGISTRATE";
    private static final Integer DURATION_MINUTES = 60;
    private static final UUID COURT_APPLICATION_ID = UUID.randomUUID();


    @InjectMocks
    private AllocatedHearingUpdatedFactory allocatedHearingUpdatedFactory;


    @Test
    public void shouldCreateAllocatedHearingUpdatedWithJudiciary() throws Exception {
        //given
        final List<JudicialRole> judiciary = Arrays.asList(JudicialRole.judicialRole()
                .withJudicialId(JUDICIAL_ID)
                .withJudicialRoleType(uk.gov.justice.listing.events.JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(JUDICIAL_ROLE_TYPE)
                        .build())
                .withIsBenchChairman(empty())
                .withIsDeputy(empty())
                .build());
        final AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing = allocatedHearingUpdatedForListing(judiciary);

        //when
        final HearingUpdated actual = allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing);

        //then
        final ConfirmedHearing listedHearing = actual.getUpdatedHearing();
        assertThat(listedHearing.getId(), is(listedHearing.getId()));

        assertThat(listedHearing.getHearingDays().get(0).getSittingDay().toInstant().toString(),
                is(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC).toInstant().toString()));
        assertThat(listedHearing.getType().getDescription(), is(TYPE));
        assertThat(listedHearing.getType().getId(), is(TYPE_ID));
        assertThat(listedHearing.getCourtCentre().getId(), is(COURT_CENTRE_ID));
        assertThat(listedHearing.getCourtCentre().getRoomId().get(), is(COURT_ROOM_ID));
        assertThat(listedHearing.getJudiciary().get(0).getJudicialId(), is(JUDICIAL_ID));
        assertThat(listedHearing.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType(), is(JUDICIAL_ROLE_TYPE));
        assertThat(listedHearing.getReportingRestrictionReason().get(), is(REPORTING_RESTRICTION_REASON));
        assertThat(listedHearing.getHearingLanguage().get().toString(), is(HEARING_LANGUAGE.toString()));
        assertThat(listedHearing.getJurisdictionType().toString(), is(JURISDICTION_TYPE.toString()));

        final ConfirmedProsecutionCase prosecutionCaseDefendantOffenceIds = listedHearing.getProsecutionCases().get(0);
        assertThat(prosecutionCaseDefendantOffenceIds.getId(), is(CASE_ID));
        final ConfirmedDefendant defendantOffenceIds = prosecutionCaseDefendantOffenceIds.getDefendants().get(0);
        assertThat(defendantOffenceIds.getId(), is(DEFENDANT_ID));
        assertThat(defendantOffenceIds.getOffences().get(0).getId(), is(OFFENCE_ID));
        assertThat(listedHearing.getCourtApplicationIds().get(0), is(COURT_APPLICATION_ID));
    }

    @Test
    public void shouldCreateAHearingConfirmedWithoutJudiciary() throws Exception {
        //given
        final List<JudicialRole> judiciary = Collections.emptyList();
        final AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing = allocatedHearingUpdatedForListing(judiciary);

        //when
        final HearingUpdated actual = allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing);

        //then
        final ConfirmedHearing listedHearing = actual.getUpdatedHearing();
        assertThat(listedHearing.getId(), is(listedHearing.getId()));

        assertNull(listedHearing.getJudiciary());

    }

    private AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing(final List<JudicialRole> judiciary) {
        final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds = Collections.singletonList(
                ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(CASE_ID)
                        .withDefendants(Collections.singletonList(DefendantOffenceIds.defendantOffenceIds()
                                .withOffenceIds(Arrays.asList(OFFENCE_ID))
                                .withId(DEFENDANT_ID)
                                .build()))
                        .build()
        );
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay()
                .withSequence(0)
                .withDurationMinutes(DURATION_MINUTES)
                .withStartTime(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC))
                .withEndTime(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME.plusMinutes(DURATION_MINUTES), BST).withZoneSameInstant(UTC))
                .build());

        final List<UUID> courtApplicationIds = Collections.singletonList(COURT_APPLICATION_ID);


        return AllocatedHearingUpdatedForListing.allocatedHearingUpdatedForListing()
                .withHearingDays(hearingDays)
                .withHearingLanguage(HEARING_LANGUAGE)
                .withReportingRestrictionReason(of(REPORTING_RESTRICTION_REASON))
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withHearingId(HEARING_ID)
                .withProsecutionCaseDefendantsOffenceIds(prosecutionCaseDefendantOffenceIds)
                .withJurisdictionType(JURISDICTION_TYPE)
                .withType(Type.type()
                        .withDescription(TYPE)
                        .withId(TYPE_ID)
                        .build())
                .withJudiciary(judiciary)
                .withCourtApplicationIds(courtApplicationIds)
                .build();
    }

}