package uk.gov.moj.cpp.listing.domain.aggregate;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.valueFor;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2.allocatedHearingExtendedForListingV2;
import static uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2.allocatedHearingUpdatedForListingV2;
import static uk.gov.justice.listing.events.AvailableSlotsForHearingFreed.availableSlotsForHearingFreed;
import static uk.gov.justice.listing.events.CourtRoomChangedForHearing.courtRoomChangedForHearing;
import static uk.gov.justice.listing.events.DefendantCourtProceedingsUpdatedV2.defendantCourtProceedingsUpdatedV2;
import static uk.gov.justice.listing.events.EndDateChangedForHearing.endDateChangedForHearing;
import static uk.gov.justice.listing.events.EndDateRemovedFromHearing.endDateRemovedFromHearing;
import static uk.gov.justice.listing.events.HearingAllocatedForListingV2.hearingAllocatedForListingV2;
import static uk.gov.justice.listing.events.HearingDaysCancelled.hearingDaysCancelled;
import static uk.gov.justice.listing.events.HearingDaysChangedForHearing.hearingDaysChangedForHearing;
import static uk.gov.justice.listing.events.HearingDaysSequenced.hearingDaysSequenced;
import static uk.gov.justice.listing.events.HearingListed.hearingListed;
import static uk.gov.justice.listing.events.HearingUnallocatedForListing.hearingUnallocatedForListing;
import static uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing.nonDefaultDaysChangedForHearing;
import static uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing.nonSittingDaysAssignedToHearing;
import static uk.gov.justice.listing.events.NonSittingDaysChangedForHearing.nonSittingDaysChangedForHearing;
import static uk.gov.justice.listing.events.StartDateChangedForHearing.startDateChangedForHearing;
import static uk.gov.justice.listing.events.StartDateRemovedForHearing.startDateRemovedForHearing;
import static uk.gov.justice.listing.events.TrialVacated.trialVacated;
import static uk.gov.justice.listing.events.TypeChangedForHearing.typeChangedForHearing;
import static uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing.weekCommencingDateChangedForHearing;
import static uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing.weekCommencingDateRemovedForHearing;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.MAGISTRATES;
import static uk.gov.moj.cpp.listing.domain.aggregate.HearingDaysCalculator.calculate;
import static uk.gov.moj.cpp.listing.domain.aggregate.HearingDaysCalculator.calculateNewNonDefaultDaysForUnscheduled;
import static uk.gov.moj.cpp.listing.domain.aggregate.NewDomainToEventConverter.updateEventDefendant;
import static uk.gov.moj.cpp.listing.domain.event.CourtToEventConverter.buildListedCase;
import static uk.gov.moj.cpp.listing.domain.utils.HearingUtil.getAdjustedDuration;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.event.CourtApplicationHearingDeleted;
import uk.gov.justice.listing.events.AddedCasesForHearing;
import uk.gov.justice.listing.events.AllocatedHearingDeleted;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListing;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2;
import uk.gov.justice.listing.events.ApplicationEjected;
import uk.gov.justice.listing.events.CaseEjected;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CaseIdentifierUpdated;
import uk.gov.justice.listing.events.CaseRemovedFromGroupCases;
import uk.gov.justice.listing.events.CaseUpdateDefendantProceedingsUpdated;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtApplicationUpdatedForHearing;
import uk.gov.justice.listing.events.CourtCentreChangedForHearing;
import uk.gov.justice.listing.events.CourtCentreDetails;
import uk.gov.justice.listing.events.CourtListRestricted;
import uk.gov.justice.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdatedV2;
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing;
import uk.gov.justice.listing.events.EndDateChangedForHearing;
import uk.gov.justice.listing.events.EndDateRemovedFromHearing;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingChangesSaved;
import uk.gov.justice.listing.events.HearingDayCourtSchedule;
import uk.gov.justice.listing.events.HearingDayCourtScheduleUpdated;
import uk.gov.justice.listing.events.HearingDaysCancelled;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDaysSequenced;
import uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.HearingLanguageChangedForHearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingListedCaseUpdated;
import uk.gov.justice.listing.events.HearingMarkedAsDeleted;
import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.listing.events.HearingMarkedForPartialUpdate;
import uk.gov.justice.listing.events.HearingPartiallyUpdated;
import uk.gov.justice.listing.events.HearingRequestedForListing;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.HearingResultStatusUpdated;
import uk.gov.justice.listing.events.HearingTrialVacated;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.HearingsUpdateCompleted;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.JudiciaryAssignedToHearing;
import uk.gov.justice.listing.events.JudiciaryChangedForHearing;
import uk.gov.justice.listing.events.JudiciaryChangedForHearingsStatus;
import uk.gov.justice.listing.events.JudiciaryRemovedFromHearing;
import uk.gov.justice.listing.events.JurisdictionChangedForHearing;
import uk.gov.justice.listing.events.LinkedCasesUpdated;
import uk.gov.justice.listing.events.NewCaseMarkerUpdated;
import uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings;
import uk.gov.justice.listing.events.NewDefendantDetailsUpdated;
import uk.gov.justice.listing.events.NextHearingDayChanged;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2;
import uk.gov.justice.listing.events.ProsecutionCases;
import uk.gov.justice.listing.events.PublicListNoteChangedForHearing;
import uk.gov.justice.listing.events.PublicListNoteRemovedFromHearing;
import uk.gov.justice.listing.events.SequencesResetOnHearingDays;
import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.listing.events.StartDateRemovedForHearing;
import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.justice.listing.events.TypeOfList;
import uk.gov.justice.listing.events.UnallocatedHearingDeleted;
import uk.gov.justice.listing.events.VideoLinkChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing;
import uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing;
import uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing;
import uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyListingNeeds;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.DefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.HearingLanguage;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.LinkedToCases;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.OffenceIds;
import uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.RestrictCourtList;
import uk.gov.moj.cpp.listing.domain.SeedingHearing;
import uk.gov.moj.cpp.listing.domain.SequenceHearing;
import uk.gov.moj.cpp.listing.domain.SimpleOffence;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.domain.aggregate.rules.HearingLanguageRule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1172", "squid:S2629", "squid:S1948", "squid:S00107", "squid:S3655", "squid:S1067", "squid:CommentedOutCodeLine", "squid:S1068", "squid:S4973", "PMD.BeanMembersShouldSerialize", "PMD.NullAssignment"})
public class Hearing implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hearing.class);

    private static final long serialVersionUID = 2971669648937137985L;

    private static final String SUMMONS_APPROVED_RESULT_TYPE_ID = "0f44eeb9-2c81-430d-9a60-bbdaf8c4a093";
    private static final String SUMMONS_REJECTED_RESULT_TYPE_ID = "d8837a45-8281-49b3-8349-49b423193148";
    public static final String SOURCE_LISTING = "Listing";
    public static final String SOURCE_HEARING = "Hearing";
    private static final String HEARING_DAYS_NOT_ASSIGNED = "HearingDays for hearing with id {} is not assigned. Should have been assigned when first listed";

    private final List<uk.gov.moj.cpp.listing.domain.aggregate.ListedCase> unAllocatedListedCases = new ArrayList<>();
    private UUID hearingId;
    private Type type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer estimatedMinutes;
    private String estimatedDuration;
    private UUID courtRoomId;
    private UUID courtCentreId;
    private List<JudicialRole> judiciary = emptyList();
    private List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds;
    private JurisdictionType jurisdictionType;
    private HearingLanguage hearingLanguage;
    private String reportingRestrictionReason;
    private List<NonDefaultDay> nonDefaultDays;
    private List<LocalDate> nonSittingDays;
    private List<HearingDay> hearingDays;
    private List<UUID> confirmedCourtApplicationIds = new ArrayList<>();
    private LocalDate weekCommencingStartDate;
    private LocalDate weekCommencingEndDate;
    private Integer weekCommencingDurationInWeeks;
    private boolean updateSlot;
    private boolean hasAdjournmentDate;
    private boolean hasVideoLink;
    private String publicListNote;
    private boolean duplicate;
    private boolean deleted;
    private boolean resulted = false;
    private Map<UUID, List<UUID>> prosecutionCaseDefendants = new HashMap<>();
    private Map<UUID, List<UUID>> applicationOffenceIds = new HashMap<>();
    private uk.gov.justice.listing.events.Hearing currentHearingEventState;

    private boolean isSummonsApprovedExists = false;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingListed.class).apply(this::onHearingListed),
                when(HearingListedCaseUpdated.class).apply(this::onHearingListedCaseUpdated),
                when(HearingLanguageChangedForHearing.class).apply(this::onHearingLanguageChanged),
                when(TypeChangedForHearing.class).apply(this::onTypeChangedForHearing),
                when(StartDateChangedForHearing.class).apply(this::onStartDateChangedForHearing),
                when(StartDateRemovedForHearing.class).apply(this::onStartDateRemovedForHearing),
                when(EndDateChangedForHearing.class).apply(this::onEndDateChangedForHearing),
                when(EndDateRemovedFromHearing.class).apply(this::onEndDateRemovedFromHearing),
                when(WeekCommencingDateChangedForHearing.class).apply(this::onWeekCommencingDateChangedForHearing),
                when(WeekCommencingDateRemovedForHearing.class).apply(this::onWeekCommencingDateRemovedForHearing),
                when(NonSittingDaysChangedForHearing.class).apply(this::onNonSittingDaysChangedForHearing),
                when(NonSittingDaysAssignedToHearing.class).apply(this::onNonSittingDaysAssignedToHearing),
                when(NonDefaultDaysChangedForHearing.class).apply(this::onNonDefaultDaysChangedForHearing),
                when(NonDefaultDaysAssignedToHearing.class).apply(this::onNonDefaultDaysAssignedToHearing),
                when(HearingDaysChangedForHearing.class).apply(this::onHearingDaysChangedForHearing),
                when(HearingDayCourtScheduleUpdated.class).apply(this::onHearingDayCourtScheduleUpdated),
                when(HearingDaysCancelled.class).apply(this::onHearingDaysCancelledForHearing),
                when(JurisdictionChangedForHearing.class).apply(this::onJurisdictionChangedForHearing),
                when(JudiciaryAssignedToHearing.class).apply(this::onJudiciaryAssignedToHearing),
                when(JudiciaryChangedForHearing.class).apply(this::onJudiciaryChangedForHearing),
                when(JudiciaryRemovedFromHearing.class).apply(this::onJudiciaryRemovedFromHearing),
                when(CourtRoomAssignedToHearing.class).apply(this::onCourtRoomAssignedToHearing),
                when(CourtRoomChangedForHearing.class).apply(this::onCourtRoomChangedForHearing),
                when(CourtRoomRemovedFromHearing.class).apply(this::onCourtRoomRemovedFromHearing),
                when(VideoLinkChangedForHearing.class).apply(this::onVideoLinkChangedForHearing),
                when(VideoLinkDetailsAssignedForHearing.class).apply(this::onVideoLinkAssignedForHearing),
                when(VideoLinkDetailsRemovedForHearing.class).apply(this::onVideoLinkRemovedForHearing),
                when(VideoLinkDetailsChangedForHearing.class).apply(this::onVideoLinkChangedForHearing),
                when(PublicListNoteChangedForHearing.class).apply(this::onPublicListNoteChangedForHearing),
                when(PublicListNoteRemovedFromHearing.class).apply(this::onPublicListNoteRemovedFromHearing),
                when(CourtCentreChangedForHearing.class).apply(this::onCourtCentreChangedForHearing),
                when(HearingAllocatedForListing.class).apply(this::onHearingAllocatedForListing),
                when(HearingAllocatedForListingV2.class).apply(this::onHearingAllocatedForListingV2),
                when(AllocatedHearingUpdatedForListing.class).apply(this::onAllocatedHearingUpdatedForListing),
                when(AllocatedHearingUpdatedForListingV2.class).apply(this::onAllocatedHearingUpdatedForListingV2),
                when(HearingUnallocatedForListing.class).apply(this::onHearingUnallocatedForListing),
                when(NewDefendantDetailsUpdated.class).apply(this::onNewDefendantDetailsUpdated),
                when(OffenceUpdated.class).apply(this::onOffenceUpdated),
                when(OffenceDeleted.class).apply(this::onOffenceDeleted),
                when(OffenceAdded.class).apply(this::onOffenceAdded),
                when(HearingDaysSequenced.class).apply(this::onHearingDaysSequenced),
                when(SequencesResetOnHearingDays.class).apply(this::onSequencesResetOnHearingDays),
                when(CourtApplicationUpdatedForHearing.class).apply(this::onCourtApplicationUpdatedForHearing),
                when(NewDefendantAddedForCourtProceedings.class).apply(this::onNewDefendantAddedForCourtProceedings),
                when(CaseUpdateDefendantProceedingsUpdated.class).apply(this::onCaseUpdateDefendantProceedingsUpdated),
                when(CaseEjected.class).apply(e -> onCaseEjected()),
                when(ApplicationEjected.class).apply(e -> onApplicationEjected()),
                when(HearingDaysWithoutCourtCentreCorrected.class).apply(this::onHearingDaysWithoutCourtCentreCorrected),
                when(HearingMarkedAsDuplicate.class).apply(this::onHearingMarkedAsDuplicate),
                when(AddedCasesForHearing.class).apply(this::onAddedCasesForHearing),
                when(HearingDeleted.class).apply(this::onHearingDeleted),
                when(AllocatedHearingDeleted.class).apply(this::onAllocatedHearingDeleted),
                when(UnallocatedHearingDeleted.class).apply(this::onUnallocatedHearingDeleted),
                when(OffencesRemovedFromHearing.class).apply(this::onOffencesRemovedFromHearing),
                when(CasesAddedToHearing.class).apply(this::onCasesAddedToHearing),
                when(AllocatedHearingExtendedForListingV2.class).apply(this::onAllocatedHearingExtendedForListingV2),
                when(AllocatedHearingExtendedForListing.class).apply(this::onAllocatedHearingExtendedForListing),
                when(OffencesRemovedFromExistingAllocatedHearing.class).apply(this::handleOffencesRemovedFromExistingAllocatedHearing),
                when(OffencesRemovedFromExistingUnallocatedHearing.class).apply(this::handleOffencesRemovedFromExistingUnallocatedHearing),
                when(CaseIdentifierUpdated.class).apply(this::onCaseIdentifierUpdated),
                when(HearingPartiallyUpdated.class).apply(this::onHearingPartiallyUpdated),
                when(NewCaseMarkerUpdated.class).apply(this::onNewCaseMarkerUpdated),
                when(DefendantCourtProceedingsUpdatedV2.class).apply(this::onDefendantCourtProceedingsUpdatedV2),
                when(AllocatedHearingExtendedForListingV2.class).apply(this::onAllocatedHearingExtendedForListingV2),
                when(AllocatedHearingExtendedForListing.class).apply(this::onAllocatedHearingExtendedForListing),
                when(CaseRemovedFromGroupCases.class).apply(this::onCaseRemovedFromGroupCases),
                when(HearingsUpdateCompleted.class).apply(this::onHearingsUpdateCompleted),
                when(HearingResultStatusUpdated.class).apply(this::onHearingResultStatusUpdated),
                otherwiseDoNothing());
    }

    private void onDefendantCourtProceedingsUpdatedV2(final DefendantCourtProceedingsUpdatedV2 defendantCourtProceedingsUpdatedV2) {
        updateCurrentHearingEventStateOnCaseAdded(asList(buildListedCase(defendantCourtProceedingsUpdatedV2.getProsecutionCase(), emptyList())));

        if (defendantCourtProceedingsUpdatedV2.getProsecutionCase().getDefendants().stream()
                .map(uk.gov.justice.core.courts.Defendant::getDefendantCaseJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .anyMatch(judicialResult -> judicialResult.getJudicialResultTypeId().equals(UUID.fromString(SUMMONS_APPROVED_RESULT_TYPE_ID)))) {
            isSummonsApprovedExists = true;
        }

        if (defendantCourtProceedingsUpdatedV2.getProsecutionCase().getDefendants().stream()
                .map(uk.gov.justice.core.courts.Defendant::getDefendantCaseJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .anyMatch(judicialResult -> judicialResult.getJudicialResultTypeId().equals(UUID.fromString(SUMMONS_REJECTED_RESULT_TYPE_ID)))) {
            isSummonsApprovedExists = false;
        }
    }

    private void onNewCaseMarkerUpdated(final NewCaseMarkerUpdated newCaseMarkerUpdated) {
        this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(this.currentHearingEventState)
                .withListedCases(this.currentHearingEventState.getListedCases().stream()
                        .map(listedCase -> !listedCase.getId().equals(newCaseMarkerUpdated.getCaseId()) ? listedCase :
                                uk.gov.justice.listing.events.ListedCase.listedCase().withValuesFrom(listedCase)
                                        .withMarkers(newCaseMarkerUpdated.getCaseMarkers())
                                        .build())
                        .collect(toList()))
                .build();

    }

    private void onHearingPartiallyUpdated(final HearingPartiallyUpdated hearingPartiallyUpdated) {
        hearingPartiallyUpdated.getProsecutionCases().forEach(prosecutionCase ->
                prosecutionCase.getDefendants().forEach(defendant ->
                        defendant.getOffences().forEach(offence ->
                                this.currentHearingEventState.getListedCases().stream()
                                        .filter(listedCase -> listedCase.getId().equals(prosecutionCase.getCaseId()))
                                        .flatMap(listedCase -> listedCase.getDefendants().stream())
                                        .filter(defendant1 -> defendant1.getId().equals(defendant.getDefendantId()))
                                        .forEach(defendant1 -> defendant1.getOffences().removeIf(offence1 -> offence1.getId().equals(offence.getOffenceId())))
                        ))
        );

        currentHearingEventState.getListedCases().forEach(listedCase -> listedCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
        currentHearingEventState.getListedCases().removeIf(pc -> pc.getDefendants().isEmpty());


        hearingPartiallyUpdated.getProsecutionCases().forEach(prosecutionCase ->
                prosecutionCase.getDefendants().forEach(defendant ->
                        defendant.getOffences().forEach(offence ->
                                prosecutionCaseDefendantOffenceIds.stream()
                                        .filter(listedCase -> listedCase.getId().equals(prosecutionCase.getCaseId()))
                                        .flatMap(listedCase -> listedCase.getDefendants().stream())
                                        .filter(defendant1 -> defendant1.getId().equals(defendant.getDefendantId()))
                                        .forEach(defendant1 -> defendant1.getOffences().removeIf(offence1 -> offence1.getId().equals(offence.getOffenceId())))
                        ))
        );
        prosecutionCaseDefendantOffenceIds.forEach(listedCase -> listedCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
        prosecutionCaseDefendantOffenceIds.removeIf(pc -> pc.getDefendants().isEmpty());
    }

    private void onCaseIdentifierUpdated(final CaseIdentifierUpdated caseIdentifierUpdated) {
        if (nonNull(this.currentHearingEventState) && nonNull(this.currentHearingEventState.getListedCases())) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withListedCases(currentHearingEventState.getListedCases().stream()
                            .map(listedCase -> listedCase.getId().equals(caseIdentifierUpdated.getProsecutionCaseId()) ? uk.gov.justice.listing.events.ListedCase.listedCase().withValuesFrom(listedCase)
                                    .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                            .withValuesFrom(listedCase.getCaseIdentifier())
                                            .withAuthorityId(caseIdentifierUpdated.getProsecutionAuthorityId())
                                            .withAuthorityCode(caseIdentifierUpdated.getProsecutionAuthorityCode())
                                            .build())
                                    .build() : listedCase)
                            .collect(toList()))
                    .build();
        }
    }

    private void handleOffencesRemovedFromExistingUnallocatedHearing(final OffencesRemovedFromExistingUnallocatedHearing offencesRemovedFromExistingUnallocatedHearing) {
        removeOffences(offencesRemovedFromExistingUnallocatedHearing.getOffenceIds());
    }

    private void handleOffencesRemovedFromExistingAllocatedHearing(final OffencesRemovedFromExistingAllocatedHearing offencesRemovedFromExistingAllocatedHearing) {
        removeOffences(offencesRemovedFromExistingAllocatedHearing.getOffenceIds());
    }

    @SuppressWarnings({"squid:S00107", "squid:S3776"})
    public Stream<Object> list(final UUID hearingId, final Type type,
                               final int estimateMinutes, final String estimatedDuration, final List<ListedCase> listedCases,
                               final UUID courtCentreId, final List<JudicialRole> judiciary,
                               final UUID courtRoomId, final String listingDirections,
                               final JurisdictionType jurisdictionType, final String prosecutorDatesToAvoid,
                               final String reportingRestrictionReason,
                               final ZonedDateTime startDate, final LocalDate endDate, final CourtCentreDefaults courtCentreDefaults,
                               final List<CourtApplication> courtApplications, final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds,
                               final Optional<String> adjournedFromDate, final Optional<LocalDate> weekCommencingStartDate,
                               final Optional<LocalDate> weekCommencingEndDate, final Optional<Integer> weekCommencingDurationInWeeks,
                               final List<uk.gov.moj.cpp.listing.domain.HearingDay> hearingDays, final List<NonDefaultDay> nonDefaultDays, final List<LocalDate> nonSittingDays,
                               final Boolean isSlotsBooked,
                               final String bookingType,
                               final String priority,
                               final List<String> specialRequirements,
                               final Optional<Boolean> isPossibleDisqualification,
                               final Optional<Boolean> isGroupProceedings,
                               final Optional<Integer> numberOfGroupCases) {

        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (notCurrentlyListed()) {
            final CourtCentre defaultCourtCentre = CourtCentre.courtCentre()
                    .withId(courtCentreId)
                    .withRoomId(courtRoomId).build();

            final uk.gov.justice.listing.events.Hearing.Builder builder = uk.gov.justice.listing.events.Hearing.hearing();
            builder.withId(hearingId)
                    .withType(uk.gov.justice.listing.events.Type.type()
                            .withId(type.getId())
                            .withDescription(type.getDescription())
                            .withWelshDescription(type.getWelshDescription())
                            .build())
                    .withAllocated(false)
                    .withAdjournedFromDate(adjournedFromDate.orElse(null))
                    .withJudiciary(judiciary.stream()
                            .map(NewDomainToEventConverter::buildJudicialRole)
                            .collect(toList()))
                    .withListedCases(listedCases.isEmpty() ? null : listedCases.stream()
                            .map(NewDomainToEventConverter::buildListedCase)
                            .collect(toList()))
                    .withListingDirections(listingDirections)
                    .withHearingLanguage(HearingLanguageRule.apply(listedCases, getHearingLanguageNeedsForAllApplicants(courtApplicationPartyListingNeeds)))
                    .withReportingRestrictionReason(reportingRestrictionReason)
                    .withCourtRoomId(defaultCourtCentre.getRoomId())
                    .withCourtCentreId(defaultCourtCentre.getId())
                    .withEstimatedMinutes(estimateMinutes)
                    .withEstimatedDuration(estimatedDuration)
                    .withProsecutorDatesToAvoid(prosecutorDatesToAvoid)
                    .withJurisdictionType(valueFor(jurisdictionType.name()).orElse(null))
                    .withIsSlotsBooked(isSlotsBooked)
                    .withIsGroupProceedings(isGroupProceedings.orElse(null))
                    .withNumberOfGroupCases(numberOfGroupCases.orElse(null))
                    .withCourtCentreDetails(nonNull(courtCentreDefaults) ? CourtCentreDetails.courtCentreDetails()
                            .withDefaultDuration(courtCentreDefaults.getDefaultDuration())
                            .withId(courtCentreDefaults.getCourtCentreId())
                            .withDefaultStartTime(courtCentreDefaults.getDefaultStartTime())
                            .build() : null);
            builder.withCourtApplications(courtApplications.stream()
                    .map(NewDomainToEventConverter::buildCourtApplications)
                    .collect((toList())));

            if (nonNull(startDate)) {
                builder.withStartDate(startDate.toLocalDate());
                builder.withNonDefaultDays(NewDomainToEventConverter.convertNonDefaultDaysDomainToEvent(nonDefaultDays));
                builder.withEndDate(endDate);
                builder.withNonSittingDays(nonSittingDays);
                builder.withHearingDays(NewDomainToEventConverter.convertHearingDaysDomainToEvent(hearingDays));
            } else {
                builder.withHearingDays(emptyList());
                builder.withNonDefaultDays(emptyList());
                builder.withNonSittingDays(emptyList());
                builder.withWeekCommencingDurationInWeeks(weekCommencingDurationInWeeks.orElse(null))
                        .withWeekCommencingStartDate(weekCommencingStartDate.orElse(null))
                        .withWeekCommencingEndDate(weekCommencingEndDate.orElse(null));
            }

            builder.withBookingType(bookingType);
            builder.withPriority(priority);
            builder.withSpecialRequirements(specialRequirements);

            isPossibleDisqualification.ifPresent(builder::withIsPossibleDisqualification);
            return apply(Stream.of(hearingListed()
                    .withHearing(builder.build())
                    .build()));
        } else {
            LOGGER.error("Cannot list hearing with id {} as it has already been listed", hearingId);
            return Stream.empty();
        }
    }

    @SuppressWarnings({"squid:S00107", "squid:S3776"})
    public Stream<Object> listForSplit(final Type type,
                                       final List<uk.gov.justice.listing.events.ListedCase> listedCases,
                                       final UUID courtCentreId,
                                       final String courtCenterName,
                                       final UUID courtRoomId,
                                       final JurisdictionType jurisdictionType,
                                       final ZonedDateTime startDate,
                                       final LocalDate weekCommencingStartDate,
                                       final Integer weekCommencingDurationInWeeks,
                                       final List<uk.gov.justice.core.courts.JudicialRole> judiciary,
                                       final List<NonDefaultDay> nonDefaultDays) {

        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }
        final CourtCentre defaultCourtCentre = CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withRoomId(courtRoomId)
                .withName(courtCenterName).build();

        final CourtHearingRequest.Builder builder = CourtHearingRequest.courtHearingRequest();
        builder.withCourtCentre(defaultCourtCentre)
                .withEstimatedMinutes(30)
                .withHearingType(HearingType.hearingType()
                        .withId(type.getId())
                        .withDescription(type.getDescription())
                        .withWelshDescription(type.getWelshDescription())
                        .build())
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.valueOf(jurisdictionType.name()))
                .withEarliestStartDateTime(startDate);

        if (isNotEmpty(judiciary)) {
            builder.withJudiciary(judiciary);
        }

        if (isNotEmpty(nonDefaultDays)) {
            builder.withNonDefaultDays(convertDomainToCore(nonDefaultDays));
        }

        if (nonNull(weekCommencingStartDate)) {
            builder.withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                    .withStartDate(weekCommencingStartDate.toString())
                    .withDuration(weekCommencingDurationInWeeks)
                    .build());
        }
        builder.withListDefendantRequests(listedCases.stream().flatMap(listedCase ->
                listedCase.getDefendants().stream().map(defendant -> ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendant.getId())
                        .withProsecutionCaseId(listedCase.getId())
                        .withDefendantOffences(defendant.getOffences().stream().map(Offence::getId).collect(toList()))
                        .withHearingLanguageNeeds(HearingLanguageRule.applyForEvent(listedCases, new ArrayList<>()))
                        .build()
                )
        ).collect(toList()));

        return apply(Stream.of(HearingRequestedForListing.hearingRequestedForListing()
                .withListNewHearing(builder.build())
                .build()));
    }


    @SuppressWarnings({"squid:S00107"})
    public Stream<Object> listUnscheduled(final UUID hearingId,
                                          final Type type,
                                          final List<ListedCase> listedCases,
                                          final UUID courtCentreId,
                                          final List<JudicialRole> judiciary,
                                          final UUID courtRoomId,
                                          final String listingDirections,
                                          final JurisdictionType jurisdictionType,
                                          final String prosecutorDatesToAvoid,
                                          final String reportingRestrictionReason,
                                          final ZonedDateTime startDate,
                                          final LocalDate endDate,
                                          final CourtCentreDefaults courtCentreDefaults,
                                          final List<CourtApplication> courtApplications,
                                          final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds,
                                          final Integer hearingTypeDuration,
                                          final Optional<LocalDate> weekCommencingStartDate,
                                          final Optional<LocalDate> weekCommencingEndDate,
                                          final Optional<Integer> weekCommencingDurationInWeeks,
                                          final TypeOfList typeOfList) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(courtCentreId).withRoomId(courtRoomId).build();

        final ZonedDateTime newStartDate = startDate != null ? startDate : ZonedDateTime.now();

        final LocalDate localStartDate = newStartDate.toLocalDate();

        final List<uk.gov.justice.listing.events.NonDefaultDay> newNonDefaultDays = calculateNewNonDefaultDaysForUnscheduled(
                hearingTypeDuration, newStartDate, courtCentreDefaults.getDefaultStartTime(), courtCentre);

        final CourtCentre defaultCourtCentre = CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withRoomId(courtRoomId).build();


        return apply(Stream.of(hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type()
                                .withId(type.getId())
                                .withDescription(type.getDescription())
                                .withWelshDescription(type.getWelshDescription())
                                .build())
                        .withAllocated(false)
                        .withUnscheduled(true)
                        .withEstimatedMinutes(hearingTypeDuration)
                        .withTypeOfList(typeOfList)
                        .withJudiciary(judiciary.stream()
                                .map(NewDomainToEventConverter::buildJudicialRole)
                                .collect(toList()))
                        .withListedCases(listedCases.isEmpty() ? null : listedCases.stream()
                                .map(NewDomainToEventConverter::buildListedCase)
                                .collect(toList()))
                        .withListingDirections(listingDirections)
                        .withHearingLanguage(HearingLanguageRule.apply(listedCases, getHearingLanguageNeedsForAllApplicants(courtApplicationPartyListingNeeds)))
                        .withReportingRestrictionReason(reportingRestrictionReason)
                        .withCourtRoomId(defaultCourtCentre.getRoomId())
                        .withCourtCentreId(defaultCourtCentre.getId())
                        .withProsecutorDatesToAvoid(prosecutorDatesToAvoid)
                        .withJurisdictionType(valueFor(jurisdictionType.name()).orElse(null))
                        .withStartDate(startDate != null ? startDate.toLocalDate() : null)
                        .withEndDate(endDate)
                        .withNonDefaultDays(newNonDefaultDays)
                        .withNonSittingDays(emptyList())
                        .withHearingDays(calculate(localStartDate, endDate, emptyList(), convertEventToDomain(newNonDefaultDays), courtCentreDefaults.getDefaultStartTime(), hearingTypeDuration, defaultCourtCentre))
                        .withCourtApplications(courtApplications.stream()
                                .map(NewDomainToEventConverter::buildCourtApplications)
                                .collect((toList())))
                        .withWeekCommencingDurationInWeeks(weekCommencingDurationInWeeks.orElse(null))
                        .withWeekCommencingStartDate(weekCommencingStartDate.orElse(null))
                        .withWeekCommencingEndDate(weekCommencingEndDate.orElse(null))
                        .build())
                .build()));
    }

    @SuppressWarnings("squid:S3358")
    private List<uk.gov.justice.listing.events.NonDefaultDay> generateNewNonDefaultDays(final int estimateMinutes,
                                                                                        final ZonedDateTime startDate,
                                                                                        final Integer hearingTypeDuration,
                                                                                        final List<NonDefaultDay> nonDefaultDays,
                                                                                        final UUID courtCentreId,
                                                                                        final UUID courtRoomId) {
        final List<uk.gov.justice.listing.events.NonDefaultDay> newList = new ArrayList<>();
        if (nonDefaultDays.isEmpty()) {
            newList.addAll(ImmutableList.of(uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay()
                    .withCourtCentreId(ofNullable(courtCentreId).map(UUID::toString).orElse(null))
                    .withRoomId(ofNullable(courtRoomId).map(UUID::toString).orElse(null))
                    .withDuration(estimateMinutes > 0 ? estimateMinutes : getAdjustedDuration(hearingTypeDuration))
                    .withStartTime(startDate)
                    .build()));
        } else {
            nonDefaultDays.forEach(ndd ->
                    newList.add(uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay()
                            .withCourtScheduleId(ndd.getCourtScheduleId().orElse(null))
                            .withOucode(ndd.getOucode().orElse(null))
                            .withSession(ndd.getSession().orElse(null))
                            .withDuration(estimateMinutes > 0 ? ndd.getDuration().get() : getAdjustedDuration(hearingTypeDuration))
                            .withCourtRoomId(ndd.getCourtRoomId().orElse(null))
                            .withStartTime(ndd.getStartTime())
                            .withRoomId(ndd.getRoomId().orElse(null))
                            .withCourtCentreId(ndd.getCourtCentreId().orElse(null))
                            .build())
            );

        }

        return newList;
    }

    private List<NonDefaultDay> convertEventToDomain(final List<uk.gov.justice.listing.events.NonDefaultDay> eventNonDefaults) {
        return eventNonDefaults.stream().map(this::getDomainNonDefaultDay).collect(toList());
    }

    private List<uk.gov.justice.core.courts.NonDefaultDay> convertDomainToCore(final List<NonDefaultDay> domainNonDefaults) {
        return domainNonDefaults.stream().map(this::getCoreNonDefaultDay).collect(toList());
    }

    private NonDefaultDay getDomainNonDefaultDay(final uk.gov.justice.listing.events.NonDefaultDay eventNonDefault) {
        final NonDefaultDay.Builder builder = NonDefaultDay.nonDefaultDay();
        if (nonNull(eventNonDefault.getStartTime())) {
            builder.withStartTime(eventNonDefault.getStartTime());
        }
        if (nonNull(eventNonDefault.getDuration())) {
            builder.withDuration(ofNullable(eventNonDefault.getDuration()));
        }
        if (nonNull(eventNonDefault.getSession())) {
            builder.withSession(ofNullable(eventNonDefault.getSession()));
        }
        if (nonNull(eventNonDefault.getOucode())) {
            builder.withOucode(ofNullable(eventNonDefault.getOucode()));
        }
        if (nonNull(eventNonDefault.getCourtScheduleId())) {
            builder.withCourtScheduleId(ofNullable(eventNonDefault.getCourtScheduleId()));
        }
        if (nonNull(eventNonDefault.getCourtRoomId())) {
            builder.withCourtRoomId(ofNullable(eventNonDefault.getCourtRoomId()));
        }
        builder.withCourtCentreId(ofNullable(eventNonDefault.getCourtCentreId()));
        builder.withRoomId(ofNullable(eventNonDefault.getRoomId()));
        return builder.build();
    }

    private uk.gov.justice.core.courts.NonDefaultDay getCoreNonDefaultDay(final NonDefaultDay nonDefaultDay) {
        final uk.gov.justice.core.courts.NonDefaultDay.Builder builder = uk.gov.justice.core.courts.NonDefaultDay.nonDefaultDay();

        builder.withStartTime(nonDefaultDay.getStartTime());
        builder.withDuration(nonDefaultDay.getDuration().orElse(null));
        builder.withSession(nonDefaultDay.getSession().orElse(null));
        builder.withOucode(nonDefaultDay.getOucode().orElse(null));
        builder.withCourtScheduleId(nonDefaultDay.getCourtScheduleId().orElse(null));
        builder.withCourtRoomId(nonDefaultDay.getCourtRoomId().orElse(null));
        builder.withCourtCentreId(nonDefaultDay.getCourtCentreId().orElse(null));
        builder.withRoomId(nonDefaultDay.getRoomId().orElse(null));

        return builder.build();
    }

    public Stream<Object> changeJurisdictionType(final JurisdictionType jurisdictionType, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.jurisdictionType)) {
            LOGGER.error("JurisdictionType for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
            return Stream.empty();
        }

        if (hasChanged(this.jurisdictionType, jurisdictionType)) {
            return apply(Stream.of(JurisdictionChangedForHearing.jurisdictionChangedForHearing()
                    .withJurisdictionType(valueFor(jurisdictionType.toString())
                            .orElseThrow(IllegalArgumentException::new))
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming JurisdictionType {} is the same as current type {} for hearing with id {} - Ignore", jurisdictionType, this.jurisdictionType, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> assignPublicListNote(final String publicListNote, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (isNotEmpty(this.publicListNote) && isBlank(publicListNote)) {
            return apply(Stream.of(PublicListNoteRemovedFromHearing.publicListNoteRemovedFromHearing()
                    .withHearingId(hearingId)
                    .build()));
        }

        if (hasChanged(publicListNote, this.publicListNote)) {
            return apply(Stream.of(PublicListNoteChangedForHearing.publicListNoteChangedForHearing()
                    .withPublicListNote(publicListNote)
                    .withHearingId(hearingId)
                    .build()));
        }

        return Stream.empty();

    }

    public Stream<Object> assignVideoLink(final boolean hasVideoLink, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (hasChanged(hasVideoLink, this.hasVideoLink)) {
            return apply(Stream.of(VideoLinkChangedForHearing.videoLinkChangedForHearing()
                    .withHasVideoLink(hasVideoLink)
                    .withHearingId(hearingId)
                    .build()));
        }

        return Stream.empty();

    }

    public Stream<Object> saveChanges(final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }
        return apply(Stream.of(HearingChangesSaved.hearingChangesSaved()
                .withHearingId(hearingId).build()));
    }

    public Stream<Object> changeType(final Type type, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.type)) {
            LOGGER.error("Type for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
            return Stream.empty();
        }

        if (hasChanged(this.type, type)) {
            return apply(Stream.of(typeChangedForHearing()
                    .withType(uk.gov.justice.listing.events.Type.type()
                            .withId(type.getId())
                            .withWelshDescription(type.getWelshDescription())
                            .withDescription(type.getDescription()).build())
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming type {} is the same as current type {} for hearing with id {} - Ignore", type, this.type, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> changeStartDate(final LocalDate startDate, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (hasChanged(this.startDate, startDate)) {
            return apply(Stream.of(startDateChangedForHearing()
                    .withStartDate(startDate.toString())
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming start date {} is the same as current start date {} for hearing with id {} - Ignore", startDate, this.startDate, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> changeWeekCommencingDate(final LocalDate weekCommencingStartDate, final LocalDate weekCommencingEndDate, final Integer weekCommencingDurationInWeeks, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (hasChanged(this.weekCommencingStartDate, weekCommencingStartDate) || hasChanged(this.weekCommencingEndDate, weekCommencingEndDate)) {

            return apply(Stream.of(weekCommencingDateChangedForHearing()
                    .withWeekCommencingStartDate(weekCommencingStartDate.toString())
                    .withWeekCommencingEndDate(weekCommencingEndDate.toString())
                    .withWeekCommencingDurationInWeeks(weekCommencingDurationInWeeks)
                    .withHearingId(hearingId)
                    .build()));

        } else {
            LOGGER.info("Incoming week commencing date {} is the same as current week commencing date {} for hearing with id {} - Ignore", this.weekCommencingStartDate, this.weekCommencingEndDate, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> changeEndDate(final LocalDate endDate, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (hasChanged(this.endDate, endDate)) {
            return apply(Stream.of(endDateChangedForHearing()
                    .withEndDate(endDate.toString())
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming end date {} is the same as current end date {} for hearing with id {} - Ignore", endDate, this.endDate, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> removeEndDate(final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (currentlyAssigned(this.endDate)) {
            return apply(Stream.of(endDateRemovedFromHearing()
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("No end date is currently assigned for hearing with id {} so cannot be removed - Ignore", hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> changeHearingLanguage(final HearingLanguage hearingLanguage, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.hearingLanguage)) {
            LOGGER.error("HearingLanguage' for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
            return Stream.empty();
        }

        if (hasChanged(this.hearingLanguage, hearingLanguage)) {
            return apply(Stream.of(HearingLanguageChangedForHearing.hearingLanguageChangedForHearing()
                    .withHearingLanguage(uk.gov.justice.core.courts.HearingLanguage.valueOf(hearingLanguage.toString()))
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming hearingLanguage {} is the same as current hearingLanguage {} for hearing with id {} - Ignore", hearingLanguage, this.hearingLanguage, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> assignNonDefaultDays(final List<uk.gov.moj.cpp.listing.domain.NonDefaultDay> nonDefaultDays, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.nonDefaultDays) || this.nonDefaultDays.isEmpty()) {
            return apply(Stream.of(NonDefaultDaysAssignedToHearing.nonDefaultDaysAssignedToHearing()
                    .withNonDefaultDays(convertNonDefaultDaysToEvents(nonDefaultDays))
                    .withHearingId(hearingId)
                    .withIsPublicEvent(false)
                    .build()));
        } else if (hasChanged(this.nonDefaultDays, nonDefaultDays)) {
            return apply(Stream.of(nonDefaultDaysChangedForHearing()
                    .withNonDefaultDays(convertNonDefaultDaysToEvents(nonDefaultDays))
                    .withHearingId(hearingId)
                    .withIsPublicEvent(false)
                    .build()
            ));
        } else {
            LOGGER.info("Incoming nonDefaultDays {} is the same as current nonDefaultDays {} for hearing with id {} - Ignore", nonDefaultDays, this.nonDefaultDays, hearingId);
            return Stream.empty();
        }
    }


    public Stream<Object> assignNonSittingDays(final List<LocalDate> nonSittingDays, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (isEmpty(nonSittingDays) && isEmpty(this.nonSittingDays)) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.nonSittingDays) || this.nonSittingDays.isEmpty()) {
            return apply(Stream.of(nonSittingDaysAssignedToHearing()
                    .withNonSittingDays(nonSittingDays)
                    .withHearingId(hearingId)
                    .build()));
        } else if (hasChanged(this.nonSittingDays, nonSittingDays)) {
            return apply(Stream.of(nonSittingDaysChangedForHearing()
                    .withNonSittingDays(nonSittingDays)
                    .withHearingId(hearingId)
                    .build()
            ));
        } else {
            LOGGER.info("Incoming nonSittingDays {} is the same as current nonSittingDays {} for hearing with id {} - Ignore", nonSittingDays, this.nonSittingDays, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> assignJudiciary(final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judiciary, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }


        if (notCurrentlyAssigned(this.judiciary) || this.judiciary.isEmpty()) {
            return apply(Stream.of(JudiciaryAssignedToHearing.judiciaryAssignedToHearing()
                    .withJudiciary(convertToEvents(judiciary))
                    .withHearingId(hearingId)
                    .build()));
        } else if (hasChanged(this.judiciary, judiciary)) {
            return apply(Stream.of(JudiciaryChangedForHearing.judiciaryChangedForHearing()
                    .withJudiciary(convertToEvents(judiciary))
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming judiciary {} is the same as current judiciary {} for hearing with id {} - Ignore", judiciary, this.judiciary, hearingId);
            return Stream.empty();
        }
    }


    public Stream<Object> removeJudiciary(final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (currentlyAssigned(this.judiciary) && !this.judiciary.isEmpty()) {
            return apply(Stream.of(JudiciaryRemovedFromHearing.judiciaryRemovedFromHearing()
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("No judiciary is currently assigned for hearing with id {} so cannot be removed - Ignore", hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> changeCourtCentre(final UUID courtCentreId, final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.courtCentreId)) {
            LOGGER.error("Court centre' for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
            return Stream.empty();
        } else if (hasChanged(this.courtCentreId, courtCentreId)) {
            return apply(Stream.of(CourtCentreChangedForHearing.courtCentreChangedForHearing()
                    .withCourtCentreId(courtCentreId)
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming court centre id {}  is the same as current court room id {} for hearing with id {} - Ignore", courtCentreId, this.courtCentreId, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> assignCourtRoom(final UUID courtRoomId, final UUID hearingId, Optional<String> panel) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.courtRoomId)) {
            final SequencesResetOnHearingDays sequencesResetOnHearingDaysEvent = createSequencesResetOnHearingDaysEvent(hearingId);
            final Stream<Object> appliedCourtRoomEvent = apply(Stream.of(CourtRoomAssignedToHearing.courtRoomAssignedToHearing()
                    .withCourtRoomId(courtRoomId)
                    .withHearingId(hearingId).withPanel(panel.orElse(null))
                    .build()));
            return concat(appliedCourtRoomEvent, apply(Stream.of(sequencesResetOnHearingDaysEvent)));
        } else if (hasChanged(this.courtRoomId, courtRoomId)) {
            final SequencesResetOnHearingDays sequencesResetOnHearingDaysEvent = createSequencesResetOnHearingDaysEvent(hearingId);
            final Stream<Object> appliedCourtRoomEvent = apply(Stream.of(courtRoomChangedForHearing()
                    .withCourtRoomId(courtRoomId)
                    .withHearingId(hearingId).withPanel(panel.orElse(null))
                    .build()));
            return concat(appliedCourtRoomEvent, apply(Stream.of(sequencesResetOnHearingDaysEvent)));
        } else {
            LOGGER.info("Incoming court room id {} is the same as current court room id {} for hearing with id {} - Ignore", courtRoomId, this.courtRoomId, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> removeCourtRoom(final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (currentlyAssigned(this.courtRoomId)) {
            final SequencesResetOnHearingDays sequencesResetOnHearingDaysEvent = createSequencesResetOnHearingDaysEvent(hearingId);
            final Stream<Object> appliedCourtRoomEvent = apply(Stream.of(CourtRoomRemovedFromHearing.courtRoomRemovedFromHearing()
                    .withHearingId(hearingId)
                    .build()));
            return concat(appliedCourtRoomEvent, apply(Stream.of(sequencesResetOnHearingDaysEvent)));
        } else {
            LOGGER.info("No court room is currently assigned for hearing with id {} so cannot be removed - Ignore", hearingId);
            return Stream.empty();
        }
    }

    //Remove as it's not in use. Tests need to be updated to use assignHearingDaysV2
    public Stream<Object> assignHearingDays(final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays, final List<NonDefaultDay> nonDefaultDays, final LocalTime defaultStartTime, final Integer defaultDuration, final UUID hearingId, final CourtCentre defaultCourtCentre) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.hearingDays)) {
            LOGGER.error(HEARING_DAYS_NOT_ASSIGNED, hearingId);
            return Stream.empty();
        }

        final List<uk.gov.justice.listing.events.HearingDay> hearingDaysChangedForHearing
                = calculate(startDate, endDate, nonSittingDays, nonDefaultDays, defaultStartTime, defaultDuration, defaultCourtCentre);

        if (!this.hearingDays.isEmpty()) {
            final Map<ZonedDateTime, HearingDay> existingHearingDays = this.hearingDays.stream()
                    .collect(toMap(HearingDay::getStartTime, hearingDay -> hearingDay, (hd1, hd2) -> hd2));
            final List<uk.gov.justice.listing.events.HearingDay> newHearingDaysWithExistingSequences =
                    mergeHearingDaySequences(hearingDaysChangedForHearing, existingHearingDays);

            return apply(Stream.of(hearingDaysChangedForHearing()
                    .withHearingDays(newHearingDaysWithExistingSequences)
                    .withHearingId(hearingId)
                    .build()));
        }
        return apply(Stream.of(hearingDaysChangedForHearing()
                .withHearingDays(hearingDaysChangedForHearing)
                .withHearingId(hearingId)
                .build()));

    }

    public Stream<Object> assignHearingDaysV2(final UUID hearingId, final List<uk.gov.moj.cpp.listing.domain.HearingDay> updatedHearingDays, final UUID oldParentCourtRoom, final UUID newParentCourtRoom, final uk.gov.justice.core.courts.JurisdictionType newJurisdictionType) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.hearingDays)) {
            LOGGER.error(HEARING_DAYS_NOT_ASSIGNED, hearingId);
            return Stream.empty();
        }

        final List<uk.gov.justice.listing.events.HearingDay> hearingDaysChangedForHearing = NewDomainToEventConverter.convertHearingDaysDomainToEvent(updatedHearingDays);

        if (!this.hearingDays.isEmpty()) {
            final Map<ZonedDateTime, HearingDay> existingHearingDays = this.hearingDays.stream()
                    .collect(toMap(HearingDay::getStartTime, hearingDay -> hearingDay, (hd1, hd2) -> hd2));

            List<uk.gov.justice.listing.events.HearingDay> newHearingDaysWithExistingInfo =
                    mergeHearingDaySequences(hearingDaysChangedForHearing, existingHearingDays);

            /** We are trying to preserve old room info only if there is no change in parent courtRoom */
            if (CROWN.equals(newJurisdictionType) && newParentCourtRoom != null && newParentCourtRoom.equals(oldParentCourtRoom)) {
                final Map<LocalDate, HearingDay> existingHearingDaysWithChangedRooms = this.hearingDays.stream()
                        .filter(hd -> hd.getCourtRoomId() != null) // we do not want to preserve any null courtroom
                        .filter(hd -> !newParentCourtRoom.equals(hd.getCourtRoomId())) // The courtRoom on the parent is not the same as the one on this day
                        .collect(toMap(HearingDay::getHearingDate, hearingDay -> hearingDay, (hd1, hd2) -> hd2));

                preservePreviouslyChangedCourtRooms(newHearingDaysWithExistingInfo, existingHearingDaysWithChangedRooms);
            }

            return apply(Stream.of(hearingDaysChangedForHearing()
                    .withHearingDays(newHearingDaysWithExistingInfo)
                    .withHearingId(hearingId)
                    .build()));
        }
        return apply(Stream.of(hearingDaysChangedForHearing()
                .withHearingDays(hearingDaysChangedForHearing)
                .withHearingId(hearingId)
                .build()));

    }

    public Stream<Object> applyAllocationRules(final Optional<UUID> bookingReference, final Boolean sendNotificationToParties, final Boolean isNotificationRelatedAllocatedFieldsUpdated,
                                               final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds, Optional<String> source, final Boolean isGroupProceedings) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (canAllocate()) {
            return onAllocationEvents(bookingReference, prosecutionCaseDefendantOffenceIds, source, sendNotificationToParties, isNotificationRelatedAllocatedFieldsUpdated, isGroupProceedings);
        } else if (canUnallocate()) {
            return onUnallocationEvents(empty());
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> applyAllocationRules(final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds, final Boolean sendNotificationToParties, final Boolean isNotificationRelatedAllocatedFieldsUpdated) {
        return getAllocationEvents(prosecutionCaseDefendantOffenceIds, empty(), sendNotificationToParties, isNotificationRelatedAllocatedFieldsUpdated, null);
    }

    public Stream<Object> applyAllocationRules(final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds, final Optional<String> source, final Boolean sendNotificationToParties, final Boolean isNotificationRelatedAllocatedFieldsUpdated, final Boolean isGroupProceedings) {
        return getAllocationEvents(prosecutionCaseDefendantOffenceIds, source, sendNotificationToParties, isNotificationRelatedAllocatedFieldsUpdated, isGroupProceedings);
    }

    private Stream<Object> getAllocationEvents(final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds, final Optional<String> source, final Boolean sendNotificationToParties, final Boolean isNotificationRelatedAllocatedFieldsUpdated, final Boolean isGroupProceedings) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }
        if (canAllocate()) {
            return onAllocationEvents(empty(), prosecutionCaseDefendantOffenceIds, source, sendNotificationToParties, isNotificationRelatedAllocatedFieldsUpdated, isGroupProceedings);
        } else if (canUnallocate()) {
            return onUnallocationEvents(source);
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> updateDefendants(final UUID caseId, final List<uk.gov.moj.cpp.listing.domain.Defendant> defendants) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            final List<Object> events = defendants.stream()
                    .filter(this::thisHearingContainsDefendant)
                    .filter(defendant -> isCaseContainsDefendant(caseId, defendant.getId()))
                    .map(defendant -> defendantDetailsUpdatedEvent(caseId, defendant))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }


    public Stream<Object> updateCaseMarkers(final UUID caseId, final List<uk.gov.moj.cpp.listing.domain.CaseMarker> caseMarkers) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            final NewCaseMarkerUpdated newCaseMarkerUpdated = caseMarkerUpdateEvent(caseId, caseMarkers);
            return apply(Stream.of(newCaseMarkerUpdated));
        }
        return Stream.empty();
    }

    public Stream<Object> linkCaseToHearing(final String linkActionType, final UUID caseId, final String caseUrn, final List<LinkedToCases> linkedToCases) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            return apply(Stream.of(LinkedCasesUpdated.linkedCasesUpdated()
                    .withLinkActionType(linkActionType)
                    .withCaseId(caseId)
                    .withCaseUrn(caseUrn)
                    .withHearingId(this.hearingId)
                    .withLinkedToCases(NewDomainToEventConverter.convertDomainToLinkedToCasesEvent(linkedToCases))
                    .build()));
        }
        return Stream.empty();
    }

    public Stream<Object> vacateTrial(final UUID hearingId, final UUID vacatingTrialReasonId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        Stream<Object> eventsStream = Stream.of(trialVacated()
                .withHearingId(hearingId)
                .withVacatedTrialReasonId(vacatingTrialReasonId)
                .withAllocated(isAllocated())
                .build());

        return apply(eventsStream);
    }

    public Stream<Object> hearingVacateTrial(final Optional<UUID> vacatingTrialReasonId) {
        if (this.duplicate || this.deleted || isNull(this.hearingId)) {
            return Stream.empty();
        }

        Stream<Object> eventsStream = Stream.of(HearingTrialVacated.hearingTrialVacated()
                .withHearingId(this.hearingId)
                .withVacatedTrialReasonId(vacatingTrialReasonId.orElse(null))
                .build());

        if (MAGISTRATES == jurisdictionType && vacatingTrialReasonId.isPresent()) {
            eventsStream = concat(eventsStream, Stream.of(availableSlotsForHearingFreed().withHearingId(this.hearingId).build()));
        }
        return apply(eventsStream);
    }

    public Stream<Object> applyRescheduledCheck(final List<Object> occurredEventList) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (isReschedulingEvent(occurredEventList)) {
            return apply(Stream.of(HearingRescheduled.hearingRescheduled()
                    .withHearingId(this.hearingId)
                    .withAllocated(isAllocated())
                    .build()));
        }

        return Stream.empty();

    }

    public Stream<Object> updatedListedCasesInHearing(final uk.gov.justice.listing.events.Hearing allocatedHearing,
                                                      final uk.gov.justice.listing.events.Hearing unAllocatedHearing,
                                                      final List<uk.gov.justice.listing.events.ListedCase> casesToAllocate) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }
        allocatedHearing.getListedCases().addAll(casesToAllocate);
        LOGGER.info("Cases added to allocated hearing: {}", casesToAllocate);
        return apply(Stream.of(HearingListedCaseUpdated.hearingListedCaseUpdated()
                .withHearing(allocatedHearing)
                .withHearingIdToBeDeleted(unAllocatedHearing.getId())
                .withUnAllocatedListedCases(casesToAllocate).build()));
    }


    public Stream<Object> addCasesToHearing(final List<ProsecutionCase> prosecutionCases, final List<UUID> shadowListedOffences, final Optional<UUID> seedingHearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }
        return apply(Stream.of(CasesAddedToHearing.casesAddedToHearing()
                .withUnAllocatedListedCases(prosecutionCases.stream()
                        .map(prosecutionCase -> buildListedCase(prosecutionCase, shadowListedOffences))
                        .collect(Collectors.toList()))
                .withHearingId(hearingId)
                .withSeedingHearingId(seedingHearingId.orElse(null))
                .build()));

    }

    public Stream<Object> addCasesToUnAllocatedHearing(final List<uk.gov.justice.listing.events.ListedCase> listedCases, final UUID existingHearingId) {
        if (this.duplicate || this.deleted || (canAllocate() && isAllocated())) {
            return Stream.empty();
        }
        return apply(Stream.of(CasesAddedToHearing.casesAddedToHearing()
                .withUnAllocatedListedCases(listedCases)
                .withHearingId(hearingId)
                .withSeedingHearingId(existingHearingId)
                .withAddCasesToUnAllocatedHearing(true)
                .build()));

    }

    public Stream<Object> deleteUnAllocatedHearing() {
        if (this.deleted) {
            return Stream.empty();
        }
        return apply(Stream.of(HearingDeleted.hearingDeleted()
                .withHearingIdToBeDeleted(this.hearingId)
                .build()));
    }

    public Stream<Object> markHearingAsDeleted(final UUID hearingId) {

        if (this.deleted || this.resulted) {
            return Stream.empty();
        }

        return apply(Stream.of(HearingMarkedAsDeleted.hearingMarkedAsDeleted()
                .withHearingIdToDelete(hearingId)
                .build()));
    }

    public Stream<Object> updateUnallocatedHearingPartially(final UUID hearingToBeUpdated, final List<ProsecutionCases> caseList, final Optional<String> splitHearing) {
        if (this.duplicate || this.deleted || this.resulted) {
            return Stream.empty();
        }

        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();

        if (splitHearing.isPresent()) {
            eventStreamBuilder.add(HearingPartiallyUpdated.hearingPartiallyUpdated()
                    .withHearingIdToBeUpdated(hearingToBeUpdated)
                    .withProsecutionCases(caseList)
                    .withSplitHearing(splitHearing.get())
                    .build());
        } else {
            eventStreamBuilder.add(HearingPartiallyUpdated.hearingPartiallyUpdated()
                    .withHearingIdToBeUpdated(hearingToBeUpdated)
                    .withProsecutionCases(caseList)
                    .build());
        }
        return apply(eventStreamBuilder.build());
    }

    public Stream<Object> markUnallocatedHearingForPartialUpdate(final UUID hearingToBeUpdated, final List<ProsecutionCases> caseList) {
        return apply(Stream.of(HearingMarkedForPartialUpdate.hearingMarkedForPartialUpdate()
                .withHearingIdToBeUpdated(hearingToBeUpdated)
                .withProsecutionCases(caseList)
                .build()));
    }

    public Stream<Object> applyAllocationRulesForExtendedHearing(final uk.gov.justice.listing.events.Hearing unallocatedHearing, final boolean fullExtension, final Boolean sendNotificationToParties) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (canAllocate() && isAllocated()) {
            return apply(Stream.of(allocatedHearingExtendedForListingEvent(unallocatedHearing, fullExtension, sendNotificationToParties)));
        } else {
            LOGGER.info("Incoming hearing cannot allocated with new list cases");
        }
        return Stream.empty();
    }

    public Stream<Object> cancelHearingDays(final UUID hearingId, final List<uk.gov.justice.listing.events.HearingDay> hearingDays) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        final List<HearingDay> updatedHearingDays = getAggregateHearingDaysAsCancelledFromCommand(hearingDays);

        Stream<Object> eventStream = Stream.of(hearingDaysCancelled().withHearingId(hearingId).withHearingDays(convertHearingDaysToEvent(updatedHearingDays)).build());

        eventStream = concat(eventStream, freeCancelledHearingDaySlots(hearingId, updatedHearingDays));

        return apply(eventStream);
    }

    public Stream<Object> updateCaseIdentifier(UUID prosecutionAuthorityId, String prosecutionAuthorityCode, UUID prosecutionCaseId) {
        final Stream<Object> eventStream = Stream.of(CaseIdentifierUpdated.caseIdentifierUpdated()
                .withHearingId(this.hearingId)
                .withProsecutionAuthorityCode(prosecutionAuthorityCode)
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build());
        return apply(eventStream);
    }

    public Stream<Object> modifyCounselsInHearing(final List<ProsecutionCase> prosecutionCases, final List<UUID> shadowListedOffences) {
        return apply(Stream.of(AddedCasesForHearing.addedCasesForHearing()
                .withUnAllocatedListedCases(prosecutionCases.stream()
                        .map(prosecutionCase -> buildListedCase(prosecutionCase, shadowListedOffences))
                        .collect(Collectors.toList()))
                .withHearingId(hearingId)
                .build()));

    }

    public Stream<Object> deleteHearing(final UUID seedingHearingId, final UUID hearingId) {
        if (deleted || duplicate || resulted) {
            return Stream.empty();
        }

        Optional<uk.gov.moj.cpp.listing.domain.OffenceIds> offenceIdsSeededByOtherHearings = Optional.empty();
        Optional<uk.gov.moj.cpp.listing.domain.OffenceIds> offenceIdsExtendedByOtherHearings = Optional.empty();
        boolean hasMultipleCase = false;
        if (nonNull(prosecutionCaseDefendantOffenceIds)) {
            offenceIdsSeededByOtherHearings = prosecutionCaseDefendantOffenceIds.stream().flatMap(pc -> pc.getDefendants().stream())
                    .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                    .filter(offence -> nonNull(offence.getSeedingHearing()))
                    .filter(offence -> isNotSeededOffenceBySeedId(seedingHearingId, offence))
                    .findFirst();

            offenceIdsExtendedByOtherHearings = prosecutionCaseDefendantOffenceIds.stream().flatMap(pc -> pc.getDefendants().stream())
                    .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                    .filter(offence -> isNull(offence.getSeedingHearing())) // The offence was not added by another resulted hearing
                    .filter(offence -> !ofNullable(offence.getIsNewOffence()).orElse(false)) // The offence was not added because it was added the case.
                    .findFirst(); // So the offence was added by extended hearing.

            hasMultipleCase = ofNullable(currentHearingEventState.getListedCases()).orElse(emptyList()).stream().map(c -> c.getId()).collect(Collectors.toSet()).size() > 1;
        }


        if ((offenceIdsSeededByOtherHearings.isPresent() && hasMultipleCase) || offenceIdsExtendedByOtherHearings.isPresent()) {
            // We need to remove Offences which are belongs only this seeded hearings and leave other offences in this hearing, if This hearing seeded or extended from another hearings.
            return getOffencesRemovedFromHearingStream(seedingHearingId, hearingId);
        } else {
            final List<UUID> caseIds = prosecutionCaseDefendants.keySet().stream().collect(toList());

            final Stream.Builder<Object> eventStreamBuilder = Stream.builder();

            if (isAllocated()) {
                eventStreamBuilder.add(AllocatedHearingDeleted.allocatedHearingDeleted()
                        .withHearingId(hearingId)
                        .withCaseIds(caseIds)
                        .build());

                if (MAGISTRATES == jurisdictionType) {
                    eventStreamBuilder.add(availableSlotsForHearingFreed()
                            .withHearingId(hearingId).build());
                }
            } else {
                eventStreamBuilder.add(UnallocatedHearingDeleted.unallocatedHearingDeleted()
                        .withHearingId(hearingId)
                        .withCaseIds(caseIds)
                        .build());
            }

            return apply(eventStreamBuilder.build());
        }
    }

    public Stream<Object> removeOffencesFromExistingHearing(final UUID seedingHearingId, final UUID hearingId) {
        // when amend and reshare first hearing, if existing next hearing has new offences , we need to remove them as well.
        final List<UUID> offenceIds = getOffencesFromSeededHearingAndNew(seedingHearingId);

        return removeSelectedOffencesFromExistingHearing(hearingId, offenceIds, SOURCE_LISTING, true);

    }

    //isResultFlow -> These event uses for multiple purposes, we need to know it is raised by amend-reshare flow.
    public Stream<Object> removeSelectedOffencesFromExistingHearing(final UUID hearingId, final List<UUID> offenceIds, final String source, final Boolean isResultFlow) {

        if (deleted || duplicate || resulted) {
            return Stream.empty();
        }
        final List<UUID> existingOffenceIds = prosecutionCaseDefendantOffenceIds
                .stream().flatMap(pc -> pc.getDefendants().stream())
                .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                .filter(offence -> offenceIds.contains(offence.getId()))
                .map(OffenceIds::getId)
                .collect(toList());

        if (existingOffenceIds.isEmpty()) {
            return Stream.empty();
        }

        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();

        if (isNotEmpty(offenceIds)) {
            if (isAllocated()) {
                eventStreamBuilder.add(OffencesRemovedFromExistingAllocatedHearing.offencesRemovedFromExistingAllocatedHearing()
                        .withHearingId(hearingId)
                        .withOffenceIds(offenceIds)
                        .withSourceContext(source)
                        .withIsResultFlow(isResultFlow)
                        .build());
            } else {
                eventStreamBuilder.add(OffencesRemovedFromExistingUnallocatedHearing.offencesRemovedFromExistingUnallocatedHearing()
                        .withHearingId(hearingId)
                        .withOffenceIds(offenceIds)
                        .withIsResultFlow(isResultFlow)
                        .build());
            }
        }

        return apply(eventStreamBuilder.build());

    }

    private boolean isReschedulingEvent(final List<Object> occuredEventList) {
        return occuredEventList.stream()
                .anyMatch(
                        o -> o instanceof StartDateChangedForHearing);

    }

    private NewCaseMarkerUpdated caseMarkerUpdateEvent(final UUID caseId, final List<CaseMarker> cm) {
        return NewCaseMarkerUpdated.newCaseMarkerUpdated()
                .withCaseId(caseId)
                .withHearingId(this.hearingId)
                .withCaseMarkers(NewDomainToEventConverter.convertCaseMarkersListToMarkers(cm))
                .build();
    }

    public Stream<Object> updateOffences(final UUID caseId, final UUID defendantId, final List<uk.gov.moj.cpp.listing.domain.Offence> offences) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(offence -> thisHearingContainsDefendantAndOffence(caseId, defendantId, offence.getId()))
                    .map(offence -> offenceUpdatedEvent(caseId, defendantId, offence))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> deleteOffences(final UUID caseId, final UUID defendantId, final List<SimpleOffence> offences) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(simpleOffence -> thisHearingContainsDefendantAndOffence(caseId, defendantId, simpleOffence.getId()))
                    .map(simpleOffence -> this.offenceDeletedEvent(caseId, simpleOffence))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> addOffences(final UUID caseId, final UUID defendantId, final List<uk.gov.moj.cpp.listing.domain.Offence> offences) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(offence -> thisHearingContainsDefendant(caseId, defendantId))
                    .map(offence -> offenceAddedEvent(caseId, defendantId, offence))
                    .collect(toList());
            if (isNotEmpty(events)) {
                return apply(events.stream());
            }
        }
        return Stream.empty();
    }

    public Stream<Object> sequenceHearingDays(final SequenceHearing sequenceHearing) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.hearingDays)) {
            LOGGER.error(HEARING_DAYS_NOT_ASSIGNED, hearingId);
            return Stream.empty();
        }

        final Map<LocalDate, Integer> hearingDaysSequencesMap = sequenceHearing.getHearingDays().stream()
                .collect(Collectors.toMap(uk.gov.moj.cpp.listing.domain.HearingDay::getHearingDate, uk.gov.moj.cpp.listing.domain.HearingDay::getSequence));
        final boolean sequenceChanged = hasSequenceChanged(hearingDaysSequencesMap);

        final List<uk.gov.justice.listing.events.HearingDay> updatedHearingDays = this.hearingDays.stream()
                .map(d -> new uk.gov.justice.listing.events.HearingDay.Builder()
                        .withDurationMinutes(d.getDurationMinutes())
                        .withEndTime(d.getEndTime())
                        .withHearingDate(d.getHearingDate())
                        .withSequence(hearingDaysSequencesMap.containsKey(d.getHearingDate())
                                ? hearingDaysSequencesMap.get(d.getHearingDate())
                                : d.getSequence())
                        .withStartTime(d.getStartTime())
                        .withIsCancelled(d.isCancelled())
                        .withCourtCentreId(d.getCourtCentreId())
                        .withCourtRoomId(d.getCourtRoomId())
                        .withCourtScheduleId(d.getCourtScheduleId())
                        .build()
                ).collect(toList());
        // Only raise allocation events if sequence changed
        final HearingDaysSequenced hearingDaysSequenced = hearingDaysSequenced()
                .withHearingId(this.hearingId)
                .withHearingDays(updatedHearingDays)
                .build();
        if (sequenceChanged) {
            this.hearingDays = convertHearingDaysToDomain(updatedHearingDays);
            return concat(applyAllocationRules(ImmutableList.of(), false, false), apply(Stream.of(hearingDaysSequenced)));
        }
        LOGGER.info("Sequence not changed for hearing id {}", hearingId);
        return apply(Stream.of(hearingDaysSequenced));
    }

    public Stream<Object> addCourtApplication(final UUID hearingId, final CourtApplication courtApplication) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }
        return apply(Stream.of(CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(hearingId)
                .withCourtApplication(NewDomainToEventConverter.buildCourtApplications(courtApplication))
                .build()));
    }

    public Stream<Object> updateCourtApplication(final UUID hearingId, final CourtApplication courtApplication) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            return apply(Stream.of(CourtApplicationUpdatedForHearing.courtApplicationUpdatedForHearing()
                    .withHearingId(hearingId)
                    .withCourtApplication(NewDomainToEventConverter.buildCourtApplications(courtApplication))
                    .build()));
        }
        return Stream.empty();
    }

    public Stream<Object> addDefendantsForCourtProceedings(final UUID caseId, final List<uk.gov.moj.cpp.listing.domain.Defendant> defendants) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (!isHearingInThePast() && isHearingContainsCase(caseId)) {
            final List<Object> events = defendants.stream()
                    .filter(defendant -> !isCaseContainsDefendant(caseId, defendant.getId()))
                    .map(defendant -> defendantsAddedForCourtProceedings(caseId, defendant))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> updateDefendantCourtProceedingForHearing(final UUID hearingId, final ProsecutionCase prosecutionCase) {

        if (duplicate || isHearingInThePast() || this.deleted) {
            return Stream.empty();
        }

        final List<UUID> defendantIds = this.prosecutionCaseDefendants.get(prosecutionCase.getId());
        if (nonNull(defendantIds)) {
            prosecutionCase.getDefendants().removeIf(defendant -> !defendantIds.contains(defendant.getId()));
        }

        if (isEmpty(prosecutionCase.getDefendants())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No more defendant left ... returning");
            }
            return Stream.empty();
        }

        return apply(Stream.of(defendantCourtProceedingsUpdatedV2()
                .withHearingId(hearingId)
                .withProsecutionCase(prosecutionCase)
                .build()));
    }

    public Stream<Object> restrictDetailsFromCourt(final UUID hearingId, final RestrictCourtList restrictCourtList) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            return apply(Stream.of(CourtListRestricted.courtListRestricted()
                    .withHearingId(hearingId)
                    .withCaseIds(restrictCourtList.getCaseIds())
                    .withDefendantIds(restrictCourtList.getDefendantIds())
                    .withOffenceIds(restrictCourtList.getOffenceIds())
                    .withCourtApplicationApplicantIds(restrictCourtList.getCourtApplicationApplicantIds())
                    .withCourtApplicationIds(restrictCourtList.getCourtApplicationIds())
                    .withCourtApplicationRespondentIds(restrictCourtList.getCourtApplicationRespondentIds())
                    .withRestrictCourtList(restrictCourtList.getRestrictFromCourtList())
                    .withCourtApplicationType(restrictCourtList.getCourtApplicationType().orElse(null))
                    .build()));
        }

        return Stream.empty();
    }

    public Stream<Object> updateDefendantLegalAidStatusForHearing(final UUID hearingId, final UUID caseId,
                                                                  final UUID defendantId, final String legalAidStatus) {
        if (this.duplicate || this.deleted || !isCaseContainsDefendant(caseId, defendantId)) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            return apply(Stream.of(DefendantLegalaidStatusUpdatedForHearing.defendantLegalaidStatusUpdatedForHearing()
                    .withHearingId(hearingId)
                    .withCaseId(caseId)
                    .withDefendantId(defendantId)
                    .withLegalAidStatus(legalAidStatus)
                    .build()));
        }

        return Stream.empty();

    }

    public Stream<Object> ejectCase(final UUID hearingIdOfEjectCase, final UUID caseId, final String removalReason) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (nonNull(hearingIdOfEjectCase)) {
            return apply(Stream.of(CaseEjected.caseEjected()
                    .withProsecutionCaseId(caseId)
                    .withHearingId(hearingIdOfEjectCase)
                    .withRemovalReason(removalReason)
                    .build()
            ));
        }
        return Stream.empty();
    }

    public Stream<Object> ejectApplication(final UUID hearingIdForApplicationToBeEjected, final UUID applicationId, final String removalReason) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (nonNull(hearingIdForApplicationToBeEjected) && hearingIdForApplicationToBeEjected.equals(this.hearingId)) {
            return apply(Stream.of(ApplicationEjected.applicationEjected()
                    .withApplicationId(applicationId)
                    .withHearingId(hearingIdForApplicationToBeEjected)
                    .withRemovalReason(removalReason)
                    .build()
            ));
        }
        return Stream.empty();

    }

    public Stream<Object> removeStartDate(final UUID hearingId, final Boolean unscheduled) {
        if (currentlyAssigned(this.startDate) && (!this.duplicate) && !this.deleted) {
            final StartDateRemovedForHearing.Builder startDateRemovedForHearing = startDateRemovedForHearing()
                    .withHearingId(hearingId);
            ofNullable(unscheduled).filter(v -> v).ifPresent(startDateRemovedForHearing::withUnscheduled);

            return apply(Stream.of(startDateRemovedForHearing.build()));

        } else {
            LOGGER.info("No start date is currently assigned for hearing with id {} so cannot be removed - Ignore", hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> removeStartDate(final UUID hearingId) {
        return removeStartDate(hearingId, null);
    }

    public Stream<Object> removeWeekCommencingDates(final UUID hearingId) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }

        if (currentlyAssigned(this.weekCommencingStartDate) && currentlyAssigned(this.weekCommencingEndDate)) {
            return apply(Stream.of(weekCommencingDateRemovedForHearing()
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("No week commencing date is currently assigned for hearing with id {} so cannot be removed - Ignore", hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> markHearingAsDuplicate(final UUID hearingId, final List<UUID> caseIds) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }
        return apply(Stream.of(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .withCaseIds(caseIds)
                .build()));
    }

    private List<String> getCaseAndApplicationIds(final uk.gov.justice.listing.events.Hearing currentHearingEventState) {
        return Stream.concat(ofNullable(currentHearingEventState.getListedCases()).map(Collection::stream).orElseGet(Stream::empty).map(uk.gov.justice.listing.events.ListedCase::getId),
                        ofNullable(currentHearingEventState.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty).map(uk.gov.justice.listing.events.CourtApplication::getId))
                .map(UUID::toString)
                .collect(toList());
    }

    public Stream<Object> markUnallocatedHearingAsDuplicate(final UUID hearingId) {
        if (duplicate || isAllocated()) {
            return Stream.empty();
        }

        return apply(Stream.of(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .build()));
    }

    private boolean isAllocated() {
        return nonNull(currentHearingEventState) && toBoolean(currentHearingEventState.getAllocated());
    }

    private void updateAllocated(final Boolean allocated) {
        this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing()
                .withValuesFrom(ofNullable(currentHearingEventState).orElse(uk.gov.justice.listing.events.Hearing.hearing().build()))
                .withAllocated(allocated)
                .build();
    }

    private boolean thisHearingContainsDefendant(final uk.gov.moj.cpp.listing.domain.Defendant defendant) {
        return isEmpty(this.prosecutionCaseDefendantOffenceIds) ? FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId ->
                        prosecutionCaseDefendantOffenceId.getDefendants().stream()
                                .anyMatch(defendantOffenceId ->
                                        defendantOffenceId.getId().equals(defendant.getId())));
    }

    private boolean isCaseContainsDefendant(final UUID caseId, final UUID defendantId) {
        final List<UUID> defendantIds = this.prosecutionCaseDefendants.get(caseId);

        return isNotEmpty(defendantIds) && defendantIds.contains(defendantId);
    }

    private boolean isHearingContainsCase(final UUID caseId) {
        return isEmpty(this.prosecutionCaseDefendantOffenceIds) ? FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId -> prosecutionCaseDefendantOffenceId.getId().equals(caseId));
    }

    private boolean thisHearingContainsDefendant(final UUID caseId, final UUID defendantId) {
        return isEmpty(this.prosecutionCaseDefendantOffenceIds) ? FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId ->
                        prosecutionCaseDefendantOffenceId.getId().equals(caseId) && prosecutionCaseDefendantOffenceId.getDefendants().stream().anyMatch(defendantOffenceIds ->
                                defendantOffenceIds.getId().equals(defendantId))
                );
    }

    private boolean thisHearingContainsDefendantAndOffence(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return isEmpty(this.prosecutionCaseDefendantOffenceIds) ? FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId ->
                        prosecutionCaseDefendantOffenceId.getId().equals(caseId) && prosecutionCaseDefendantOffenceId.getDefendants().stream().anyMatch(defendantOffenceIds ->
                                defendantOffenceIds.getId().equals(defendantId) && defendantOffenceIds.getOffences().stream().anyMatch(offence ->
                                        offence.getId().equals(offenceId)))
                );
    }

    private boolean isHearingInThePast() {
        return this.endDate != null && LocalDate.now().isAfter(this.endDate);
    }

    private Stream<Object> onAllocationEvents(final Optional<UUID> bookingReference, final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds, final Optional<String> source, final Boolean sendNotificationToParties,
                                              final Boolean isNotificationRelatedAllocatedFieldsUpdated, final Boolean isGroupProceedings) {
        if (isAllocated()) {
            return apply(Stream.of(allocatedHearingUpdatedForListingEvent(source, sendNotificationToParties, isNotificationRelatedAllocatedFieldsUpdated)));
        }
        return apply(Stream.of(Stream.of(hearingAllocatedForListingEvent(bookingReference, prosecutionCaseDefendantOffenceIds, source, sendNotificationToParties, isNotificationRelatedAllocatedFieldsUpdated, isGroupProceedings))).flatMap(i -> i));
    }

    public boolean isDuplicateOrDeleted() {
        return this.duplicate || this.deleted;
    }

    public Stream<Object> judiciaryChangedForHearingsStatus() {
        return apply(Stream.of(JudiciaryChangedForHearingsStatus.judiciaryChangedForHearingsStatus()
                .withStatus("Success")
                .build()));
    }

    public Stream<Object> hearingsUpdateCompleted(final Set<UUID> failedHearingIds) {
        final List<UUID> failedIdList = new ArrayList<>(failedHearingIds);
        return apply(Stream.of(HearingsUpdateCompleted
                .hearingsUpdateCompleted()
                .withFailedHearingIds(failedIdList).build()));
    }

    private Stream<Object> onUnallocationEvents(final Optional<String> source) {
        final Stream<Object> appliedBusinessRuleEvents = apply(onUnallocationBusinessRules());
        final HearingUnallocatedForListing unallocateEvent = hearingUnallocatedForListingEvent(source);
        return concat(appliedBusinessRuleEvents, apply(Stream.of(unallocateEvent)));
    }

    private Stream<Object> onUnallocationBusinessRules() {
        // Currently no unallocated business rules to apply
        return Stream.empty();
    }

    private boolean canAllocate() {
        return canAllocateForMags() || canAllocateForCrown();
    }

    private boolean canAllocateForCrown() {
        return currentlyAssigned(this.hearingLanguage) && currentlyAssigned(this.jurisdictionType) && JurisdictionType.CROWN.equals(this.jurisdictionType)
                && currentlyAssigned(this.courtRoomId) && currentlyAssigned(this.endDate) && currentlyAssigned(this.startDate);
    }

    private boolean canAllocateForMags() {
        return currentlyAssigned(this.hearingLanguage) && currentlyAssigned(this.jurisdictionType) && MAGISTRATES.equals(this.jurisdictionType) && hasCourtScheduleIds(this.hearingDays)
                && currentlyAssigned(this.courtRoomId) && currentlyAssigned(this.endDate) && currentlyAssigned(this.startDate);
    }

    private boolean hasCourtScheduleIds(final List<HearingDay> hearingDays) {
        return isNotEmpty(hearingDays) &&
                hearingDays.stream()
                        .allMatch(hearingDay -> nonNull(hearingDay.getCourtScheduleId()));
    }

    @SuppressWarnings({"squid:S1067"})
    private boolean canUnallocate() {
        return isAllocated() && (notCurrentlyAssigned(this.hearingLanguage) || notCurrentlyAssigned(this.courtRoomId)
                || notCurrentlyAssigned(this.jurisdictionType) || notCurrentlyAssigned(this.endDate));
    }

    private boolean notCurrentlyListed() {
        return notCurrentlyAssigned(this.hearingId) && notCurrentlyAssigned(this.type)
                && notCurrentlyAssigned(this.startDate) && notCurrentlyAssigned(this.estimatedMinutes);
    }

    private <T> boolean notCurrentlyAssigned(final T hearingData) {
        return hearingData == null;
    }

    private <T> boolean currentlyAssigned(final T hearingData) {
        return !notCurrentlyAssigned(hearingData);
    }

    private <T> boolean hasChanged(final T currentValue, final T newValue) {
        return !Objects.equals(currentValue, newValue);
    }

    // Helper methods to create events

    private HearingAllocatedForListingV2 hearingAllocatedForListingEvent(final Optional<UUID> bookingReference, List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds, final Optional<String> source,
                                                                         final Boolean sendNotificationToParties, final Boolean isNotificationRelatedAllocatedFieldsUpdated, final Boolean isGroupProceedings) {

        if (isEmpty(prosecutionCaseDefendantOffenceIds)) {
            prosecutionCaseDefendantOffenceIds = this.prosecutionCaseDefendantOffenceIds;
        }
        return hearingAllocatedForListingV2()
                .withHearingId(this.hearingId)
                .withBookingId(bookingReference.orElse(null))
                .withType(buildHearingType())
                .withEstimatedMinutes(this.estimatedMinutes)
                .withEstimatedDuration(this.estimatedDuration)
                .withCourtCentreId(this.courtCentreId)
                .withJudiciary(this.judiciary.stream()
                        .map(this::buildJudicialRole)
                        .collect(toList()))
                .withHearingLanguage(uk.gov.justice.core.courts.HearingLanguage.valueFor(this.hearingLanguage.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withJurisdictionType(valueFor(this.jurisdictionType.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withReportingRestrictionReason(this.reportingRestrictionReason)
                .withCourtRoomId(this.courtRoomId)
                .withHearingDays(convertHearingDaysToEvent(this.hearingDays))
                .withProsecutionCaseDefendantsOffenceIds(isEmpty(prosecutionCaseDefendantOffenceIds) ? null : prosecutionCaseDefendantOffenceIds.stream()
                        .map(lc -> ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(lc.getId())
                                .withGroupId(lc.getGroupId())
                                .withIsGroupMaster(lc.getIsGroupMaster())
                                .withIsGroupMember(lc.getIsGroupMember())
                                .withIsCivil(lc.getIsCivil())
                                .withDefendants(lc.getDefendants().stream()
                                        .filter(defendant -> !defendant.getOffences().isEmpty())
                                        .map(this::buildEventDefendantOffenceIdsV2)
                                        .collect(toList()))
                                .build()
                        ).filter(prosecutionCaseDefendantOffenceIdsV2 -> !prosecutionCaseDefendantOffenceIdsV2.getDefendants().isEmpty())
                        .collect(toList()))
                .withCourtApplicationIds(this.confirmedCourtApplicationIds.isEmpty() ? null : this.confirmedCourtApplicationIds)
                .withUpdateSlot(this.updateSlot)
                .withHasAdjournmentDate(this.hasAdjournmentDate)
                .withApplicationOffenceIds(getAllOffenceIds())
                .withSource(source.orElse(null))
                .withIsGroupProceedings(isGroupProceedings)
                .withSource(source.isPresent() ? source.get() : null)
                .withSendNotificationToParties(sendNotificationToParties)
                .withIsNotificationAllocationFieldUpdated(isNotificationRelatedAllocatedFieldsUpdated)
                .build();
    }


    private uk.gov.justice.listing.events.Type buildHearingType() {
        return uk.gov.justice.listing.events.Type.type()
                .withId(this.type.getId())
                .withDescription(this.type.getDescription())
                .withWelshDescription(this.type.getWelshDescription())
                .build();
    }

    private AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListingEvent(final Optional<String> source, final Boolean sendNotificationToParties, final Boolean isNotificationRelatedAllocatedFieldsUpdated) {

        return allocatedHearingUpdatedForListingV2()
                .withHearingId(this.hearingId)
                .withType(buildHearingType())
                .withEstimatedMinutes(this.estimatedMinutes)
                .withCourtCentreId(this.courtCentreId)
                .withJudiciary(this.judiciary.stream()
                        .map(this::buildJudicialRole)
                        .collect(toList()))
                .withHearingLanguage(uk.gov.justice.core.courts.HearingLanguage.valueFor(this.hearingLanguage.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withJurisdictionType(valueFor(this.jurisdictionType.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withReportingRestrictionReason(this.reportingRestrictionReason)
                .withCourtRoomId(this.courtRoomId)
                .withHearingDays(convertHearingDaysToEvent(this.hearingDays))
                .withProsecutionCaseDefendantsOffenceIds(isEmpty(this.prosecutionCaseDefendantOffenceIds) ? null : this.prosecutionCaseDefendantOffenceIds.stream()
                        .map(lc -> uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(lc.getId())
                                .withDefendants(lc.getDefendants().stream()
                                        .filter(defendant -> !defendant.getOffences().isEmpty())
                                        .map(this::buildEventDefendantOffenceIdsV2)
                                        .collect(toList()))
                                .build()
                        ).filter(pc -> !pc.getDefendants().isEmpty())
                        .collect(toList()))
                .withCourtApplicationIds(this.confirmedCourtApplicationIds.isEmpty() ? null : this.confirmedCourtApplicationIds)
                .withUpdateSlot(this.updateSlot)
                .withSource(source.isPresent() ? source.get() : null)
                .withSendNotificationToParties(sendNotificationToParties)
                .withIsNotificationAllocationFieldUpdated(isNotificationRelatedAllocatedFieldsUpdated)
                .build();
    }


    private HearingUnallocatedForListing hearingUnallocatedForListingEvent(final Optional<String> source) {
        if (nonNull(prosecutionCaseDefendantOffenceIds)) {
            final Optional<OffenceIds> offenceIds = prosecutionCaseDefendantOffenceIds.stream()
                    .flatMap(pc -> pc.getDefendants().stream())
                    .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                    .filter(o -> nonNull(o.getSeedingHearing()))
                    .findFirst();
            if (offenceIds.isPresent()) {
                return hearingUnallocatedForListing()
                        .withHearingId(this.hearingId)
                        .withSeededHearing(true)
                        .withSource(source.isPresent() ? source.get() : null)
                        .withCourtCentreId(this.courtCentreId)
                        .build();
            }
        }

        return hearingUnallocatedForListing()
                .withHearingId(this.hearingId)
                .build();
    }

    private NewDefendantDetailsUpdated defendantDetailsUpdatedEvent(final UUID caseId, final Defendant defendant) {
        return NewDefendantDetailsUpdated.newDefendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendant(NewDomainToEventConverter.buildNewBaseDefendant(defendant))
                .withHearingId(this.hearingId)
                .build();
    }

    private NewDefendantAddedForCourtProceedings defendantsAddedForCourtProceedings(final UUID caseId, final Defendant defendant) {
        final ZonedDateTime hearingDateTime = getEarliestStartDate();

        return NewDefendantAddedForCourtProceedings.newDefendantAddedForCourtProceedings()
                .withCaseId(caseId)
                .withDefendant(NewDomainToEventConverter.buildDefendant(defendant))
                .withHearingId(this.hearingId)
                .withCourtCentreId(this.courtCentreId)
                .withCourtRoomId(this.courtRoomId)
                .withHearingDateTime(hearingDateTime)
                .build();
    }

    private OffenceUpdated offenceUpdatedEvent(final UUID caseId, final UUID defendantId, final uk.gov.moj.cpp.listing.domain.Offence offence) {
        return OffenceUpdated.offenceUpdated()
                .withOffence(NewDomainToEventConverter.buildOffence(offence))
                .withHearingId(this.hearingId)
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .build();
    }

    private OffenceDeleted offenceDeletedEvent(final UUID caseId, final SimpleOffence offence) {
        return OffenceDeleted.offenceDeleted()
                .withDefendantId(offence.getDefendantId())
                .withOffenceId(offence.getId())
                .withHearingId(this.hearingId)
                .withCaseId(caseId)
                .build();
    }

    private OffenceAdded offenceAddedEvent(final UUID caseId, final UUID defendantId, final uk.gov.moj.cpp.listing.domain.Offence offence) {
        return OffenceAdded.offenceAdded()
                .withHearingId(this.hearingId)
                .withOffence(NewDomainToEventConverter.buildOffence(offence))
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .build();
    }

    // Methods to apply aggregate state
    @SuppressWarnings({"squid:S1172"})
    private void onNewDefendantDetailsUpdated(final NewDefendantDetailsUpdated event) {
        if (nonNull(this.currentHearingEventState) && nonNull(currentHearingEventState.getListedCases())) {
            currentHearingEventState.getListedCases().stream()
                    .filter(listedCase -> listedCase.getId().equals(event.getCaseId()))
                    .findFirst()
                    .ifPresent(listedCase -> listedCase.getDefendants().stream().filter(defendant -> defendant.getId().equals(event.getDefendant().getId())).findFirst()
                            .ifPresent(defendant -> {
                                final int index = listedCase.getDefendants().indexOf(defendant);
                                listedCase.getDefendants().set(index, updateEventDefendant(event.getDefendant(), defendant));
                            })
                    );
        }
    }

    @SuppressWarnings({"squid:S1172"})
    private void onOffenceUpdated(final OffenceUpdated offenceUpdated) {
        updateCurrentHearingEventStateWithOffence(offenceUpdated.getCaseId(), offenceUpdated.getDefendantId(), offenceUpdated.getOffence().getId(), offenceUpdated.getOffence());
    }

    @SuppressWarnings({"squid:S1172"})
    private void onCourtApplicationUpdatedForHearing(final CourtApplicationUpdatedForHearing courtApplicationUpdatedForHearing) {
        if (nonNull(this.currentHearingEventState) && nonNull(currentHearingEventState.getCourtApplications())) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withCourtApplications(currentHearingEventState.getCourtApplications().stream()
                            .map(courtApplication -> courtApplication.getId().equals(courtApplicationUpdatedForHearing.getCourtApplication().getId()) ?
                                    courtApplicationUpdatedForHearing.getCourtApplication() : courtApplication)
                            .collect(toList()))
                    .build();
        }
    }

    private void onOffenceAdded(final OffenceAdded offenceAdded) {
        final UUID caseId = offenceAdded.getCaseId();
        final Offence offence = offenceAdded.getOffence();
        final UUID defendantId = offenceAdded.getDefendantId();

        final Optional<DefendantOffenceIds> defendantOffences = getDefendantOffenceIds(caseId, defendantId);
        // the offence was added to case so it is added all future hearings as well. I need to know this offence is a new offence, not seeded or extended from another hearings.
        defendantOffences.ifPresent(defendantOffenceIds -> defendantOffenceIds.getOffences().add(EventToDomainConverter.buildOffenceIdsForNewOffence(offence)));

        updateCurrentHearingEventStateWithOffence(offenceAdded.getCaseId(), offenceAdded.getDefendantId(), null, offenceAdded.getOffence());
    }

    private void onOffenceDeleted(final OffenceDeleted offenceDeleted) {
        final UUID caseId = offenceDeleted.getCaseId();
        final UUID offenceId = offenceDeleted.getOffenceId();
        final UUID defendantId = offenceDeleted.getDefendantId();

        final Optional<DefendantOffenceIds> defendantOffences = getDefendantOffenceIds(caseId, defendantId);

        defendantOffences.ifPresent(defendantOffenceIds -> defendantOffenceIds.getOffences().removeIf(d -> d.getId().equals(offenceId)));

        updateCurrentHearingEventStateWithOffence(offenceDeleted.getCaseId(), offenceDeleted.getDefendantId(), offenceDeleted.getOffenceId(), null);
    }


    @SuppressWarnings({"squid:S3655"})
    private Optional<DefendantOffenceIds> getDefendantOffenceIds(final UUID caseId, final UUID defendantId) {
        if (isEmpty(this.prosecutionCaseDefendantOffenceIds)) {
            return empty();
        }
        final Optional<ProsecutionCaseDefendantOffenceIds> caseDefendants = this.prosecutionCaseDefendantOffenceIds.stream()
                .filter(prosecutionCaseDefendantOffenceId -> prosecutionCaseDefendantOffenceId.getId().equals(caseId))
                .findFirst();

        return caseDefendants.map(pcDefendantOffenceIds ->
                pcDefendantOffenceIds.getDefendants().stream()
                        .filter(defendantOffenceIds -> defendantOffenceIds.getId().equals(defendantId))
                        .findFirst()
        ).orElse(empty());
    }

    private void onNewDefendantAddedForCourtProceedings(final NewDefendantAddedForCourtProceedings event) {

        final UUID caseId = event.getCaseId();
        final UUID defendantId = event.getDefendant().getId();

        final Optional<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIdsList = this.prosecutionCaseDefendantOffenceIds.stream()
                .filter(prosecutionCaseDefendantOffenceId -> prosecutionCaseDefendantOffenceId.getId().equals(caseId))
                .findFirst();
        prosecutionCaseDefendantOffenceIdsList.ifPresent(caseDefendantOffences ->
                caseDefendantOffences.getDefendants().add(DefendantOffenceIds.defendantOffenceIds().withId(defendantId).
                        withOffences(event.getDefendant().getOffences().stream().map(EventToDomainConverter::buildOffenceIds).collect(toList())).build())
        );

        if (nonNull(this.currentHearingEventState) && nonNull(currentHearingEventState.getListedCases())) {
            currentHearingEventState.getListedCases().stream()
                    .filter(listedCase -> listedCase.getId().equals(event.getCaseId()))
                    .findFirst()
                    .ifPresent(listedCase -> listedCase.getDefendants().add(event.getDefendant()));
        }

    }

    private void onHearingListed(final HearingListed event) {
        LOGGER.info("onHearingListed() event:{}", event);
        final uk.gov.justice.listing.events.Hearing hearing = event.getHearing();
        this.hearingId = hearing.getId();
        this.type = Type.type()
                .withId(hearing.getType().getId())
                .withDescription(hearing.getType().getDescription())
                .withWelshDescription(hearing.getType().getWelshDescription())
                .build();
        this.startDate = hearing.getStartDate();
        this.endDate = hearing.getEndDate();
        this.estimatedMinutes = hearing.getEstimatedMinutes();
        this.estimatedDuration = hearing.getEstimatedDuration();
        this.nonSittingDays = hearing.getNonSittingDays();

        if (hearing.getJudiciary() != null) {
            this.judiciary = hearing.getJudiciary().stream().map(jr -> JudicialRole.judicialRole()
                            .withJudicialRoleType(uk.gov.moj.cpp.listing.domain.JudicialRoleType.judicialRoleType()
                                    .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                                    .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId())
                                    .build())
                            .withJudicialId(jr.getJudicialId())
                            .withIsDeputy(ofNullable(jr.getIsDeputy()))
                            .withIsBenchChairman(ofNullable(jr.getIsBenchChairman()))
                            .build())
                    .filter(Objects::nonNull)
                    .collect(toList());
        }

        this.hearingLanguage = HearingLanguage.valueFor(hearing.getHearingLanguage().toString())
                .orElseThrow(IllegalArgumentException::new);
        this.courtRoomId = hearing.getCourtRoomId();
        this.courtCentreId = hearing.getCourtCentreId();
        if (hearing.getNonDefaultDays() != null) {
            this.nonDefaultDays = hearing.getNonDefaultDays().stream()
                    .map(ndd -> NonDefaultDay.nonDefaultDay().withCourtScheduleId(ofNullable(ndd.getCourtScheduleId()))
                            .withStartTime(ndd.getStartTime())
                            .withDuration(ofNullable(ndd.getDuration()))
                            .build())
                    .collect(toList());
        }
        this.reportingRestrictionReason = hearing.getReportingRestrictionReason();
        this.jurisdictionType = JurisdictionType.valueFor(hearing.getJurisdictionType().name()).orElse(null);
        // Standalone CourtApplication will not have any associated case
        if (nonNull(hearing.getListedCases())) {
            this.prosecutionCaseDefendantOffenceIds = hearing.getListedCases().stream()
                    .map(lc -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(lc.getId())
                            .withDefendants(lc.getDefendants().stream()
                                    .filter(defendant -> !defendant.getOffences().isEmpty())
                                    .map(this::buildDomainDefendantOffenceIds)
                                    .collect(toList()))
                            .build()
                    ).filter(pcd -> !pcd.getDefendants().isEmpty())
                    .collect(toList());
            hearing.getListedCases().forEach(
                    listedCase -> prosecutionCaseDefendants.put(listedCase.getId(), listedCase.getDefendants().stream()
                            .map(uk.gov.justice.listing.events.Defendant::getId)
                            .collect(toList())));
        }
        this.hearingDays = convertHearingDaysToDomain(hearing.getHearingDays());

        if (hearing.getCourtApplications() != null) {
            this.confirmedCourtApplicationIds = hearing.getCourtApplications().stream().distinct()
                    .map(uk.gov.justice.listing.events.CourtApplication::getId).collect(toList());
            hearing.getCourtApplications().stream().distinct().filter(ca -> isNotEmpty(ca.getOffences())).forEach(courtApplication ->
                    applicationOffenceIds.put(courtApplication.getId(), courtApplication.getOffences().stream().map(Offence::getId).collect(toList())));
        }

        this.hasAdjournmentDate = StringUtils.isNotEmpty(hearing.getAdjournedFromDate());

        this.weekCommencingStartDate = hearing.getWeekCommencingStartDate();
        this.weekCommencingEndDate = hearing.getWeekCommencingEndDate();
        this.weekCommencingDurationInWeeks = hearing.getWeekCommencingDurationInWeeks();

        initialiseCurrentHearingState(hearing);
    }

    private void onAddedCasesForHearing(AddedCasesForHearing event) {
        if (nonNull(event.getUnAllocatedListedCases())) {
            event.getUnAllocatedListedCases().forEach(
                    listedCase -> prosecutionCaseDefendants.put(listedCase.getId(), listedCase.getDefendants().stream()
                            .map(uk.gov.justice.listing.events.Defendant::getId)
                            .collect(toList())));
        }
        if (nonNull(this.currentHearingEventState)) {
            updateCurrentHearingEventStateOnCaseAdded(event.getUnAllocatedListedCases());
        }
    }

    private List<uk.gov.justice.listing.events.HearingDay> mergeHearingDaySequences(final List<uk.gov.justice.listing.events.HearingDay> hearingDaysChangedForHearing, final Map<ZonedDateTime, HearingDay> existingHearingDays) {
        return hearingDaysChangedForHearing.stream()
                .map(cd -> uk.gov.justice.listing.events.HearingDay.hearingDay()
                        .withDurationMinutes(cd.getDurationMinutes())
                        .withEndTime(cd.getEndTime())
                        .withSequence(existingHearingDays.containsKey(cd.getStartTime())
                                ? existingHearingDays.get(cd.getStartTime()).getSequence()
                                : 0)
                        .withHearingDate(cd.getHearingDate())
                        .withStartTime(cd.getStartTime())
                        .withIsCancelled(cd.getIsCancelled())
                        .withCourtScheduleId(cd.getCourtScheduleId())
                        .withCourtRoomId(cd.getCourtRoomId())
                        .withCourtCentreId(cd.getCourtCentreId())
                        .build())
                .collect(toList());
    }

    private void preservePreviouslyChangedCourtRooms(final List<uk.gov.justice.listing.events.HearingDay> hearingDaysChangedForHearing, final Map<LocalDate, HearingDay> existingHearingDays) {
        hearingDaysChangedForHearing.replaceAll(hd -> {
            uk.gov.moj.cpp.listing.domain.aggregate.HearingDay previousHearingDayToKeep = existingHearingDays.get(hd.getHearingDate());
            if (previousHearingDayToKeep == null) {
                return hd;
            }
            return convertDomainToHearingDayEvent(previousHearingDayToKeep);
        });

    }

    private SequencesResetOnHearingDays createSequencesResetOnHearingDaysEvent(final UUID hearingId) {
        return SequencesResetOnHearingDays.sequencesResetOnHearingDays()
                .withHearingId(hearingId)
                .build();
    }

    private void onTypeChangedForHearing(final TypeChangedForHearing event) {
        this.type = Type.type().withId(event.getType().getId()).withDescription(event.getType().getDescription()).withWelshDescription(event.getType().getWelshDescription()).build();
        if (nonNull(this.currentHearingEventState)) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withType(event.getType())
                    .build();
        }
    }

    private void onStartDateChangedForHearing(final StartDateChangedForHearing event) {
        this.startDate = LocalDate.parse(event.getStartDate());
    }

    private void onEndDateChangedForHearing(final EndDateChangedForHearing event) {
        this.endDate = LocalDate.parse(event.getEndDate());
    }

    private void onWeekCommencingDateChangedForHearing(final WeekCommencingDateChangedForHearing event) {
        updateWeekCommencings(LocalDate.parse(event.getWeekCommencingStartDate()), LocalDate.parse(event.getWeekCommencingEndDate()), event.getWeekCommencingDurationInWeeks());
    }

    private void onNonSittingDaysChangedForHearing(final NonSittingDaysChangedForHearing event) {
        this.nonSittingDays = event.getNonSittingDays();
    }

    private void onNonSittingDaysAssignedToHearing(final NonSittingDaysAssignedToHearing event) {
        this.nonSittingDays = event.getNonSittingDays();
    }

    private void onNonDefaultDaysAssignedToHearing(final NonDefaultDaysAssignedToHearing event) {
        this.nonDefaultDays = convertNonDefaultDaysToDomain(event.getNonDefaultDays());
        this.updateSlot = event.getNonDefaultDays().stream().anyMatch(ndd -> StringUtils.isNotEmpty(ndd.getCourtScheduleId()));
        updateCurrentHearingEventStateWithNonDefaultDays();
    }

    private void onHearingLanguageChanged(final HearingLanguageChangedForHearing event) {
        this.hearingLanguage = HearingLanguage.valueFor(event.getHearingLanguage().toString())
                .orElseThrow(IllegalArgumentException::new);
        if (nonNull(currentHearingEventState)) {
            currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing()
                    .withValuesFrom(currentHearingEventState)
                    .withHearingLanguage(event.getHearingLanguage())
                    .build();
        }
    }

    private void initialiseCurrentHearingState(final uk.gov.justice.listing.events.Hearing hearing) {
        currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing()
                .withValuesFrom(hearing)
                .withCourtApplications(nonNull(hearing.getCourtApplications()) ? hearing.getCourtApplications().stream().distinct().collect(toList()) : null)
                .build();
    }

    private boolean hasSequenceChanged(final Map<LocalDate, Integer> hearingDaysSequencesMap) {
        return this.hearingDays.stream().anyMatch(d -> ((!hearingDaysSequencesMap.containsKey(d.getHearingDate()))
                || (hearingDaysSequencesMap.containsKey(d.getHearingDate()) && !(hearingDaysSequencesMap.get(d.getHearingDate()).equals(d.getSequence())))));
    }

    private void onNonDefaultDaysChangedForHearing(final NonDefaultDaysChangedForHearing event) {
        this.nonDefaultDays = convertNonDefaultDaysToDomain(event.getNonDefaultDays());
        this.updateSlot = event.getNonDefaultDays().stream().anyMatch(ndd -> StringUtils.isNotEmpty(ndd.getCourtScheduleId()));
        updateCurrentHearingEventStateWithNonDefaultDays();
    }

    private void onHearingDaysChangedForHearing(final HearingDaysChangedForHearing hearingDaysChangedForHearing) {
        updateHearingDays(hearingDaysChangedForHearing.getHearingDays());
    }

    private void onHearingDaysCancelledForHearing(final HearingDaysCancelled hearingDaysCancelled) {
        updateHearingDays(hearingDaysCancelled.getHearingDays());
    }

    private void onJudiciaryAssignedToHearing(final JudiciaryAssignedToHearing event) {
        withJudiary(event.getJudiciary());

    }

    private void onJurisdictionChangedForHearing(final JurisdictionChangedForHearing event) {
        this.jurisdictionType = JurisdictionType.valueFor(event.getJurisdictionType().toString())
                .orElseThrow(IllegalArgumentException::new);
        if (nonNull(this.currentHearingEventState)) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withJurisdictionType(event.getJurisdictionType())
                    .build();
        }
    }

    private void onJudiciaryChangedForHearing(final JudiciaryChangedForHearing event) {
        withJudiary(event.getJudiciary());
    }

    @SuppressWarnings({"squid:S1172"})
    private void onJudiciaryRemovedFromHearing(final JudiciaryRemovedFromHearing event) {
        this.judiciary = emptyList();
    }

    private void onCourtRoomAssignedToHearing(final CourtRoomAssignedToHearing event) {
        updateCourtRoomId(event.getCourtRoomId());
    }

    private void onCourtCentreChangedForHearing(final CourtCentreChangedForHearing event) {
        this.courtCentreId = event.getCourtCentreId();
        if (nonNull(this.currentHearingEventState)) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withCourtCentreId(courtCentreId).build();
        }
    }

    private void onCourtRoomChangedForHearing(final CourtRoomChangedForHearing event) {
        updateCourtRoomId(event.getCourtRoomId());
    }

    private void onVideoLinkChangedForHearing(final VideoLinkDetailsChangedForHearing event) {
        this.publicListNote = event.getVideoLinkDetails();
        this.hasVideoLink = event.getHasVideoLink();
    }

    private void onVideoLinkAssignedForHearing(final VideoLinkDetailsAssignedForHearing event) {
        this.publicListNote = event.getVideoLinkDetails();
        this.hasVideoLink = event.getHasVideoLink();
    }

    private void onVideoLinkRemovedForHearing(final VideoLinkDetailsRemovedForHearing event) {
        this.publicListNote = null;
        this.hasVideoLink = false;
    }

    private void onVideoLinkChangedForHearing(final VideoLinkChangedForHearing event) {
        this.hasVideoLink = event.getHasVideoLink();
    }

    private void onPublicListNoteChangedForHearing(final PublicListNoteChangedForHearing event) {
        this.publicListNote = event.getPublicListNote();
    }

    private void onPublicListNoteRemovedFromHearing(final PublicListNoteRemovedFromHearing event) {
        this.publicListNote = null;
    }

    @SuppressWarnings({"squid:S1172"})
    private void onCourtRoomRemovedFromHearing(final CourtRoomRemovedFromHearing event) {
        updateCourtRoomId(null);
    }

    @SuppressWarnings({"squid:S1172"})
    private void onSequencesResetOnHearingDays(final SequencesResetOnHearingDays event) {
        if (!this.hearingDays.isEmpty()) {
            this.hearingDays = this.hearingDays.stream()
                    .map(cd -> HearingDay.hearingDay()
                            .withDurationMinutes(cd.getDurationMinutes())
                            .withEndTime(cd.getEndTime())
                            .withSequence(0)
                            .withStartTime(cd.getStartTime())
                            .withHearingDate(cd.getHearingDate())
                            .withIsCancelled(cd.isCancelled())
                            .withCourtScheduleId(cd.getCourtScheduleId())
                            .withCourtRoomId(cd.getCourtRoomId())
                            .withCourtCentreId(cd.getCourtCentreId())
                            .build())
                    .collect(toList());

            updateCurrentHearingEventStateWithHearingDays();

        }
    }

    @SuppressWarnings({"squid:S1172"})
    private void onEndDateRemovedFromHearing(final EndDateRemovedFromHearing event) {
        this.endDate = null;
    }

    private void onStartDateRemovedForHearing(final StartDateRemovedForHearing event) {
        this.startDate = null;
        final boolean unscheduled = ofNullable(event.getUnscheduled()).orElse(false);
        this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(this.currentHearingEventState)
                .withUnscheduled(unscheduled)
                .build();
    }

    private void onWeekCommencingDateRemovedForHearing(final WeekCommencingDateRemovedForHearing event) {
        updateWeekCommencings(null, null, null);
    }

    private void onHearingDaysSequenced(final HearingDaysSequenced hearingDaysSequenced) {
        updateHearingDays(hearingDaysSequenced.getHearingDays());
    }

    @SuppressWarnings({"squid:S1172"})
    private void onHearingAllocatedForListing(final HearingAllocatedForListing event) {
        updateAllocated(TRUE);

        if (isNotEmpty(event.getProsecutionCaseDefendantsOffenceIds())) {
            this.prosecutionCaseDefendantOffenceIds = event.getProsecutionCaseDefendantsOffenceIds().stream()
                    .map(lc -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(lc.getId())
                            .withDefendants(lc.getDefendants().stream()
                                    .filter(d -> !d.getOffenceIds().isEmpty())
                                    .map(d -> DefendantOffenceIds.defendantOffenceIds()
                                            .withId(d.getId())
                                            .withOffences(nonNull(d.getOffenceIds()) ?
                                                    d.getOffenceIds().stream().map(offence -> OffenceIds.offenceIds()
                                                            .withId(offence)
                                                            .build()).collect(toList()) : null)
                                            .build())
                                    .collect(toList()))
                            .build()
                    ).filter(pcd -> !pcd.getDefendants().isEmpty())
                    .collect(toList());

            this.prosecutionCaseDefendants.clear();
            event.getProsecutionCaseDefendantsOffenceIds().forEach(
                    prosecutionCase -> this.prosecutionCaseDefendants.put(prosecutionCase.getId(), prosecutionCase.getDefendants().stream()
                            .filter(defendant -> !defendant.getOffenceIds().isEmpty())
                            .map(uk.gov.justice.listing.events.DefendantOffenceIds::getId)
                            .collect(toList())));
        } else {
            this.prosecutionCaseDefendantOffenceIds = emptyList();
        }
    }

    @SuppressWarnings({"squid:S1172"})
    private void onHearingAllocatedForListingV2(final HearingAllocatedForListingV2 event) {
        updateAllocated(TRUE);

        if (isNotEmpty(event.getProsecutionCaseDefendantsOffenceIds())) {
            final Map<UUID, SeedingHearing> seedingHearingMap = getOffencesSeedingHearingMap();

            this.prosecutionCaseDefendantOffenceIds = event.getProsecutionCaseDefendantsOffenceIds().stream()
                    .map(lc -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(lc.getId())
                            .withDefendants(lc.getDefendants().stream()
                                    .filter(d -> !d.getOffenceIds().isEmpty())
                                    .map(d -> DefendantOffenceIds.defendantOffenceIds()
                                            .withId(d.getId())
                                            .withOffences(nonNull(d.getOffenceIds()) ?
                                                    d.getOffenceIds().stream().map(offence -> OffenceIds.offenceIds()
                                                            .withId(offence.getId())
                                                            .withSeedingHearing(seedingHearingMap.get(offence.getId()))
                                                            .build()).collect(toList()) : null)
                                            .build())
                                    .collect(toList()))
                            .build()
                    ).collect(toList());

            this.prosecutionCaseDefendants.clear();
            event.getProsecutionCaseDefendantsOffenceIds().forEach(
                    prosecutionCase -> this.prosecutionCaseDefendants.put(prosecutionCase.getId(), prosecutionCase.getDefendants().stream()
                            .map(uk.gov.justice.listing.events.DefendantOffenceIdsV2::getId)
                            .collect(toList())));
        } else {
            this.prosecutionCaseDefendantOffenceIds = emptyList();
        }
    }

    /**
     * This method is for seedingHearing for each offences. Map is populated by
     * prosecutionCaseDefendantOffenceIds which is loaded when the hearing listed or updated.
     *
     * @return Map<UUID, SeedingHearing>
     */
    private Map<UUID, SeedingHearing> getOffencesSeedingHearingMap() {
        final Map<UUID, SeedingHearing> seedingHearingMap = new HashMap<>();
        if (isNotEmpty(this.prosecutionCaseDefendantOffenceIds)) {
            this.prosecutionCaseDefendantOffenceIds.stream()
                    .filter(pc -> isNotEmpty(pc.getDefendants()))
                    .forEach(pc -> pc.getDefendants()
                            .forEach(d -> d.getOffences().stream()
                                    .filter(o -> nonNull(o.getSeedingHearing()))
                                    .forEach(o -> seedingHearingMap.put(o.getId(), o.getSeedingHearing()))));
        }
        return seedingHearingMap;
    }

    @SuppressWarnings({"squid:S1172"})
    private void onAllocatedHearingUpdatedForListing(final AllocatedHearingUpdatedForListing event) {
        // Do nothing
    }

    private void onAllocatedHearingUpdatedForListingV2(final AllocatedHearingUpdatedForListingV2 event) {
        // Do nothing
    }

    private void onHearingsUpdateCompleted(final HearingsUpdateCompleted event) {
        // Do nothing
    }


    private void onAllocatedHearingExtendedForListingV2(final AllocatedHearingExtendedForListingV2 event) {
        // Do nothing
    }

    private void onAllocatedHearingExtendedForListing(final AllocatedHearingExtendedForListing event) {
        // Do nothing
    }

    @SuppressWarnings({"squid:S1172"})
    private void onHearingUnallocatedForListing(final HearingUnallocatedForListing event) {
        updateAllocated(FALSE);
    }

    private void onCasesAddedToHearing(final CasesAddedToHearing casesAddedToHearing) {
        casesAddedToHearing.getUnAllocatedListedCases().forEach(listedCase ->
        {
            final Optional<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceId =
                    prosecutionCaseDefendantOffenceIds.stream().filter(pc -> pc.getId().equals(listedCase.getId())).findFirst();
            if (prosecutionCaseDefendantOffenceId.isPresent()) {
                listedCase.getDefendants().forEach(defendant ->
                        addDefendantToProsecutionCaseDefendantsOffenceIds(prosecutionCaseDefendantOffenceId, defendant));
            } else {
                addCaseToProsecutionCaseDefendantsOffenceIds(listedCase);
            }
        });

        if (nonNull(this.currentHearingEventState)) {
            updateCurrentHearingEventStateOnCaseAdded(casesAddedToHearing.getUnAllocatedListedCases());
        }
    }

    private void addCaseToProsecutionCaseDefendantsOffenceIds(final uk.gov.justice.listing.events.ListedCase listedCase) {
        prosecutionCaseDefendantOffenceIds.add(ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                .withId(listedCase.getId())
                .withDefendants(listedCase.getDefendants().stream()
                        .filter(defendant -> !defendant.getOffences().isEmpty())
                        .map(this::buildDomainDefendantOffenceIds)
                        .collect(toList()))
                .build());
    }

    private void addDefendantToProsecutionCaseDefendantsOffenceIds(final Optional<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceId, final uk.gov.justice.listing.events.Defendant defendant) {
        final Optional<DefendantOffenceIds> defendantOffenceId = prosecutionCaseDefendantOffenceId.get().getDefendants().stream()
                .filter(defendantOffenceIds -> defendantOffenceIds.getId().equals(defendant.getId()))
                .findFirst();
        if (defendantOffenceId.isPresent()) {
            defendant.getOffences()
                    .forEach(offence ->
                            addOffenceToProsecutionCaseDefendantsOffenceIds(defendantOffenceId, offence));
        } else {
            prosecutionCaseDefendantOffenceId.get().getDefendants().add(buildDomainDefendantOffenceIds(defendant));
        }
    }

    private void addOffenceToProsecutionCaseDefendantsOffenceIds(final Optional<DefendantOffenceIds> defendantOffenceId, final Offence offence) {
        final Optional<OffenceIds> offenceIds = defendantOffenceId.get().getOffences().stream()
                .filter(o -> o.getId().equals(offence.getId()))
                .findFirst();
        if (!offenceIds.isPresent()) {
            defendantOffenceId.get().getOffences().add(EventToDomainConverter.buildOffenceIds(offence));
        }
    }

    private void onOffencesRemovedFromHearing(final OffencesRemovedFromHearing event) {
        removeSeededOffences(event);
    }

    /**
     * @param event if previous hearings that contains offence id seeded by another hearing, delete
     *              all offences match the seedId and also delete case which all offences seeded by
     *              seedingHearingId but not delete all hearing
     */
    private void removeSeededOffences(final OffencesRemovedFromHearing event) {
        prosecutionCaseDefendantOffenceIds.removeIf(pc -> event.getCaseIdsSeededByOnlySeedingHearingId().contains(pc.getId()));

        prosecutionCaseDefendantOffenceIds.forEach(pc -> pc.getDefendants()
                .forEach(defendantOffenceIds -> defendantOffenceIds.getOffences().removeIf(o -> nonNull(o.getSeedingHearing()) &&
                        event.getSeedingHearingId().equals(o.getSeedingHearing().getSeedingHearingId()))));

        prosecutionCaseDefendantOffenceIds.removeIf(pc -> pc.getDefendants().isEmpty());

        prosecutionCaseDefendants.clear();
        prosecutionCaseDefendantOffenceIds.forEach(pc -> prosecutionCaseDefendants.put(pc.getId(), pc.getDefendants().stream().map(DefendantOffenceIds::getId).collect(toList())));


        if (nonNull(this.currentHearingEventState) && nonNull(currentHearingEventState.getListedCases())) {
            currentHearingEventState.getListedCases().removeIf(pc -> event.getCaseIdsSeededByOnlySeedingHearingId().contains(pc.getId()));

            currentHearingEventState.getListedCases().stream()
                    .flatMap(listedCase -> listedCase.getDefendants().stream())
                    .forEach(defendant -> defendant.getOffences().removeIf(offence -> nonNull(offence.getSeedingHearing()) &&
                            event.getSeedingHearingId().equals(offence.getSeedingHearing().getSeedingHearingId())));

            currentHearingEventState.getListedCases().forEach(listedCase -> listedCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
            currentHearingEventState.getListedCases().removeIf(pc -> pc.getDefendants().isEmpty());

        }
    }


    private List<uk.gov.justice.listing.events.NonDefaultDay> convertNonDefaultDaysToEvents(final List<uk.gov.moj.cpp.listing.domain.NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay()
                        .withStartTime(ndd.getStartTime())
                        .withDuration(ndd.getDuration().orElse(null))
                        .withSession(ndd.getSession().orElse(null))
                        .withOucode(ndd.getOucode().orElse(null))
                        .withCourtScheduleId(ndd.getCourtScheduleId().orElse(null))
                        .withCourtRoomId(ndd.getCourtRoomId().orElse(null))
                        .withRoomId(ndd.getRoomId().orElse(null))
                        .withCourtCentreId(ndd.getCourtCentreId().orElse(null))
                        .build())
                .collect(toList());
    }


    private List<NonDefaultDay> convertNonDefaultDaysToDomain(final List<uk.gov.justice.listing.events.NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> NonDefaultDay.nonDefaultDay()
                        .withStartTime(ndd.getStartTime())
                        .withDuration(ofNullable(ndd.getDuration()))
                        .withSession(ofNullable(ndd.getSession()))
                        .withOucode(ofNullable(ndd.getOucode()))
                        .withCourtScheduleId(ofNullable(ndd.getCourtScheduleId()))
                        .withCourtRoomId(ofNullable(ndd.getCourtRoomId()))
                        .withRoomId(ofNullable(ndd.getRoomId()))
                        .withCourtCentreId(ofNullable(ndd.getCourtCentreId()))
                        .build())
                .collect(toList());
    }

    private List<HearingDay> convertHearingDaysToDomain(final List<uk.gov.justice.listing.events.HearingDay> hearingDays) {
        if (isEmpty(hearingDays)) {
            return emptyList();
        }
        return hearingDays.stream()
                .map(cd -> HearingDay.hearingDay()
                        .withCourtScheduleId(cd.getCourtScheduleId())
                        .withDurationMinutes(cd.getDurationMinutes())
                        .withEndTime(cd.getEndTime())
                        .withSequence(cd.getSequence())
                        .withStartTime(cd.getStartTime())
                        .withHearingDate(cd.getHearingDate())
                        .withIsCancelled(cd.getIsCancelled())
                        .withCourtCentreId(cd.getCourtCentreId())
                        .withCourtRoomId(cd.getCourtRoomId())
                        .build())
                .collect(toList());
    }

    private List<uk.gov.justice.listing.events.HearingDay> convertDomainToHearingDays(final List<HearingDay> hearingDays) {
        if (isEmpty(hearingDays)) {
            return emptyList();
        }
        return hearingDays.stream()
                .map(cd -> convertDomainToHearingDayEvent(cd))
                .collect(toList());
    }

    private static uk.gov.justice.listing.events.HearingDay convertDomainToHearingDayEvent(final HearingDay cd) {
        return uk.gov.justice.listing.events.HearingDay.hearingDay()
                .withCourtScheduleId(cd.getCourtScheduleId())
                .withDurationMinutes(cd.getDurationMinutes())
                .withEndTime(cd.getEndTime())
                .withSequence(cd.getSequence())
                .withStartTime(cd.getStartTime())
                .withHearingDate(cd.getHearingDate())
                .withIsCancelled(cd.isCancelled())
                .withCourtCentreId(cd.getCourtCentreId())
                .withCourtRoomId(cd.getCourtRoomId())
                .build();
    }

    private List<uk.gov.justice.listing.events.HearingDay> convertHearingDaysToEvent(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(cd -> uk.gov.justice.listing.events.HearingDay.hearingDay()
                        .withCourtScheduleId(cd.getCourtScheduleId())
                        .withDurationMinutes(getAdjustedDuration(cd.getDurationMinutes()))
                        .withEndTime(cd.getEndTime())
                        .withSequence(cd.getSequence())
                        .withStartTime(cd.getStartTime())
                        .withHearingDate(cd.getHearingDate())
                        .withIsCancelled(cd.isCancelled())
                        .withCourtCentreId(cd.getCourtCentreId())
                        .withCourtRoomId(cd.getCourtRoomId())
                        .build())
                .collect(toList());
    }

    private List<uk.gov.justice.listing.events.JudicialRole> convertToEvents(final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles) {
        return judicialRoles.stream()
                .map(this::buildJudicialRole)
                .collect(toList());
    }

    private List<JudicialRole> convertToDomain(final List<uk.gov.justice.listing.events.JudicialRole> judicialRoles) {
        return judicialRoles.stream()
                .map(jr -> JudicialRole.judicialRole()
                        .withJudicialRoleType(uk.gov.moj.cpp.listing.domain.JudicialRoleType.judicialRoleType()
                                .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                                .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId())
                                .build())
                        .withJudicialId(jr.getJudicialId())
                        .withIsDeputy(ofNullable(jr.getIsDeputy()))
                        .withIsBenchChairman(ofNullable(jr.getIsBenchChairman()))
                        .withUserId(jr.getUserId())
                        .build())
                .collect(toList());
    }

    private uk.gov.justice.listing.events.JudicialRole buildJudicialRole(final JudicialRole jr) {
        return uk.gov.justice.listing.events.JudicialRole.judicialRole()
                .withIsBenchChairman(jr.getIsBenchChairman().orElse(null))
                .withIsDeputy(jr.getIsDeputy().orElse(null))
                .withJudicialId(jr.getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                        .build())
                .withUserId(jr.getUserId())
                .build();
    }

    private uk.gov.justice.listing.events.DefendantOffenceIds buildEventDefendantOffenceIds(final uk.gov.moj.cpp.listing.domain.DefendantOffenceIds d) {
        return uk.gov.justice.listing.events.DefendantOffenceIds.defendantOffenceIds()
                .withId(d.getId())
                .withOffenceIds(d.getOffences().stream().map(uk.gov.moj.cpp.listing.domain.OffenceIds::getId).collect(toList()))
                .build();
    }

    /**
     * Update hearing for listing command's payload does not have seedingHearing information in the
     * offence level. Offences SeedingHearing map  pulls out from prosecutionCaseDefendantOffenceIds
     * which is stored on HearingListed.
     */
    private uk.gov.justice.listing.events.DefendantOffenceIdsV2 buildEventDefendantOffenceIdsV2(final uk.gov.moj.cpp.listing.domain.DefendantOffenceIds d) {
        final Map<UUID, SeedingHearing> seedingHearingMap = getOffencesSeedingHearingMap();
        return uk.gov.justice.listing.events.DefendantOffenceIdsV2.defendantOffenceIdsV2()
                .withId(d.getId())
                .withOffenceIds(d.getOffences().stream().map(offence -> uk.gov.justice.listing.events.OffenceIds.offenceIds()
                        .withId(offence.getId())
                        .withSeedingHearing(seedingHearingMap.containsKey(offence.getId()) ? NewDomainToEventConverter.buildSeedingHearing(seedingHearingMap.get(offence.getId())).get() : null)
                        .build()).collect(toList()))
                .build();
    }


    private uk.gov.moj.cpp.listing.domain.DefendantOffenceIds buildDomainDefendantOffenceIds(final uk.gov.justice.listing.events.Defendant d) {
        return uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                .withId(d.getId())
                .withOffences(d.getOffences().stream()
                        .map(EventToDomainConverter::buildOffenceIds)
                        .collect(toList()))
                .build();
    }

    private List<HearingLanguageNeeds> getHearingLanguageNeedsForAllApplicants(final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds) {
        return courtApplicationPartyListingNeeds.stream().filter(
                v -> v.getHearingLanguageNeeds().isPresent()).map(v -> v.getHearingLanguageNeeds().get()).collect(toList());
    }

    private void onCaseEjected() {
        // Do nothing
    }

    private void onApplicationEjected() {
        //Do nothing
    }

    private void onHearingDaysWithoutCourtCentreCorrected(final HearingDaysWithoutCourtCentreCorrected hearingDaysWithoutCourtCentreCorrected) {
        final List<uk.gov.justice.listing.events.HearingDay> correctedHearingDays = hearingDaysWithoutCourtCentreCorrected.getHearingDays();

        if (isEmpty(correctedHearingDays) || isEmpty(this.hearingDays)) {
            return;
        }

        final UUID centreId = correctedHearingDays.get(0).getCourtCentreId();
        final UUID roomId = correctedHearingDays.get(0).getCourtRoomId();

        correctHearingDaysWithoutCourtCentre(ofNullable(centreId), ofNullable(roomId));

        if (isNotEmpty(this.nonDefaultDays)) {
            correctNonDefaultDaysWithoutCourtCentre(ofNullable(centreId), ofNullable(roomId));
        }

    }

    private void onHearingDayCourtScheduleUpdated(final HearingDayCourtScheduleUpdated hearingDayCourtScheduleUpdated) {
        final List<HearingDayCourtSchedule> hearingDayCourtSchedules = hearingDayCourtScheduleUpdated.getHearingDayCourtSchedules();

        if (isEmpty(hearingDayCourtSchedules) || isEmpty(hearingDays)) {
            return;
        }

        final Map<LocalDate, UUID> scheduledHearingDateMap = new HashMap<>();
        hearingDayCourtSchedules.forEach(
                hd -> scheduledHearingDateMap.put(hd.getHearingDate(), hd.getCourtScheduleId()));

        if (isNotEmpty(hearingDays)) {
            hearingDays.replaceAll(hd -> {
                final UUID scheduleIdFromCourtScheduler = scheduledHearingDateMap.get(hd.getHearingDate());
                if (scheduleIdFromCourtScheduler != null && !scheduleIdFromCourtScheduler.equals(hd.getCourtScheduleId())) {
                    return HearingDay.hearingDay()
                            .withValuesFrom(hd)
                            .withCourtScheduleId(scheduleIdFromCourtScheduler)
                            .build();
                }
                return hd;
            });

            updateCurrentHearingEventStateWithHearingDays();

        }

        if (isNotEmpty(nonDefaultDays)) {
            nonDefaultDays.replaceAll(nd -> {
                final UUID scheduleIdFromCourtScheduler = scheduledHearingDateMap.get(nd.getStartTime().toLocalDate());
                if (scheduleIdFromCourtScheduler != null
                        && !scheduleIdFromCourtScheduler.toString().equals(nd.getCourtScheduleId().orElse(null))) {
                    return NonDefaultDay.nonDefaultDay()
                            .withValuesFrom(nd)
                            .withCourtScheduleId(Optional.of(scheduleIdFromCourtScheduler.toString()))
                            .build();
                }
                return nd;
            });
            updateCurrentHearingEventStateWithNonDefaultDays();

        }

    }

    private void correctHearingDaysWithoutCourtCentre(final Optional<UUID> centreId, final Optional<UUID> roomId) {
        this.hearingDays.replaceAll(hearingDay -> HearingDay.hearingDay()
                .withCourtCentreId(hearingDay.getCourtCentreId() == null ? centreId.orElse(null) : hearingDay.getCourtCentreId())
                .withCourtRoomId(hearingDay.getCourtRoomId() == null ? roomId.orElse(null) : hearingDay.getCourtRoomId())
                .withCourtScheduleId(hearingDay.getCourtScheduleId())
                .withDurationMinutes(hearingDay.getDurationMinutes())
                .withEndTime(hearingDay.getEndTime())
                .withHearingDate(hearingDay.getHearingDate())
                .withSequence(hearingDay.getSequence())
                .withStartTime(hearingDay.getStartTime())
                .withIsCancelled(hearingDay.isCancelled())
                .build());
        updateCurrentHearingEventStateWithHearingDays();
    }

    private void correctNonDefaultDaysWithoutCourtCentre(final Optional<UUID> centreId, final Optional<UUID> roomId) {
        this.nonDefaultDays.replaceAll(nonDefaultDay -> NonDefaultDay.nonDefaultDay()
                .withCourtScheduleId(nonDefaultDay.getCourtScheduleId())
                .withSession(nonDefaultDay.getSession())
                .withDuration(nonDefaultDay.getDuration())
                .withStartTime(nonDefaultDay.getStartTime())
                .withCourtRoomId(nonDefaultDay.getCourtRoomId())
                .withOucode(nonDefaultDay.getOucode())
                .withRoomId(nonDefaultDay.getRoomId().map(Optional::of).orElse(roomId.map(UUID::toString)))
                .withCourtCentreId(nonDefaultDay.getCourtCentreId().map(Optional::of).orElse(centreId.map(UUID::toString))).build());
    }

    @SuppressWarnings({"squid:S1172"})
    private void onCaseUpdateDefendantProceedingsUpdated(final CaseUpdateDefendantProceedingsUpdated caseUpdateDefendantProceedingsUpdated) {
        this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                .withListedCases(ofNullable(currentHearingEventState.getListedCases()).map(Collection::stream).orElseGet(Stream::empty)
                        .map(listedCase -> listedCase.getId().equals(caseUpdateDefendantProceedingsUpdated.getProsecutionCase().getId()) ?
                                buildListedCase(caseUpdateDefendantProceedingsUpdated.getProsecutionCase(), emptyList()) : listedCase)
                        .collect(toList()))
                .build();
    }

    private void onHearingMarkedAsDuplicate(final HearingMarkedAsDuplicate hearingMarkedAsDuplicate) {
        duplicate = true;
    }

    private void onAllocatedHearingDeleted(final AllocatedHearingDeleted allocatedHearingDeleted) {
        duplicate = true;
    }

    private void onUnallocatedHearingDeleted(final UnallocatedHearingDeleted allocatedHearingDeleted) {
        duplicate = true;
    }

    private void onHearingDeleted(final HearingDeleted hearingDeleted) {
        if (isDeletingMergedUnAllocatedHearing(hearingDeleted)) {
            deleted = true;
            this.currentHearingEventState = null;
        }
    }

    private void onHearingResultStatusUpdated(final HearingResultStatusUpdated event) {
        this.resulted = true;
    }

    private void onHearingListedCaseUpdated(final HearingListedCaseUpdated event) {
        LOGGER.info("onHearingListedCaseUpdated() event:{}", event);
        final uk.gov.justice.listing.events.Hearing hearing = event.getHearing();

        this.hearingId = hearing.getId();
        this.type = Type.type()
                .withId(hearing.getType().getId())
                .withDescription(hearing.getType().getDescription())
                .withWelshDescription(hearing.getType().getDescription())
                .build();
        this.startDate = hearing.getStartDate();
        this.endDate = hearing.getEndDate();
        this.estimatedMinutes = hearing.getEstimatedMinutes();
        this.nonSittingDays = hearing.getNonSittingDays();

        if (hearing.getJudiciary() != null) {
            this.judiciary = hearing.getJudiciary().stream().map(jr -> JudicialRole.judicialRole()
                            .withJudicialRoleType(uk.gov.moj.cpp.listing.domain.JudicialRoleType.judicialRoleType()
                                    .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                                    .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId())
                                    .build())
                            .withJudicialId(jr.getJudicialId())
                            .withIsDeputy(ofNullable(jr.getIsDeputy()))
                            .withIsBenchChairman(ofNullable(jr.getIsBenchChairman()))
                            .build())
                    .filter(Objects::nonNull)
                    .collect(toList());
        }

        this.hearingLanguage = HearingLanguage.valueFor(hearing.getHearingLanguage().toString())
                .orElseThrow(IllegalArgumentException::new);
        this.courtRoomId = hearing.getCourtRoomId();
        this.courtCentreId = hearing.getCourtCentreId();
        if (hearing.getNonDefaultDays() != null) {
            this.nonDefaultDays = hearing.getNonDefaultDays().stream()
                    .map(ndd -> NonDefaultDay.nonDefaultDay()
                            .withStartTime(ndd.getStartTime())
                            .withDuration(ofNullable(ndd.getDuration()))
                            .build())
                    .collect(toList());
        }
        this.reportingRestrictionReason = hearing.getReportingRestrictionReason();
        this.jurisdictionType = JurisdictionType.valueFor(hearing.getJurisdictionType().name()).orElse(null);
        // Standalone CourtApplication will not have any associated case

        if (nonNull(hearing.getListedCases())) {
            this.prosecutionCaseDefendantOffenceIds = ofNullable(this.prosecutionCaseDefendantOffenceIds).orElseGet(ArrayList::new);
            event.getUnAllocatedListedCases().stream()
                    .map(lc -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(lc.getId())
                            .withDefendants(lc.getDefendants().stream()
                                    .filter(defendant -> !defendant.getOffences().isEmpty())
                                    .map(this::buildDomainDefendantOffenceIds)
                                    .collect(toList()))
                            .build()
                    ).filter(pcd -> !pcd.getDefendants().isEmpty())
                    .forEach(this.prosecutionCaseDefendantOffenceIds::add);
        }
        this.hearingDays = convertHearingDaysToDomain(hearing.getHearingDays());

        if (hearing.getCourtApplications() != null) {
            this.confirmedCourtApplicationIds = hearing.getCourtApplications().stream()
                    .map(uk.gov.justice.listing.events.CourtApplication::getId).collect(toList());
        }
        if (!event.getUnAllocatedListedCases().isEmpty()) {
            unAllocatedListedCases.addAll(event.getUnAllocatedListedCases().stream().map(EventAggregateConverter::buildAggregateListedCase).collect(toList()));
        }
        initialiseCurrentHearingState(hearing);
    }

    private AllocatedHearingExtendedForListingV2 allocatedHearingExtendedForListingEvent(final uk.gov.justice.listing.events.Hearing unallocatedHearing, final boolean fullExtension, final Boolean sendNotificationToParties) {

        return allocatedHearingExtendedForListingV2()
                .withHearingId(this.hearingId)
                .withType(buildHearingType())
                .withEstimatedMinutes(this.estimatedMinutes)
                .withCourtCentreId(this.courtCentreId)
                .withJudiciary(this.judiciary.stream()
                        .map(this::buildJudicialRole)
                        .collect(toList()))
                .withHearingLanguage(uk.gov.justice.core.courts.HearingLanguage.valueFor(this.hearingLanguage.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withJurisdictionType(valueFor(this.jurisdictionType.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withReportingRestrictionReason(this.reportingRestrictionReason)
                .withCourtRoomId(this.courtRoomId)
                .withHearingDays(convertHearingDaysToEvent(this.hearingDays))
                .withProsecutionCaseDefendantsOffenceIds(isEmpty(this.prosecutionCaseDefendantOffenceIds) ? null : this.prosecutionCaseDefendantOffenceIds.stream()
                        .map(lc -> uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                                .withId(lc.getId())
                                .withDefendants(lc.getDefendants().stream()
                                        .filter(defendant -> !defendant.getOffences().isEmpty())
                                        .map(this::buildEventDefendantOffenceIds)
                                        .collect(toList()))
                                .build()
                        ).filter(pcd -> !pcd.getDefendants().isEmpty())
                        .collect(toList()))
                .withCourtApplicationIds(this.confirmedCourtApplicationIds.isEmpty() ? null : this.confirmedCourtApplicationIds)
                .withUnAllocatedListedCases(this.unAllocatedListedCases.isEmpty() ? null : this.unAllocatedListedCases.stream().map(EventAggregateConverter::buildEventListedCase).collect(toList()))
                .withExistingHearingId(unallocatedHearing.getId())
                .withFullExtension(fullExtension)
                .withSendNotificationToParties(sendNotificationToParties)
                .build();
    }

    public Stream<Object> addCasesForHearing(final List<ProsecutionCase> prosecutionCases, final List<UUID> shadowListedOffences) {
        if (this.duplicate || this.deleted) {
            return Stream.empty();
        }
        return apply(Stream.of(AddedCasesForHearing.addedCasesForHearing()
                .withUnAllocatedListedCases(prosecutionCases.stream()
                        .map(prosecutionCase -> buildListedCase(prosecutionCase, shadowListedOffences))
                        .collect(Collectors.toList()))
                .withHearingId(hearingId)
                .build()));
    }

    public Stream<Object> raiseHearingDaysWithoutCourtCentreCorrected(final UUID hearingId, final List<uk.gov.justice.listing.events.HearingDay> hearingDays) {
        return apply(Stream.of(HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withId(hearingId)
                .withHearingDays(hearingDays)
                .build()));
    }

    public Stream<Object> raiseHearingDayCourtSchedulesUpdated(UUID hearingId,
                                                               List<HearingDayCourtSchedule> hearingDayCourtSchedules) {
        return apply(Stream.of(HearingDayCourtScheduleUpdated.hearingDayCourtScheduleUpdated()
                .withHearingId(hearingId)
                .withHearingDayCourtSchedules(hearingDayCourtSchedules)
                .build()));
    }


    public Stream<Object> changeNextHearingDate(final UUID hearingId) {
        Stream<Object> events = Stream.empty();
        if (nonNull(hearingDays)) {
            final Set<UUID> seedingHearingIds = prosecutionCaseDefendantOffenceIds.stream()
                    .flatMap(pc -> pc.getDefendants().stream())
                    .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                    .filter(offenceIds -> nonNull(offenceIds.getSeedingHearing()))
                    .map(offenceIds -> offenceIds.getSeedingHearing().getSeedingHearingId())
                    .collect(Collectors.toSet());

            for (final UUID seedingHearingId : seedingHearingIds) {
                events = concat(events, Stream.of(NextHearingDayChanged.nextHearingDayChanged()
                        .withHearingId(hearingId)
                        .withSeedingHearingId(seedingHearingId)
                        .withHearingStartDate(getEarliestHearingStartDate())
                        .build()));
            }

        }
        return events;
    }

    public Stream<Object> deleteCourtApplicationHearing(final UUID hearingId) {
        return apply(Stream.of(CourtApplicationHearingDeleted.courtApplicationHearingDeleted()
                .withHearingId(hearingId)
                .build()
        ));
    }

    public Stream<Object> setHearingResultStatus(final UUID hearingId) {
        return apply(Stream.of(HearingResultStatusUpdated.hearingResultStatusUpdated()
                .withHearingId(hearingId)
                .build()
        ));
    }

    private ZonedDateTime getEarliestHearingStartDate() {
        return hearingDays.stream().min(comparing(HearingDay::getStartTime)).get().getStartTime();
    }

    /**
     * remove all offences which seeded by previous hearing and unallocate to hearing
     *
     * @param seedingHearingId
     * @param hearingId
     * @return caseIdsSeededByOnlySeedingHearingId : List for cases which all offences  seeded by
     * only seedingHearing Id
     */
    private Stream<Object> getOffencesRemovedFromHearingStream(final UUID seedingHearingId, final UUID hearingId) {

        final List<UUID> caseIdsSeededByOnlyOtherHearings = prosecutionCaseDefendantOffenceIds.stream()
                .filter(pc -> pc.getDefendants().stream()
                        .flatMap(d -> d.getOffences().stream())
                        .filter(offence -> nonNull(offence.getSeedingHearing()))
                        .anyMatch(offence -> isNotSeededOffenceBySeedId(seedingHearingId, offence)))
                .map(ProsecutionCaseDefendantOffenceIds::getId)
                .collect(toList());

        final List<UUID> caseIdsExtendedByOtherHearings = prosecutionCaseDefendantOffenceIds.stream()
                .filter(pc -> pc.getDefendants().stream()
                        .flatMap(d -> d.getOffences().stream())
                        .anyMatch(offence -> !ofNullable(offence.getIsNewOffence()).orElse(false) && isNull(offence.getSeedingHearing())))
                .map(ProsecutionCaseDefendantOffenceIds::getId)
                .collect(toList());

        final List<UUID> caseIdsSeededBySeedingHearingId = prosecutionCaseDefendantOffenceIds.stream()
                .filter(pc -> !caseIdsSeededByOnlyOtherHearings.contains(pc.getId()) && !caseIdsExtendedByOtherHearings.contains(pc.getId()))
                .map(ProsecutionCaseDefendantOffenceIds::getId)
                .collect(toList());

        final List<UUID> offencesSeededBySeedingHearing = getOffencesFromSeededHearingAndNew(seedingHearingId);

        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();

        eventStreamBuilder.add(OffencesRemovedFromHearing.offencesRemovedFromHearing()
                .withHearingId(hearingId)
                .withSeedingHearingId(seedingHearingId)
                .withCaseIdsSeededByOnlySeedingHearingId(caseIdsSeededBySeedingHearingId)
                .withUnallocated(!isAllocated())
                .withSeededOffences(offencesSeededBySeedingHearing)
                .build());

        if (isAllocated() && MAGISTRATES == jurisdictionType) {
            eventStreamBuilder.add(availableSlotsForHearingFreed()
                    .withHearingId(hearingId).build());
        }

        return apply(eventStreamBuilder.build());
    }

    // If a new offence has been added to hearing we need to remove them with seeded offences.
    private List<UUID> getOffencesFromSeededHearingAndNew(final UUID seedingHearingId) {
        final List<UUID> caseIdsSeededBySeededHearing = prosecutionCaseDefendantOffenceIds.stream()
                .filter(pc -> pc.getDefendants().stream()
                        .flatMap(d -> d.getOffences().stream())
                        .filter(offence -> nonNull(offence.getSeedingHearing()))
                        .anyMatch(offence -> isSeededOffenceBySeedId(seedingHearingId, offence)))
                .map(ProsecutionCaseDefendantOffenceIds::getId)
                .collect(toList());

        final List<UUID> offencesSeededBySeedingHearing = prosecutionCaseDefendantOffenceIds.stream()
                .filter(pc -> caseIdsSeededBySeededHearing.contains(pc.getId()))
                .flatMap(pc -> pc.getDefendants().stream())
                .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                .filter(offenceIds -> ofNullable(offenceIds.getIsNewOffence()).orElse(false) || isSeededOffenceBySeedId(seedingHearingId, offenceIds)) // We need to remove the offence resulted by the seeded hearing and the offence was added because the offence added to the case.
                .map(OffenceIds::getId)
                .collect(toList());
        return offencesSeededBySeedingHearing;
    }

    private boolean isNotSeededOffenceBySeedId(final UUID seedingHearingId, final uk.gov.moj.cpp.listing.domain.OffenceIds offence) {
        return isNull(offence.getSeedingHearing()) || !offence.getSeedingHearing().getSeedingHearingId().equals(seedingHearingId);
    }

    private boolean isSeededOffenceBySeedId(final UUID seedingHearingId, final uk.gov.moj.cpp.listing.domain.OffenceIds offence) {
        return !isNotSeededOffenceBySeedId(seedingHearingId, offence);
    }

    private Stream<Object> freeCancelledHearingDaySlots(final UUID hearingId, final List<HearingDay> hearingDaysFromAggregate) {
        if (MAGISTRATES != jurisdictionType) {
            return Stream.empty();
        }
        //TODO:This needs to be address because in mags we won't have any nondefault days.
        // noncancelled Hearingdays should be retained and slots should be payed back for cancelled ones.
        final List<NonDefaultDay> nonDefaultDaysToRetain = hearingDaysFromAggregate.stream()
                .filter(hearingDay -> !hearingDay.isCancelled())
                .flatMap(hearingDayNotCancelled -> this.nonDefaultDays.stream().filter(matchingNonDefaultDay(hearingDayNotCancelled)))
                .collect(toList());

        if (nonDefaultDaysToRetain.isEmpty()) {
            LOGGER.info("Could not find any non default days to be retained for hearing {}", hearingId);
            return Stream.empty();
        }

        return Stream.of(nonDefaultDaysChangedForHearing()
                .withHearingId(hearingId)
                .withNonDefaultDays(convertNonDefaultDaysToEvents(nonDefaultDaysToRetain))
                .withIsPublicEvent(true)
                .build());
    }

    private List<HearingDay> getAggregateHearingDaysAsCancelledFromCommand(final List<uk.gov.justice.listing.events.HearingDay> hearingDaysFromCommand) {
        final List<HearingDay> updatedHearingDaysWithAggregateState = newArrayList();

        this.hearingDays.forEach(hearingDayFromAggregate -> {
            final Optional<uk.gov.justice.listing.events.HearingDay> matchedOptionalHearingDayInCommand = hearingDaysFromCommand.stream().filter(matchingHearingDay(hearingDayFromAggregate)).findFirst();
            final boolean cancelled = matchedOptionalHearingDayInCommand
                    .map(uk.gov.justice.listing.events.HearingDay::getIsCancelled)
                    .orElseGet(hearingDayFromAggregate::isCancelled);
            updatedHearingDaysWithAggregateState.add(buildHearingDayWithCancelledStatus(hearingDayFromAggregate, cancelled));
        });
        return updatedHearingDaysWithAggregateState;
    }

    private Predicate<NonDefaultDay> matchingNonDefaultDay(final HearingDay hearingDayFromAggregate) {
        return nonDefaultDay -> nonDefaultDay.getStartTime().toLocalDate().equals(hearingDayFromAggregate.getHearingDate());
    }

    private Predicate<uk.gov.justice.listing.events.HearingDay> matchingHearingDay(final HearingDay hearingDayFromAggregate) {
        return hearingDayInCommand -> hearingDayInCommand.getHearingDate().equals(hearingDayFromAggregate.getHearingDate());
    }

    private HearingDay buildHearingDayWithCancelledStatus(final HearingDay hearingDayInAggregate, final boolean cancelled) {
        return new HearingDay(hearingDayInAggregate.getDurationMinutes(), hearingDayInAggregate.getEndTime(), hearingDayInAggregate.getHearingDate(),
                hearingDayInAggregate.getSequence(), hearingDayInAggregate.getStartTime(), hearingDayInAggregate.getCourtScheduleId(), cancelled,
                hearingDayInAggregate.getCourtCentreId(), hearingDayInAggregate.getCourtRoomId());
    }

    private ZonedDateTime getEarliestStartDate() {
        if (CollectionUtils.isEmpty(hearingDays)) {
            return null;
        }
        return hearingDays.stream()
                .map(HearingDay::getStartTime)
                .sorted()
                .findFirst().orElse(null);
    }

    private List<UUID> getAllOffenceIds() {
        final List<UUID> allOffenceIds = new ArrayList<>();
        applicationOffenceIds.forEach((uuid, uuids) -> allOffenceIds.addAll(uuids));
        return allOffenceIds;
    }

    /**
     * HearingDeleted event is moved with DD-14484 To block the event replay from marking the
     * historic hearings as deleted, we need to make sure the hearingId provided is matching with
     * the hearingId in the aggregate which will verify the unallocated hearing is being deleted.
     *
     * @param hearingDeleted Contains id of the unallocated and merged hearing to be marked as
     *                       deleted.
     * @return boolean
     */
    private boolean isDeletingMergedUnAllocatedHearing(final HearingDeleted hearingDeleted) {
        return hearingId.equals(hearingDeleted.getHearingIdToBeDeleted());
    }

    private void updateCourtRoomId(final UUID courtRoomId) {
        this.courtRoomId = courtRoomId;
        if (nonNull(this.currentHearingEventState)) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withCourtRoomId(courtRoomId).build();
        }
    }

    private void updateCurrentHearingEventStateWithOffence(UUID caseId, UUID defendantId, UUID oldOffenceId, Offence newOffence) {
        if (nonNull(this.currentHearingEventState)) {
            this.currentHearingEventState.getListedCases().stream()
                    .filter(listedCase -> listedCase.getId().equals(caseId))
                    .flatMap(listedCase -> listedCase.getDefendants().stream())
                    .filter(defendant -> defendant.getId().equals(defendantId))
                    .findFirst()
                    .ifPresent(defendant -> {
                        if (nonNull(oldOffenceId) && nonNull(newOffence)) {
                            defendant.getOffences().stream().filter(offence -> offence.getId().equals(oldOffenceId)).findFirst()
                                    .ifPresent(offence -> {
                                        final int index = defendant.getOffences().indexOf(offence);
                                        defendant.getOffences().set(index, newOffence);
                                    });
                        } else if (nonNull(oldOffenceId)) {
                            defendant.getOffences().removeIf(offence -> offence.getId().equals(oldOffenceId));
                        } else if (nonNull(newOffence)) {
                            defendant.getOffences().add(newOffence);
                        }
                    });
            currentHearingEventState.getListedCases().forEach(listedCase -> listedCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
            currentHearingEventState.getListedCases().removeIf(pc -> pc.getDefendants().isEmpty());
        }
    }

    private void updateHearingDays(final List<uk.gov.justice.listing.events.HearingDay> hearingDays) {
        this.hearingDays = convertHearingDaysToDomain(hearingDays);
        updateCurrentHearingEventStateWithHearingDays();
    }

    private void updateCurrentHearingEventStateWithHearingDays() {
        if (nonNull(this.currentHearingEventState)) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withHearingDays(convertDomainToHearingDays(this.hearingDays)).build();
        }
    }

    private void updateCurrentHearingEventStateWithNonDefaultDays() {
        if (nonNull(this.currentHearingEventState)) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withNonDefaultDays(convertNonDefaultDaysToEvents(this.nonDefaultDays)).build();
        }
    }

    private void withJudiary(final List<uk.gov.justice.listing.events.JudicialRole> judiciary) {
        this.judiciary = convertToDomain(judiciary);
        if (nonNull(this.currentHearingEventState)) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withJudiciary(convertToEvents(this.judiciary)).build();
        }
    }

    private void updateWeekCommencings(final LocalDate weekCommencingStartDate, final LocalDate weekCommencingEndDate, final Integer weekCommencingDurationInWeeks) {
        this.weekCommencingStartDate = weekCommencingStartDate;
        this.weekCommencingEndDate = weekCommencingEndDate;
        this.weekCommencingDurationInWeeks = weekCommencingDurationInWeeks;

        if (nonNull(this.currentHearingEventState)) {
            this.currentHearingEventState = uk.gov.justice.listing.events.Hearing.hearing().withValuesFrom(currentHearingEventState)
                    .withWeekCommencingStartDate(this.weekCommencingStartDate)
                    .withWeekCommencingEndDate(this.weekCommencingEndDate)
                    .withWeekCommencingDurationInWeeks(this.weekCommencingDurationInWeeks)
                    .build();
        }
    }

    private void removeOffences(final List<UUID> offenceIds2) {
        removeOffencesFromProsecutionCaseDefendantOffenceIds(offenceIds2);

        if (nonNull(this.currentHearingEventState) && nonNull(this.currentHearingEventState.getListedCases())) {
            removeOffencesFromCurrentHearingEventState(offenceIds2);
        }
    }

    private void removeOffencesFromProsecutionCaseDefendantOffenceIds(final List<UUID> offenceIds2) {
        prosecutionCaseDefendantOffenceIds.stream()
                .flatMap(obj -> obj.getDefendants().stream())
                .forEach(def -> def.getOffences().removeIf(offenceIds -> offenceIds2.stream().anyMatch(uuid -> uuid.equals(offenceIds.getId()))));
        prosecutionCaseDefendantOffenceIds.forEach(listedCase -> listedCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
        prosecutionCaseDefendantOffenceIds.removeIf(pc -> pc.getDefendants().isEmpty());

        prosecutionCaseDefendantOffenceIds
                .forEach(obj -> obj.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
        prosecutionCaseDefendantOffenceIds.removeIf(lc -> lc.getDefendants().isEmpty());
    }

    private void removeOffencesFromCurrentHearingEventState(final List<UUID> offenceIds2) {
        currentHearingEventState.getListedCases().stream()
                .flatMap(listedCase -> listedCase.getDefendants().stream())
                .forEach(defendant -> defendant.getOffences().removeIf(offence -> offenceIds2.contains(offence.getId())));

        currentHearingEventState.getListedCases().forEach(listedCase -> listedCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()));
        currentHearingEventState.getListedCases().removeIf(pc -> pc.getDefendants().isEmpty());
    }


    /**
     * This method is responsible for merging cases, defendants and offences between
     * HearingListingNeeds which is passed in payload and hearing in aggregate. Since the case can
     * be splitted at case, defendant and offence levels when we are merging here we need to compare
     * at every level and merge it back.
     *
     * @param listedCases Listed cases from event payload
     */
    @SuppressWarnings("squid:S1188")
    private void updateCurrentHearingEventStateOnCaseAdded(final List<uk.gov.justice.listing.events.ListedCase> listedCases) {
        if (nonNull(this.currentHearingEventState.getListedCases()) && nonNull(listedCases)) {

            listedCases.forEach(prosecutionCase -> {

                final Optional<uk.gov.justice.listing.events.ListedCase> matchingCaseOptional = this.currentHearingEventState.getListedCases().stream().filter(hearingCase -> hearingCase.getId().equals(prosecutionCase.getId())).findFirst();
                if (matchingCaseOptional.isPresent()) {

                    prosecutionCase.getDefendants().forEach(defendant -> {

                        final Optional<uk.gov.justice.listing.events.Defendant> matchingDefendantOptional = this.currentHearingEventState.getListedCases().stream()
                                .flatMap(hearingCase -> hearingCase.getDefendants().stream())
                                .filter(hearingCaseDefendant -> hearingCaseDefendant.getId().equals(defendant.getId()))
                                .findFirst();

                        if (matchingDefendantOptional.isPresent()) {
                            final Set<Offence> offenceSet = new HashSet<>(matchingDefendantOptional.get().getOffences());
                            offenceSet.addAll(defendant.getOffences().stream().filter(offence1 -> offenceSet.stream().noneMatch(offence2 -> offence2.getId().equals(offence1.getId()))).collect(toList()));
                            matchingDefendantOptional.get().getOffences().clear();
                            matchingDefendantOptional.get().getOffences().addAll(offenceSet);

                        } else {
                            matchingCaseOptional.get().getDefendants().add(defendant);
                        }
                    });

                } else {
                    this.currentHearingEventState.getListedCases().add(prosecutionCase);
                }

            });
        }
    }

    public uk.gov.justice.listing.events.Hearing getCurrentHearingEventState() {
        return currentHearingEventState;
    }

    public boolean getIsSummonsApprovedExists() {
        return isSummonsApprovedExists;
    }

    public Boolean isNotificationRelatedAllocatedFieldsUpdated(final List<uk.gov.justice.listing.commands.HearingDay> updatedHearingDays) {
        if (isNotEmpty(updatedHearingDays) && (notCurrentlyAssigned(this.hearingDays) || this.hearingDays.isEmpty())) {
            return true;
        }

        if (hasChanged(this.hearingDays, updatedHearingDays)) {
            final Optional<uk.gov.justice.listing.commands.HearingDay> updatedDays = updatedHearingDays.stream()
                    .filter(updatedDay -> hearingDays.stream()
                            .anyMatch(originalDay -> !Objects.equals(updatedDay.getCourtCentreId(), originalDay.getCourtCentreId()) ||
                                    !Objects.equals(updatedDay.getCourtRoomId(), originalDay.getCourtRoomId()) ||
                                    !Objects.equals(updatedDay.getStartTime(), originalDay.getStartTime())))
                    .findAny();
            return updatedDays.isPresent();
        }
        return false;
    }


    public Stream<Object> removeCaseFromGroupCases(final UUID hearingId, final UUID groupId,
                                                   final ListedCase removedCase,
                                                   final ListedCase newGroupMaster) {
        return apply(Stream.of(CaseRemovedFromGroupCases.caseRemovedFromGroupCases()
                .withHearingId(hearingId)
                .withGroupId(groupId)
                .withRemovedCase(NewDomainToEventConverter.buildListedCase(removedCase))
                .withNewGroupMaster(nonNull(newGroupMaster) ? NewDomainToEventConverter.buildListedCase(newGroupMaster) : null)
                .build()));
    }

    private void onCaseRemovedFromGroupCases(final CaseRemovedFromGroupCases caseRemovedFromGroupCases) {
        if (isNotEmpty(this.unAllocatedListedCases)) {
            final UUID removedCaseId = caseRemovedFromGroupCases.getRemovedCase().getId();
            if (this.unAllocatedListedCases.stream()
                    .anyMatch(lc -> removedCaseId.equals(lc.getId()))) {
                updateListedCase(this.unAllocatedListedCases, removedCaseId, FALSE, FALSE);
            }

            if (nonNull(caseRemovedFromGroupCases.getNewGroupMaster())) {
                final UUID newGroupMasterId = caseRemovedFromGroupCases.getNewGroupMaster().getId();
                if (this.unAllocatedListedCases.stream()
                        .anyMatch(lc -> newGroupMasterId.equals(lc.getId()))) {
                    updateListedCase(this.unAllocatedListedCases, newGroupMasterId, TRUE, TRUE);
                }
            }
        }
    }

    private void updateListedCase(final List<uk.gov.moj.cpp.listing.domain.aggregate.ListedCase> cases,
                                  final UUID caseId, final Boolean isGroupMember, final Boolean isGroupMaster) {
        final uk.gov.moj.cpp.listing.domain.aggregate.ListedCase updatedCase =
                uk.gov.moj.cpp.listing.domain.aggregate.ListedCase.listedCase()
                        .withValuesFrom(cases.stream()
                                .filter(lc -> lc.getId().equals(caseId))
                                .findFirst().get())
                        .withIsGroupMember(isGroupMember)
                        .withIsGroupMaster(isGroupMaster)
                        .build();
        cases.removeIf(lc -> lc.getId().equals(caseId));
        cases.add(updatedCase);
    }

    @VisibleForTesting
    public List<ProsecutionCaseDefendantOffenceIds> getProsecutionCaseDefendantOffenceIds() {
        return prosecutionCaseDefendantOffenceIds;
    }

}
