package uk.gov.moj.cpp.listing.command.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.util.Collections;

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
    @InjectMocks
    JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

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

        assertThat(actual.getProsecutorDatesToAvoid(), is(commandHearing.getProsecutorDatesToAvoid()));
        assertThat(actual.getNonDefaultDays(), is(Collections.emptyList()));
        assertThat(actual.getListingDirections(), is(commandHearing.getListingDirections()));
        assertThat(actual.getType().getId(), is(commandHearing.getType().getId()));
        assertThat(actual.getType().getDescription(), is(commandHearing.getType().getDescription()));
        assertThat(actual.getCourtRoomId(), is(commandHearing.getCourtCentre().getRoomId()));
        assertThat(actual.getCourtCentreId(), is(commandHearing.getCourtCentre().getId()));
        assertThat(actual.getEstimatedMinutes(), is(commandHearing.getEstimatedMinutes()));
        assertThat(actual.getStartDate(), is(commandHearing.getEarliestStartDateTime().toLocalDate()));
        assertThat(actual.getEndDate(), is(commandHearing.getEndDate()));
        assertThat(actual.getReportingRestrictionReason(), is(commandHearing.getReportingRestrictionReason()));

    }

    private void assertListedCases(HearingListingNeeds commandHearing, uk.gov.moj.cpp.listing.domain.Hearing actual) {
        ListedCase actualListedCase = actual.getListedCases().get(0);
        ProsecutionCase commandProsecutionCase = commandHearing.getProsecutionCases().get(0);
        DefendantListingNeeds commandListDefendantRequests = commandHearing.getDefendantListingNeeds().get(0);

        assertThat(actualListedCase.getId(), is(commandProsecutionCase.getId()));
        assertListedCaseIdentifier(actualListedCase, commandProsecutionCase);

        assertDefendant(actualListedCase, commandProsecutionCase, commandListDefendantRequests);


    }

    private void assertDefendant(ListedCase actualListedCase, ProsecutionCase commandProsecutionCase, DefendantListingNeeds commandListDefendantRequests) {
        Defendant actualDefendant = actualListedCase.getDefendants().get(0);
        uk.gov.justice.core.courts.Defendant commandDefendant = commandProsecutionCase.getDefendants().get(0);

        Defendant actualDefendant2 = actualListedCase.getDefendants().get(1);
        uk.gov.justice.core.courts.Defendant commandDefendant2 = commandProsecutionCase.getDefendants().get(1);

        assertThat(actualDefendant.getId(), is(commandDefendant.getId()));
        assertThat(actualDefendant.getBailStatus().get().toString(), is(commandDefendant.getPersonDefendant().get().getBailStatus().get().toString()));
        assertThat(actualDefendant.getFirstName(), is(commandDefendant.getPersonDefendant().get().getPersonDetails().getFirstName()));
        assertThat(actualDefendant.getLastName().get(), is(commandDefendant.getPersonDefendant().get().getPersonDetails().getLastName()));
        assertThat(actualDefendant.getDefenceOrganisation().get(), is(commandDefendant.getDefenceOrganisation().get().getName()));
        assertThat(actualDefendant.getSpecificRequirements(), is(commandDefendant.getPersonDefendant().get().getPersonDetails().getSpecificRequirements()));
        assertThat(actualDefendant.getDateOfBirth(), is(commandDefendant.getPersonDefendant().get().getPersonDetails().getDateOfBirth()));
        assertThat(actualDefendant.getCustodyTimeLimit(),is(commandDefendant.getPersonDefendant().get().getCustodyTimeLimit()));
        assertThat(actualDefendant.getHearingLanguageNeeds().get().toString(), is(commandListDefendantRequests.getHearingLanguageNeeds().get().toString()));
        assertThat(actualDefendant.getDatesToAvoid(), is(commandListDefendantRequests.getDatesToAvoid()));


        assertThat(actualDefendant2.getOrganisationName().get(), is(commandDefendant2.getLegalEntityDefendant().get().getOrganisation().getName()));


        assertOffence(actualDefendant, commandDefendant);
    }

    private void assertOffence(Defendant actualDefendant, uk.gov.justice.core.courts.Defendant commandDefendant) {
        Offence actualOffence = actualDefendant.getOffences().get(0);
        uk.gov.justice.core.courts.Offence commandoffence = commandDefendant.getOffences().get(0);

        assertThat(actualOffence.getId(), is(commandoffence.getId()));
        assertThat(actualOffence.getStartDate(), is(commandoffence.getStartDate()));
        assertThat(actualOffence.getEndDate(), is(commandoffence.getEndDate()));
        assertThat(actualOffence.getOffenceCode(), is(commandoffence.getOffenceCode()));

        assertStatementOfOFfence(actualOffence, commandoffence);
    }

    private void assertStatementOfOFfence(Offence actualOffence, uk.gov.justice.core.courts.Offence commandoffence) {
        StatementOfOffence actualStatementOfOffence = actualOffence.getStatementOfOffence();
        assertThat(actualStatementOfOffence.getTitle(), is(commandoffence.getOffenceTitle()));
        assertThat(actualStatementOfOffence.getWelshTitle(), is(commandoffence.getOffenceTitleWelsh().get()));
        assertThat(actualStatementOfOffence.getLegislation().get(), is(commandoffence.getOffenceLegislation().get()));
        assertThat(actualStatementOfOffence.getWelshLegislation().get(), is(commandoffence.getOffenceLegislationWelsh().get()));
    }

    private void assertListedCaseIdentifier(ListedCase actualListedCase, ProsecutionCase commandProsecutionCases) {
        assertThat(actualListedCase.getCaseIdentifier().getAuthorityCode(), is(commandProsecutionCases.getProsecutionCaseIdentifier().getProsecutionAuthorityCode()));
        assertThat(actualListedCase.getCaseIdentifier().getAuthorityId(), is(commandProsecutionCases.getProsecutionCaseIdentifier().getProsecutionAuthorityId()));
        assertThat(actualListedCase.getCaseIdentifier().getCaseReference(), is(commandProsecutionCases.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()));
    }

    private void assertJudicialRole(HearingListingNeeds commandHearing, uk.gov.moj.cpp.listing.domain.Hearing actual) {
        JudicialRole actualJudicialRole = actual.getJudiciary().get(0);
        uk.gov.justice.core.courts.JudicialRole commandJudicialRole = commandHearing.getJudiciary().get(0);

        assertThat(actualJudicialRole.getIsBenchChairman(), is(commandJudicialRole.getIsBenchChairman()));
        assertThat(actualJudicialRole.getIsDeputy(), is(commandJudicialRole.getIsDeputy()));
        assertThat(actualJudicialRole.getJudicialId(), is(commandJudicialRole.getJudicialId()));
        assertThat(actualJudicialRole.getJudicialRoleType().getJudiciaryType(), is(commandJudicialRole.getJudicialRoleType().getJudiciaryType()));
        assertThat(actualJudicialRole.getJudicialRoleType().getJudicialRoleTypeId(), is(commandJudicialRole.getJudicialRoleType().getJudicialRoleTypeId()));
    }

}