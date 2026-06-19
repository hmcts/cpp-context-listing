package uk.gov.moj.cpp.listing.event.processor.util;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Gender.NOT_KNOWN;
import static uk.gov.justice.core.courts.LegalEntityDefendant.legalEntityDefendant;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BreachType;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Jurisdiction;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceActiveOrder;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.listing.events.ApplicantRespondent;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataCache;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;


public class HearingObjectsListingToCoreConverter {

    private static final String HEARING_CODE = "FHG";

    @Inject
    private ReferenceDataCache referenceDataCache;

    public uk.gov.justice.core.courts.Hearing convert(final Hearing hearing) {
        final List<uk.gov.justice.listing.events.CourtApplication> courtApplication = hearing.getCourtApplications();
        final List<ListedCase> cases = hearing.getListedCases();
        HearingType currentHearingType = null;
        if (nonNull(hearing.getType())) {
            currentHearingType = HearingType.hearingType()
                    .withId(hearing.getType().getId())
                    .withDescription(hearing.getType().getDescription())
                    .withWelshDescription(hearing.getType().getWelshDescription())
                    .build();
        } else {
            final Optional<uk.gov.moj.cpp.listing.domain.referencedata.HearingType> hearingCacheData = referenceDataCache.getHearingTypeCodeCache(HEARING_CODE);
            if (hearingCacheData.isPresent()) {
                final uk.gov.moj.cpp.listing.domain.referencedata.HearingType firstHearingData = hearingCacheData.get();
                currentHearingType = HearingType.hearingType()
                        .withId(firstHearingData.getId())
                        .withDescription(firstHearingData.getHearingDescription())
                        .withWelshDescription(firstHearingData.getWelshHearingDescription()).build();
            }
        }

        final uk.gov.justice.core.courts.Hearing.Builder hearingBuilder = new uk.gov.justice.core.courts.Hearing.Builder();

        hearingBuilder
                .withType(currentHearingType)
                .withId(hearing.getId())
                .withJurisdictionType(JurisdictionType.valueOf(hearing.getJurisdictionType().name()))
                .withHearingLanguage(hearing.getHearingLanguage())
                .withCourtCentre(CourtCentre.courtCentre().withId(hearing.getCourtCentreId())
                        .withName("UNKNOWN").build())
                .withBookingType(hearing.getBookingType())
                .withPriority(hearing.getPriority())
                .withSpecialRequirements(hearing.getSpecialRequirements())
                .withIsGroupProceedings(hearing.getIsGroupProceedings())
                .build();

        if (isNotEmpty(courtApplication)) {
            hearingBuilder.withCourtApplications(courtApplication.stream().map(this::convert).collect(toList()));
        }

        if (isNotEmpty(cases)) {
            hearingBuilder.withProsecutionCases(cases.stream().map(this::convert).collect(toList()));
        }

        if (isNotEmpty(hearing.getHearingDays())) {
            hearingBuilder.withHearingDays(hearing.getHearingDays().stream().map(this::convert).collect(toList()));
        }

        return hearingBuilder.build();
    }

    private HearingDay convert(final uk.gov.justice.listing.events.HearingDay hearingDay) {
        return HearingDay.hearingDay()
                .withCourtCentreId(hearingDay.getCourtCentreId())
                .withCourtRoomId(hearingDay.getCourtRoomId())
                .withIsCancelled(hearingDay.getIsCancelled())
                .withListedDurationMinutes(hearingDay.getDurationMinutes())
                .withListingSequence(hearingDay.getSequence())
                .withSittingDay(hearingDay.getStartTime())
                .build();
    }

    private ProsecutionCase convert(final ListedCase listedCase) {
        return ProsecutionCase.prosecutionCase()
                .withDefendants(listedCase.getDefendants().stream().map(defendant -> convert(defendant, listedCase.getId())).collect(toList()))
                .withProsecutor(convert(listedCase.getProsecutor()))
                .withId(listedCase.getId())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(listedCase.getCaseIdentifier().getAuthorityCode())
                        .withProsecutionAuthorityId(listedCase.getCaseIdentifier().getAuthorityId())
                        .withCaseURN(listedCase.getCaseIdentifier().getCaseReference())
                        .build())
                .withInitiationCode(InitiationCode.C)
                .build();

    }

    private Prosecutor convert(final uk.gov.justice.listing.events.Prosecutor prosecutor) {
        if (prosecutor == null) {
            return null;
        } else {
            return Prosecutor.prosecutor()
                    .withProsecutorId(prosecutor.getProsecutorId())
                    .withProsecutorName(prosecutor.getProsecutorName())
                    .withProsecutorCode(prosecutor.getProsecutorCode())
                    .build();
        }
    }

    public Defendant convert(final uk.gov.justice.listing.events.Defendant defendant, final UUID caseId) {
        final Defendant.Builder builder = defendant()
                .withId(defendant.getId())
                .withMasterDefendantId(defendant.getId())
                .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                .withProsecutionCaseId(caseId);
        if (defendant.getOrganisationName() != null) {
            builder.withLegalEntityDefendant(legalEntityDefendant().withOrganisation(organisation().withName(defendant.getOrganisationName()).withAddress(defendant.getAddress()).build()).build());
        } else {
            builder.withPersonDefendant(personDefendant()
                    .withPersonDetails(person()
                            .withFirstName(defendant.getFirstName())
                            .withLastName(defendant.getLastName())
                            .withGender(NOT_KNOWN)
                            .withDateOfBirth(defendant.getDateOfBirth())
                            .withAddress(defendant.getAddress())
                            .build())
                    .build());
        }
        builder.withOffences(defendant.getOffences().stream().map(toOffence()).collect(toList()));
        return builder.build();
    }

    private CourtApplication convert(final uk.gov.justice.listing.events.CourtApplication courtApplication) {
        List<Offence> offences = null;
        if (isNotEmpty(courtApplication.getOffences())) {
            offences = courtApplication.getOffences().stream().map(toOffence()).collect(toList());
        }

        final CourtApplication.Builder courtApplicationBuilder = CourtApplication.courtApplication();

        courtApplicationBuilder
                .withId(courtApplication.getId())
                .withApplicant(convert(courtApplication.getApplicant()))
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(courtApplication.getApplicationType())
                        .withId(courtApplication.getId())
                        .withCategoryCode("CategoryCode")
                        .withLinkType(LinkType.FIRST_HEARING)
                        .withJurisdiction(Jurisdiction.CROWN)
                        .withSummonsTemplateType(SummonsTemplateType.FIRST_HEARING)
                        .withBreachType(BreachType.GENERIC_BREACH)
                        .withAppealFlag(false)
                        .withApplicantAppellantFlag(false)
                        .withPleaApplicableFlag(false)
                        .withCommrOfOathFlag(false)
                        .withCourtOfAppealFlag(false)
                        .withCourtExtractAvlFlag(false)
                        .withProsecutorThirdPartyFlag(false)
                        .withSpiOutApplicableFlag(false)
                        .withOffenceActiveOrder(OffenceActiveOrder.OFFENCE)
                        .build())
                .withApplicationReceivedDate("2022-03-27")
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(courtApplication.getApplicant().getId())
                        .withSummonsRequired(false)
                        .withNotificationRequired(false)
                        .build())
                .withApplicationStatus(ApplicationStatus.DRAFT);

        if (isNotEmpty(courtApplication.getRespondents())) {
            courtApplicationBuilder.withRespondents(courtApplication.getRespondents().stream().map(this::convert).collect(toList()));
        }

        if (isNotEmpty(courtApplication.getLinkedCaseIds())) {
            courtApplicationBuilder.withCourtApplicationCases(Collections.singletonList(CourtApplicationCase.courtApplicationCase()
                    .withProsecutionCaseId(courtApplication.getLinkedCaseIds().get(0))
                    .withOffences(offences)
                    .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                            .withCaseURN("CASEURN")
                            .withProsecutionAuthorityId(UUID.fromString("94c71d51-dc29-47e3-ba35-885a535160f3"))
                            .withProsecutionAuthorityCode("CODE")
                            .build())
                    .withIsSJP(false)
                    .withCaseStatus("ACTIVE")
                    .build()));
        }

        return courtApplicationBuilder.build();
    }


    private CourtApplicationParty convert(final ApplicantRespondent applicantRespondent) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(applicantRespondent.getId())
                .withPersonDetails(person()
                        .withFirstName(applicantRespondent.getFirstName())
                        .withLastName(applicantRespondent.getLastName())
                        .withGender(NOT_KNOWN)
                        .build())
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();
    }

    private static Function<uk.gov.justice.listing.events.Offence, Offence> toOffence() {
        final Offence.Builder offenceBuilder = Offence.offence();
        return offence -> {
            offenceBuilder
                    .withWording(offence.getOffenceWording())
                    .withOffenceDefinitionId(offence.getId())
                    .withOffenceTitle(offence.getStatementOfOffence().getTitle())
                    .withId(offence.getId())
                    .withOffenceCode(offence.getOffenceCode())
                    .withStartDate(offence.getStartDate())
                    .withOrderIndex(offence.getOrderIndex())
                    .withCount(offence.getCount())
                    .withOffenceLegislation(offence.getStatementOfOffence().getLegislation());

            if (isNotEmpty(offence.getReportingRestrictions())) {
                offenceBuilder.withReportingRestrictions(offence.getReportingRestrictions().stream().map(convertReportingRestriction()).collect(toList()));
            }
            return offenceBuilder.build();
        };
    }

    private static Function<uk.gov.justice.listing.events.ReportingRestriction, uk.gov.justice.core.courts.ReportingRestriction> convertReportingRestriction() {
        return reportingRestriction -> uk.gov.justice.core.courts.ReportingRestriction.reportingRestriction()
                .withId(reportingRestriction.getId())
                .withJudicialResultId(reportingRestriction.getJudicialResultId())
                .withLabel(reportingRestriction.getLabel())
                .withOrderedDate(reportingRestriction.getOrderedDate().toString())
                .build();
    }
}
