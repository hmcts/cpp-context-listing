package uk.gov.moj.cpp.listing.domain.aggregate;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.events.AllocatedHearingExtendedForListing.allocatedHearingExtendedForListing;
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
import static uk.gov.justice.listing.events.JurisdictionType.valueFor;
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
import static uk.gov.moj.cpp.listing.domain.utils.HearingUtil.getAdjustedDuration;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.AddedCasesForHearing;
import uk.gov.justice.listing.events.AllocatedHearingDeleted;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListing;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2;
import uk.gov.justice.listing.events.ApplicationEjected;
import uk.gov.justice.listing.events.CaseEjected;
import uk.gov.justice.listing.events.CaseIdentifierUpdated;
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
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing;
import uk.gov.justice.listing.events.EndDateChangedForHearing;
import uk.gov.justice.listing.events.EndDateRemovedFromHearing;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingDaysCancelled;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDaysSequenced;
import uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.NextHearingDayChanged;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing;
import uk.gov.justice.listing.events.UnallocatedHearingDeleted;
import uk.gov.justice.listing.events.HearingLanguageChangedForHearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingListedCaseUpdated;
import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.listing.events.HearingPartiallyUpdated;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.HearingTrialVacated;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.JudiciaryAssignedToHearing;
import uk.gov.justice.listing.events.JudiciaryChangedForHearing;
import uk.gov.justice.listing.events.JudiciaryRemovedFromHearing;
import uk.gov.justice.listing.events.JurisdictionChangedForHearing;
import uk.gov.justice.listing.events.LinkedCasesUpdated;
import uk.gov.justice.listing.events.NewCaseMarkerUpdated;
import uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings;
import uk.gov.justice.listing.events.NewDefendantDetailsUpdated;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.justice.listing.events.ProsecutionCases;
import uk.gov.justice.listing.events.PublicListNoteChangedForHearing;
import uk.gov.justice.listing.events.PublicListNoteRemovedFromHearing;
import uk.gov.justice.listing.events.SequencesResetOnHearingDays;
import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.listing.events.StartDateRemovedForHearing;
import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.justice.listing.events.TypeOfList;
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
import uk.gov.moj.cpp.listing.domain.aggregate.rules.HearingEndDateRule;
import uk.gov.moj.cpp.listing.domain.aggregate.rules.HearingLanguageRule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1172", "squid:S2629", "squid:S1948", "squid:S00107", "squid:S3655", "squid:S1067", "squid:CommentedOutCodeLine", "squid:S1068", "squid:S4973", "PMD.BeanMembersShouldSerialize", "PMD.NullAssignment"})
public class Hearing implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hearing.class);

    private static final long serialVersionUID = 4401532074124929479L;

    private final List<uk.gov.moj.cpp.listing.domain.aggregate.ListedCase> unAllocatedListedCases = new ArrayList<>();
    private UUID hearingId;
    private boolean allocated;
    private Type type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer estimatedMinutes;
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
    private Map<UUID, List<UUID>> prosecutionCaseDefendants = new HashMap<>();

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
                when(AllocatedHearingDeleted.class).apply(this::onAllocatedHearingDeleted),
                when(UnallocatedHearingDeleted.class).apply(this::onUnallocatedHearingDeleted),
                when(OffencesRemovedFromHearing.class).apply(this::onOffencesRemovedFromHearing),
                when(CasesAddedToHearing.class).apply(this::onCasesAddedToHearing),
                otherwiseDoNothing());
    }


    @SuppressWarnings({"squid:S00107"})
    public Stream<Object> list(final UUID hearingId, final Type type,
                               final int estimateMinutes, final List<ListedCase> listedCases,
                               final UUID courtCentreId, final List<JudicialRole> judiciary,
                               final UUID courtRoomId, final String listingDirections,
                               final JurisdictionType jurisdictionType, final String prosecutorDatesToAvoid,
                               final String reportingRestrictionReason,
                               final ZonedDateTime startDate, final LocalDate endDate, final CourtCentreDefaults courtCentreDefaults,
                               final List<CourtApplication> courtApplications, final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds,
                               final Integer hearingTypeDuration, final Optional<String> adjournedFromDate, final Optional<LocalDate> weekCommencingStartDate,
                               final Optional<LocalDate> weekCommencingEndDate, final Optional<Integer> weekCommencingDurationInWeeks, final List<NonDefaultDay> nonDefaultDays,
                               final Boolean isSlotsBooked) {

        if (this.duplicate) {
            return Stream.empty();
        }

        if (notCurrentlyListed()) {

            final CourtCentre defaultCourtCentre = CourtCentre.courtCentre()
                    .withId(courtCentreId)
                    .withRoomId(ofNullable(courtRoomId)).build();

            final uk.gov.justice.listing.events.Hearing.Builder builder = uk.gov.justice.listing.events.Hearing.hearing();
            builder.withId(hearingId)
                    .withType(uk.gov.justice.listing.events.Type.type()
                            .withId(type.getId())
                            .withDescription(type.getDescription())
                            .withWelshDescription(type.getWelshDescription())
                            .build())
                    .withAllocated(false)
                    .withAdjournedFromDate(adjournedFromDate)
                    .withJudiciary(judiciary.stream()
                            .map(NewDomainToEventConverter::buildJudicialRole)
                            .collect(toList()))
                    .withListedCases(listedCases.isEmpty() ? null : listedCases.stream()
                            .map(NewDomainToEventConverter::buildListedCase)
                            .collect(toList()))
                    .withListingDirections(ofNullable(listingDirections))
                    .withHearingLanguage(HearingLanguageRule.apply(listedCases, getHearingLanguageNeedsForAllApplicants(courtApplicationPartyListingNeeds)))
                    .withReportingRestrictionReason(ofNullable(reportingRestrictionReason))
                    .withCourtRoomId(defaultCourtCentre.getRoomId())
                    .withCourtCentreId(defaultCourtCentre.getId())
                    .withEstimatedMinutes(estimateMinutes)
                    .withProsecutorDatesToAvoid(ofNullable(prosecutorDatesToAvoid))
                    .withJurisdictionType(valueFor(jurisdictionType.name()).orElse(null))
                    .withEndDate(nonNull(endDate) ? Optional.of(endDate) : empty())
                    .withIsSlotsBooked(of(isSlotsBooked))
                    .withCourtCentreDetails(nonNull(courtCentreDefaults) ? of(CourtCentreDetails.courtCentreDetails()
                            .withDefaultDuration(courtCentreDefaults.getDefaultDuration())
                            .withId(courtCentreDefaults.getCourtCentreId())
                            .withDefaultStartTime(courtCentreDefaults.getDefaultStartTime())
                            .build()) : empty());

            if (nonNull(startDate)) {
                builder.withStartDate(of(startDate.toLocalDate()));
                final Optional<LocalDate> hearingEndDate = Optional.of(HearingEndDateRule.apply(endDate, startDate.toLocalDate()));
                final List<uk.gov.justice.listing.events.NonDefaultDay> newNonDefaultDays = generateNewNonDefaultDays(estimateMinutes, startDate, hearingTypeDuration, nonDefaultDays, courtCentreId, courtRoomId);
                final LocalTime defaultStartTime = nonNull(courtCentreDefaults) ? courtCentreDefaults.getDefaultStartTime() : null;
                builder.withNonDefaultDays(newNonDefaultDays);

                final LocalDate enddDate = newNonDefaultDays.stream()
                        .sorted(Comparator.comparing(uk.gov.justice.listing.events.NonDefaultDay::getStartTime).reversed())
                        .map(nonDefaultDay -> nonDefaultDay.getStartTime().toLocalDate())
                        .filter(nonDefaultDay -> !nonDefaultDay.isBefore(startDate.toLocalDate()))
                        .findFirst().orElse(hearingEndDate.get());
                builder.withEndDate(enddDate);

                final long numOfDaysBetween = ChronoUnit.DAYS.between(startDate.toLocalDate(), enddDate);

                final List<LocalDate> allNonSittingDays = IntStream.iterate(0, i -> i + 1)
                        .limit(numOfDaysBetween)
                        .mapToObj(i -> startDate.toLocalDate().plusDays(i))
                        .filter(day -> newNonDefaultDays.stream().noneMatch(nonDefaultDay -> nonDefaultDay.getStartTime().toLocalDate().equals(day)))
                        .collect(Collectors.toList());

                builder.withNonSittingDays(allNonSittingDays);

                final List<uk.gov.justice.listing.events.HearingDay> hearingDaysCalculated = calculate(startDate.toLocalDate(), enddDate, allNonSittingDays, convertEventToDomain(newNonDefaultDays), defaultStartTime, hearingTypeDuration, defaultCourtCentre);
                builder.withHearingDays(hearingDaysCalculated);
            } else {
                builder.withHearingDays(emptyList());
                builder.withStartDate(empty());
                builder.withNonDefaultDays(emptyList());
                builder.withNonSittingDays(emptyList());
            }

            builder.withCourtApplications(courtApplications.stream()
                    .map(NewDomainToEventConverter::buildCourtApplications)
                    .collect((toList())))
                    .withWeekCommencingDurationInWeeks(weekCommencingDurationInWeeks)
                    .withWeekCommencingStartDate(weekCommencingStartDate)
                    .withWeekCommencingEndDate(weekCommencingEndDate)
                    .build();

            return apply(Stream.of(hearingListed()
                    .withHearing(builder.build())
                    .build()));
        } else {
            LOGGER.error("Cannot list hearing with id {} as it has already been listed", hearingId);
            return Stream.empty();
        }
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
        if (this.duplicate) {
            return Stream.empty();
        }

        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(courtCentreId).withRoomId(courtRoomId).build();

        final ZonedDateTime newStartDate = startDate != null ? startDate : ZonedDateTime.now();

        final LocalDate localStartDate = newStartDate.toLocalDate();

        final List<uk.gov.justice.listing.events.NonDefaultDay> newNonDefaultDays = calculateNewNonDefaultDaysForUnscheduled(
                hearingTypeDuration, newStartDate, courtCentreDefaults.getDefaultStartTime(), courtCentre);

        final CourtCentre defaultCourtCentre = CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withRoomId(ofNullable(courtRoomId)).build();


        return apply(Stream.of(hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type()
                                .withId(type.getId())
                                .withDescription(type.getDescription())
                                .withWelshDescription(type.getWelshDescription())
                                .build())
                        .withAllocated(false)
                        .withUnscheduled(of(true))
                        .withEstimatedMinutes(hearingTypeDuration)
                        .withTypeOfList(of(typeOfList))
                        .withAdjournedFromDate(empty())
                        .withJudiciary(judiciary.stream()
                                .map(NewDomainToEventConverter::buildJudicialRole)
                                .collect(toList()))
                        .withListedCases(listedCases.isEmpty() ? null : listedCases.stream()
                                .map(NewDomainToEventConverter::buildListedCase)
                                .collect(toList()))
                        .withListingDirections(ofNullable(listingDirections))
                        .withHearingLanguage(HearingLanguageRule.apply(listedCases, getHearingLanguageNeedsForAllApplicants(courtApplicationPartyListingNeeds)))
                        .withReportingRestrictionReason(ofNullable(reportingRestrictionReason))
                        .withCourtRoomId(defaultCourtCentre.getRoomId())
                        .withCourtCentreId(defaultCourtCentre.getId())
                        .withProsecutorDatesToAvoid(ofNullable(prosecutorDatesToAvoid))
                        .withJurisdictionType(valueFor(jurisdictionType.name()).orElse(null))
                        .withStartDate(ofNullable(startDate != null ? startDate.toLocalDate() : null))
                        .withEndDate(ofNullable(endDate))
                        .withNonDefaultDays(newNonDefaultDays)
                        .withNonSittingDays(emptyList())
                        .withHearingDays(calculate(localStartDate, endDate, emptyList(), convertEventToDomain(newNonDefaultDays), courtCentreDefaults.getDefaultStartTime(), hearingTypeDuration, defaultCourtCentre))
                        .withCourtApplications(courtApplications.stream()
                                .map(NewDomainToEventConverter::buildCourtApplications)
                                .collect((toList())))
                        .withWeekCommencingDurationInWeeks(weekCommencingDurationInWeeks)
                        .withWeekCommencingStartDate(weekCommencingStartDate)
                        .withWeekCommencingEndDate(weekCommencingEndDate)
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
                    .withCourtScheduleId(empty())
                    .withCourtCentreId(ofNullable(courtCentreId).map(UUID::toString).orElse(null))
                    .withRoomId(ofNullable(courtRoomId).map(UUID::toString).orElse(null))
                    .withDuration(of(estimateMinutes > 0 ? estimateMinutes : getAdjustedDuration(hearingTypeDuration)))
                    .withStartTime(startDate)
                    .build()));
        } else {
            nonDefaultDays.forEach(ndd ->
                    newList.add(uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay()
                            .withCourtScheduleId(ndd.getCourtScheduleId())
                            .withOucode(ndd.getOucode())
                            .withSession(ndd.getSession())
                            .withDuration(of(estimateMinutes > 0 ? ndd.getDuration().get() : getAdjustedDuration(hearingTypeDuration)))
                            .withCourtRoomId(ndd.getCourtRoomId())
                            .withStartTime(ndd.getStartTime())
                            .withRoomId(ndd.getRoomId())
                            .withCourtCentreId(ndd.getCourtCentreId())
                            .build())
            );

        }

        return newList;
    }

    private List<NonDefaultDay> convertEventToDomain(final List<uk.gov.justice.listing.events.NonDefaultDay> eventNonDefaults) {
        return eventNonDefaults.stream().map(this::getDomainNonDefaultDay).collect(toList());
    }

    private NonDefaultDay getDomainNonDefaultDay(final uk.gov.justice.listing.events.NonDefaultDay eventNonDefault) {
        final NonDefaultDay.Builder builder = NonDefaultDay.nonDefaultDay();
        if (nonNull(eventNonDefault.getStartTime())) {
            builder.withStartTime(eventNonDefault.getStartTime());
        }
        if (nonNull(eventNonDefault.getDuration())) {
            builder.withDuration(eventNonDefault.getDuration());
        }
        if (nonNull(eventNonDefault.getSession())) {
            builder.withSession(eventNonDefault.getSession());
        }
        if (nonNull(eventNonDefault.getOucode())) {
            builder.withOucode(eventNonDefault.getOucode());
        }
        if (nonNull(eventNonDefault.getCourtScheduleId())) {
            builder.withCourtScheduleId(eventNonDefault.getCourtScheduleId());
        }
        if (nonNull(eventNonDefault.getCourtRoomId())) {
            builder.withCourtRoomId(eventNonDefault.getCourtRoomId());
        }
        eventNonDefault.getCourtCentreId().map(Optional::of).ifPresent(builder::withCourtCentreId);
        eventNonDefault.getRoomId().map(Optional::of).ifPresent(builder::withRoomId);
        return builder.build();
    }

    public Stream<Object> changeJurisdictionType(final JurisdictionType jurisdictionType, final UUID hearingId) {
        if (this.duplicate) {
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
        if (this.duplicate) {
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
        if (this.duplicate) {
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

    public Stream<Object> changeType(final Type type, final UUID hearingId) {
        if (this.duplicate) {
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
        if (this.duplicate) {
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
        if (this.duplicate) {
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
        if (this.duplicate) {
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
        if (this.duplicate) {
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
        if (this.duplicate) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.hearingLanguage)) {
            LOGGER.error("HearingLanguage' for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
            return Stream.empty();
        }

        if (hasChanged(this.hearingLanguage, hearingLanguage)) {
            return apply(Stream.of(HearingLanguageChangedForHearing.hearingLanguageChangedForHearing()
                    .withHearingLanguage(uk.gov.justice.listing.events.HearingLanguage.valueOf(hearingLanguage.toString()))
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming hearingLanguage {} is the same as current hearingLanguage {} for hearing with id {} - Ignore", hearingLanguage, this.hearingLanguage, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> assignNonDefaultDays(final List<uk.gov.moj.cpp.listing.domain.NonDefaultDay> nonDefaultDays, final UUID hearingId) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.nonDefaultDays) || this.nonDefaultDays.isEmpty()) {
            return apply(Stream.of(NonDefaultDaysAssignedToHearing.nonDefaultDaysAssignedToHearing()
                    .withNonDefaultDays(convertNonDefaultDaysToEvents(nonDefaultDays))
                    .withHearingId(hearingId)
                    .build()));
        } else if (hasChanged(this.nonDefaultDays, nonDefaultDays)) {
            return apply(Stream.of(nonDefaultDaysChangedForHearing()
                    .withNonDefaultDays(convertNonDefaultDaysToEvents(nonDefaultDays))
                    .withHearingId(hearingId)
                    .build()
            ));
        } else {
            LOGGER.info("Incoming nonDefaultDays {} is the same as current nonDefaultDays {} for hearing with id {} - Ignore", nonDefaultDays, this.nonDefaultDays, hearingId);
            return Stream.empty();
        }
    }


    public Stream<Object> assignNonSittingDays(final List<LocalDate> nonSittingDays, final UUID hearingId) {
        if (this.duplicate) {
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
        if (this.duplicate) {
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
        if (this.duplicate) {
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
        if (this.duplicate) {
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

    public Stream<Object> assignCourtRoom(final UUID courtRoomId, final UUID hearingId) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.courtRoomId)) {
            final SequencesResetOnHearingDays sequencesResetOnHearingDaysEvent = createSequencesResetOnHearingDaysEvent(hearingId);
            final Stream<Object> appliedCourtRoomEvent = apply(Stream.of(CourtRoomAssignedToHearing.courtRoomAssignedToHearing()
                    .withCourtRoomId(courtRoomId)
                    .withHearingId(hearingId)
                    .build()));
            return concat(appliedCourtRoomEvent, apply(Stream.of(sequencesResetOnHearingDaysEvent)));
        } else if (hasChanged(this.courtRoomId, courtRoomId)) {
            final SequencesResetOnHearingDays sequencesResetOnHearingDaysEvent = createSequencesResetOnHearingDaysEvent(hearingId);
            final Stream<Object> appliedCourtRoomEvent = apply(Stream.of(courtRoomChangedForHearing()
                    .withCourtRoomId(courtRoomId)
                    .withHearingId(hearingId)
                    .build()));
            return concat(appliedCourtRoomEvent, apply(Stream.of(sequencesResetOnHearingDaysEvent)));
        } else {
            LOGGER.info("Incoming court room id {} is the same as current court room id {} for hearing with id {} - Ignore", courtRoomId, this.courtRoomId, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> removeCourtRoom(final UUID hearingId) {
        if (this.duplicate) {
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

    public Stream<Object> assignHearingDays(final LocalDate startDate, final LocalDate endDate, final List<LocalDate> nonSittingDays, final List<NonDefaultDay> nonDefaultDays, final LocalTime defaultStartTime, final Integer defaultDuration, final UUID hearingId, final CourtCentre defaultCourtCentre) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.hearingDays)) {
            LOGGER.error("HearingDays for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
            return Stream.empty();
        }

        final List<uk.gov.justice.listing.events.HearingDay> hearingDaysChangedForHearing
                = calculate(startDate, endDate, nonSittingDays, nonDefaultDays, defaultStartTime, defaultDuration, defaultCourtCentre);

        if (!this.hearingDays.isEmpty()) {
            final Map<ZonedDateTime, HearingDay> existingHearingDays = this.hearingDays.stream()
                    .collect(toMap(HearingDay::getStartTime, hd -> hd));
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


    public Stream<Object> applyAllocationRules(final Optional<UUID> bookingReference) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (canAllocate()) {
            return onAllocationEvents(bookingReference, emptyList());
        } else if (canUnallocate()) {
            return onUnallocationEvents();
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> applyAllocationRules(final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (canAllocate()) {
            return onAllocationEvents(Optional.empty(), prosecutionCaseDefendantOffenceIds);
        } else if (canUnallocate()) {
            return onUnallocationEvents();
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> updateDefendants(final UUID caseId, final List<uk.gov.moj.cpp.listing.domain.Defendant> defendants) {
        if (this.duplicate) {
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
        if (this.duplicate) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            final NewCaseMarkerUpdated newCaseMarkerUpdated = caseMarkerUpdateEvent(caseId, caseMarkers);
            return apply(Stream.of(newCaseMarkerUpdated));
        }
        return Stream.empty();
    }

    public Stream<Object> linkCaseToHearing(final String linkActionType, final UUID caseId, final String caseUrn, final List<LinkedToCases> linkedToCases) {
        if (this.duplicate) {
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
        if (this.duplicate) {
            return Stream.empty();
        }

        Stream<Object> eventsStream = Stream.of(trialVacated()
                .withHearingId(hearingId)
                .withVacatedTrialReasonId(vacatingTrialReasonId)
                .withAllocated(this.allocated)
                .build());

        if (this.allocated && MAGISTRATES == jurisdictionType) {
            eventsStream = concat(eventsStream, Stream.of(availableSlotsForHearingFreed()
                    .withHearingId(hearingId).build()));
        }

        return apply(eventsStream);
    }

    public Stream<Object> hearingVacateTrial(final Optional<UUID> vacatingTrialReasonId) {
        if (this.duplicate) {
            return Stream.empty();
        }

        Stream<Object> eventsStream = Stream.of(HearingTrialVacated.hearingTrialVacated()
                .withHearingId(this.hearingId)
                .withVacatedTrialReasonId(vacatingTrialReasonId)
                .build());

        if (MAGISTRATES == jurisdictionType && vacatingTrialReasonId.isPresent()) {
            eventsStream = concat(eventsStream, Stream.of(availableSlotsForHearingFreed().withHearingId(this.hearingId).build()));
        }
        return apply(eventsStream);
    }

    public Stream<Object> applyRescheduledCheck(final List<Object> occurredEventList) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (isReschedulingEvent(occurredEventList)) {
            return apply(Stream.of(HearingRescheduled.hearingRescheduled()
                    .withHearingId(this.hearingId)
                    .withAllocated(this.allocated)
                    .build()));
        }

        return Stream.empty();

    }

    public Stream<Object> updatedListedCasesInHearing(final uk.gov.justice.listing.events.Hearing allocatedHearing,
                                                      final uk.gov.justice.listing.events.Hearing unAllocatedHearing,
                                                      final List<uk.gov.justice.listing.events.ListedCase> casesToAllocate) {
        if (this.duplicate) {
            return Stream.empty();
        }
        allocatedHearing.getListedCases().addAll(casesToAllocate);
        LOGGER.info("Cases added to allocated hearing: {}", casesToAllocate);
        return apply(Stream.of(HearingListedCaseUpdated.hearingListedCaseUpdated()
                .withHearing(allocatedHearing)
                .withHearingIdToBeDeleted(of(unAllocatedHearing.getId()))
                .withUnAllocatedListedCases(casesToAllocate).build()));
    }


    public Stream<Object> addCasesToHearing(final List<ProsecutionCase> prosecutionCases, final List<UUID> shadowListedOffences, final Optional<UUID> seedingHearingId) {
        if (this.duplicate) {
            return Stream.empty();
        }
        return apply(Stream.of(CasesAddedToHearing.casesAddedToHearing()
                .withUnAllocatedListedCases(prosecutionCases.stream()
                        .map(prosecutionCase -> CourtToEventConverter.buildListedCase(prosecutionCase, shadowListedOffences))
                        .collect(Collectors.toList()))
                .withHearingId(hearingId)
                .withSeedingHearingId(seedingHearingId)
                .build()));

    }


    public Stream<Object> deleteUnAllocatedHearing(final UUID hearingIdToBeDeleted) {
        return apply(Stream.of(HearingDeleted.hearingDeleted()
                .withHearingIdToBeDeleted(hearingIdToBeDeleted)
                .build()));
    }

    public Stream<Object> updateUnallocatedHearingPartially(final UUID hearingToBeUpdated, final List<ProsecutionCases> caseList) {
        if (this.duplicate) {
            return Stream.empty();
        }
        return apply(Stream.of(HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(hearingToBeUpdated)
                .withProsecutionCases(caseList)
                .build()));
    }

    public Stream<Object> applyAllocationRulesForExtendedHearing(final uk.gov.justice.listing.events.Hearing unallocatedHearing) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (canAllocate() && allocated) {
            return apply(Stream.of(allocatedHearingExtendedForListingEvent(unallocatedHearing)));
        } else {
            LOGGER.info("Incoming hearing cannot allocated with new list cases");
        }
        return Stream.empty();
    }

    public Stream<Object> cancelHearingDays(final UUID hearingId, final List<uk.gov.justice.listing.events.HearingDay> hearingDays) {
        if (this.duplicate) {
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
                        .map(prosecutionCase -> CourtToEventConverter.buildListedCase(prosecutionCase, shadowListedOffences))
                        .collect(Collectors.toList()))
                .withHearingId(hearingId)
                .build()));

    }

    public Stream<Object> deleteHearing(final UUID seedingHearingId, final UUID hearingId) {
        final Optional<uk.gov.moj.cpp.listing.domain.OffenceIds> offenceIdsSeededByOtherHearings = prosecutionCaseDefendantOffenceIds.stream().flatMap(pc -> pc.getDefendants().stream())
                .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                .filter(offence -> isNotSeededOffenceBySeedId(seedingHearingId, offence))
                .findFirst();
        if (offenceIdsSeededByOtherHearings.isPresent()) {
            return getOffencesRemovedFromHearingStream(seedingHearingId, hearingId);
        } else {
            final List<UUID> caseIds = prosecutionCaseDefendants.keySet().stream().collect(toList());

            final Stream.Builder<Object> eventStreamBuilder = Stream.builder();

            if (allocated) {
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

        final List<UUID> offenceIds = prosecutionCaseDefendantOffenceIds
                .stream().flatMap(pc -> pc.getDefendants().stream())
                .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                .filter(offence -> isSeededOffenceBySeedId(seedingHearingId, offence))
                .map(OffenceIds::getId)
                .collect(toList());

        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();

        if (isNotEmpty(offenceIds)) {
            if (allocated) {
                eventStreamBuilder.add(OffencesRemovedFromExistingAllocatedHearing.offencesRemovedFromExistingAllocatedHearing()
                        .withHearingId(hearingId)
                        .withOffenceIds(offenceIds)
                        .build());
            } else {
                eventStreamBuilder.add(OffencesRemovedFromExistingUnallocatedHearing.offencesRemovedFromExistingUnallocatedHearing()
                        .withHearingId(hearingId)
                        .withOffenceIds(offenceIds)
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
        if (this.duplicate) {
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
        if (this.duplicate) {
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
        if (this.duplicate) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(offence -> thisHearingContainsDefendant(caseId, defendantId))
                    .map(offence -> offenceAddedEvent(caseId, defendantId, offence))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> sequenceHearingDays(final SequenceHearing sequenceHearing) {
        if (this.duplicate) {
            return Stream.empty();
        }

        if (notCurrentlyAssigned(this.hearingDays)) {
            LOGGER.error("HearingDays for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
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
            return concat(applyAllocationRules(ImmutableList.of()), apply(Stream.of(hearingDaysSequenced)));
        }
        LOGGER.info("Sequence not changed for hearing id {}", hearingId);
        return apply(Stream.of(hearingDaysSequenced));
    }

    public Stream<Object> addCourtApplication(final UUID hearingId, final CourtApplication courtApplication) {
        if (this.duplicate) {
            return Stream.empty();
        }
        return apply(Stream.of(CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(hearingId)
                .withCourtApplication(NewDomainToEventConverter.buildCourtApplications(courtApplication))
                .build()));
    }

    public Stream<Object> updateCourtApplication(final UUID hearingId, final CourtApplication courtApplication) {
        if (this.duplicate) {
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
        if (this.duplicate) {
            return Stream.empty();
        }

        if (!isHearingInThePast()) {
            final List<Object> events = defendants.stream()
                    .map(defendant -> defendantsAddedForCourtProceedings(caseId, defendant))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> updateDefendantCourtProceedingForHearing(final UUID hearingId, final ProsecutionCase prosecutionCase) {

        if (duplicate || isHearingInThePast()) {
            return Stream.empty();
        }

        final List<UUID> defendantIds = this.prosecutionCaseDefendants.get(prosecutionCase.getId());
        prosecutionCase.getDefendants().removeIf(defendant -> !defendantIds.contains(defendant.getId()));

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
        if (this.duplicate) {
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
                    .withCourtApplicationType(restrictCourtList.getCourtApplicationType())
                    .build()));
        }

        return Stream.empty();
    }

    public Stream<Object> updateDefendantLegalAidStatusForHearing(final UUID hearingId, final UUID caseId,
                                                                  final UUID defendantId, final String legalAidStatus) {
        if (this.duplicate || !isCaseContainsDefendant(caseId, defendantId)) {
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
        if (this.duplicate) {
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
        if (this.duplicate) {
            return Stream.empty();
        }

        if (nonNull(hearingIdForApplicationToBeEjected)) {
            return apply(Stream.of(ApplicationEjected.applicationEjected()
                    .withApplicationId(applicationId)
                    .withHearingId(hearingIdForApplicationToBeEjected)
                    .withRemovalReason(removalReason)
                    .build()
            ));
        }
        return Stream.empty();

    }

    public Stream<Object> removeStartDate(final UUID hearingId) {
        if (currentlyAssigned(this.startDate) && !this.duplicate) {
            return apply(Stream.of(startDateRemovedForHearing()
                    .withHearingId(hearingId)
                    .build()));

        } else {
            LOGGER.info("No start date is currently assigned for hearing with id {} so cannot be removed - Ignore", hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> removeWeekCommencingDates(final UUID hearingId) {
        if (this.duplicate) {
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
        if (this.duplicate) {
            return Stream.empty();
        }
        return Stream.of(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .withCaseIds(caseIds)
                .build());
    }

    public Stream<Object> markUnallocatedHearingAsDuplicate(final UUID hearingId) {
        if (duplicate || allocated) {
            return Stream.empty();
        }

        return Stream.of(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .build());
    }

    private boolean thisHearingContainsDefendant(final uk.gov.moj.cpp.listing.domain.Defendant defendant) {
        return isEmpty(this.prosecutionCaseDefendantOffenceIds) ? Boolean.FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId ->
                        prosecutionCaseDefendantOffenceId.getDefendants().stream()
                                .anyMatch(defendantOffenceId ->
                                        defendantOffenceId.getId().equals(defendant.getId())));
    }

    private boolean isCaseContainsDefendant(final UUID caseId, final UUID defendantId) {
        final List<UUID> defendantIds = this.prosecutionCaseDefendants.get(caseId);

        return CollectionUtils.isNotEmpty(defendantIds) && defendantIds.contains(defendantId);
    }

    private boolean thisHearingContainsDefendant(final UUID caseId, final UUID defendantId) {
        return isEmpty(this.prosecutionCaseDefendantOffenceIds) ? Boolean.FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId ->
                        prosecutionCaseDefendantOffenceId.getId().equals(caseId) && prosecutionCaseDefendantOffenceId.getDefendants().stream().anyMatch(defendantOffenceIds ->
                                defendantOffenceIds.getId().equals(defendantId))
                );
    }

    private boolean thisHearingContainsDefendantAndOffence(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return isEmpty(this.prosecutionCaseDefendantOffenceIds) ? Boolean.FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId ->
                        prosecutionCaseDefendantOffenceId.getId().equals(caseId) && prosecutionCaseDefendantOffenceId.getDefendants().stream().anyMatch(defendantOffenceIds ->
                                defendantOffenceIds.getId().equals(defendantId) && defendantOffenceIds.getOffences().stream().anyMatch(offence ->
                                        offence.getId().equals(offenceId)))
                );
    }

    private boolean isHearingInThePast() {
        return this.endDate != null && LocalDate.now().isAfter(this.endDate);
    }

    private Stream<Object> onAllocationEvents(final Optional<UUID> bookingReference, final List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds) {
        if (allocated) {
            return apply(Stream.of(allocatedHearingUpdatedForListingEvent()));
        }
        return apply(Stream.of(hearingAllocatedForListingEvent(bookingReference, prosecutionCaseDefendantOffenceIds)));
    }

    private Stream<Object> onUnallocationEvents() {
        final Stream<Object> appliedBusinessRuleEvents = apply(onUnallocationBusinessRules());
        final HearingUnallocatedForListing unallocateEvent = hearingUnallocatedForListingEvent();
        return concat(appliedBusinessRuleEvents, apply(Stream.of(unallocateEvent)));
    }

    private Stream<Object> onUnallocationBusinessRules() {
        // Currently no unallocated business rules to apply
        return Stream.empty();
    }

    private boolean canAllocate() {
        return currentlyAssigned(this.hearingLanguage) && currentlyAssigned(this.jurisdictionType)
                && currentlyAssigned(this.courtRoomId) && currentlyAssigned(this.endDate) && currentlyAssigned(this.startDate);
    }

    @SuppressWarnings({"squid:S1067"})
    private boolean canUnallocate() {
        return this.allocated && (notCurrentlyAssigned(this.hearingLanguage) || notCurrentlyAssigned(this.courtRoomId)
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

    private HearingAllocatedForListingV2 hearingAllocatedForListingEvent(final Optional<UUID> bookingReference, List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds) {

        if (isEmpty(prosecutionCaseDefendantOffenceIds)) {
            prosecutionCaseDefendantOffenceIds = this.prosecutionCaseDefendantOffenceIds;
        }
        return hearingAllocatedForListingV2()
                .withHearingId(this.hearingId)
                .withBookingId(Optional.of(bookingReference).get())
                .withType(buildHearingType())
                .withEstimatedMinutes(of(this.estimatedMinutes))
                .withCourtCentreId(this.courtCentreId)
                .withJudiciary(this.judiciary.stream()
                        .map(this::buildJudicialRole)
                        .collect(toList()))
                .withHearingLanguage(uk.gov.justice.listing.events.HearingLanguage.valueFor(this.hearingLanguage.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withJurisdictionType(valueFor(this.jurisdictionType.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withReportingRestrictionReason(ofNullable(this.reportingRestrictionReason))
                .withCourtRoomId(this.courtRoomId)
                .withHearingDays(convertHearingDaysToEvent(this.hearingDays))
                .withProsecutionCaseDefendantsOffenceIds(isEmpty(prosecutionCaseDefendantOffenceIds) ? null : prosecutionCaseDefendantOffenceIds.stream()
                        .map(lc -> uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(lc.getId())
                                .withDefendants(lc.getDefendants().stream()
                                        .map(this::buildEventDefendantOffenceIdsV2)
                                        .collect(toList()))
                                .build()
                        ).collect(toList()))
                .withCourtApplicationIds(this.confirmedCourtApplicationIds.isEmpty() ? null : this.confirmedCourtApplicationIds)
                .withUpdateSlot(of(this.updateSlot))
                .withHasAdjournmentDate(Optional.of(this.hasAdjournmentDate))
                .build();
    }


    private uk.gov.justice.listing.events.Type buildHearingType() {
        return uk.gov.justice.listing.events.Type.type()
                .withId(this.type.getId())
                .withDescription(this.type.getDescription())
                .withWelshDescription(this.type.getWelshDescription())
                .build();
    }

    private AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListingEvent() {

        return allocatedHearingUpdatedForListingV2()
                .withHearingId(this.hearingId)
                .withType(buildHearingType())
                .withEstimatedMinutes(of(this.estimatedMinutes))
                .withCourtCentreId(this.courtCentreId)
                .withJudiciary(this.judiciary.stream()
                        .map(this::buildJudicialRole)
                        .collect(toList()))
                .withHearingLanguage(uk.gov.justice.listing.events.HearingLanguage.valueFor(this.hearingLanguage.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withJurisdictionType(valueFor(this.jurisdictionType.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withReportingRestrictionReason(ofNullable(this.reportingRestrictionReason))
                .withCourtRoomId(this.courtRoomId)
                .withHearingDays(convertHearingDaysToEvent(this.hearingDays))
                .withProsecutionCaseDefendantsOffenceIds(isEmpty(this.prosecutionCaseDefendantOffenceIds) ? null : this.prosecutionCaseDefendantOffenceIds.stream()
                        .map(lc -> uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(lc.getId())
                                .withDefendants(lc.getDefendants().stream()
                                        .map(this::buildEventDefendantOffenceIdsV2)
                                        .collect(toList()))
                                .build()
                        ).collect(toList()))
                .withCourtApplicationIds(this.confirmedCourtApplicationIds.isEmpty() ? null : this.confirmedCourtApplicationIds)
                .withUpdateSlot(of(this.updateSlot))
                .build();
    }


    private HearingUnallocatedForListing hearingUnallocatedForListingEvent() {
        if (nonNull(prosecutionCaseDefendantOffenceIds)) {
            final Optional<OffenceIds> offenceIds = prosecutionCaseDefendantOffenceIds.stream()
                    .flatMap(pc -> pc.getDefendants().stream())
                    .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                    .filter(o -> nonNull(o.getSeedingHearing()))
                    .findFirst();
            if (offenceIds.isPresent()) {
                return hearingUnallocatedForListing()
                        .withHearingId(this.hearingId)
                        .withSeededHearing(of(true))
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
        // Do nothing
    }

    @SuppressWarnings({"squid:S1172"})
    private void onOffenceUpdated(final OffenceUpdated offenceUpdated) {
        // Do nothing
    }

    @SuppressWarnings({"squid:S1172"})
    private void onCourtApplicationUpdatedForHearing(final CourtApplicationUpdatedForHearing courtApplicationUpdatedForHearing) {
        // Do nothing
    }

    private void onOffenceAdded(final OffenceAdded offenceAdded) {
        final UUID caseId = offenceAdded.getCaseId();
        final Offence offence = offenceAdded.getOffence();
        final UUID defendantId = offenceAdded.getDefendantId();

        final Optional<DefendantOffenceIds> defendantOffences = getDefendantOffenceIds(caseId, defendantId);

        defendantOffences.ifPresent(defendantOffenceIds -> defendantOffenceIds.getOffences().add(EventToDomainConverter.buildOffenceIds(offence)));
    }

    private void onOffenceDeleted(final OffenceDeleted offenceDeleted) {
        final UUID caseId = offenceDeleted.getCaseId();
        final UUID offenceId = offenceDeleted.getOffenceId();
        final UUID defendantId = offenceDeleted.getDefendantId();

        final Optional<DefendantOffenceIds> defendantOffences = getDefendantOffenceIds(caseId, defendantId);

        defendantOffences.ifPresent(defendantOffenceIds -> defendantOffenceIds.getOffences().removeIf(d -> d.getId().equals(offenceId)));
    }


    @SuppressWarnings({"squid:S3655"})
    private Optional<DefendantOffenceIds> getDefendantOffenceIds(final UUID caseId, final UUID defendantId) {
        if (isEmpty(this.prosecutionCaseDefendantOffenceIds)) {
            return Optional.empty();
        }
        final Optional<ProsecutionCaseDefendantOffenceIds> caseDefendants = this.prosecutionCaseDefendantOffenceIds.stream()
                .filter(prosecutionCaseDefendantOffenceId -> prosecutionCaseDefendantOffenceId.getId().equals(caseId))
                .findFirst();

        return caseDefendants.map(pcDefendantOffenceIds ->
                pcDefendantOffenceIds.getDefendants().stream()
                        .filter(defendantOffenceIds -> defendantOffenceIds.getId().equals(defendantId))
                        .findFirst()
        ).get();
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
        this.startDate = hearing.getStartDate().isPresent() ? hearing.getStartDate().get() : null;
        this.endDate = hearing.getEndDate().isPresent() ? hearing.getEndDate().get() : null;
        this.estimatedMinutes = hearing.getEstimatedMinutes();
        this.nonSittingDays = hearing.getNonSittingDays();

        if (hearing.getJudiciary() != null) {
            this.judiciary = hearing.getJudiciary().stream().map(jr -> JudicialRole.judicialRole()
                    .withJudicialRoleType(uk.gov.moj.cpp.listing.domain.JudicialRoleType.judicialRoleType()
                            .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                            .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                            .build())
                    .withJudicialId(jr.getJudicialId())
                    .withIsDeputy(jr.getIsDeputy())
                    .withIsBenchChairman(jr.getIsBenchChairman())
                    .build())
                    .filter(Objects::nonNull)
                    .collect(toList());
        }

        this.hearingLanguage = HearingLanguage.valueFor(hearing.getHearingLanguage().toString())
                .orElseThrow(IllegalArgumentException::new);
        this.courtRoomId = hearing.getCourtRoomId().orElse(null);
        this.courtCentreId = hearing.getCourtCentreId();
        if (hearing.getNonDefaultDays() != null) {
            this.nonDefaultDays = hearing.getNonDefaultDays().stream()
                    .map(ndd -> NonDefaultDay.nonDefaultDay().withCourtScheduleId(ndd.getCourtScheduleId())
                            .withStartTime(ndd.getStartTime())
                            .withDuration(ndd.getDuration())
                            .build())
                    .collect(toList());
        }
        this.reportingRestrictionReason = hearing.getReportingRestrictionReason().orElse(null);
        this.jurisdictionType = JurisdictionType.valueFor(hearing.getJurisdictionType().name()).orElse(null);
        // Standalone CourtApplication will not have any associated case
        if (nonNull(hearing.getListedCases())) {
            this.prosecutionCaseDefendantOffenceIds = hearing.getListedCases().stream()
                    .map(lc -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(lc.getId())
                            .withDefendants(lc.getDefendants().stream()
                                    .map(this::buildDomainDefendantOffenceIds)
                                    .collect(toList()))
                            .build()
                    ).collect(toList());
            hearing.getListedCases().forEach(
                    listedCase -> prosecutionCaseDefendants.put(listedCase.getId(), listedCase.getDefendants().stream()
                            .map(uk.gov.justice.listing.events.Defendant::getId)
                            .collect(toList())));
        }
        this.hearingDays = convertHearingDaysToDomain(hearing.getHearingDays());
        this.allocated = Boolean.FALSE;

        if (hearing.getCourtApplications() != null) {
            this.confirmedCourtApplicationIds = hearing.getCourtApplications().stream()
                    .map(uk.gov.justice.listing.events.CourtApplication::getId).collect(toList());
        }

        this.hasAdjournmentDate = hearing.getAdjournedFromDate().isPresent();

        this.weekCommencingStartDate = hearing.getWeekCommencingStartDate().orElse(null);
        this.weekCommencingEndDate = hearing.getWeekCommencingEndDate().orElse(null);
        this.weekCommencingDurationInWeeks = hearing.getWeekCommencingDurationInWeeks().orElse(null);

    }

    private void onAddedCasesForHearing(AddedCasesForHearing event) {
        if (nonNull(event.getUnAllocatedListedCases())) {
            event.getUnAllocatedListedCases().forEach(
                    listedCase -> prosecutionCaseDefendants.put(listedCase.getId(), listedCase.getDefendants().stream()
                            .map(uk.gov.justice.listing.events.Defendant::getId)
                            .collect(toList())));
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

    private SequencesResetOnHearingDays createSequencesResetOnHearingDaysEvent(final UUID hearingId) {
        return SequencesResetOnHearingDays.sequencesResetOnHearingDays()
                .withHearingId(hearingId)
                .build();
    }

    private void onTypeChangedForHearing(final TypeChangedForHearing event) {
        this.type = Type.type().withId(event.getType().getId()).withDescription(event.getType().getDescription()).withWelshDescription(event.getType().getWelshDescription()).build();
    }

    private void onStartDateChangedForHearing(final StartDateChangedForHearing event) {
        this.startDate = LocalDate.parse(event.getStartDate());
    }

    private void onEndDateChangedForHearing(final EndDateChangedForHearing event) {
        this.endDate = LocalDate.parse(event.getEndDate());
    }

    private void onWeekCommencingDateChangedForHearing(final WeekCommencingDateChangedForHearing event) {
        this.weekCommencingStartDate = LocalDate.parse(event.getWeekCommencingStartDate());
        this.weekCommencingEndDate = LocalDate.parse(event.getWeekCommencingEndDate());
        this.weekCommencingDurationInWeeks = event.getWeekCommencingDurationInWeeks();
    }

    private void onNonSittingDaysChangedForHearing(final NonSittingDaysChangedForHearing event) {
        this.nonSittingDays = event.getNonSittingDays();
    }

    private void onNonSittingDaysAssignedToHearing(final NonSittingDaysAssignedToHearing event) {
        this.nonSittingDays = event.getNonSittingDays();
    }

    private void onNonDefaultDaysAssignedToHearing(final NonDefaultDaysAssignedToHearing event) {
        this.nonDefaultDays = convertNonDefaultDaysToDomain(event.getNonDefaultDays());
        this.updateSlot = event.getNonDefaultDays().stream().anyMatch(ndd -> ndd.getCourtScheduleId().isPresent());
    }

    private void onHearingLanguageChanged(final HearingLanguageChangedForHearing event) {
        this.hearingLanguage = HearingLanguage.valueFor(event.getHearingLanguage().toString())
                .orElseThrow(IllegalArgumentException::new);
    }

    private boolean hasSequenceChanged(final Map<LocalDate, Integer> hearingDaysSequencesMap) {
        return this.hearingDays.stream().anyMatch(d -> ((!hearingDaysSequencesMap.containsKey(d.getHearingDate()))
                || (hearingDaysSequencesMap.containsKey(d.getHearingDate()) && !(hearingDaysSequencesMap.get(d.getHearingDate()).equals(d.getSequence())))));
    }

    private void onNonDefaultDaysChangedForHearing(final NonDefaultDaysChangedForHearing event) {
        this.nonDefaultDays = convertNonDefaultDaysToDomain(event.getNonDefaultDays());
        this.updateSlot = event.getNonDefaultDays().stream().anyMatch(ndd -> ndd.getCourtScheduleId().isPresent());
    }

    private void onHearingDaysChangedForHearing(final HearingDaysChangedForHearing hearingDaysChangedForHearing) {
        this.hearingDays = convertHearingDaysToDomain(hearingDaysChangedForHearing.getHearingDays());
    }

    private void onHearingDaysCancelledForHearing(final HearingDaysCancelled hearingDaysCancelled) {
        this.hearingDays = convertHearingDaysToDomain(hearingDaysCancelled.getHearingDays());
    }

    private void onJudiciaryAssignedToHearing(final JudiciaryAssignedToHearing event) {
        this.judiciary = convertToDomain(event.getJudiciary());
    }

    private void onJurisdictionChangedForHearing(final JurisdictionChangedForHearing event) {
        this.jurisdictionType = JurisdictionType.valueFor(event.getJurisdictionType().toString())
                .orElseThrow(IllegalArgumentException::new);
    }

    private void onJudiciaryChangedForHearing(final JudiciaryChangedForHearing event) {
        this.judiciary = convertToDomain(event.getJudiciary());
    }

    @SuppressWarnings({"squid:S1172"})
    private void onJudiciaryRemovedFromHearing(final JudiciaryRemovedFromHearing event) {
        this.judiciary = null;
    }

    private void onCourtRoomAssignedToHearing(final CourtRoomAssignedToHearing event) {
        this.courtRoomId = event.getCourtRoomId();
    }

    private void onCourtCentreChangedForHearing(final CourtCentreChangedForHearing event) {
        this.courtCentreId = event.getCourtCentreId();
    }

    private void onCourtRoomChangedForHearing(final CourtRoomChangedForHearing event) {
        this.courtRoomId = event.getCourtRoomId();
    }

    private void onVideoLinkChangedForHearing(final VideoLinkDetailsChangedForHearing event) {
        this.publicListNote = event.getVideoLinkDetails().orElse(null);
        this.hasVideoLink = event.getHasVideoLink();
    }

    private void onVideoLinkAssignedForHearing(final VideoLinkDetailsAssignedForHearing event) {
        this.publicListNote = event.getVideoLinkDetails().orElse(null);
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
        this.courtRoomId = null;
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
        }
    }

    @SuppressWarnings({"squid:S1172"})
    private void onEndDateRemovedFromHearing(final EndDateRemovedFromHearing event) {
        this.endDate = null;
    }

    private void onStartDateRemovedForHearing(final StartDateRemovedForHearing event) {
        this.startDate = null;
    }

    private void onWeekCommencingDateRemovedForHearing(final WeekCommencingDateRemovedForHearing event) {
        this.weekCommencingStartDate = null;
        this.weekCommencingEndDate = null;
        this.weekCommencingDurationInWeeks = null;
    }

    private void onHearingDaysSequenced(final HearingDaysSequenced hearingDaysSequenced) {
        this.hearingDays = convertHearingDaysToDomain(hearingDaysSequenced.getHearingDays());
    }

    @SuppressWarnings({"squid:S1172"})
    private void onHearingAllocatedForListing(final HearingAllocatedForListing event) {
        this.allocated = Boolean.TRUE;

        if (isNotEmpty(event.getProsecutionCaseDefendantsOffenceIds())) {
            this.prosecutionCaseDefendantOffenceIds = event.getProsecutionCaseDefendantsOffenceIds().stream()
                    .map(lc -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(lc.getId())
                            .withDefendants(lc.getDefendants().stream()
                                    .map(d -> DefendantOffenceIds.defendantOffenceIds()
                                            .withId(d.getId())
                                            .withOffences(nonNull(d.getOffenceIds()) ?
                                                    d.getOffenceIds().stream().map(offence -> OffenceIds.offenceIds()
                                                            .withId(offence)
                                                            .build()).collect(toList()) : null)
                                            .build())
                                    .collect(toList()))
                            .build()
                    ).collect(toList());

            this.prosecutionCaseDefendants.clear();
            event.getProsecutionCaseDefendantsOffenceIds().forEach(
                    prosecutionCase -> this.prosecutionCaseDefendants.put(prosecutionCase.getId(), prosecutionCase.getDefendants().stream()
                            .map(uk.gov.justice.listing.events.DefendantOffenceIds::getId)
                            .collect(toList())));
        } else {
            this.prosecutionCaseDefendantOffenceIds = emptyList();
        }
    }

    @SuppressWarnings({"squid:S1172"})
    private void onHearingAllocatedForListingV2(final HearingAllocatedForListingV2 event) {
        this.allocated = Boolean.TRUE;

        if (isNotEmpty(event.getProsecutionCaseDefendantsOffenceIds())) {
            final Map<UUID, SeedingHearing> seedingHearingMap = getOffencesSeedingHearingMap();

            this.prosecutionCaseDefendantOffenceIds = event.getProsecutionCaseDefendantsOffenceIds().stream()
                    .map(lc -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(lc.getId())
                            .withDefendants(lc.getDefendants().stream()
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

    @SuppressWarnings({"squid:S1172"})
    private void onHearingUnallocatedForListing(final HearingUnallocatedForListing event) {
        this.allocated = Boolean.FALSE;
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

    }

    private void addCaseToProsecutionCaseDefendantsOffenceIds(final uk.gov.justice.listing.events.ListedCase listedCase) {
        prosecutionCaseDefendantOffenceIds.add(ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                .withId(listedCase.getId())
                .withDefendants(listedCase.getDefendants().stream()
                        .map(this::buildDomainDefendantOffenceIds)
                        .collect(toList()))
                .build());
    }

    private void addDefendantToProsecutionCaseDefendantsOffenceIds(final Optional<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceId, final uk.gov.justice.listing.events.Defendant defendant) {
        final Optional<DefendantOffenceIds> defendantOffenceId = prosecutionCaseDefendantOffenceId.get().getDefendants().stream()
                .filter(defendantOffenceIds -> defendantOffenceIds.getId().equals(defendant.getId()))
                .findFirst();
        if (defendantOffenceId.isPresent()) {
            defendant.getOffences().forEach(offence ->
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
        this.allocated = Boolean.FALSE;
        removeSeededOffences(event);
    }

    /**
     * @param event if previous hearings that contains offence id seeded by
     *                                   another hearing, delete all offences match the seedId and
     *                                   also delete case which all offences seeded by
     *                                   seedingHearingId but not delete all hearing
     */
    private void removeSeededOffences(final OffencesRemovedFromHearing event) {
        prosecutionCaseDefendantOffenceIds.removeIf(pc -> event.getCaseIdsSeededByOnlySeedingHearingId().contains(pc.getId()));

        prosecutionCaseDefendantOffenceIds.forEach(pc -> pc.getDefendants()
                .forEach(defendantOffenceIds -> defendantOffenceIds.getOffences().removeIf(o -> nonNull(o.getSeedingHearing()) &&
                        event.getSeedingHearingId().equals(o.getSeedingHearing().getSeedingHearingId()))));

        prosecutionCaseDefendantOffenceIds.removeIf(pc -> pc.getDefendants().isEmpty());

        prosecutionCaseDefendants.clear();
        prosecutionCaseDefendantOffenceIds.forEach(pc -> prosecutionCaseDefendants.put(pc.getId(), pc.getDefendants().stream().map(DefendantOffenceIds::getId).collect(toList())));
    }


    private List<uk.gov.justice.listing.events.NonDefaultDay> convertNonDefaultDaysToEvents(final List<uk.gov.moj.cpp.listing.domain.NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay()
                        .withStartTime(ndd.getStartTime())
                        .withDuration(ndd.getDuration())
                        .withSession(ndd.getSession())
                        .withOucode(ndd.getOucode())
                        .withCourtScheduleId(ndd.getCourtScheduleId())
                        .withCourtRoomId(ndd.getCourtRoomId())
                        .withRoomId(ndd.getRoomId())
                        .withCourtCentreId(ndd.getCourtCentreId())
                        .build())
                .collect(toList());
    }


    private List<NonDefaultDay> convertNonDefaultDaysToDomain(final List<uk.gov.justice.listing.events.NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> NonDefaultDay.nonDefaultDay()
                        .withStartTime(ndd.getStartTime())
                        .withDuration(ndd.getDuration())
                        .withSession(ndd.getSession())
                        .withOucode(ndd.getOucode())
                        .withCourtScheduleId(ndd.getCourtScheduleId())
                        .withCourtRoomId(ndd.getCourtRoomId())
                        .withRoomId(ndd.getRoomId())
                        .withCourtCentreId(ndd.getCourtCentreId())
                        .build())
                .collect(toList());
    }

    private List<HearingDay> convertHearingDaysToDomain(final List<uk.gov.justice.listing.events.HearingDay> hearingDays) {
        if (hearingDays.isEmpty()) {
            return emptyList();
        }
        return hearingDays.stream()
                .map(cd -> HearingDay.hearingDay()
                        .withCourtScheduleId(cd.getCourtScheduleId().orElse(null))
                        .withDurationMinutes(cd.getDurationMinutes())
                        .withEndTime(cd.getEndTime())
                        .withSequence(cd.getSequence())
                        .withStartTime(cd.getStartTime())
                        .withHearingDate(cd.getHearingDate())
                        .withIsCancelled(cd.getIsCancelled().orElse(null))
                        .withCourtCentreId(cd.getCourtCentreId().orElse(null))
                        .withCourtRoomId(cd.getCourtRoomId().orElse(null))
                        .build())
                .collect(toList());
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
                                .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                                .build())
                        .withJudicialId(jr.getJudicialId())
                        .withIsDeputy(jr.getIsDeputy())
                        .withIsBenchChairman(jr.getIsBenchChairman())
                        .withUserId(jr.getUserId().orElse(null))
                        .build())
                .collect(toList());
    }

    private uk.gov.justice.listing.events.JudicialRole buildJudicialRole(final JudicialRole jr) {
        return uk.gov.justice.listing.events.JudicialRole.judicialRole()
                .withIsBenchChairman(jr.getIsBenchChairman())
                .withIsDeputy(jr.getIsDeputy())
                .withJudicialId(jr.getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId())
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
     *
     * Update hearing for listing command's payload does not have seedingHearing information in the offence level.
     * Offences SeedingHearing map  pulls out from prosecutionCaseDefendantOffenceIds  which is stored on HearingListed.
     *
     *
     * */
    private uk.gov.justice.listing.events.DefendantOffenceIdsV2 buildEventDefendantOffenceIdsV2(final uk.gov.moj.cpp.listing.domain.DefendantOffenceIds d) {
        final Map<UUID, SeedingHearing> seedingHearingMap = getOffencesSeedingHearingMap();
        return uk.gov.justice.listing.events.DefendantOffenceIdsV2.defendantOffenceIdsV2()
                .withId(d.getId())
                .withOffenceIds(d.getOffences().stream().map(offence -> uk.gov.justice.listing.events.OffenceIds.offenceIds()
                        .withId(offence.getId())
                        .withSeedingHearing(seedingHearingMap.containsKey(offence.getId()) ? NewDomainToEventConverter.buildSeedingHearing(seedingHearingMap.get(offence.getId())) : empty())
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

        final Optional<UUID> centreId = correctedHearingDays.get(0).getCourtCentreId();
        final Optional<UUID> roomId = correctedHearingDays.get(0).getCourtRoomId();

        correctHearingDaysWithoutCourtCentre(centreId, roomId);

        if (isNotEmpty(this.nonDefaultDays)) {
            correctNonDefaultDaysWithoutCourtCentre(centreId, roomId);
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
        // do nothing
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

    private void onHearingListedCaseUpdated(final HearingListedCaseUpdated event) {
        LOGGER.info("onHearingListedCaseUpdated() event:{}", event);
        final uk.gov.justice.listing.events.Hearing hearing = event.getHearing();

        this.hearingId = hearing.getId();
        this.type = Type.type()
                .withId(hearing.getType().getId())
                .withDescription(hearing.getType().getDescription())
                .withWelshDescription(hearing.getType().getDescription())
                .build();
        this.startDate = hearing.getStartDate().isPresent() ? hearing.getStartDate().get() : null;
        this.endDate = hearing.getEndDate().isPresent() ? hearing.getEndDate().get() : null;
        this.estimatedMinutes = hearing.getEstimatedMinutes();
        this.nonSittingDays = hearing.getNonSittingDays();

        if (hearing.getJudiciary() != null) {
            this.judiciary = hearing.getJudiciary().stream().map(jr -> JudicialRole.judicialRole()
                    .withJudicialRoleType(uk.gov.moj.cpp.listing.domain.JudicialRoleType.judicialRoleType()
                            .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                            .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                            .build())
                    .withJudicialId(jr.getJudicialId())
                    .withIsDeputy(jr.getIsDeputy())
                    .withIsBenchChairman(jr.getIsBenchChairman())
                    .build())
                    .filter(Objects::nonNull)
                    .collect(toList());
        }

        this.hearingLanguage = HearingLanguage.valueFor(hearing.getHearingLanguage().toString())
                .orElseThrow(IllegalArgumentException::new);
        this.courtRoomId = hearing.getCourtRoomId().orElse(null);
        this.courtCentreId = hearing.getCourtCentreId();
        if (hearing.getNonDefaultDays() != null) {
            this.nonDefaultDays = hearing.getNonDefaultDays().stream()
                    .map(ndd -> NonDefaultDay.nonDefaultDay()
                            .withStartTime(ndd.getStartTime())
                            .withDuration(ndd.getDuration())
                            .build())
                    .collect(toList());
        }
        this.reportingRestrictionReason = hearing.getReportingRestrictionReason().orElse(null);
        this.jurisdictionType = JurisdictionType.valueFor(hearing.getJurisdictionType().name()).orElse(null);
        // Standalone CourtApplication will not have any associated case

        if (nonNull(hearing.getListedCases())) {
            this.prosecutionCaseDefendantOffenceIds = event.getUnAllocatedListedCases().stream()
                    .map(lc -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(lc.getId())
                            .withDefendants(lc.getDefendants().stream()
                                    .map(this::buildDomainDefendantOffenceIds)
                                    .collect(toList()))
                            .build()
                    ).collect(toList());
        }
        this.hearingDays = convertHearingDaysToDomain(hearing.getHearingDays());
        this.allocated = TRUE;

        if (hearing.getCourtApplications() != null) {
            this.confirmedCourtApplicationIds = hearing.getCourtApplications().stream()
                    .map(uk.gov.justice.listing.events.CourtApplication::getId).collect(toList());
        }
        if (!event.getUnAllocatedListedCases().isEmpty()) {
            unAllocatedListedCases.addAll(event.getUnAllocatedListedCases().stream().map(EventAggregateConverter::buildAggregateListedCase).collect(toList()));
        }

    }

    private AllocatedHearingExtendedForListing allocatedHearingExtendedForListingEvent(final uk.gov.justice.listing.events.Hearing unallocatedHearing) {

        return allocatedHearingExtendedForListing()
                .withHearingId(this.hearingId)
                .withType(buildHearingType())
                .withEstimatedMinutes(of(this.estimatedMinutes))
                .withCourtCentreId(this.courtCentreId)
                .withJudiciary(this.judiciary.stream()
                        .map(this::buildJudicialRole)
                        .collect(toList()))
                .withHearingLanguage(uk.gov.justice.listing.events.HearingLanguage.valueFor(this.hearingLanguage.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withJurisdictionType(valueFor(this.jurisdictionType.toString())
                        .orElseThrow(IllegalArgumentException::new))
                .withReportingRestrictionReason(ofNullable(this.reportingRestrictionReason))
                .withCourtRoomId(this.courtRoomId)
                .withHearingDays(convertHearingDaysToEvent(this.hearingDays))
                .withProsecutionCaseDefendantsOffenceIds(isEmpty(this.prosecutionCaseDefendantOffenceIds) ? null : this.prosecutionCaseDefendantOffenceIds.stream()
                        .map(lc -> uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                                .withId(lc.getId())
                                .withDefendants(lc.getDefendants().stream()
                                        .map(this::buildEventDefendantOffenceIds)
                                        .collect(toList()))
                                .build()
                        ).collect(toList()))
                .withCourtApplicationIds(this.confirmedCourtApplicationIds.isEmpty() ? null : this.confirmedCourtApplicationIds)
                .withUnAllocatedListedCases(this.unAllocatedListedCases.isEmpty() ? null : this.unAllocatedListedCases.stream().map(EventAggregateConverter::buildEventListedCase).collect(toList()))
                .withExistingHearingId(unallocatedHearing.getId())
                .build();
    }

    public Stream<Object> addCasesForHearing(final List<ProsecutionCase> prosecutionCases, final List<UUID> shadowListedOffences) {
        if (this.duplicate) {
            return Stream.empty();
        }
        return apply(Stream.of(AddedCasesForHearing.addedCasesForHearing()
                .withUnAllocatedListedCases(prosecutionCases.stream()
                        .map(prosecutionCase -> CourtToEventConverter.buildListedCase(prosecutionCase, shadowListedOffences))
                        .collect(Collectors.toList()))
                .withHearingId(hearingId)
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

    private  ZonedDateTime getEarliestHearingStartDate() {
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
        final List<UUID> offencesSeededBySeedingHearing = prosecutionCaseDefendantOffenceIds.stream()
                .flatMap(pc -> pc.getDefendants().stream())
                .flatMap(defendantOffenceIds -> defendantOffenceIds.getOffences().stream())
                .filter(offenceIds -> nonNull(offenceIds.getSeedingHearing()) && offenceIds.getSeedingHearing().getSeedingHearingId().equals(seedingHearingId))
                .map(OffenceIds::getId)
                .collect(toList());

        final List<UUID> caseIdsSeededByOnlyOtherHearings = prosecutionCaseDefendantOffenceIds.stream()
                .filter(pc -> pc.getDefendants().stream()
                        .flatMap(d -> d.getOffences().stream())
                        .anyMatch(offence -> isNotSeededOffenceBySeedId(seedingHearingId, offence)))
                .map(ProsecutionCaseDefendantOffenceIds::getId)
                .collect(toList());

        final List<UUID> caseIdsSeededBySeedingHearingId = prosecutionCaseDefendantOffenceIds.stream()
                .filter(pc -> !caseIdsSeededByOnlyOtherHearings.contains(pc.getId()))
                .map(ProsecutionCaseDefendantOffenceIds::getId)
                .collect(toList());

        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();

        eventStreamBuilder.add(OffencesRemovedFromHearing.offencesRemovedFromHearing()
                .withHearingId(hearingId)
                .withSeedingHearingId(seedingHearingId)
                .withCaseIdsSeededByOnlySeedingHearingId(caseIdsSeededBySeedingHearingId)
                .withUnallocated(allocated ? of(true) : empty())
                .withSeededOffences(offencesSeededBySeedingHearing)
                .build());

        if (allocated && MAGISTRATES == jurisdictionType) {
            eventStreamBuilder.add(availableSlotsForHearingFreed()
                    .withHearingId(hearingId).build());
        }

        return apply(eventStreamBuilder.build());
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
                .build());
    }

    private List<HearingDay> getAggregateHearingDaysAsCancelledFromCommand(final List<uk.gov.justice.listing.events.HearingDay> hearingDaysFromCommand) {
        final List<HearingDay> updatedHearingDaysWithAggregateState = newArrayList();

        this.hearingDays.forEach(hearingDayFromAggregate -> {
            final Optional<uk.gov.justice.listing.events.HearingDay> matchedOptionalHearingDayInCommand = hearingDaysFromCommand.stream().filter(matchingHearingDay(hearingDayFromAggregate)).findFirst();
            final boolean cancelled = matchedOptionalHearingDayInCommand
                    .map(matchedHearingDayInCommand -> matchedHearingDayInCommand.getIsCancelled().orElse(false))
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

}
