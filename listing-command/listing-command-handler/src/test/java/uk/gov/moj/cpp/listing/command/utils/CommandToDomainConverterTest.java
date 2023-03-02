package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static uk.gov.moj.cpp.listing.command.utils.CommandToDomainConverter.REFERRAL_REASON_FOR_DISQUALIFICATION;

import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.SeedingHearing;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CommandToDomainConverterTest {

    private final CommandToDomainConverter commandToDomainConverter = new CommandToDomainConverter();

    @Spy
    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    ObjectToJsonValueConverter objectToJsonValueConverter = new JsonObjectConvertersFactory().objectToJsonValueConverter();

    @InjectMocks
    CommandBuilder commandBuilder;

    @Test
    public void shouldConvertHearingCommandToHearingDomain() {

        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearing();

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing);

        //then
        assertThat(actual.getId(), is(commandHearing.getId()));
        assertThat(actual.getJurisdictionType().name(), is(commandHearing.getJurisdictionType().name()));
        assertJudicialRole(commandHearing, actual);
        assertListedCases(commandHearing, actual);
        assertCourtApplications(commandHearing, actual);

        assertThat(actual.getProsecutorDatesToAvoid().get(), is(commandHearing.getProsecutorDatesToAvoid()));
        assertThat(actual.getNonDefaultDays(), is(Collections.emptyList()));
        assertThat(actual.getListingDirections().get(), is(commandHearing.getListingDirections()));
        assertThat(actual.getType().getId(), is(commandHearing.getType().getId()));
        assertThat(actual.getType().getDescription(), is(commandHearing.getType().getDescription()));
        assertThat(actual.getCourtRoomId().get(), is(commandHearing.getCourtCentre().getRoomId()));
        assertThat(actual.getCourtCentreId(), is(commandHearing.getCourtCentre().getId()));
        assertThat(actual.getEstimatedMinutes(), is(commandHearing.getEstimatedMinutes()));
        assertThat(actual.getStartDateTime(), is(ZonedDateTimes.fromString(commandHearing.getEarliestStartDateTime().toString())));
        assertThat(actual.getEndDate().orElse(null), is(commandHearing.getEndDate()));
        assertThat(actual.getReportingRestrictionReason().get(), is(commandHearing.getReportingRestrictionReason()));
        assertThat(actual.getWeekCommencingStartDate(), is(of(LocalDate.parse(commandHearing.getWeekCommencingDate().getStartDate()))));
        assertThat(actual.getWeekCommencingDurationInWeeks().get(), is(commandHearing.getWeekCommencingDate().getDuration()));

    }

    @Test
    public void shouldConvertHearingCommandToHearingDomainWithMandatorySeedingHearingFields() {

        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingWithMandatorySeedingHearing();

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing);

        assertListedCases(commandHearing, actual);

    }


        @Test
    public void shouldConvertHearingCommandToHearingDomainForBookedSlots() {

        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingForBookedSlots();

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing);

        //then
        assertThat(actual.getId(), is(commandHearing.getId()));
        assertThat(actual.getJurisdictionType().name(), is(commandHearing.getJurisdictionType().name()));
        assertJudicialRole(commandHearing, actual);
        assertCourtApplications(commandHearing, actual);

        assertThat(actual.getProsecutorDatesToAvoid().get(), is(commandHearing.getProsecutorDatesToAvoid()));
        final RotaSlot rotaSlot = commandHearing.getBookedSlots().get(0);
        final NonDefaultDay nonDefaultDay = actual.getNonDefaultDays().get(0);
        assertThat(nonDefaultDay.getCourtRoomId().get(), is(rotaSlot.getCourtRoomId()));
        assertThat(nonDefaultDay.getCourtScheduleId().get(), is(rotaSlot.getCourtScheduleId()));
        assertThat(nonDefaultDay.getDuration().get(), is(rotaSlot.getDuration()));
        assertThat(nonDefaultDay.getOucode().get(), is(rotaSlot.getOucode()));
        assertThat(nonDefaultDay.getSession().get(), is(rotaSlot.getSession()));
        assertThat(nonDefaultDay.getStartTime(), is(rotaSlot.getStartTime()));

        assertThat(actual.getListingDirections().get(), is(commandHearing.getListingDirections()));
        assertThat(actual.getType().getId(), is(commandHearing.getType().getId()));
        assertThat(actual.getType().getDescription(), is(commandHearing.getType().getDescription()));
        assertThat(actual.getCourtRoomId().get(), is(commandHearing.getCourtCentre().getRoomId()));
        assertThat(actual.getCourtCentreId(), is(commandHearing.getCourtCentre().getId()));
        assertThat(actual.getEstimatedMinutes(), is(commandHearing.getEstimatedMinutes()));
        assertThat(actual.getStartDateTime(), is(ZonedDateTimes.fromString(commandHearing.getEarliestStartDateTime().toString())));
        assertThat(actual.getEndDate().orElse(null), is(commandHearing.getEndDate()));
        assertThat(actual.getReportingRestrictionReason().get(), is(commandHearing.getReportingRestrictionReason()));
    }


    @Test
    public void shouldConvertHearingCommandToHearingDomainForStandaloneApplication() {

        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingStandalone();

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing);

        //then
        assertThat(actual.getId(), is(commandHearing.getId()));
        assertThat(actual.getJurisdictionType().name(), is(commandHearing.getJurisdictionType().name()));
        assertCourtApplications(commandHearing, actual);

        assertThat(actual.getProsecutorDatesToAvoid().get(), is(commandHearing.getProsecutorDatesToAvoid()));
        assertThat(actual.getNonDefaultDays(), is(Collections.emptyList()));
        assertThat(actual.getListingDirections().get(), is(commandHearing.getListingDirections()));
        assertThat(actual.getType().getId(), is(commandHearing.getType().getId()));
        assertThat(actual.getType().getDescription(), is(commandHearing.getType().getDescription()));
        assertThat(actual.getCourtRoomId().get(), is(commandHearing.getCourtCentre().getRoomId()));
        assertThat(actual.getCourtCentreId(), is(commandHearing.getCourtCentre().getId()));
        assertThat(actual.getEstimatedMinutes(), is(commandHearing.getEstimatedMinutes()));
        assertThat(actual.getStartDateTime(), is(ZonedDateTimes.fromString(commandHearing.getEarliestStartDateTime().toString())));
        assertThat(actual.getEndDate().orElse(null), is(commandHearing.getEndDate()));
        assertThat(actual.getReportingRestrictionReason().get(), is(commandHearing.getReportingRestrictionReason()));

    }

    @Test
    public void shouldUseListedStartDateOverEarliestStartDate() {
        //given
        HearingListingNeeds commandHearing = commandBuilder.buildHearingWithListedStartDateTime();

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing);

        //then
        assertThat(actual.getStartDateTime(), not(commandHearing.getEarliestStartDateTime().toLocalDate()));
        assertThat(actual.getStartDateTime(), is(ZonedDateTimes.fromString(commandHearing.getListedStartDateTime().toString())));
    }

    @Test
    public void shouldSetShadowListedFlagForOneOffence(){
        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingWithMultipleOffences();
        List<UUID> shadowListedOffences = Collections.singletonList(UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"));

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing, Collections.emptyList(), shadowListedOffences);

        //then
        final Defendant defendant1 = actual.getListedCases().get(0).getDefendants().get(0);
        final Defendant defendant2 = actual.getListedCases().get(0).getDefendants().get(1);

        assertThat(actual.getListedCases().get(0).getShadowListed(), is(of(false)));
        assertThat(actual.getListedCases().get(0).getTrialReceiptType(), is("Voluntary bill"));
        assertThat(defendant1.getOffences().get(0).getShadowListed(), is(of(true)));
        assertThat(defendant1.getOffences().get(1).getShadowListed(), is(of(false)));
        assertThat(defendant2.getOffences().get(0).getShadowListed(), is(of(true)));
        assertThat(defendant2.getOffences().get(1).getShadowListed(), is(of(false)));
    }

    @Test
    public void shouldSetShadowListedFlagForCase(){
        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingWithMultipleOffences();
        List<UUID> shadowListedOffences = Arrays.asList(
                UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"),
                UUID.fromString("8a2d31c5-ac51-4c38-b1b4-dcef68d078b9"));

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing, Collections.emptyList(), shadowListedOffences);

        //then
        final Defendant defendant1 = actual.getListedCases().get(0).getDefendants().get(0);
        final Defendant defendant2 = actual.getListedCases().get(0).getDefendants().get(1);

        assertThat(actual.getListedCases().get(0).getShadowListed(), is(of(true)));
        assertThat(defendant1.getOffences().get(0).getShadowListed(), is(of(true)));
        assertThat(defendant1.getOffences().get(1).getShadowListed(), is(of(true)));
        assertThat(defendant2.getOffences().get(0).getShadowListed(), is(of(true)));
        assertThat(defendant2.getOffences().get(1).getShadowListed(), is(of(true)));
    }

    @Test
    public void shouldNotSetShadowListedFlagWhenNoOffences(){
        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingWithMultipleOffences();
        List<UUID> shadowListedOffences = Collections.emptyList();

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing, Collections.emptyList(), shadowListedOffences);

        //then
        final Defendant defendant1 = actual.getListedCases().get(0).getDefendants().get(0);
        final Defendant defendant2 = actual.getListedCases().get(0).getDefendants().get(1);

        assertThat(actual.getListedCases().get(0).getShadowListed(), is(of(false)));
        assertThat(defendant1.getOffences().get(0).getShadowListed(), is(of(false)));
        assertThat(defendant1.getOffences().get(1).getShadowListed(), is(of(false)));
        assertThat(defendant2.getOffences().get(0).getShadowListed(), is(of(false)));
        assertThat(defendant2.getOffences().get(1).getShadowListed(), is(of(false)));
    }

    @Test
    public void shouldNotSetShadowListedFlagWhenListIsNull(){
        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingWithMultipleOffences();
        List<UUID> shadowListedOffences = null;

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing, Collections.emptyList(), shadowListedOffences);

        //then
        final Defendant defendant1 = actual.getListedCases().get(0).getDefendants().get(0);
        final Defendant defendant2 = actual.getListedCases().get(0).getDefendants().get(1);

        assertThat(actual.getListedCases().get(0).getShadowListed(), is(of(false)));
        assertThat(defendant1.getOffences().get(0).getShadowListed(), is(of(false)));
        assertThat(defendant1.getOffences().get(1).getShadowListed(), is(of(false)));
        assertThat(defendant2.getOffences().get(0).getShadowListed(), is(of(false)));
        assertThat(defendant2.getOffences().get(1).getShadowListed(), is(of(false)));
    }

    @Test
    public void shouldPopulateReportingRestrictions(){
        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingWithReportingRestrictions();

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing, Collections.emptyList(), Collections.emptyList());

        //then
        final Defendant defendant1 = actual.getListedCases().get(0).getDefendants().get(0);

        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(0).getId(), notNullValue());
        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(0).getJudicialResultId(), notNullValue());
        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(0).getLabel(), is("RR Label 1"));
        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(0).getOrderedDate(), notNullValue());
        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(1).getId(), notNullValue());
        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(1).getJudicialResultId(), notNullValue());
        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(1).getLabel(), is("RR Label 2"));
        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(1).getOrderedDate(), is(of(LocalDate.of(2020, 10, 10))));
    }

    private void assertCourtApplications(final HearingListingNeeds commandHearing, final uk.gov.moj.cpp.listing.domain.Hearing actual) {
        CourtApplication actualCourtApplication = actual.getCourtApplications().get(0);
        assertThat(commandHearing.getCourtApplications().size(), is(actual.getCourtApplications().size()));
        assertThat(commandHearing.getCourtApplications().get(0).getId(), is(actualCourtApplication.getId()));
    }

    private void assertListedCases(final HearingListingNeeds commandHearing, final uk.gov.moj.cpp.listing.domain.Hearing actual) {
        ListedCase actualListedCase = actual.getListedCases().get(0);
        ProsecutionCase commandProsecutionCase = commandHearing.getProsecutionCases().get(0);
        DefendantListingNeeds commandListDefendantRequests = commandHearing.getDefendantListingNeeds().get(0);

        assertThat(actualListedCase.getId(), is(commandProsecutionCase.getId()));
        assertListedCaseIdentifier(actualListedCase, commandProsecutionCase);

        assertDefendant(actualListedCase, commandProsecutionCase, commandListDefendantRequests);


    }

    private void assertDefendant(final ListedCase actualListedCase, final ProsecutionCase commandProsecutionCase, final DefendantListingNeeds commandListDefendantRequests) {
        Defendant actualDefendant = actualListedCase.getDefendants().get(0);
        uk.gov.justice.core.courts.Defendant commandDefendant = commandProsecutionCase.getDefendants().get(0);

        Defendant actualDefendant2 = actualListedCase.getDefendants().get(1);
        uk.gov.justice.core.courts.Defendant commandDefendant2 = commandProsecutionCase.getDefendants().get(1);

        assertThat(actualDefendant.getId(), is(commandDefendant.getId()));
        assertThat(actualDefendant.getBailStatus().get().getCode(), is(commandDefendant.getPersonDefendant().getBailStatus().getCode()));
        assertThat(actualDefendant.getBailStatus().get().getId(), is(commandDefendant.getPersonDefendant().getBailStatus().getId()));
        assertThat(actualDefendant.getBailStatus().get().getDescription(), is(commandDefendant.getPersonDefendant().getBailStatus().getDescription()));
        assertThat(actualDefendant.getFirstName().get(), is(commandDefendant.getPersonDefendant().getPersonDetails().getFirstName()));
        assertThat(actualDefendant.getLastName().get(), is(commandDefendant.getPersonDefendant().getPersonDetails().getLastName()));
        assertThat(actualDefendant.getDefenceOrganisation().get(), is(commandDefendant.getDefenceOrganisation().getName()));
        assertThat(actualDefendant.getSpecificRequirements().get(), is(commandDefendant.getPersonDefendant().getPersonDetails().getSpecificRequirements()));
        assertThat(actualDefendant.getDateOfBirth().get(), is(commandDefendant.getPersonDefendant().getPersonDetails().getDateOfBirth().toString()));
        assertThat(actualDefendant.getCustodyTimeLimit().get(), is(commandDefendant.getPersonDefendant().getCustodyTimeLimit().toString()));
        assertThat(actualDefendant.getHearingLanguageNeeds().get().toString(), is(commandListDefendantRequests.getHearingLanguageNeeds().toString()));
        assertThat(actualDefendant.getDatesToAvoid().get(), is(commandListDefendantRequests.getDatesToAvoid()));


        assertThat(actualDefendant2.getOrganisationName().get(), is(commandDefendant2.getLegalEntityDefendant().getOrganisation().getName()));


        assertOffence(actualDefendant, commandDefendant);
    }

    private void assertOffence(final Defendant actualDefendant, final uk.gov.justice.core.courts.Defendant commandDefendant) {
        Offence actualOffence = actualDefendant.getOffences().get(0);
        uk.gov.justice.core.courts.Offence commandOffence = commandDefendant.getOffences().get(0);

        assertThat(actualOffence.getId(), is(commandOffence.getId()));
        assertThat(actualOffence.getStartDate(), is(commandOffence.getStartDate().toString()));
        assertThat(actualOffence.getEndDate().get(), is(commandOffence.getEndDate().toString()));
        assertThat(actualOffence.getOffenceCode(), is(commandOffence.getOffenceCode()));

        assertStatementOfOffence(actualOffence, commandOffence);
        assertSeedingHearing(actualOffence, commandOffence);
    }

    private void assertStatementOfOffence(final Offence actualOffence, final uk.gov.justice.core.courts.Offence commandOffence) {
        StatementOfOffence actualStatementOfOffence = actualOffence.getStatementOfOffence();
        assertThat(actualStatementOfOffence.getTitle(), is(commandOffence.getOffenceTitle()));
        assertThat(actualStatementOfOffence.getWelshTitle(), is(commandOffence.getOffenceTitleWelsh()));
        assertThat(actualStatementOfOffence.getLegislation().get(), is(commandOffence.getOffenceLegislation()));
        assertThat(actualStatementOfOffence.getWelshLegislation().get(), is(commandOffence.getOffenceLegislationWelsh()));
    }

    private void assertSeedingHearing(final Offence actualOffence, final uk.gov.justice.core.courts.Offence commandOffence) {
        final SeedingHearing actualSeedingHearing = actualOffence.getSeedingHearing().get();
        final uk.gov.justice.core.courts.SeedingHearing seedingHearing = commandOffence.getSeedingHearing();
        assertThat(actualSeedingHearing.getSeedingHearingId(), is(seedingHearing.getSeedingHearingId()));
        assertThat(actualSeedingHearing.getJurisdictionType().name(), is(seedingHearing.getJurisdictionType().name()));
        if(nonNull(seedingHearing.getSittingDay())) {
            assertThat(actualSeedingHearing.getSittingDay(), is(seedingHearing.getSittingDay()));
        }
        else{
            assertThat(actualSeedingHearing.getSittingDay(), is(nullValue()));
        }

    }

    private void assertListedCaseIdentifier(final ListedCase actualListedCase, final ProsecutionCase commandProsecutionCases) {
        assertThat(actualListedCase.getCaseIdentifier().getAuthorityCode(), is(commandProsecutionCases.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(actualListedCase.getCaseIdentifier().getAuthorityId(), is(commandProsecutionCases.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(actualListedCase.getCaseIdentifier().getCaseReference(), is(commandProsecutionCases.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));
    }

    private void assertJudicialRole(final HearingListingNeeds commandHearing, final uk.gov.moj.cpp.listing.domain.Hearing actual) {
        JudicialRole actualJudicialRole = actual.getJudiciary().get(0);
        uk.gov.justice.core.courts.JudicialRole commandJudicialRole = commandHearing.getJudiciary().get(0);

        assertThat(actualJudicialRole.getIsBenchChairman().get(), is(commandJudicialRole.getIsBenchChairman()));
        assertThat(actualJudicialRole.getIsDeputy().get(), is(commandJudicialRole.getIsDeputy()));
        assertThat(actualJudicialRole.getJudicialId(), is(commandJudicialRole.getJudicialId()));
        assertThat(actualJudicialRole.getUserId(), is(commandJudicialRole.getUserId()));
        assertThat(actualJudicialRole.getJudicialRoleType().getJudiciaryType(), is(commandJudicialRole.getJudicialRoleType().getJudiciaryType()));
        assertThat(actualJudicialRole.getJudicialRoleType().getJudicialRoleTypeId().orElse(null), is(commandJudicialRole.getJudicialRoleType().getJudicialRoleTypeId()));
    }

    @Test
    public void shouldConvertHearingCommandToHearingDomainWithLegalEntity() {

        //given
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingWithLegalEntity();

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing);

        //then
        assertThat(actual.getId(), is(commandHearing.getId()));
        assertThat(actual.getNonDefaultDays().get(0).getRoomId(), notNullValue());
        assertThat(actual.getNonDefaultDays().get(0).getCourtCentreId(), notNullValue());
        assertThat(actual.getJurisdictionType().name(), is(commandHearing.getJurisdictionType().name()));
        assertCourtApplications(commandHearing, actual);
        assertThat(actual.getCourtApplications().get(0).getApplicant().getId(),
                is(commandHearing.getCourtApplications().get(0).getApplicant().getId()));
        assertThat(actual.getListedCases().get(0).getDefendants().get(0).getOrganisationName().get(),
                is(commandHearing.getProsecutionCases().get(0).getDefendants().get(0).getLegalEntityDefendant().getOrganisation().getName()));


    }

    @Test
    public void shouldSetIsPossibleDisqualificationFlagWhenListingReasonForDisqualification(){
        //given
        final UUID defendantId = UUID.randomUUID();
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingWithMultipleOffences();
        commandHearing.getDefendantListingNeeds().add(DefendantListingNeeds.defendantListingNeeds()
                        .withDefendantId(defendantId)
                        .withListingReason(REFERRAL_REASON_FOR_DISQUALIFICATION).build());
        List<UUID> shadowListedOffences = Collections.singletonList(UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"));

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing, Collections.emptyList(), shadowListedOffences);

        //then
        assertThat(actual.getIsPossibleDisqualification().isPresent(), is(true));
        assertThat(actual.getIsPossibleDisqualification().get(), is(Boolean.TRUE));
    }

    @Test
    public void shouldNotSetIsPossibleDisqualificationFlagWhenListingReasonNotForDisqualification(){
        //given
        final UUID defendantId = UUID.randomUUID();
        HearingListingNeeds commandHearing = commandBuilder.buildCommandHearingWithMultipleOffences();
        commandHearing.getDefendantListingNeeds().add(DefendantListingNeeds.defendantListingNeeds()
                        .withDefendantId(defendantId)
                        .withListingReason("Random reason").build());
        List<UUID> shadowListedOffences = Collections.singletonList(UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1"));

        //when
        uk.gov.moj.cpp.listing.domain.Hearing actual = commandToDomainConverter.convert(commandHearing, Collections.emptyList(), shadowListedOffences);

        //then
        assertThat(actual.getIsPossibleDisqualification().isPresent(), is(false));
    }


}