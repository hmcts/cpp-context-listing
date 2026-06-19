package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.listing.domain.Prosecutor;

import java.util.List;
import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class EventAggregateConverterTest {

    private final UUID defendantId = UUID.randomUUID();
    private final UUID masterDefendantId = UUID.randomUUID();
    private final UUID bailStatusId = UUID.randomUUID();
    private final UUID listedCaseId = UUID.randomUUID();
    private final UUID authorityId = UUID.randomUUID();
    private final UUID markedId = UUID.randomUUID();
    private final UUID markerTypeId = UUID.randomUUID();
    private final UUID offenceId = UUID.randomUUID();
    private final UUID laaReferenceStatusId = UUID.randomUUID();
    private final UUID PROSECUTOR_ID = UUID.randomUUID();
    private final String PROSECUTOR_CODE = "CPS-SW";
    private final String PROSECUTOR_NAME = "Prosecution South West";


    @Test
    public void shouldConvertEventListedCaseToAggregateListedCase() {

        final uk.gov.moj.cpp.listing.domain.aggregate.ListedCase result = EventAggregateConverter.buildAggregateListedCase(createEventListedCase());
        final uk.gov.moj.cpp.listing.domain.aggregate.ListedCase expected = createAggregateListedCase();

        Assert.assertThat(result, is(expected));

    }

    @Test
    public void shouldConvertAggregateListedCaseToEventListedCase() {

        final uk.gov.justice.listing.events.ListedCase result = EventAggregateConverter.buildEventListedCase(createAggregateListedCase());
        final uk.gov.justice.listing.events.ListedCase expected = createEventListedCase();

        MatcherAssert.assertThat(result, equalTo(expected));
    }

    @Test
    public void shouldConvertAggregateListedCaseToEventListedCaseWithProsecutor() {

        final uk.gov.justice.listing.events.ListedCase result = EventAggregateConverter.buildEventListedCase(createAggregateListedCaseWithProsecutor());
        final uk.gov.justice.listing.events.ListedCase expected = createEventListedCaseWithProsecutor();

        MatcherAssert.assertThat(result, equalTo(expected));
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.ListedCase createAggregateListedCase() {
        return uk.gov.moj.cpp.listing.domain.aggregate.ListedCase.listedCase()
                .withId(listedCaseId)
                .withDefendants(createAggregateDefendants())
                .withCaseMarkers(createAggregateCaseMarkers())
                .withCaseIdentifier(createAggregateCaseIdentifier())
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.ListedCase createAggregateListedCaseWithProsecutor() {
        return uk.gov.moj.cpp.listing.domain.aggregate.ListedCase.listedCase()
                .withValuesFrom(createAggregateListedCase())
                .withProsecutor(Prosecutor.prosecutor()
                        .withProsecutorId(PROSECUTOR_ID)
                        .withProsecutorCode(PROSECUTOR_CODE)
                        .withProsecutorName(PROSECUTOR_NAME)
                        .build())
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.CaseIdentifier createAggregateCaseIdentifier() {
        return uk.gov.moj.cpp.listing.domain.CaseIdentifier.caseIdentifier()
                .withCaseReference("Case Reference")
                .withAuthorityCode("Case Authority Code")
                .withAuthorityId(authorityId)
                .build();
    }

    private List<uk.gov.moj.cpp.listing.domain.aggregate.CaseMarker> createAggregateCaseMarkers() {
        final uk.gov.moj.cpp.listing.domain.aggregate.CaseMarker markers = uk.gov.moj.cpp.listing.domain.aggregate.CaseMarker.caseMarker()
                .withId(markedId)
                .withMarkerTypeid(markerTypeId)
                .withMarkerTypeDescription("Marker Type Desc")
                .withMarkerTypeCode("Marker Type Code")
                .build();
        return singletonList(markers);
    }


    private List<uk.gov.moj.cpp.listing.domain.aggregate.Defendant> createAggregateDefendants() {
        final uk.gov.moj.cpp.listing.domain.aggregate.Defendant defendant = uk.gov.moj.cpp.listing.domain.aggregate.Defendant.defendant()
                .withProceedingsConcluded(true)
                .withAssociatedDefenceOrganisation(createAggregateAssociatedDefenceOrganisation())
                .withNationalityDescription("Nationality Desc")
                .withLegalAidStatus("LegalAidStatus")
                .withIsYouth(true)
                .withRestrictFromCourtList(false)
                .withAddress(createAggregateAddressForDefendant())
                .withOrganisationName("Org Name")
                .withLastName("Last Name")
                .withDateOfBirth("11/12/1983")
                .withDefenceOrganisation("Defence Org")
                .withCustodyTimeLimit("Custody Time Limit")
                .withDatesToAvoid("Dates to avoid")
                .withSpecificRequirements("Specific Reqs")
                .withMasterDefendantId(masterDefendantId)
                .withFirstName("Defendant First Name")
                .withCourtProceedingsInitiated(ZonedDateTimes.fromString("2020-08-12T11:03:25.000Z"))
                .withId(defendantId)
                .withBailStatus(createAggregateBailStatus())
                .withHearingLanguageNeeds(HearingLanguage.ENGLISH)
                .withOffences(createAggregateOffences())
                .build();
        return singletonList(defendant);
    }

    private List<uk.gov.moj.cpp.listing.domain.aggregate.Offence> createAggregateOffences() {
        final uk.gov.moj.cpp.listing.domain.aggregate.Offence offence = uk.gov.moj.cpp.listing.domain.aggregate.Offence.offence()
                .withLaaApplnReference(createAggregateLaaReference())
                .withRestrictFromCourtList(true)
                .withLaidDate("Laid Date")
                .withOffenceWording("Offence Wording")
                .withStartDate("Start Date")
                .withEndDate("End Date")
                .withOffenceCode("Offence Code")
                .withId(offenceId)
                .withStatementOfOffence(createAggregateStatementOfOffence())
                .build();
        return singletonList(offence);
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.StatementOfOffence createAggregateStatementOfOffence() {
        return uk.gov.moj.cpp.listing.domain.aggregate.StatementOfOffence.statementOfOffence()
                .withTitle("Title")
                .withLegislation("Legislation")
                .withWelshTitle("Welsh Title")
                .withWelshLegislation("Welsh Legislation")
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.LaaReference createAggregateLaaReference() {
        return uk.gov.moj.cpp.listing.domain.aggregate.LaaReference.laaReference()
                .withEffectiveEndDate("01/02/2023")
                .withEffectiveStartDate("01/05/2020")
                .withApplicationReference("App Reference")
                .withStatusDate("Status Date")
                .withStatusDescription("Status Desc")
                .withStatusCode("Status Code")
                .withStatusId(laaReferenceStatusId)
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.BailStatus createAggregateBailStatus() {
        return uk.gov.moj.cpp.listing.domain.aggregate.BailStatus.bailStatus()
                .withCustodyTimeLimit(createAggregateCustodyTimeLimit())
                .withDescription("Bail Status Desc")
                .withCode("Bail Status Code")
                .withId(bailStatusId)
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.CustodyTimeLimit createAggregateCustodyTimeLimit() {
        return uk.gov.moj.cpp.listing.domain.aggregate.CustodyTimeLimit.custodyTimeLimit()
                .withTimeLimit("CTL")
                .withDaysSpent(10)
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.AssociatedDefenceOrganisation createAggregateAssociatedDefenceOrganisation() {

        return uk.gov.moj.cpp.listing.domain.aggregate.AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withDefenceOrganisation(createAggregateDefenceOrganisation())
                .withFundingType(FundingType.PRIVATE)
                .withIsAssociatedByLAA(true)
                .withAssociationEndDate("Association End Date")
                .withAssociationStartDate("Association Start Date")
                .withApplicationReference("Application Reference")
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.DefenceOrganisation createAggregateDefenceOrganisation() {
        return uk.gov.moj.cpp.listing.domain.aggregate.DefenceOrganisation.defenceOrganisation()
                .withOrganisation(createAggregateOrganisation())
                .withLaaContractNumber("LAA Contract Number")
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.Organisation createAggregateOrganisation() {
        return uk.gov.moj.cpp.listing.domain.aggregate.Organisation.organisation()
                .withAddress(createAggregateAddressForOrganisation())
                .withContact(createAggregateContactNumber())
                .withRegisteredCharityNumber("Reg Charity No")
                .withIncorporationNumber("Incorp Number")
                .withName("Contact Name")
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.Address createAggregateAddressForDefendant() {
        return uk.gov.moj.cpp.listing.domain.aggregate.Address.address()
                .withAddress1("Address Line 1")
                .withAddress2("Address Line 2")
                .withAddress3("Address Line 3")
                .withAddress4("Address Line 4")
                .withAddress5("Address Line 5")
                .withWelshAddress1("Welsh Address Line 1")
                .withWelshAddress2("Welsh Address Line 2")
                .withWelshAddress3("Welsh Address Line 3")
                .withWelshAddress4("Welsh Address Line 4")
                .withWelshAddress5("Welsh Address Line 5")
                .withPostcode("PostCode")
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.Address createAggregateAddressForOrganisation() {
        return uk.gov.moj.cpp.listing.domain.aggregate.Address.address()
                .withAddress1("Org Address Line 1")
                .withAddress2("Org Address Line 2")
                .withAddress3("Org Address Line 3")
                .withAddress4("Org Address Line 4")
                .withAddress5("Org Address Line 5")
                .withWelshAddress1("Org Welsh Address Line 1")
                .withWelshAddress2("Org Welsh Address Line 2")
                .withWelshAddress3("Org Welsh Address Line 3")
                .withWelshAddress4("Org Welsh Address Line 4")
                .withWelshAddress5("Org Welsh Address Line 5")
                .withPostcode("Org PostCode")
                .build();
    }

    private uk.gov.moj.cpp.listing.domain.aggregate.ContactNumber createAggregateContactNumber() {
        return uk.gov.moj.cpp.listing.domain.aggregate.ContactNumber.contactNumber()
                .withHome("Home Phone")
                .withWork("Work Phone")
                .withMobile("Mobile Phone")
                .withFax("Fax")
                .withPrimaryEmail("primary@email.com")
                .withSecondaryEmail("secondary@email.com")
                .build();
    }

    private uk.gov.justice.listing.events.ListedCase createEventListedCase() {
        return uk.gov.justice.listing.events.ListedCase.listedCase()
                .withId(listedCaseId)
                .withDefendants(createEventDefendants())
                .withMarkers(createEventMarkers())
                .withCaseIdentifier(createEventCaseIdentifier())
                .build();
    }

    private uk.gov.justice.listing.events.ListedCase createEventListedCaseWithProsecutor() {
        return uk.gov.justice.listing.events.ListedCase.listedCase()
                .withValuesFrom(createEventListedCase())
                .withProsecutor(uk.gov.justice.listing.events.Prosecutor.prosecutor()
                        .withProsecutorId(PROSECUTOR_ID)
                        .withProsecutorCode(PROSECUTOR_CODE)
                        .withProsecutorName(PROSECUTOR_NAME)
                        .build())
                .build();
    }

    private CaseIdentifier createEventCaseIdentifier() {
        return CaseIdentifier.caseIdentifier()
                .withCaseReference("Case Reference")
                .withAuthorityCode("Case Authority Code")
                .withAuthorityId(authorityId)
                .build();
    }

    private List<Marker> createEventMarkers() {
        final uk.gov.justice.listing.events.Marker markers = uk.gov.justice.listing.events.Marker.marker()
                .withId(markedId)
                .withMarkerTypeid(markerTypeId)
                .withMarkerTypeDescription("Marker Type Desc")
                .withMarkerTypeCode("Marker Type Code")
                .build();
        return singletonList(markers);
    }


    private List<Defendant> createEventDefendants() {
        final uk.gov.justice.listing.events.Defendant defendant = Defendant.defendant()
                .withProceedingsConcluded(true)
                .withAssociatedDefenceOrganisation(createEventAssociatedDefenceOrganisation())
                .withNationalityDescription("Nationality Desc")
                .withLegalAidStatus("LegalAidStatus")
                .withIsYouth(true)
                .withRestrictFromCourtList(false)
                .withAddress(createEventAddress())
                .withOrganisationName("Org Name")
                .withLastName("Last Name")
                .withDateOfBirth("11/12/1983")
                .withDefenceOrganisation("Defence Org")
                .withCustodyTimeLimit("Custody Time Limit")
                .withDatesToAvoid("Dates to avoid")
                .withSpecificRequirements("Specific Reqs")
                .withMasterDefendantId(masterDefendantId)
                .withFirstName("Defendant First Name")
                .withCourtProceedingsInitiated(ZonedDateTimes.fromString("2020-08-12T11:03:25.000Z"))
                .withId(defendantId)
                .withBailStatus(createEventBailStatus())
                .withHearingLanguageNeeds(HearingLanguage.ENGLISH)
                .withOffences(createEventOffences())
                .build();
        return singletonList(defendant);
    }

    private List<uk.gov.justice.listing.events.Offence> createEventOffences() {
        final uk.gov.justice.listing.events.Offence offence = uk.gov.justice.listing.events.Offence.offence()
                .withLaaApplnReference(createEventLaaReference())
                .withRestrictFromCourtList(true)
                .withLaidDate("Laid Date")
                .withOffenceWording("Offence Wording")
                .withStartDate("Start Date")
                .withEndDate("End Date")
                .withOffenceCode("Offence Code")
                .withId(offenceId)
                .withStatementOfOffence(createEventStatementOfOffence())
                .build();
        return singletonList(offence);
    }

    private uk.gov.justice.listing.events.StatementOfOffence createEventStatementOfOffence() {
        return uk.gov.justice.listing.events.StatementOfOffence.statementOfOffence()
                .withTitle("Title")
                .withLegislation("Legislation")
                .withWelshTitle("Welsh Title")
                .withWelshLegislation("Welsh Legislation")
                .build();
    }

    private uk.gov.justice.listing.events.LaaReference createEventLaaReference() {
        return uk.gov.justice.listing.events.LaaReference.laaReference()
                .withEffectiveEndDate("01/02/2023")
                .withEffectiveStartDate("01/05/2020")
                .withApplicationReference("App Reference")
                .withStatusDate("Status Date")
                .withStatusDescription("Status Desc")
                .withStatusCode("Status Code")
                .withStatusId(laaReferenceStatusId)
                .build();
    }

    private uk.gov.justice.core.courts.BailStatus createEventBailStatus() {
        return uk.gov.justice.core.courts.BailStatus.bailStatus()
                .withCustodyTimeLimit(createEventCustodyTimeLimit())
                .withDescription("Bail Status Desc")
                .withCode("Bail Status Code")
                .withId(bailStatusId)
                .build();
    }

    private uk.gov.justice.core.courts.CustodyTimeLimit createEventCustodyTimeLimit() {
        return uk.gov.justice.core.courts.CustodyTimeLimit.custodyTimeLimit()
                .withTimeLimit("CTL")
                .withDaysSpent(10)
                .build();
    }

    private uk.gov.justice.core.courts.AssociatedDefenceOrganisation createEventAssociatedDefenceOrganisation() {

        return uk.gov.justice.core.courts.AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withDefenceOrganisation(createEventDefenceOrganisation())
                .withFundingType(FundingType.PRIVATE)
                .withIsAssociatedByLAA(true)
                .withAssociationEndDate("Association End Date")
                .withAssociationStartDate("Association Start Date")
                .withApplicationReference("Application Reference")
                .build();
    }

    private uk.gov.justice.core.courts.DefenceOrganisation createEventDefenceOrganisation() {
        return uk.gov.justice.core.courts.DefenceOrganisation.defenceOrganisation()
                .withOrganisation(createEventOrganisation())
                .withLaaContractNumber("LAA Contract Number")
                .build();
    }

    private uk.gov.justice.core.courts.Organisation createEventOrganisation() {
        return uk.gov.justice.core.courts.Organisation.organisation()
                .withAddress(createEventAddressForOrganisation())
                .withContact(createEventContactNumber())
                .withRegisteredCharityNumber("Reg Charity No")
                .withIncorporationNumber("Incorp Number")
                .withName("Contact Name")
                .build();
    }

    private uk.gov.justice.core.courts.Address createEventAddress() {
        return uk.gov.justice.core.courts.Address.address()
                .withAddress1("Address Line 1")
                .withAddress2("Address Line 2")
                .withAddress3("Address Line 3")
                .withAddress4("Address Line 4")
                .withAddress5("Address Line 5")
                .withWelshAddress1("Welsh Address Line 1")
                .withWelshAddress2("Welsh Address Line 2")
                .withWelshAddress3("Welsh Address Line 3")
                .withWelshAddress4("Welsh Address Line 4")
                .withWelshAddress5("Welsh Address Line 5")
                .withPostcode("PostCode")
                .build();
    }

    private uk.gov.justice.core.courts.Address createEventAddressForOrganisation() {
        return uk.gov.justice.core.courts.Address.address()
                .withAddress1("Org Address Line 1")
                .withAddress2("Org Address Line 2")
                .withAddress3("Org Address Line 3")
                .withAddress4("Org Address Line 4")
                .withAddress5("Org Address Line 5")
                .withWelshAddress1("Org Welsh Address Line 1")
                .withWelshAddress2("Org Welsh Address Line 2")
                .withWelshAddress3("Org Welsh Address Line 3")
                .withWelshAddress4("Org Welsh Address Line 4")
                .withWelshAddress5("Org Welsh Address Line 5")
                .withPostcode("Org PostCode")
                .build();
    }

    private uk.gov.justice.core.courts.ContactNumber createEventContactNumber() {
        return uk.gov.justice.core.courts.ContactNumber.contactNumber()
                .withHome("Home Phone")
                .withWork("Work Phone")
                .withMobile("Mobile Phone")
                .withFax("Fax")
                .withPrimaryEmail("primary@email.com")
                .withSecondaryEmail("secondary@email.com")
                .build();
    }
}
