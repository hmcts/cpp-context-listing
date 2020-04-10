package uk.gov.moj.cpp.listing.event.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingLanguage;
import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JurisdictionType;
import uk.gov.justice.listing.events.OrganisationUnit;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.processor.service.ReferenceDataService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

@RunWith(MockitoJUnitRunner.class)
public class HearingConfirmedFactoryTest {

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
    private static final String REPORTING_RESTRICTION_REASON = STRING.next();
    private static final String JUDICIAL_ROLE_TYPE = "MAGISTRATE";
    private static final int DURATION_MINUTES = 60;


    @InjectMocks
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Mock
    private ReferenceDataService referenceDataService;


    @Test
    public void shouldCreateAHearingConfirmedWithJudiciary() {
        //given
        final List<JudicialRole> judiciary = Arrays.asList(JudicialRole.judicialRole()
                .withJudicialId(JUDICIAL_ID)
                .withJudicialRoleType(uk.gov.justice.listing.events.JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(JUDICIAL_ROLE_TYPE)
                        .build())
                .withIsDeputy(empty())
                .withIsBenchChairman(empty())
                .build());
        final HearingAllocatedForListing hearingAllocated = hearingAllocatedForListing(judiciary);
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        when(referenceDataService.getOrganizationUnitById(any(), eq(envelope))).thenReturn(OrganisationUnit.organisationUnit().withOucodeL3Name(java.util.Optional.of("test Court Centre")).build());


        //when
        final HearingConfirmed actual = hearingConfirmedFactory.create(hearingAllocated, envelope);

        //then
        final ConfirmedHearing listedHearing = actual.getConfirmedHearing();
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
    }

    @Test
    public void shouldCreateAHearingConfirmedWithoutJudiciary() {
        //given
        final List<JudicialRole> judiciary = Collections.emptyList();
        final HearingAllocatedForListing hearingAllocated = hearingAllocatedForListing(judiciary);
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        when(referenceDataService.getOrganizationUnitById(any(), eq(envelope))).thenReturn(OrganisationUnit.organisationUnit().withOucodeL3Name(java.util.Optional.of("test Court Centre")).build());

        //when
        final HearingConfirmed actual = hearingConfirmedFactory.create(hearingAllocated, envelope);

        //then
        final ConfirmedHearing listedHearing = actual.getConfirmedHearing();
        assertThat(listedHearing.getId(), is(listedHearing.getId()));

        assertThat(listedHearing.getJudiciary(),is(nullValue()));

    }


    private static HearingAllocatedForListing hearingAllocatedForListing(final List<JudicialRole> judiciary) {
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


        return HearingAllocatedForListing.hearingAllocatedForListing()
                .withHearingDays(hearingDays)
                .withCourtApplicationIds(Collections.singletonList(UUID.randomUUID()))
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
                .build();
    }
}