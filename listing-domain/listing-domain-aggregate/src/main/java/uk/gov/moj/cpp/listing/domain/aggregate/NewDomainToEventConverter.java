package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CommittingCourt;
import uk.gov.justice.listing.events.CourtApplicationPartyType;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.LaaReference;
import uk.gov.justice.listing.events.LinkedToCases;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.NewBaseDefendant;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.Prosecutor;
import uk.gov.justice.listing.events.SeedingHearing;
import uk.gov.justice.listing.events.SimpleOffence;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.ApplicantRespondent;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.aggregate.converter.ReportingRestrictionConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:S2583", "PMD.NullAssignment"})
public class NewDomainToEventConverter {

    private NewDomainToEventConverter() {
    }

    public static uk.gov.justice.listing.events.ListedCase buildListedCase(final ListedCase lc) {
        final uk.gov.justice.listing.events.ListedCase.Builder builder = uk.gov.justice.listing.events.ListedCase.listedCase()
                .withId(lc.getId())
                .withCaseIdentifier(buildCaseIdentifier(lc))
                .withIsCivil(lc.getIsCivil().orElse(null))
                .withGroupId(lc.getGroupId().orElse(null))
                .withIsGroupMember(lc.getIsGroupMember().orElse(null))
                .withIsGroupMaster(lc.getIsGroupMaster().orElse(null))
                .withMarkers(isNull(lc.getCaseMarkers()) ? emptyList() : lc.getCaseMarkers().stream()
                        .map(NewDomainToEventConverter::convertCaseMarkersToMarkers)
                        .collect(toList()))
                .withDefendants(lc.getDefendants().stream()
                        .map(NewDomainToEventConverter::buildDefendant)
                        .collect(toList()))
                .withRestrictFromCourtList(false)
                .withShadowListed(nonNull(lc.getShadowListed()) && lc.getShadowListed().isPresent() ? lc.getShadowListed().get() : null)
                .withTrialReceiptType(lc.getTrialReceiptType());

        if(nonNull(lc.getProsecutor())){
            builder.withProsecutor(buildProsecutor(lc));
        }

        constructCivilInfo(lc, builder);

        return builder.build();
    }

    private static void constructCivilInfo(final ListedCase lc, final uk.gov.justice.listing.events.ListedCase.Builder builder) {
        if (nonNull(lc.getIsCivil()) && lc.getIsCivil().isPresent()) {
            builder.withIsCivil(lc.getIsCivil().get());
        }

        if (nonNull(lc.getIsGroupMaster()) && lc.getIsGroupMaster().isPresent()) {
            builder.withIsGroupMaster(lc.getIsGroupMaster().get());
        }

        if (nonNull(lc.getIsGroupMember()) && lc.getIsGroupMember().isPresent()) {
            builder.withIsGroupMember(lc.getIsGroupMember().get());
        }
        if (nonNull(lc.getGroupId()) && lc.getGroupId().isPresent()) {
            builder.withGroupId(lc.getGroupId().get());
        }
    }

    public static List<Marker> convertCaseMarkersListToMarkers(final List<CaseMarker> caseMarkers) {
        return caseMarkers.stream().map(NewDomainToEventConverter::convertCaseMarkersToMarkers).collect(toList());
    }

    public static Marker convertCaseMarkersToMarkers(final CaseMarker caseMarker) {
        return Marker.marker().withId(caseMarker.getId())
                .withMarkerTypeCode(caseMarker.getMarkerTypeCode())
                .withMarkerTypeDescription(caseMarker.getMarkerTypeDescription())
                .withMarkerTypeid(caseMarker.getMarkerTypeid()).build();
    }


    @SuppressWarnings({"squid:S3655", "squid:S1067"})
    public static Defendant buildDefendant(final uk.gov.moj.cpp.listing.domain.Defendant d) {
        final Optional<HearingLanguageNeeds> hearingLanguageNeeds = d.getHearingLanguageNeeds();
        return Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(d.getMasterDefendantId().orElse(null))
                .withCourtProceedingsInitiated(d.getCourtProceedingsInitiated().orElse(null))
                .withCustodyTimeLimit(d.getCustodyTimeLimit().orElse(null))
                .withDateOfBirth(d.getDateOfBirth().orElse(null))
                .withFirstName(d.getFirstName().orElse(null))
                .withLastName(d.getLastName().orElse(null))
                .withDatesToAvoid(d.getDatesToAvoid().orElse(null))
                .withHearingLanguageNeeds(hearingLanguageNeeds.isPresent()
                        ? uk.gov.justice.core.courts.HearingLanguage.valueFor(hearingLanguageNeeds.get().toString()).get()
                        : null)
                .withOrganisationName(d.getOrganisationName().orElse(null))
                .withSpecificRequirements(d.getSpecificRequirements().orElse(null))
                .withOffences(d.getOffences().stream()
                        .map(NewDomainToEventConverter::buildOffence)
                        .collect(toList()))
                .withDefenceOrganisation(d.getDefenceOrganisation().orElse(null))
                .withBailStatus(buildBailStatusEvent(d.getBailStatus()))
                .withRestrictFromCourtList(false)
                .withIsYouth(d.getIsYouth().orElse(null))
                .withAddress(nonNull(d.getAddress()) && d.getAddress().isPresent() ? buildAddress(d.getAddress().get()) : null)
                .withNationalityDescription(d.getNationalityDescription().orElse(null))
                .build();
    }


    @SuppressWarnings({"squid:S3655", "squid:S1067"})
    private static uk.gov.justice.core.courts.Address buildAddress(final uk.gov.moj.cpp.listing.domain.Address address) {
        if (nonNull(address)) {
            return uk.gov.justice.core.courts.Address.address()
                    .withAddress1(ofNullable(address.getAddress1()).orElse(""))
                    .withAddress2(address.getAddress2().orElse(null))
                    .withAddress3(address.getAddress3().orElse(null))
                    .withAddress4(address.getAddress4().orElse(null))
                    .withAddress5(address.getAddress5().orElse(null))
                    .withPostcode(address.getPostcode().orElse(null))
                    .withWelshAddress1(address.getWelshAddress1().orElse(null))
                    .withWelshAddress2(address.getWelshAddress2().orElse(null))
                    .withWelshAddress3(address.getWelshAddress3().orElse(null))
                    .withWelshAddress4(address.getWelshAddress4().orElse(null))
                    .withWelshAddress5(address.getWelshAddress5().orElse(null))
                    .build();
        }
        return null;
    }

    public static NewBaseDefendant buildNewBaseDefendant(final uk.gov.moj.cpp.listing.domain.Defendant d) {
        return NewBaseDefendant.newBaseDefendant()
                .withId(d.getId())
                .withMasterDefendantId(d.getMasterDefendantId().orElse(null))
                .withCustodyTimeLimit(d.getCustodyTimeLimit().orElse(null))
                .withDateOfBirth(d.getDateOfBirth().orElse(null))
                .withFirstName(d.getFirstName().orElse(null))
                .withLastName(d.getLastName().orElse(null))
                .withOrganisationName(d.getOrganisationName().orElse(null))
                .withSpecificRequirements(d.getSpecificRequirements().orElse(null))
                .withDefenceOrganisation(d.getDefenceOrganisation().orElse(null))
                .withBailStatus(buildBailStatusEvent(d.getBailStatus()))
                .withIsYouth(d.getIsYouth().orElse(null))
                .withAddress(nonNull(d.getAddress()) && d.getAddress().isPresent() ? buildAddress(d.getAddress().get()) : null)
                .withNationalityDescription(d.getNationalityDescription().orElse(null))
                .build();
    }

    public static Defendant updateEventDefendant(NewBaseDefendant newDefendant, Defendant defendant){
        return uk.gov.justice.listing.events.Defendant.defendant()
                .withValuesFrom(defendant)
                .withFirstName(newDefendant.getFirstName())
                .withLastName(newDefendant.getLastName())
                .withOrganisationName(newDefendant.getOrganisationName())
                .withBailStatus(newDefendant.getBailStatus())
                .withCustodyTimeLimit(newDefendant.getCustodyTimeLimit())
                .withId(newDefendant.getId())
                .withDateOfBirth(newDefendant.getDateOfBirth())
                .withDefenceOrganisation(newDefendant.getDefenceOrganisation())
                .withSpecificRequirements(newDefendant.getSpecificRequirements())
                .withIsYouth(newDefendant.getIsYouth())
                .withAddress(newDefendant.getAddress())
                .withNationalityDescription(newDefendant.getNationalityDescription())
                .withMasterDefendantId(newDefendant.getMasterDefendantId())
                .build();

    }

    public static List<SimpleOffence> buildSimpleOffences(final List<uk.gov.moj.cpp.listing.domain.SimpleOffence> offences) {
        return offences.stream().map(simpleOffence ->
                SimpleOffence.simpleOffence()
                        .withId(simpleOffence.getId())
                        .withDefendantId(simpleOffence.getDefendantId())
                        .build())
                .collect(toList());
    }

    public static List<Offence> buildOffences(final List<uk.gov.moj.cpp.listing.domain.Offence> o) {
        return o.stream().map(NewDomainToEventConverter::buildOffence).collect(toList());
    }

    public static List<LinkedToCases> convertDomainToLinkedToCasesEvent(final List<uk.gov.moj.cpp.listing.domain.LinkedToCases> linkedToCases) {
        return linkedToCases.stream()
                .map(NewDomainToEventConverter::buildLinkedToCases)
                .collect(toList());
    }

    private static LinkedToCases buildLinkedToCases(final uk.gov.moj.cpp.listing.domain.LinkedToCases linkedToCases) {
        return LinkedToCases.linkedToCases()
                .withCaseId(linkedToCases.getCaseId())
                .withCaseUrn(linkedToCases.getCaseUrn())
                .build();
    }

    @SuppressWarnings({"squid:S3655"})
    public static Offence buildOffence(final uk.gov.moj.cpp.listing.domain.Offence offence) {
        final Offence.Builder builder = Offence.offence()
                .withId(offence.getId())
                .withEndDate(offence.getEndDate().orElse(null))
                .withStartDate(offence.getStartDate())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceWording(offence.getOffenceWording())
                .withCount(offence.getCount())
                .withIndictmentParticular(offence.getIndictmentParticular())
                .withOrderIndex(offence.getOrderIndex())
                .withStatementOfOffence(buildStatementOfOffence(offence))
                .withOffenceWording(offence.getOffenceWording())
                .withRestrictFromCourtList(false)
                .withLaaApplnReference(offence.getLaaApplnReference().isPresent() ? buildLaaReference(offence.getLaaApplnReference().get()) : null)
                .withLaidDate(offence.getLaidDate().orElse(null))
                .withShadowListed(offence.getShadowListed().orElse(null));

        if(nonNull(offence.getSeedingHearing()) && offence.getSeedingHearing().isPresent()) {
            builder.withSeedingHearing(buildSeedingHearing(offence.getSeedingHearing().get()).get());
        }

        if (nonNull(offence.getCommittingCourt()) && offence.getCommittingCourt().isPresent()) {
            builder.withCommittingCourt(buildCommittingCourt(offence.getCommittingCourt().get()));
        }

        if (!isNull(offence.getReportingRestrictions()) && !offence.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(offence.getReportingRestrictions().stream()
                    .map(ReportingRestrictionConverter::domainToEvents)
                    .collect(toList())
            );
        }
        if(nonNull(offence.getCivilOffence())) {
            builder.withCivilOffence(buildCivilOffence(offence));
        }

        return builder.build();
    }

    private static uk.gov.justice.core.courts.CivilOffence buildCivilOffence(final uk.gov.moj.cpp.listing.domain.Offence offence) {
        return uk.gov.justice.core.courts.CivilOffence.civilOffence()
                .withIsExParte(offence.getCivilOffence().getIsExParte())
                .build();
    }

    private static uk.gov.justice.core.courts.BailStatus buildBailStatusEvent(final Optional<uk.gov.moj.cpp.listing.domain.BailStatus> bailStatus) {
        if (bailStatus.isPresent()) {
            return bailStatus.map(bs -> uk.gov.justice.core.courts.BailStatus.bailStatus().withCode(bs.getCode()).withDescription(bs.getDescription()).withId(bs.getId()).build()).get();
        }
        return null;
    }

    private static StatementOfOffence buildStatementOfOffence(final uk.gov.moj.cpp.listing.domain.Offence o) {
        return StatementOfOffence.statementOfOffence()
                .withLegislation(nonNull(o.getStatementOfOffence().getLegislation()) && o.getStatementOfOffence().getLegislation().isPresent() ? o.getStatementOfOffence().getLegislation().get() : null)
                .withTitle(o.getStatementOfOffence().getTitle())
                .withWelshLegislation(nonNull(o.getStatementOfOffence().getWelshLegislation()) && o.getStatementOfOffence().getWelshLegislation().isPresent() ? o.getStatementOfOffence().getWelshLegislation().get() : null)
                .withWelshTitle(o.getStatementOfOffence().getWelshTitle())
                .build();
    }

    private static CaseIdentifier buildCaseIdentifier(final ListedCase lc) {
        return CaseIdentifier.caseIdentifier()
                .withAuthorityCode(lc.getCaseIdentifier().getAuthorityCode())
                .withAuthorityId(lc.getCaseIdentifier().getAuthorityId())
                .withCaseReference(lc.getCaseIdentifier().getCaseReference())
                .build();
    }

    private static Prosecutor buildProsecutor(final ListedCase lc) {
        return Prosecutor.prosecutor()
                .withProsecutorId(lc.getProsecutor().getProsecutorId())
                .withProsecutorCode(lc.getProsecutor().getProsecutorCode())
                .withProsecutorName(lc.getProsecutor().getProsecutorName())
                .build();
    }

    public static uk.gov.justice.listing.events.JudicialRole buildJudicialRole(final JudicialRole domainJudicialRole) {
        return uk.gov.justice.listing.events.JudicialRole.judicialRole()
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudicialRoleTypeId(domainJudicialRole.getJudicialRoleType().getJudicialRoleTypeId().isPresent() ? domainJudicialRole.getJudicialRoleType().getJudicialRoleTypeId().get() : null)
                        .withJudiciaryType(domainJudicialRole.getJudicialRoleType().getJudiciaryType())
                        .build())
                .withJudicialId(domainJudicialRole.getJudicialId())
                .withIsDeputy(domainJudicialRole.getIsDeputy().orElse(null))
                .withIsBenchChairman(domainJudicialRole.getIsBenchChairman().orElse(null))
                .withUserId(domainJudicialRole.getUserId())
                .build();
    }

    public static uk.gov.justice.listing.events.CourtApplication buildCourtApplications(final CourtApplication courtApplication) {
        final uk.gov.justice.listing.events.CourtApplication.Builder builder = uk.gov.justice.listing.events.CourtApplication.courtApplication()
                .withId(courtApplication.getId())
                .withLinkedCaseIds(courtApplication.getLinkedCaseIds())
                .withOffences(buildApplicationOffences(courtApplication.getOffences()))
                .withApplicationType(courtApplication.getApplicationType())
                .withApplicant(buildApplicantRespondent(courtApplication.getApplicant()))
                .withRespondents(ofNullable(courtApplication.getRespondents())
                        .map(respondents -> respondents
                                .stream()
                                .map(NewDomainToEventConverter::buildApplicantRespondent)
                                .collect(toList()))
                        .orElse(null))

                .withRestrictFromCourtList(false)
                .withRestrictCourtApplicationType(false);

        if(nonNull(courtApplication.getParentApplicationId()) && courtApplication.getParentApplicationId().isPresent()) {
            builder.withParentApplicationId(courtApplication.getParentApplicationId().get());
        }
        if(nonNull(courtApplication.getApplicationReference()) && courtApplication.getApplicationReference().isPresent()) {
            builder.withApplicationReference(courtApplication.getApplicationReference().get());
        }
        if(nonNull(courtApplication.getApplicationParticulars()) && courtApplication.getApplicationParticulars().isPresent()) {
            builder.withApplicationParticulars(courtApplication.getApplicationParticulars().get());
        }
        return builder.build();
    }

    private static uk.gov.justice.listing.events.ApplicantRespondent buildApplicantRespondent(final ApplicantRespondent applicant) {
        if(null != applicant) {
            return  uk.gov.justice.listing.events.ApplicantRespondent.applicantRespondent()
                    .withId(applicant.getId())
                    .withFirstName(applicant.getFirstName().orElse(null))
                    .withLastName(applicant.getLastName())
                    .withIsRespondent(applicant.getIsRespondent())
                    .withRestrictFromCourtList(false)
                    .withCourtApplicationPartyType(buildCourtApplicationPartyTypeEvent(applicant.getCourtApplicationPartyType()))
                    .withAddress(NewDomainToEventConverter.buildAddress(applicant.getAddress()))
                    .build();
        }
        return null;
    }

    private static CourtApplicationPartyType buildCourtApplicationPartyTypeEvent(final uk.gov.moj.cpp.listing.domain.CourtApplicationPartyType courtApplicationPartyType) {

        return CourtApplicationPartyType.valueOf(courtApplicationPartyType.name());
    }

    private static LaaReference buildLaaReference(final uk.gov.moj.cpp.listing.domain.LaaReference laaReference) {

        return LaaReference.laaReference()
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveEndDate(laaReference.getEffectiveEndDate().orElse(null))
                .withEffectiveStartDate(laaReference.getEffectiveStartDate().orElse(null))
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusId(laaReference.getStatusId())
                .build();
    }

    private static CommittingCourt buildCommittingCourt(final uk.gov.moj.cpp.listing.domain.CommittingCourt committingCourt) {

        return CommittingCourt.committingCourt()
                .withCourtCentreId(committingCourt.getCourtCentreId())
                .withCourtHouseCode(committingCourt.getCourtHouseCode().orElse(null))
                .withCourtHouseName(committingCourt.getCourtHouseName())
                .withCourtHouseShortName(committingCourt.getCourtHouseShortName().orElse(null))
                .build();
    }

    public static Optional<SeedingHearing> buildSeedingHearing(final uk.gov.moj.cpp.listing.domain.SeedingHearing seedingHearing) {
        if (null != seedingHearing) {
            return of(SeedingHearing.seedingHearing()
                    .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                    .withJurisdictionType(JurisdictionType.valueOf(seedingHearing.getJurisdictionType().name()))
                    .withSittingDay(seedingHearing.getSittingDay())
                    .build());
        }
        return empty();
    }

    private static List<uk.gov.justice.listing.events.Offence> buildApplicationOffences(final List<uk.gov.moj.cpp.listing.domain.Offence> offences) {
        final List<uk.gov.justice.listing.events.Offence> offencesEvents = new ArrayList<>();
        if (isNotEmpty(offences)) {
            offences.forEach(offence -> offencesEvents.add(uk.gov.justice.listing.events.Offence.offence()
                    .withId(offence.getId())
                    .withCount(offence.getCount())
                    .withOrderIndex(offence.getOrderIndex())
                    .withStatementOfOffence(StatementOfOffence.statementOfOffence().withLegislation(offence.getStatementOfOffence().getLegislation().orElse(null)).build())
                    .withOffenceCode(offence.getOffenceCode())
                    .withOffenceWording(offence.getOffenceWording())
                    .withStartDate(offence.getStartDate())
                    .withStatementOfOffence(buildStatementOfOffence(offence))
                    .build()));
        }
        return offencesEvents;
    }

    public static List<uk.gov.justice.listing.events.HearingDay> convertHearingDaysDomainToEvent(final List<uk.gov.moj.cpp.listing.domain.HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(hearingDay -> uk.gov.justice.listing.events.HearingDay.hearingDay()
                        .withDurationMinutes(hearingDay.getDurationMinutes())
                        .withEndTime(hearingDay.getEndTime())
                        .withSequence(hearingDay.getSequence())
                        .withHearingDate(hearingDay.getHearingDate())
                        .withStartTime(hearingDay.getStartTime())
                        .withIsCancelled(hearingDay.getIsCancelled().orElse(false))
                        .withCourtScheduleId(hearingDay.getCourtScheduleId().orElse(null))
                        .withCourtRoomId(hearingDay.getCourtRoomId().orElse(null))
                        .withCourtCentreId(hearingDay.getCourtCentreId().orElse(null))
                        .build())
                .toList();

    }

    public static List<uk.gov.justice.listing.events.NonDefaultDay> convertHearingDaysDomainToNonDefaultDaysEvent(final List<uk.gov.moj.cpp.listing.domain.HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(hearingDay -> {
                    uk.gov.justice.listing.events.NonDefaultDay.Builder builder = uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay();
                    builder.withDuration(hearingDay.getDurationMinutes())
                            .withStartTime(hearingDay.getStartTime())
                            .withCourtCentreId(hearingDay.getCourtCentreId().toString());
                    if (hearingDay.getCourtRoomId().isPresent() && nonNull(hearingDay.getCourtRoomId())) {
                        builder.withRoomId(hearingDay.getCourtRoomId().toString());
                    }
                    if (hearingDay.getCourtScheduleId().isPresent() && nonNull(hearingDay.getCourtScheduleId())) {
                        builder.withCourtScheduleId(hearingDay.getCourtScheduleId().toString());
                    }
                    return builder.build();
                })
                .toList();

    }

    public static List<uk.gov.justice.listing.events.NonDefaultDay> convertNonDefaultDaysDomainToEvent(final List<NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(nonDefaultDay -> uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay()
                        .withCourtCentreId(nonDefaultDay.getCourtCentreId().orElse(null))
                        .withCourtRoomId(nonDefaultDay.getCourtRoomId().orElse(null))
                        .withCourtScheduleId(nonDefaultDay.getCourtScheduleId().orElse(null))
                        .withDuration(nonDefaultDay.getDuration().orElse(null))
                        .withOucode(nonDefaultDay.getOucode().orElse(null))
                        .withRoomId(nonDefaultDay.getRoomId().orElse(null))
                        .withSession(nonDefaultDay.getSession().orElse(null))
                        .withStartTime(nonDefaultDay.getStartTime())
                        .build())
                .toList();

    }
}
