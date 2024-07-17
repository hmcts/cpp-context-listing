package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.Prosecutor;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize", "squid:S3655", "squid:S1067", "PMD.NullAssignment"})
public final class EventAggregateConverter {

    private EventAggregateConverter() {
    }

    public static uk.gov.justice.listing.events.ListedCase buildEventListedCase(final ListedCase listedCase) {
        final uk.gov.justice.listing.events.ListedCase.Builder builder =  uk.gov.justice.listing.events.ListedCase.listedCase().withId(listedCase.getId())
                .withIsCivil(listedCase.getIsCivil())
                .withGroupId(listedCase.getGroupId())
                .withIsGroupMember(listedCase.getIsGroupMember())
                .withIsGroupMaster(listedCase.getIsGroupMaster())
                .withDefendants(listedCase.getDefendants().stream().map(EventAggregateConverter::buildEventDefendant).collect(toList()))
                .withCaseIdentifier(nonNull(listedCase.getCaseIdentifier()) ? buildEventCaseIdentifier(listedCase.getCaseIdentifier()) : null)
                .withMarkers(listedCase.getCaseMarkers().stream().map(EventAggregateConverter::buildEventCaseMarker).collect(toList()));

        if(nonNull(listedCase.getProsecutor())){
            builder.withProsecutor(buildEventProsecutor(listedCase.getProsecutor()));
        }
        return builder.build();
    }

    public static uk.gov.justice.listing.events.Marker buildEventCaseMarker(final CaseMarker caseMarker) {
        return uk.gov.justice.listing.events.Marker.marker()
                .withId(caseMarker.getId())
                .withMarkerTypeid(caseMarker.getMarkerTypeid())
                .withMarkerTypeCode(caseMarker.getMarkerTypeCode())
                .withMarkerTypeDescription(caseMarker.getMarkerTypeDescription())
                .build();
    }

    public static uk.gov.justice.listing.events.Defendant buildEventDefendant(final Defendant defendant) {
        final HearingLanguage hearingLanguageNeeds = defendant.getHearingLanguageNeeds();
        return uk.gov.justice.listing.events.Defendant.defendant()
                .withId(defendant.getId())
                .withOffences(defendant.getOffences().stream().map(EventAggregateConverter::buildEventOffence).collect(toList()))
                .withBailStatus(nonNull(defendant.getBailStatus()) ? buildEventBailStatus(defendant.getBailStatus()) : null)
                .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                .withFirstName(defendant.getFirstName())
                .withLastName(defendant.getLastName())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withSpecificRequirements(defendant.getSpecificRequirements())
                .withDatesToAvoid(defendant.getDatesToAvoid())
                .withCustodyTimeLimit(defendant.getCustodyTimeLimit())
                .withDateOfBirth(defendant.getDateOfBirth())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withOrganisationName(defendant.getOrganisationName())
                .withAddress(nonNull(defendant.getAddress()) ? buildEventAddress(defendant.getAddress()) : null)
                .withRestrictFromCourtList(defendant.getRestrictFromCourtList())
                .withHearingLanguageNeeds(hearingLanguageNeeds)
                .withIsYouth(defendant.getIsYouth())
                .withLegalAidStatus(defendant.getLegalAidStatus())
                .withNationalityDescription(defendant.getNationalityDescription())
                .withAssociatedDefenceOrganisation(nonNull(defendant.getAssociatedDefenceOrganisation()) ? buildEventAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation()) : null)
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .build();
    }

    public static uk.gov.justice.core.courts.AssociatedDefenceOrganisation buildEventAssociatedDefenceOrganisation(final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        return uk.gov.justice.core.courts.AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withApplicationReference(associatedDefenceOrganisation.getApplicationReference())
                .withAssociationStartDate(associatedDefenceOrganisation.getAssociationStartDate())
                .withAssociationEndDate(associatedDefenceOrganisation.getAssociationEndDate())
                .withIsAssociatedByLAA(associatedDefenceOrganisation.getIsAssociatedByLAA())
                .withDefenceOrganisation(nonNull(associatedDefenceOrganisation.getDefenceOrganisation()) ? buildEventDefenceOrganisation(associatedDefenceOrganisation.getDefenceOrganisation()) : null)
                .withFundingType(associatedDefenceOrganisation.getFundingType())
                .build();
    }

    public static uk.gov.justice.core.courts.DefenceOrganisation buildEventDefenceOrganisation(final DefenceOrganisation defenceOrganisation) {
        return uk.gov.justice.core.courts.DefenceOrganisation.defenceOrganisation()
                .withLaaContractNumber(defenceOrganisation.getLaaContractNumber())
                .withOrganisation(nonNull(defenceOrganisation.getOrganisation()) ? buildEventOrganisation(defenceOrganisation.getOrganisation()) : null)
                .build();
    }

    public static uk.gov.justice.core.courts.Organisation buildEventOrganisation(final Organisation organisation) {
        return uk.gov.justice.core.courts.Organisation.organisation()
                .withName(organisation.getName())
                .withIncorporationNumber(organisation.getIncorporationNumber())
                .withRegisteredCharityNumber(organisation.getRegisteredCharityNumber())
                .withAddress(nonNull(organisation.getAddress()) ? buildEventAddress(organisation.getAddress()) : null)
                .withContact(nonNull(organisation.getContact()) ? buildEventContactNumber(organisation.getContact()) : null)
                .build();
    }


    public static uk.gov.justice.core.courts.ContactNumber buildEventContactNumber(final ContactNumber contactNumber) {
        return uk.gov.justice.core.courts.ContactNumber.contactNumber()
                .withHome(contactNumber.getHome())
                .withMobile(contactNumber.getMobile())
                .withWork(contactNumber.getWork())
                .withFax(contactNumber.getFax())
                .withPrimaryEmail(contactNumber.getPrimaryEmail())
                .withSecondaryEmail(contactNumber.getSecondaryEmail())
                .build();
    }


    public static uk.gov.justice.core.courts.Address buildEventAddress(final Address address) {
        return uk.gov.justice.core.courts.Address.address()
                .withAddress1(address.getAddress1())
                .withAddress2(address.getAddress2())
                .withAddress3(address.getAddress3())
                .withAddress4(address.getAddress4())
                .withAddress5(address.getAddress5())
                .withWelshAddress1(address.getWelshAddress1())
                .withWelshAddress2(address.getWelshAddress2())
                .withWelshAddress3(address.getWelshAddress3())
                .withWelshAddress4(address.getWelshAddress4())
                .withWelshAddress5(address.getWelshAddress5())
                .withPostcode(address.getPostcode())
                .build();
    }

    public static uk.gov.justice.core.courts.BailStatus buildEventBailStatus(final BailStatus bailStatus) {
        return uk.gov.justice.core.courts.BailStatus.bailStatus()
                .withId(bailStatus.getId())
                .withCode(bailStatus.getCode())
                .withDescription(bailStatus.getDescription())
                .withCustodyTimeLimit(nonNull(bailStatus.getCustodyTimeLimit()) ? buildEventCustodyTimeLimit(bailStatus.getCustodyTimeLimit()) : null)
                .build();
    }

    public static uk.gov.justice.core.courts.CustodyTimeLimit buildEventCustodyTimeLimit(final CustodyTimeLimit custodyTimeLimit) {
        return uk.gov.justice.core.courts.CustodyTimeLimit.custodyTimeLimit()
                .withDaysSpent(custodyTimeLimit.getDaysSpent())
                .withTimeLimit(custodyTimeLimit.getTimeLimit())
                .build();
    }

    public static uk.gov.justice.listing.events.Offence buildEventOffence(final Offence offence) {
        return uk.gov.justice.listing.events.Offence.offence()
                .withId(offence.getId())
                .withOffenceCode(offence.getOffenceCode())
                .withStartDate(offence.getStartDate())
                .withEndDate(offence.getEndDate())
                .withStatementOfOffence(nonNull(offence.getStatementOfOffence()) ? buildEventStatementOfOffence(offence.getStatementOfOffence()) : null)
                .withOffenceWording(offence.getOffenceWording())
                .withLaidDate(offence.getLaidDate())
                .withLaaApplnReference(nonNull(offence.getLaaApplnReference()) ? buildEventLaaReference(offence.getLaaApplnReference()) : null)
                .withRestrictFromCourtList(offence.getRestrictFromCourtList())
                .build();
    }



    public static uk.gov.justice.listing.events.LaaReference buildEventLaaReference(final LaaReference laaReference) {
        return uk.gov.justice.listing.events.LaaReference.laaReference()
                .withStatusId(laaReference.getStatusId())
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusDate(laaReference.getStatusDate())
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveStartDate(laaReference.getEffectiveStartDate())
                .withEffectiveEndDate(laaReference.getEffectiveEndDate())
                .build();
    }

    public static uk.gov.justice.listing.events.StatementOfOffence buildEventStatementOfOffence(final StatementOfOffence statementOfOffence) {
        return uk.gov.justice.listing.events.StatementOfOffence.statementOfOffence()
                .withTitle(statementOfOffence.getTitle())
                .withLegislation(statementOfOffence.getLegislation())
                .withWelshTitle(statementOfOffence.getWelshTitle())
                .withWelshLegislation(statementOfOffence.getWelshLegislation())
                .build();
    }

    public static uk.gov.justice.listing.events.CaseIdentifier buildEventCaseIdentifier(final CaseIdentifier caseIdentifier) {
        return uk.gov.justice.listing.events.CaseIdentifier.caseIdentifier()
                .withAuthorityCode(caseIdentifier.getAuthorityCode())
                .withAuthorityId(caseIdentifier.getAuthorityId())
                .withCaseReference(caseIdentifier.getCaseReference())
                .build();
    }

    public static uk.gov.justice.listing.events.Prosecutor buildEventProsecutor(final Prosecutor prosecutor) {
        return uk.gov.justice.listing.events.Prosecutor.prosecutor()
                .withProsecutorCode(prosecutor.getProsecutorCode())
                .withProsecutorId(prosecutor.getProsecutorId())
                .withProsecutorName(prosecutor.getProsecutorName())
                .build();
    }


    public static ListedCase buildAggregateListedCase(final uk.gov.justice.listing.events.ListedCase listedCase) {
        final uk.gov.moj.cpp.listing.domain.aggregate.ListedCase.Builder builder = uk.gov.moj.cpp.listing.domain.aggregate.ListedCase.listedCase()
                .withId(listedCase.getId())
                .withIsCivil(listedCase.getIsCivil())
                .withGroupId(listedCase.getGroupId())
                .withIsGroupMember(listedCase.getIsGroupMember())
                .withIsGroupMaster(listedCase.getIsGroupMaster())
                .withDefendants(listedCase.getDefendants().stream().map(EventAggregateConverter::buildAggregateDefendant).collect(toList()))
                .withCaseIdentifier(nonNull(listedCase.getCaseIdentifier()) ? buildAggregateCaseIdentifier(listedCase.getCaseIdentifier()) : null)
                .withCaseMarkers(listedCase.getMarkers().stream().map(EventAggregateConverter::buildAggregateCaseMarker).collect(toList()));
        if(nonNull(listedCase.getProsecutor())){
            builder.withProsecutor(buildAggregateProsecutor(listedCase.getProsecutor()));
        }
        return builder.build();
    }

    public static CaseMarker buildAggregateCaseMarker(final uk.gov.justice.listing.events.Marker caseMarker) {
        return uk.gov.moj.cpp.listing.domain.aggregate.CaseMarker.caseMarker()
                .withId(caseMarker.getId())
                .withMarkerTypeid(caseMarker.getMarkerTypeid())
                .withMarkerTypeCode(caseMarker.getMarkerTypeCode())
                .withMarkerTypeDescription(caseMarker.getMarkerTypeDescription())
                .build();
    }

    public static Defendant buildAggregateDefendant(final uk.gov.justice.listing.events.Defendant defendant) {
        return uk.gov.moj.cpp.listing.domain.aggregate.Defendant.defendant()
                .withId(defendant.getId())
                .withOffences(defendant.getOffences().stream().map(EventAggregateConverter::buildAggregateOffence).collect(toList()))
                .withBailStatus(defendant.getBailStatus() != null ? buildAggregateBailStatus(defendant.getBailStatus()) : null)
                .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                .withFirstName(defendant.getFirstName())
                .withLastName(defendant.getLastName())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withSpecificRequirements(defendant.getSpecificRequirements())
                .withDatesToAvoid(defendant.getDatesToAvoid())
                .withCustodyTimeLimit(defendant.getCustodyTimeLimit())
                .withDateOfBirth(defendant.getDateOfBirth())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withOrganisationName(defendant.getOrganisationName())
                .withAddress(defendant.getAddress() != null ? buildAggregateAddress(defendant.getAddress()) : null)
                .withRestrictFromCourtList(defendant.getRestrictFromCourtList())
                .withHearingLanguageNeeds(defendant.getHearingLanguageNeeds())
                .withIsYouth(defendant.getIsYouth())
                .withLegalAidStatus(defendant.getLegalAidStatus())
                .withNationalityDescription(defendant.getNationalityDescription())
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation() != null ? buildAggregateAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation()) : null)
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .build();
    }

    public static AssociatedDefenceOrganisation buildAggregateAssociatedDefenceOrganisation(final uk.gov.justice.core.courts.AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        return uk.gov.moj.cpp.listing.domain.aggregate.AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withApplicationReference(associatedDefenceOrganisation.getApplicationReference())
                .withAssociationStartDate(associatedDefenceOrganisation.getAssociationStartDate())
                .withAssociationEndDate(associatedDefenceOrganisation.getAssociationEndDate())
                .withIsAssociatedByLAA(associatedDefenceOrganisation.getIsAssociatedByLAA())
                .withDefenceOrganisation(nonNull(associatedDefenceOrganisation.getDefenceOrganisation()) ? buildAggregateDefenceOrganisation(associatedDefenceOrganisation.getDefenceOrganisation()) : null)
                .withFundingType(associatedDefenceOrganisation.getFundingType())
                .build();
    }

    public static DefenceOrganisation buildAggregateDefenceOrganisation(final uk.gov.justice.core.courts.DefenceOrganisation defenceOrganisation) {
        return uk.gov.moj.cpp.listing.domain.aggregate.DefenceOrganisation.defenceOrganisation()
                .withLaaContractNumber(defenceOrganisation.getLaaContractNumber())
                .withOrganisation(nonNull(defenceOrganisation.getOrganisation()) ? buildAggregateOrganisation(defenceOrganisation.getOrganisation()) : null)
                .build();
    }

    public static Organisation buildAggregateOrganisation(final uk.gov.justice.core.courts.Organisation organisation) {
        return uk.gov.moj.cpp.listing.domain.aggregate.Organisation.organisation()
                .withAddress(organisation.getAddress() != null ? buildAggregateAddress(organisation.getAddress()) : null)
                .withName(organisation.getName())
                .withContact(organisation.getContact() != null ? buildAggregateContactNumber(organisation.getContact()) : null)
                .withIncorporationNumber(organisation.getIncorporationNumber())
                .withRegisteredCharityNumber(organisation.getRegisteredCharityNumber())
                .build();
    }

    public static ContactNumber buildAggregateContactNumber(final uk.gov.justice.core.courts.ContactNumber contactNumber) {
        return uk.gov.moj.cpp.listing.domain.aggregate.ContactNumber.contactNumber()
                .withHome(contactNumber.getHome())
                .withWork(contactNumber.getWork())
                .withMobile(contactNumber.getMobile())
                .withFax(contactNumber.getFax())
                .withPrimaryEmail(contactNumber.getPrimaryEmail())
                .withSecondaryEmail(contactNumber.getSecondaryEmail())
                .build();
    }


    public static Address buildAggregateAddress(final uk.gov.justice.core.courts.Address address) {
        return uk.gov.moj.cpp.listing.domain.aggregate.Address.address()
                .withAddress1(address.getAddress1())
                .withAddress2(address.getAddress2())
                .withAddress3(address.getAddress3())
                .withAddress4(address.getAddress4())
                .withAddress5(address.getAddress5())
                .withWelshAddress1(address.getWelshAddress1())
                .withWelshAddress2(address.getWelshAddress2())
                .withWelshAddress3(address.getWelshAddress3())
                .withWelshAddress4(address.getWelshAddress4())
                .withWelshAddress5(address.getWelshAddress5())
                .withPostcode(address.getPostcode())
                .build();
    }

    public static BailStatus buildAggregateBailStatus(final uk.gov.justice.core.courts.BailStatus bailStatus) {
        return uk.gov.moj.cpp.listing.domain.aggregate.BailStatus.bailStatus()
                .withId(bailStatus.getId())
                .withCode(bailStatus.getCode())
                .withDescription(bailStatus.getDescription())
                .withCustodyTimeLimit(bailStatus.getCustodyTimeLimit() != null ? buildAggregateCustodyTimeLimit(bailStatus.getCustodyTimeLimit()) : null)
                .build();
    }


    public static CustodyTimeLimit buildAggregateCustodyTimeLimit(final uk.gov.justice.core.courts.CustodyTimeLimit custodyTimeLimit) {
        return uk.gov.moj.cpp.listing.domain.aggregate.CustodyTimeLimit.custodyTimeLimit()
                .withDaysSpent(custodyTimeLimit.getDaysSpent())
                .withTimeLimit(custodyTimeLimit.getTimeLimit())
                .build();
    }


    public static Offence buildAggregateOffence(final uk.gov.justice.listing.events.Offence offence) {
        return uk.gov.moj.cpp.listing.domain.aggregate.Offence.offence()
                .withId(offence.getId())
                .withOffenceCode(offence.getOffenceCode())
                .withStartDate(offence.getStartDate())
                .withEndDate(offence.getEndDate())
                .withStatementOfOffence(nonNull(offence.getStatementOfOffence()) ? buildAggregateStatementOfOffence(offence.getStatementOfOffence()) : null)
                .withOffenceWording(offence.getOffenceWording())
                .withLaidDate(offence.getLaidDate())
                .withLaaApplnReference(offence.getLaaApplnReference() != null ? buildAggregateLaaReference(offence.getLaaApplnReference()) : null)
                .withRestrictFromCourtList(offence.getRestrictFromCourtList())
                .build();
    }

    public static LaaReference buildAggregateLaaReference(final uk.gov.justice.listing.events.LaaReference laaReference) {
        return uk.gov.moj.cpp.listing.domain.aggregate.LaaReference.laaReference()
                .withStatusId(laaReference.getStatusId())
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusDate(laaReference.getStatusDate())
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveStartDate(laaReference.getEffectiveStartDate())
                .withEffectiveEndDate(laaReference.getEffectiveEndDate())
                .build();
    }

    public static StatementOfOffence buildAggregateStatementOfOffence(final uk.gov.justice.listing.events.StatementOfOffence statementOfOffence) {
        return uk.gov.moj.cpp.listing.domain.aggregate.StatementOfOffence.statementOfOffence()
                .withTitle(statementOfOffence.getTitle())
                .withLegislation(statementOfOffence.getLegislation())
                .withWelshTitle(statementOfOffence.getWelshTitle())
                .withWelshLegislation(statementOfOffence.getWelshLegislation())
                .build();
    }

    public static CaseIdentifier buildAggregateCaseIdentifier(final uk.gov.justice.listing.events.CaseIdentifier caseIdentifier) {
        return uk.gov.moj.cpp.listing.domain.CaseIdentifier.caseIdentifier()
                .withAuthorityCode(caseIdentifier.getAuthorityCode())
                .withAuthorityId(caseIdentifier.getAuthorityId())
                .withCaseReference(caseIdentifier.getCaseReference())
                .build();
    }

    public static Prosecutor buildAggregateProsecutor(final uk.gov.justice.listing.events.Prosecutor prosecutor) {
        return uk.gov.moj.cpp.listing.domain.Prosecutor.prosecutor()
                .withProsecutorCode(prosecutor.getProsecutorCode())
                .withProsecutorId(prosecutor.getProsecutorId())
                .build();
    }
}
