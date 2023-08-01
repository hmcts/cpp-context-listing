package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListing;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings({"squid:S1607"})
@RunWith(MockitoJUnitRunner.class)
public class AllocatedHearingExtendedFactoryTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID CASE2_ID = UUID.randomUUID();
    private static final UUID OFFENCE2_ID = UUID.randomUUID();
    private static final UUID DEFENDANT2_ID = UUID.randomUUID();
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
    private AllocatedHearingExtendedFactory allocatedHearingExtendedFactory;

    @Mock
    private ReferenceDataService referenceDataService;

    @Test
    public void shouldCreateExtendedHearingForHearing() {
        //given
        final List<JudicialRole> judiciary = Arrays.asList(JudicialRole.judicialRole()
                .withJudicialId(JUDICIAL_ID)
                .withJudicialRoleType(uk.gov.justice.listing.events.JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(JUDICIAL_ROLE_TYPE)
                        .build())
                .withIsDeputy(null)
                .withIsBenchChairman(null)
                .build());

        final ListedCase listedCase = ListedCase.listedCase().withId(CASE_ID).build();
        final List<ListedCase> listedCaseList = new ArrayList<>();
        listedCaseList.add(listedCase);

        final AllocatedHearingExtendedForListing allocatedHearingExtendedForListing = allocatedHearingExtendedForListing(judiciary, listedCaseList);
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        when(referenceDataService.getOrganizationUnitById(any(), eq(envelope))).thenReturn(OrganisationUnit.organisationUnit().withOucodeL3Name("test Court Centre").build());


        //when
        final HearingConfirmed actual = allocatedHearingExtendedFactory.create(allocatedHearingExtendedForListing, envelope);

        //then
        final ConfirmedHearing extendedHearing = actual.getConfirmedHearing();
        assertThat(extendedHearing.getId(), is(extendedHearing.getId()));

        assertThat(extendedHearing.getHearingDays().get(0).getSittingDay().toInstant().toString(),
                is(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC).toInstant().toString()));
        assertThat(extendedHearing.getType().getDescription(), is(TYPE));
        assertThat(extendedHearing.getType().getId(), is(TYPE_ID));
        assertThat(extendedHearing.getCourtCentre().getId(), is(COURT_CENTRE_ID));
        assertThat(extendedHearing.getCourtCentre().getRoomId(), is(COURT_ROOM_ID));
        assertThat(extendedHearing.getJudiciary().get(0).getJudicialId(), is(JUDICIAL_ID));
        assertThat(extendedHearing.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType(), is(JUDICIAL_ROLE_TYPE));
        assertThat(extendedHearing.getReportingRestrictionReason(), is(REPORTING_RESTRICTION_REASON));
        assertThat(extendedHearing.getHearingLanguage().toString(), is(HEARING_LANGUAGE.toString()));
        assertThat(extendedHearing.getJurisdictionType().toString(), is(JURISDICTION_TYPE.toString()));

        ConfirmedProsecutionCase prosecutionCaseDefendantOffenceIds = extendedHearing.getProsecutionCases().get(0);
        assertThat(prosecutionCaseDefendantOffenceIds.getId(), is(CASE_ID));
        ConfirmedDefendant defendantOffenceIds = prosecutionCaseDefendantOffenceIds.getDefendants().get(0);
        assertThat(defendantOffenceIds.getId(), is(DEFENDANT_ID));
        assertThat(defendantOffenceIds.getOffences().get(0).getId(), is(OFFENCE_ID));
    }

    @Test
    public void shouldCreateExtendedHearingV2WithOnlyNewCasesWhenHearingExtendedWithNewCases() {
        //given
        final List<JudicialRole> judiciary = Arrays.asList(JudicialRole.judicialRole()
                .withJudicialId(JUDICIAL_ID)
                .withJudicialRoleType(uk.gov.justice.listing.events.JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(JUDICIAL_ROLE_TYPE)
                        .build())
                .withIsDeputy(null)
                .withIsBenchChairman(null)
                .build());

        final ListedCase listedCase = ListedCase.listedCase().withId(CASE2_ID)
                .withDefendants(singletonList(Defendant.defendant()
                        .withId(DEFENDANT2_ID)
                        .withOffences(singletonList(Offence.offence()
                                .withId(OFFENCE2_ID)
                                .build()))
                        .build())).build();
        final List<ListedCase> listedCaseList = new ArrayList<>();
        listedCaseList.add(listedCase);

        final AllocatedHearingExtendedForListingV2 allocatedHearingExtendedForListing = allocatedHearingExtendedForListingV2(judiciary, listedCaseList);
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        when(referenceDataService.getOrganizationUnitById(any(), eq(envelope))).thenReturn(OrganisationUnit.organisationUnit().withOucodeL3Name("test Court Centre").build());


        //when
        final HearingConfirmed actual = allocatedHearingExtendedFactory.create(allocatedHearingExtendedForListing, envelope);

        //then
        final ConfirmedHearing extendedHearing = actual.getConfirmedHearing();
        assertThat(extendedHearing.getId(), is(extendedHearing.getId()));

        assertThat(extendedHearing.getHearingDays().get(0).getSittingDay().toInstant().toString(),
                is(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC).toInstant().toString()));
        assertThat(extendedHearing.getType().getDescription(), is(TYPE));
        assertThat(extendedHearing.getType().getId(), is(TYPE_ID));
        assertThat(extendedHearing.getCourtCentre().getId(), is(COURT_CENTRE_ID));
        assertThat(extendedHearing.getCourtCentre().getRoomId(), is(COURT_ROOM_ID));
        assertThat(extendedHearing.getJudiciary().get(0).getJudicialId(), is(JUDICIAL_ID));
        assertThat(extendedHearing.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType(), is(JUDICIAL_ROLE_TYPE));
        assertThat(extendedHearing.getReportingRestrictionReason(), is(REPORTING_RESTRICTION_REASON));
        assertThat(extendedHearing.getHearingLanguage().toString(), is(HEARING_LANGUAGE.toString()));
        assertThat(extendedHearing.getJurisdictionType().toString(), is(JURISDICTION_TYPE.toString()));

        assertThat(extendedHearing.getProsecutionCases().size(), is(1));
        ConfirmedProsecutionCase prosecutionCaseDefendantOffenceIds = extendedHearing.getProsecutionCases().get(0);
        assertThat(prosecutionCaseDefendantOffenceIds.getId(), is(CASE2_ID));
        ConfirmedDefendant defendantOffenceIds = prosecutionCaseDefendantOffenceIds.getDefendants().get(0);
        assertThat(defendantOffenceIds.getId(), is(DEFENDANT2_ID));
        assertThat(defendantOffenceIds.getOffences().get(0).getId(), is(OFFENCE2_ID));
    }

    private AllocatedHearingExtendedForListing allocatedHearingExtendedForListing(final List<JudicialRole> judiciary,
                                                                                  final List<ListedCase> listedCaseList) {
        List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds = singletonList(
                ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(CASE_ID)
                        .withDefendants(singletonList(DefendantOffenceIds.defendantOffenceIds()
                                .withOffenceIds(Arrays.asList(OFFENCE_ID))
                                .withId(DEFENDANT_ID)
                                .build()))
                        .build()
        );
        List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay()
                .withSequence(0)
                .withDurationMinutes(DURATION_MINUTES)
                .withStartTime(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC))
                .withEndTime(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME.plusMinutes(DURATION_MINUTES), BST).withZoneSameInstant(UTC))
                .build());


        return AllocatedHearingExtendedForListing.allocatedHearingExtendedForListing()
                .withHearingDays(hearingDays)
                .withCourtApplicationIds(singletonList(UUID.randomUUID()))
                .withHearingLanguage(HEARING_LANGUAGE)
                .withReportingRestrictionReason(REPORTING_RESTRICTION_REASON)
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
                .withUnAllocatedListedCases(listedCaseList)
                .build();
    }

    private AllocatedHearingExtendedForListingV2 allocatedHearingExtendedForListingV2(final List<JudicialRole> judiciary,
                                                                                  final List<ListedCase> listedCaseList) {
        List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds = Arrays.asList(
                ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(CASE_ID)
                        .withDefendants(singletonList(DefendantOffenceIds.defendantOffenceIds()
                                .withOffenceIds(Arrays.asList(OFFENCE_ID))
                                .withId(DEFENDANT_ID)
                                .build()))
                        .build(),
                ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(CASE2_ID)
                        .withDefendants(singletonList(DefendantOffenceIds.defendantOffenceIds()
                                .withOffenceIds(Arrays.asList(OFFENCE2_ID))
                                .withId(DEFENDANT2_ID)
                                .build()))
                        .build()
        );
        List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay()
                .withSequence(0)
                .withDurationMinutes(DURATION_MINUTES)
                .withStartTime(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME, BST).withZoneSameInstant(UTC))
                .withEndTime(ZonedDateTime.of(UPDATED_START_DATE, UPDATED_START_TIME.plusMinutes(DURATION_MINUTES), BST).withZoneSameInstant(UTC))
                .build());


        return AllocatedHearingExtendedForListingV2.allocatedHearingExtendedForListingV2()
                .withHearingDays(hearingDays)
                .withCourtApplicationIds(singletonList(UUID.randomUUID()))
                .withHearingLanguage(HEARING_LANGUAGE)
                .withReportingRestrictionReason(REPORTING_RESTRICTION_REASON)
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
                .withUnAllocatedListedCases(listedCaseList)
                .build();
    }
}