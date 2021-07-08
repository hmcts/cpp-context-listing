package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.valueFor;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.courts.TypeOfList;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.listing.domain.BailStatus;
import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CommittingCourt;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyListingNeeds;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.domain.aggregate.converter.ReportingRestrictionConverter;
import uk.gov.moj.cpp.listing.domain.exception.DataValidationException;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"pmd:NullAssignment", "squid:S2583", "squid:S1172", "squid:CommentedOutCodeLine", "pmd:NullAssignment", "squid:MethodCyclomaticComplexity", "squid:S3655", "squid:S1067"})
public class CommandToDomainConverter implements Converter<HearingListingNeeds, Hearing> {

    @SuppressWarnings({"squid:S3655"})
    public static ZonedDateTime extractStartDate(final HearingListingNeeds commandHearing) {
        return extractStartDate(commandHearing.getListedStartDateTime(), commandHearing.getEarliestStartDateTime());
    }

    public static ZonedDateTime extractStartDate(final HearingUnscheduledListingNeeds commandHearing) {
        final ZonedDateTime startDateTime = extractStartDate(commandHearing.getListedStartDateTime(), commandHearing.getEarliestStartDateTime());
        return startDateTime != null ? ZonedDateTimes.fromString(startDateTime.toString()) : null;
    }

    private static ZonedDateTime extractStartDate(final Optional<ZonedDateTime> listedStartDateTime, final Optional<ZonedDateTime> earliestStartDateTime) {
        return listedStartDateTime.orElseGet(() -> (earliestStartDateTime.orElse(null)));
    }


    @SuppressWarnings({"squid:S3655"})
    @Override
    public Hearing convert(final HearingListingNeeds commandHearing) {
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        if (isNotEmpty(commandHearing.getBookedSlots())) {
            commandHearing.getBookedSlots().forEach(rotaSlot -> nonDefaultDays.add(convertNonDefaultDay(rotaSlot)));
        }
        return convert(commandHearing, nonDefaultDays, emptyList());
    }

    public Hearing convert(final HearingListingNeeds commandHearing, final List<NonDefaultDay> nonDefaultDays, final List<UUID> shadowListedOffences) {
        List<uk.gov.moj.cpp.listing.domain.JudicialRole> domainJudicialRoles = emptyList();
        if (commandHearing.getJudiciary() != null) {
            domainJudicialRoles = commandHearing.getJudiciary().stream()
                    .map(this::buildJudiciary)
                    .collect(toList());
        }

        final List<ListedCase> domainListedCases = listStandAloneApplicationsOrBookedSlots(commandHearing) ? emptyList() : commandHearing.getProsecutionCases().stream()
                .map(prosecutionCase -> buildListedCases(commandHearing, prosecutionCase, shadowListedOffences))
                .collect(toList());

        final Optional<LocalDate> weekCommencingStartDate = commandHearing.getWeekCommencingDate().isPresent() && nonNull(commandHearing.getWeekCommencingDate().get().getStartDate()) ?
                of(LocalDate.parse(commandHearing.getWeekCommencingDate().get().getStartDate())) : empty();

        final Optional<Integer> weekCommencingDurationInWeeks = commandHearing.getWeekCommencingDate().isPresent() ? commandHearing.getWeekCommencingDate().get().getDuration() : empty();

        final Optional<LocalDate> weekCommencingEndDate = weekCommencingStartDate.isPresent() && weekCommencingDurationInWeeks.isPresent() ?
                of(weekCommencingStartDate.get().plusWeeks(weekCommencingDurationInWeeks.get())) : empty();

        return Hearing.hearing()
                .withId(commandHearing.getId())
                .withType(buildHearingType(commandHearing.getType()))
                .withHearingLanguage(empty())
                .withEstimatedMinutes(commandHearing.getEstimatedMinutes())
                .withStartDateTime(nonNull(extractStartDate(commandHearing)) ? ZonedDateTimes.fromString(extractStartDate(commandHearing).toString()) : null)
                .withCourtCentreId(commandHearing.getCourtCentre().getId())
                .withCourtRoomId(commandHearing.getCourtCentre().getRoomId())
                .withListingDirections(commandHearing.getListingDirections())
                .withProsecutorDatesToAvoid(commandHearing.getProsecutorDatesToAvoid())
                .withReportingRestrictionReason(commandHearing.getReportingRestrictionReason())
                .withJudiciary(domainJudicialRoles)
                .withJurisdictionType(JurisdictionType.valueFor(commandHearing.getJurisdictionType().name())
                        .orElseThrow(IllegalArgumentException::new))
                .withListedCases(domainListedCases)
                .withEndDate(commandHearing.getEndDate().isPresent() ? of(LocalDate.parse(commandHearing.getEndDate().get())) : empty())
                .withNonSittingDays(emptyList())
                .withNonDefaultDays(nonDefaultDays)
                .withHearingDays(emptyList())
                .withCourtApplication(isEmpty(commandHearing.getCourtApplications()) ? emptyList() : commandHearing.getCourtApplications()
                        .stream().map(ca -> new CourtApplicationToDomainConverter().convert(ca)).collect(toList()))
                .withCourtApplicationPartyNeeds(isNull(commandHearing.getCourtApplicationPartyListingNeeds())
                        ? emptyList() : commandHearing.getCourtApplicationPartyListingNeeds().stream()
                        .map(this::buildCourtApplicationPartyNeeds).collect(toList()))
                .withWeekCommencingStartDate(weekCommencingStartDate)
                .withWeekCommencingEndDate(weekCommencingEndDate)
                .withWeekCommencingDurationInWeeks(weekCommencingDurationInWeeks)
                .build();
    }

    @SuppressWarnings("squid:S1168")
    public List<ListedCase> mapToListedCases(final HearingUnscheduledListingNeeds commandHearing, final List<ProsecutionCase> prosecutionCases) {
        if (isEmpty(prosecutionCases)) {
            return Collections.emptyList();
        } else {
            return prosecutionCases.stream().map(prosecutionCase ->
                    mapToListedCase(commandHearing, prosecutionCase)
            ).collect(toList());
        }
    }

    private ListedCase mapToListedCase(final HearingUnscheduledListingNeeds commandHearing, final ProsecutionCase prosecutionCase) {
        final ListedCase.Builder builder = ListedCase.listedCase()
                .withId(prosecutionCase.getId())
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                        .withAuthorityId(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())
                        .withCaseReference(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference() != null
                                ? prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()
                                : prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                        .build())
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(d -> buildDefendants(commandHearing, d))
                        .collect(toList()));

        if (isNotEmpty(prosecutionCase.getCaseMarkers())) {
            builder.withCaseMarkers(prosecutionCase.getCaseMarkers().stream()
                    .map(this::buildCaseMarker)
                    .collect(toList()));
        }

        return builder.build();
    }

    @SuppressWarnings({"squid:S3655"})
    public static ZonedDateTime getStartDateTime(final HearingListingNeeds commandHearing) {
        final ZonedDateTime listedStartDateTime = commandHearing.getListedStartDateTime().isPresent() ? commandHearing.getListedStartDateTime().get() : null;
        final ZonedDateTime earliestStartDateTime = commandHearing.getEarliestStartDateTime().isPresent() ? commandHearing.getEarliestStartDateTime().get() : null;

        return Optional.ofNullable(listedStartDateTime).orElseGet(() -> earliestStartDateTime);
    }

    public Type buildHearingType(final HearingType type) {
        return Type.type()
                .withId(type.getId())
                .withDescription(type.getDescription())
                .withWelshDescription(type.getWelshDescription())
                .build();
    }

    private boolean listStandAloneApplicationsOrBookedSlots(final HearingListingNeeds hearingListingNeeds) {
        if (isNull(hearingListingNeeds.getProsecutionCases())) {
            if (linkedCourtApplications(hearingListingNeeds) && isEmpty(hearingListingNeeds.getBookedSlots())) {
                throw new DataValidationException("List of prosecution cases must be supplied for a linked case application");
            }
            return true;
        }
        return false;
    }

    private boolean linkedCourtApplications(final HearingListingNeeds hearingListingNeeds) {
        return isNotEmpty(hearingListingNeeds.getProsecutionCases());
    }

    public JudicialRole buildJudiciary(final uk.gov.justice.core.courts.JudicialRole judicialRole) {
        return JudicialRole.judicialRole()
                .withJudicialId(judicialRole.getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(judicialRole.getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(judicialRole.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                        .build())
                .withIsDeputy(judicialRole.getIsDeputy())
                .withIsBenchChairman(judicialRole.getIsBenchChairman())
                .withUserId(judicialRole.getUserId().orElse(null))
                .build();
    }

    private ListedCase buildListedCases(final HearingListingNeeds commandHearing, final ProsecutionCase prosecutionCase, final List<UUID> shadowListedOffences) {
        final List<uk.gov.moj.cpp.listing.domain.Defendant> defendants = prosecutionCase.getDefendants().stream()
                .map(d -> buildDefendants(commandHearing, d, shadowListedOffences))
                .collect(toList());

        final boolean caseShadowListed = defendants.stream()
                .flatMap(defendant -> defendant.getOffences().stream())
                .allMatch(offence -> offence.getShadowListed().orElse(Boolean.FALSE));

        final ListedCase.Builder builder = ListedCase.listedCase()
                .withId(prosecutionCase.getId())
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityCode(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                        .withAuthorityId(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())
                        .withCaseReference(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference() != null
                                ? prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()
                                : prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                        .build())
                .withDefendants(defendants)
                .withShadowListed(of(caseShadowListed))
                .withTrialReceiptType(prosecutionCase.getTrialReceiptType().orElse(null));

        if (isNotEmpty(prosecutionCase.getCaseMarkers())) {
            builder.withCaseMarkers(prosecutionCase.getCaseMarkers().stream()
                    .map(this::buildCaseMarker)
                    .collect(toList()));
        }

        return builder.build();
    }

    private CaseMarker buildCaseMarker(final Marker marker) {
        return CaseMarker.caseMarker()
                .withId(marker.getId())
                .withMarkerTypeCode(marker.getMarkerTypeCode())
                .withMarkerTypeDescription(marker.getMarkerTypeDescription())
                .withMarkerTypeid(marker.getMarkerTypeid())
                .build();
    }

    @SuppressWarnings({"squid:S3655", "squid:S1067", "squid:MethodCyclomaticComplexity"})
    private uk.gov.moj.cpp.listing.domain.Defendant buildDefendants(final HearingUnscheduledListingNeeds commandHearing, final Defendant d) {
        return uk.gov.moj.cpp.listing.domain.Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(Optional.ofNullable(d.getMasterDefendantId()))
                .withCourtProceedingsInitiated(Optional.ofNullable(d.getCourtProceedingsInitiated()))
                .withFirstName(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getPersonDetails().getFirstName().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getFirstName().get()) : empty())
                .withLastName(d.getPersonDefendant().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getLastName()) : empty())
                .withBailStatus(mapBailStatus(d))
                .withSpecificRequirements(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getSpecificRequirements() : empty())
                .withDateOfBirth(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getDateOfBirth() : empty())
                .withCustodyTimeLimit(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getCustodyTimeLimit() : empty())
                .withDefenceOrganisation(d.getDefenceOrganisation().isPresent() ? of(d.getDefenceOrganisation().get().getName()) : empty())
                .withOrganisationName(d.getLegalEntityDefendant().isPresent() ? of(d.getLegalEntityDefendant().get().getOrganisation().getName()) : empty())
                .withDatesToAvoid(getDatesToAvoid(commandHearing, d))
                .withHearingLanguageNeeds(getHearingLanguageNeeds(commandHearing, d))
                .withOffences(d.getOffences().stream()
                        .map(offence -> buildOffence(offence, null))
                        .collect(toList()))
                .withIsYouth(d.getIsYouth().isPresent() ? d.getIsYouth() : empty())
                .withAddress(buildAddress(d))
                .withNationalityDescription(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getPersonDetails().getNationalityDescription().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getNationalityDescription() : empty())
                .build();
    }

    @SuppressWarnings({"squid:S3655", "squid:S1067", "squid:MethodCyclomaticComplexity"})
    private uk.gov.moj.cpp.listing.domain.Defendant buildDefendants(final HearingListingNeeds commandHearing, final Defendant d, final List<UUID> shadowListedOffences) {
        return uk.gov.moj.cpp.listing.domain.Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(Optional.ofNullable(d.getMasterDefendantId()))
                .withCourtProceedingsInitiated(Optional.ofNullable(d.getCourtProceedingsInitiated()))
                .withFirstName(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getPersonDetails().getFirstName().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getFirstName().get()) : empty())
                .withLastName(d.getPersonDefendant().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getLastName()) : empty())
                .withBailStatus(mapBailStatus(d))
                .withSpecificRequirements(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getSpecificRequirements() : empty())
                .withDateOfBirth(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getDateOfBirth() : empty())
                .withCustodyTimeLimit(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getCustodyTimeLimit() : empty())
                .withDefenceOrganisation(d.getDefenceOrganisation().isPresent() ? of(d.getDefenceOrganisation().get().getName()) : empty())
                .withOrganisationName(d.getLegalEntityDefendant().isPresent() ? of(d.getLegalEntityDefendant().get().getOrganisation().getName()) : empty())
                .withDatesToAvoid(getDatesToAvoid(commandHearing, d))
                .withHearingLanguageNeeds(getHearingLanguageNeeds(commandHearing, d))
                .withOffences(d.getOffences().stream()
                        .map(offence -> buildOffence(offence, shadowListedOffences))
                        .collect(toList()))
                .withIsYouth(d.getIsYouth().isPresent() ? d.getIsYouth() : empty())
                .withAddress(buildAddress(d))
                .withNationalityDescription(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getPersonDetails().getNationalityDescription().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getNationalityDescription() : empty())
                .build();
    }

    @SuppressWarnings({"squid:S3655", "squid:S1067", "squid:MethodCyclomaticComplexity"})
    private Optional<uk.gov.moj.cpp.listing.domain.Address> buildAddress(final Defendant defendant) {
        Optional<Address> d = empty();

        if (nonNull(defendant) && defendant.getPersonDefendant().isPresent()) {
            d = defendant.getPersonDefendant().get().getPersonDetails().getAddress();

        } else if (nonNull(defendant) && defendant.getLegalEntityDefendant().isPresent()) {
            d = defendant.getLegalEntityDefendant().get().getOrganisation().getAddress();
        }

        return Optional.of(uk.gov.moj.cpp.listing.domain.Address.address().
                withAddress1(d.isPresent() ? d.get().getAddress1() : "")
                .withAddress2(d.isPresent() ? d.get().getAddress2() : empty())
                .withAddress3(d.isPresent() ? d.get().getAddress3() : empty())
                .withAddress4(d.isPresent() ? d.get().getAddress4() : empty())
                .withAddress5(d.isPresent() ? d.get().getAddress5() : empty())
                .withPostcode(d.isPresent() ? d.get().getPostcode() : empty())
                .build());

    }

    private Optional<BailStatus> mapBailStatus(final Defendant defendant) {
        if (defendant.getPersonDefendant().isPresent()) {
            final Optional<uk.gov.justice.core.courts.BailStatus> optBailStatus = defendant.getPersonDefendant().map(PersonDefendant::getBailStatus).orElse(Optional.empty());
            return optBailStatus.map(bailStatus -> new BailStatus.Builder().withCode(bailStatus.getCode()).withDescription(bailStatus.getDescription()).withId(bailStatus.getId()).build());
        }
        return empty();
    }

    private Optional<String> getDatesToAvoid(final HearingListingNeeds commandHearing, final Defendant d) {
        final Optional<DefendantListingNeeds> listDefendantRequest = findListDefendantRequestByDefendantId(commandHearing.getDefendantListingNeeds(), d.getId());
        if (listDefendantRequest.isPresent() && listDefendantRequest.get().getDatesToAvoid().isPresent()) {
            return listDefendantRequest.get().getDatesToAvoid();
        }
        return empty();
    }

    private Optional<String> getDatesToAvoid(final HearingUnscheduledListingNeeds commandHearing, final Defendant d) {
        final Optional<DefendantListingNeeds> listDefendantRequest = findListDefendantRequestByDefendantId(commandHearing.getDefendantListingNeeds(), d.getId());
        if (listDefendantRequest.isPresent() && listDefendantRequest.get().getDatesToAvoid().isPresent()) {
            return listDefendantRequest.get().getDatesToAvoid();
        }
        return empty();
    }

    private Optional<HearingLanguageNeeds> getHearingLanguageNeeds(final HearingListingNeeds commandHearing, final Defendant d) {
        final Optional<DefendantListingNeeds> listDefendantRequest = findListDefendantRequestByDefendantId(commandHearing.getDefendantListingNeeds(), d.getId());
        if (listDefendantRequest.isPresent() && listDefendantRequest.get().getHearingLanguageNeeds().isPresent()) {
            return valueFor(listDefendantRequest.orElseThrow(IllegalArgumentException::new)
                    .getHearingLanguageNeeds().orElseThrow(IllegalArgumentException::new).toString());
        }
        return empty();
    }

    private Optional<HearingLanguageNeeds> getHearingLanguageNeeds(final HearingUnscheduledListingNeeds commandHearing, final Defendant d) {
        final Optional<DefendantListingNeeds> listDefendantRequest = findListDefendantRequestByDefendantId(commandHearing.getDefendantListingNeeds(), d.getId());
        if (listDefendantRequest.isPresent() && listDefendantRequest.get().getHearingLanguageNeeds().isPresent()) {
            return valueFor(listDefendantRequest.orElseThrow(IllegalArgumentException::new)
                    .getHearingLanguageNeeds().orElseThrow(IllegalArgumentException::new).toString());
        }
        return empty();
    }


    @SuppressWarnings({"squid:S3655"})
    public uk.gov.moj.cpp.listing.domain.Offence buildOffence(final uk.gov.justice.core.courts.Offence o, final List<UUID> shadowListedOffences) {
        boolean shadowListed = false;
        if (shadowListedOffences != null) {
            shadowListed = shadowListedOffences.stream()
                    .anyMatch(offenceId -> offenceId.equals(o.getId()));
        }

        final Offence.Builder builder = Offence.offence()
                .withId(o.getId())
                .withEndDate(o.getEndDate())
                .withStartDate(o.getStartDate())
                .withLaidDate(o.getLaidDate())
                .withOffenceCode(o.getOffenceCode())
                .withOffenceWording(o.getWording())
                .withStatementOfOffence(buildStatementOfOffence(o))
                .withSeedingHearing(o.getSeedingHearing().isPresent() ? buildSeedingHearing(o.getSeedingHearing().get()) : empty())
                .withLaaApplnReference(o.getLaaApplnReference().isPresent() ? buildLaaReference((o.getLaaApplnReference().get())) : empty())
                .withShadowListed(of(shadowListed));

        if (nonNull(o.getCommittingCourt()) && o.getCommittingCourt().isPresent()) {
            builder.withCommittingCourt(buildCommittingCourt(o.getCommittingCourt().get()));
        }
        if (!isNull(o.getReportingRestrictions()) && !o.getReportingRestrictions().isEmpty()) {
            builder.withReportingRestrictions(o.getReportingRestrictions().stream()
                    .map(ReportingRestrictionConverter::courtsToDomain)
                    .collect(toList())
            );
        }

        return builder.build();
    }

    private static Optional<uk.gov.moj.cpp.listing.domain.CommittingCourt> buildCommittingCourt(final uk.gov.justice.core.courts.CommittingCourt committingCourt) {

        return of(CommittingCourt.committingCourt().withCourtCentreId(committingCourt.getCourtCentreId())
                .withCourtHouseCode(committingCourt.getCourtHouseCode())
                .withCourtHouseName(committingCourt.getCourtHouseName())
                .withCourtHouseShortName(committingCourt.getCourtHouseShortName())
                .build());
    }

    private StatementOfOffence buildStatementOfOffence(final uk.gov.justice.core.courts.Offence offence) {
        return StatementOfOffence.statementOfOffence()
                .withTitle(offence.getOffenceTitle())
                .withLegislation(offence.getOffenceLegislation())
                .withWelshLegislation(offence.getOffenceLegislationWelsh())
                .withWelshTitle(offence.getOffenceTitleWelsh().orElse(offence.getOffenceTitle()))
                .build();
    }

    private Optional<DefendantListingNeeds> findListDefendantRequestByDefendantId(final List<DefendantListingNeeds> listDefendantRequests, final UUID defendantId) {
        return isNull(listDefendantRequests) ? empty() : listDefendantRequests.stream().filter(ldr -> ldr.getDefendantId().equals(defendantId)).findFirst();
    }

    @SuppressWarnings({"squid:S3655"})
    public CourtApplicationPartyListingNeeds buildCourtApplicationPartyNeeds(final uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds partyNeeds) {
        final Optional<HearingLanguage> hearingLanguageNeeds = partyNeeds.getHearingLanguageNeeds();
        return CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                .withCourtApplicationId(partyNeeds.getCourtApplicationId())
                .withCourtApplicationPartyId(partyNeeds.getCourtApplicationPartyId())
                .withHearingLanguageNeeds(HearingLanguageNeeds.valueFor(
                        hearingLanguageNeeds.isPresent() ? hearingLanguageNeeds.get().toString() : null).orElse(null))
                .build();
    }

    @SuppressWarnings({"squid:S3655", "squid:S1067"})
    private uk.gov.moj.cpp.listing.domain.Defendant buildDefendantsForCourtProceedings(final List<ListHearingRequest> listHearingRequests, final uk.gov.justice.core.courts.Defendant d) {

        return uk.gov.moj.cpp.listing.domain.Defendant.defendant()
                .withId(d.getId())
                .withMasterDefendantId(Optional.ofNullable(d.getMasterDefendantId()))
                .withCourtProceedingsInitiated(Optional.ofNullable(d.getCourtProceedingsInitiated()))
                .withFirstName(d.getPersonDefendant().isPresent() && d.getPersonDefendant().get().getPersonDetails().getFirstName().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getFirstName().get()) : empty())
                .withLastName(d.getPersonDefendant().isPresent() ? of(d.getPersonDefendant().get().getPersonDetails().getLastName()) : empty())
                .withBailStatus(mapBailStatus(d))
                .withDefenceOrganisation(d.getDefenceOrganisation().isPresent() ? of(d.getDefenceOrganisation().get().getName()) : empty())
                .withOrganisationName(d.getLegalEntityDefendant().isPresent() ? of(d.getLegalEntityDefendant().get().getOrganisation().getName()) : empty())
                .withSpecificRequirements(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getSpecificRequirements() : empty())
                .withDatesToAvoid(getDatesToAvoidForListHearingRequest(listHearingRequests, d))
                .withDateOfBirth(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getPersonDetails().getDateOfBirth() : empty())
                .withCustodyTimeLimit(d.getPersonDefendant().isPresent() ? d.getPersonDefendant().get().getCustodyTimeLimit() : empty())
                .withHearingLanguageNeeds(getHearingLanguageNeeds(listHearingRequests, d))
                .withOffences(d.getOffences().stream()
                        .map(offence -> buildOffence(offence, null))
                        .collect(toList()))
                .withProsecutionCaseId(d.getProsecutionCaseId())
                .build();
    }

    private Optional<String> getDatesToAvoidForListHearingRequest(final List<ListHearingRequest> commandHearing, final Defendant d) {
        final Optional<ListDefendantRequest> listDefendantRequest = findListDefendantRequestForDefendantId(
                commandHearing.stream().flatMap(lhr -> lhr.getListDefendantRequests().stream()).collect(Collectors.toList()), d.getId());
        if (listDefendantRequest.isPresent() && listDefendantRequest.get().getDatesToAvoid().isPresent()) {
            return listDefendantRequest.get().getDatesToAvoid();
        }
        return empty();
    }

    private Optional<HearingLanguageNeeds> getHearingLanguageNeeds(final List<ListHearingRequest> commandHearing, final Defendant d) {
        final Optional<ListDefendantRequest> listDefendantRequest = findListDefendantRequestForDefendantId(
                commandHearing.stream().flatMap(lhr -> lhr.getListDefendantRequests().stream()).collect(Collectors.toList()), d.getId());
        if (listDefendantRequest.isPresent() && listDefendantRequest.get().getHearingLanguageNeeds().isPresent()) {
            return valueFor(listDefendantRequest.orElseThrow(IllegalArgumentException::new)
                    .getHearingLanguageNeeds().orElseThrow(IllegalArgumentException::new).toString());
        }
        return empty();
    }

    public Optional<ListDefendantRequest> findListDefendantRequestForDefendantId(final List<ListDefendantRequest> listDefendantRequests, final UUID defendantId) {
        return isNull(listDefendantRequests) ? empty() : listDefendantRequests.stream().filter(ldr -> ldr.getDefendantId().toString().equals(defendantId.toString())).findFirst();
    }

    public List<uk.gov.moj.cpp.listing.domain.Defendant> convertDefendant(final List<Defendant> commandDefendants, final List<ListHearingRequest> listHearingRequests) {
        return commandDefendants.stream().map(d -> buildDefendantsForCourtProceedings(listHearingRequests, d)).collect(Collectors.toList());
    }

    private Optional<uk.gov.moj.cpp.listing.domain.LaaReference> buildLaaReference(final LaaReference laaReference) {

        return Optional.of(uk.gov.moj.cpp.listing.domain.LaaReference.laaReference()
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveEndDate((laaReference.getEffectiveEndDate()))
                .withEffectiveStartDate((laaReference.getEffectiveStartDate()))
                .withStatusCode(laaReference.getStatusCode())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusDescription(laaReference.getStatusDescription())
                .withStatusId(laaReference.getStatusId())
                .build());
    }

    public Optional<uk.gov.moj.cpp.listing.domain.SeedingHearing> buildSeedingHearing(final SeedingHearing seedingHearing) {

        return Optional.of(uk.gov.moj.cpp.listing.domain.SeedingHearing.seedingHearing()
                .withJurisdictionType(JurisdictionType.valueOf(seedingHearing.getJurisdictionType().name()))
                .withSittingDay(seedingHearing.getSittingDay())
                .withSeedingHearingId(seedingHearing.getSeedingHearingId())
                .build());
    }

    private NonDefaultDay convertNonDefaultDay(final RotaSlot rotaSlot) {
        return NonDefaultDay.nonDefaultDay()
                .withSession(rotaSlot.getSession())
                .withOucode(rotaSlot.getOucode())
                .withCourtRoomId(rotaSlot.getCourtRoomId())
                .withDuration(rotaSlot.getDuration())
                .withStartTime(rotaSlot.getStartTime())
                .withCourtScheduleId(rotaSlot.getCourtScheduleId())
                .withRoomId(rotaSlot.getRoomId())
                .withCourtCentreId(rotaSlot.getCourtCentreId())
                .build();
    }

    public uk.gov.justice.listing.events.TypeOfList convertTypeOfList(final TypeOfList typeOfList) {
        return uk.gov.justice.listing.events.TypeOfList.typeOfList()
                .withId(typeOfList.getId())
                .withDescription(typeOfList.getDescription())
                .build();
    }

    public List<CourtApplicationPartyListingNeeds> getCourtApplicationPartyListingNeeds(final HearingUnscheduledListingNeeds commandHearing) {
        return isNull(commandHearing.getCourtApplicationPartyListingNeeds())
                ? emptyList() : commandHearing.getCourtApplicationPartyListingNeeds().stream()
                .map(this::buildCourtApplicationPartyNeeds).collect(toList());
    }

    public List<uk.gov.moj.cpp.listing.domain.CourtApplication> getCourtApplications(final HearingUnscheduledListingNeeds commandHearing) {
        return isEmpty(commandHearing.getCourtApplications()) ? emptyList() : commandHearing.getCourtApplications()
                .stream().map(ca -> new CourtApplicationToDomainConverter().convert(ca)).collect(toList());
    }

    public JurisdictionType getJurisdictionType(final HearingUnscheduledListingNeeds commandHearing) {
        return JurisdictionType.valueFor(commandHearing.getJurisdictionType().name())
                .orElseThrow(IllegalArgumentException::new);
    }

    public List<JudicialRole> getJudicialRoles(final HearingUnscheduledListingNeeds commandHearing) {
        final List<JudicialRole> domainJudicialRoles;

        if (commandHearing.getJudiciary() != null) {
            domainJudicialRoles = commandHearing.getJudiciary().stream()
                    .map(this::buildJudiciary)
                    .collect(toList());
        } else {
            domainJudicialRoles = emptyList();
        }
        return domainJudicialRoles;
    }

    public Optional<LocalDate> getWeekCommencingEndDate(final Optional<LocalDate> weekCommencingStartDate, final Optional<Integer> weekCommencingDurationInWeeks) {
        return weekCommencingStartDate.isPresent() && weekCommencingDurationInWeeks.isPresent() ?
                of(weekCommencingStartDate.get().plusWeeks(weekCommencingDurationInWeeks.get())) : empty();
    }

    public Optional<Integer> getWeekCommencingDurationInWeeks(final HearingUnscheduledListingNeeds commandHearing) {
        return commandHearing.getWeekCommencingDate().isPresent() ? commandHearing.getWeekCommencingDate().get().getDuration() : empty();
    }

    public Optional<LocalDate> getWeekCommencingStartDate(final HearingUnscheduledListingNeeds commandHearing) {
        return commandHearing.getWeekCommencingDate().isPresent() && nonNull(commandHearing.getWeekCommencingDate().get().getStartDate()) ?
                of(LocalDate.parse(commandHearing.getWeekCommencingDate().get().getStartDate())) : empty();
    }

    public CourtCentreDefaults getCourtCentreDefaults(final Map<UUID, CourtCentreDetails> courtCentres, final HearingUnscheduledListingNeeds commandHearing) {
        final CourtCentreDetails courtCentre = courtCentres.get(commandHearing.getCourtCentre().getId());

        return CourtCentreDefaults.courtCentreDefaults()
                .withDefaultDuration(courtCentre.getDefaultDuration())
                .withDefaultStartTime(courtCentre.getDefaultStartTime())
                .withCourtCentreId(courtCentre.getId())
                .build();
    }
}


