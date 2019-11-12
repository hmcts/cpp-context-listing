package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing.allocatedHearingUpdatedForListing;
import static uk.gov.justice.listing.events.CourtRoomChangedForHearing.courtRoomChangedForHearing;
import static uk.gov.justice.listing.events.EndDateChangedForHearing.endDateChangedForHearing;
import static uk.gov.justice.listing.events.EndDateRemovedFromHearing.endDateRemovedFromHearing;
import static uk.gov.justice.listing.events.HearingAllocatedForListing.hearingAllocatedForListing;
import static uk.gov.justice.listing.events.HearingDaysChangedForHearing.hearingDaysChangedForHearing;
import static uk.gov.justice.listing.events.HearingListed.hearingListed;
import static uk.gov.justice.listing.events.HearingUnallocatedForListing.hearingUnallocatedForListing;
import static uk.gov.justice.listing.events.JurisdictionType.valueFor;
import static uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing.nonSittingDaysAssignedToHearing;
import static uk.gov.justice.listing.events.NonSittingDaysChangedForHearing.nonSittingDaysChangedForHearing;
import static uk.gov.justice.listing.events.StartDateChangedForHearing.startDateChangedForHearing;
import static uk.gov.justice.listing.events.TypeChangedForHearing.typeChangedForHearing;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtApplicationUpdatedForHearing;
import uk.gov.justice.listing.events.CourtCentreChangedForHearing;
import uk.gov.justice.listing.events.CourtListRestricted;
import uk.gov.justice.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.listing.events.EndDateChangedForHearing;
import uk.gov.justice.listing.events.EndDateRemovedFromHearing;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDaysSequenced;
import uk.gov.justice.listing.events.HearingLanguageChangedForHearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.JudiciaryAssignedToHearing;
import uk.gov.justice.listing.events.JudiciaryChangedForHearing;
import uk.gov.justice.listing.events.JudiciaryRemovedFromHearing;
import uk.gov.justice.listing.events.JurisdictionChangedForHearing;
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
import uk.gov.justice.listing.events.SequencesResetOnHearingDays;
import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyListingNeeds;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.DefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.HearingDay;
import uk.gov.moj.cpp.listing.domain.HearingLanguage;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.RestrictCourtList;
import uk.gov.moj.cpp.listing.domain.SequenceHearing;
import uk.gov.moj.cpp.listing.domain.SimpleOffence;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.domain.aggregate.rules.HearingEndDateRule;
import uk.gov.moj.cpp.listing.domain.aggregate.rules.HearingLanguageRule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1172", "squid:S2629", "squid:S1948", "squid:S00107", "squid:S3655", "squid:S1067", "squid:CommentedOutCodeLine"})
public class Hearing implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hearing.class);

    private static final long serialVersionUID = 101L;

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
    private List<UUID> confirmedCourtApplicationIds = new ArrayList();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingListed.class).apply(this::onHearingListed),
                when(HearingLanguageChangedForHearing.class).apply(this::onHearingLanguageChanged),
                when(TypeChangedForHearing.class).apply(this::onTypeChangedForHearing),
                when(StartDateChangedForHearing.class).apply(this::onStartDateChangedForHearing),
                when(EndDateChangedForHearing.class).apply(this::onEndDateChangedForHearing),
                when(EndDateRemovedFromHearing.class).apply(this::onEndDateRemovedFromHearing),
                when(NonSittingDaysChangedForHearing.class).apply(this::onNonSittingDaysChangedForHearing),
                when(NonSittingDaysAssignedToHearing.class).apply(this::onNonSittingDaysAssignedToHearing),
                when(NonDefaultDaysChangedForHearing.class).apply(this::onNonDefaultDaysChangedForHearing),
                when(NonDefaultDaysAssignedToHearing.class).apply(this::onNonDefaultDaysAssignedToHearing),
                when(HearingDaysChangedForHearing.class).apply(this::onHearingDaysChangedForHearing),
                when(JurisdictionChangedForHearing.class).apply(this::onJurisdictionChangedForHearing),
                when(JudiciaryAssignedToHearing.class).apply(this::onJudiciaryAssignedToHearing),
                when(JudiciaryChangedForHearing.class).apply(this::onJudiciaryChangedForHearing),
                when(JudiciaryRemovedFromHearing.class).apply(this::onJudiciaryRemovedFromHearing),
                when(CourtRoomAssignedToHearing.class).apply(this::onCourtRoomAssignedToHearing),
                when(CourtRoomChangedForHearing.class).apply(this::onCourtRoomChangedForHearing),
                when(CourtRoomRemovedFromHearing.class).apply(this::onCourtRoomRemovedFromHearing),
                when(CourtCentreChangedForHearing.class).apply(this::onCourtCentreChangedForHearing),
                when(HearingAllocatedForListing.class).apply(this::onHearingAllocatedForListing),
                when(AllocatedHearingUpdatedForListing.class).apply(this::onAllocatedHearingUpdatedForListing),
                when(HearingUnallocatedForListing.class).apply(this::onHearingUnallocatedForListing),
                when(NewDefendantDetailsUpdated.class).apply(this::onNewDefendantDetailsUpdated),
                when(OffenceUpdated.class).apply(this::onOffenceUpdated),
                when(OffenceDeleted.class).apply(this::onOffenceDeleted),
                when(OffenceAdded.class).apply(this::onOffenceAdded),
                when(HearingDaysSequenced.class).apply(this::onHearingDaysSequenced),
                when(SequencesResetOnHearingDays.class).apply(this::onSequencesResetOnHearingDays),
                when(CourtApplicationUpdatedForHearing.class).apply(this::onCourtApplicationUpdatedForHearing),
                when(NewDefendantAddedForCourtProceedings.class).apply(this::onNewDefendantAddedForCourtProceedings),
                otherwiseDoNothing());
    }


    public Stream<Object> list(final UUID hearingId, final Type type,
                               final int estimateMinutes, final List<ListedCase> listedCases,
                               final UUID courtCentreId, final List<JudicialRole> judiciary,
                               final UUID courtRoomId, final String listingDirections,
                               final JurisdictionType jurisdictionType, final String prosecutorDatesToAvoid,
                               final String reportingRestrictionReason,
                               final ZonedDateTime startDate, final LocalDate endDate, CourtCentreDefaults courtCentreDefaults,
                               final List<CourtApplication> courtApplications, final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds,
                               final Integer hearingTypeDuration) {

        if (notCurrentlyListed()) {
            final LocalTime startTime = startDate.toLocalTime();
            final List<uk.gov.justice.listing.events.NonDefaultDay> newNonDefaultDays = Arrays.asList(uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay()
                    .withDuration(of(estimateMinutes > 0 ? estimateMinutes : hearingTypeDuration))
                    .withStartTime(startDate)
                    .build());

            LocalDate hearingEndDate = HearingEndDateRule.apply(endDate, startDate.toLocalDate());
            return apply(Stream.of(hearingListed()
                    .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                            .withId(hearingId)
                            .withType(uk.gov.justice.listing.events.Type.type()
                                    .withId(type.getId())
                                    .withDescription(type.getDescription())
                                    .build())
                            .withAllocated(false)
                            .withJudiciary(judiciary.stream()
                                    .map(NewDomainToEventConverter::buildJudicialRole)
                                    .collect(toList()))
                            .withListedCases(listedCases.isEmpty() ? null : listedCases.stream()
                                    .map(NewDomainToEventConverter::buildListedCase)
                                    .collect(toList()))
                            .withListingDirections(ofNullable(listingDirections))
                            .withHearingLanguage(HearingLanguageRule.apply(listedCases, getHearingLanguageNeedsForAllApplicants(courtApplicationPartyListingNeeds)))
                            .withReportingRestrictionReason(ofNullable(reportingRestrictionReason))
                            .withCourtRoomId(ofNullable(courtRoomId))
                            .withCourtCentreId(courtCentreId)
                            .withEstimatedMinutes(estimateMinutes)
                            .withProsecutorDatesToAvoid(ofNullable(prosecutorDatesToAvoid))
                            .withJurisdictionType(valueFor(jurisdictionType.name()).orElse(null))
                            .withStartDate(startDate.toLocalDate())
                            .withEndDate(hearingEndDate)
                            .withNonDefaultDays(startTime.compareTo(courtCentreDefaults.getDefaultStartTime()) != 0 ? newNonDefaultDays : emptyList())
                            .withNonSittingDays(emptyList())
                            .withHearingDays(HearingDaysCalculator.calculate(startDate.toLocalDate(), hearingEndDate, emptyList(), convertEventToDomain(newNonDefaultDays), courtCentreDefaults.getDefaultStartTime(), hearingTypeDuration))
                            .withCourtApplications(courtApplications.stream()
                                    .map(NewDomainToEventConverter::buildCourtApplications)
                                    .collect((toList())))
                            .build())
                    .build()));
        } else {
            LOGGER.error("Cannot list hearing with id {} as it has already been listed", hearingId);
            return Stream.empty();
        }
    }

    private List<NonDefaultDay> convertEventToDomain(List<uk.gov.justice.listing.events.NonDefaultDay> eventNonDefaults) {
        return eventNonDefaults.stream().map(this::getDomainNonDefaultDay).collect(toList());
    }

    private NonDefaultDay getDomainNonDefaultDay(final uk.gov.justice.listing.events.NonDefaultDay eventNonDefault) {
        return NonDefaultDay.nonDefaultDay().withStartTime(eventNonDefault.getStartTime()).withDuration(eventNonDefault.getDuration()).build();
    }

    public Stream<Object> changeJurisdictionType(final JurisdictionType jurisdictionType, final UUID hearingId) {
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

    public Stream<Object> changeType(final Type type, final UUID hearingId) {
        if (notCurrentlyAssigned(this.type)) {
            LOGGER.error("Type for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
            return Stream.empty();
        }

        if (hasChanged(this.type, type)) {
            return apply(Stream.of(typeChangedForHearing()
                    .withType(uk.gov.justice.listing.events.Type.type()
                            .withId(type.getId())
                            .withDescription(type.getDescription()).build())
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming type {} is the same as current type {} for hearing with id {} - Ignore", type, this.type, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> changeStartDate(final LocalDate startDate, final UUID hearingId) {
        if (notCurrentlyAssigned(this.startDate)) {
            LOGGER.error("Start date' for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
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

    public Stream<Object> changeEndDate(final LocalDate endDate, final UUID hearingId) {
        if (notCurrentlyAssigned(this.endDate)) {
            LOGGER.error("End date' for hearing with id {} is not assigned. Should have been assigned or defaulted when first listed", hearingId);
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
        if (notCurrentlyAssigned(this.nonDefaultDays) || this.nonDefaultDays.isEmpty()) {
            return apply(Stream.of(NonDefaultDaysAssignedToHearing.nonDefaultDaysAssignedToHearing()
                    .withNonDefaultDays(convertNonDefaultDaysToEvents(nonDefaultDays))
                    .withHearingId(hearingId)
                    .build()));
        } else if (hasChanged(this.nonDefaultDays, nonDefaultDays)) {
            return apply(Stream.of(NonDefaultDaysChangedForHearing.nonDefaultDaysChangedForHearing()
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
        if (currentlyAssigned(this.judiciary)) {
            return apply(Stream.of(JudiciaryRemovedFromHearing.judiciaryRemovedFromHearing()
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("No judiciary is currently assigned for hearing with id {} so cannot be removed - Ignore", hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> changeCourtCentre(final UUID courtCentreId, final UUID hearingId) {
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
        if (notCurrentlyAssigned(this.courtRoomId)) {
            final SequencesResetOnHearingDays sequencesResetOnHearingDaysEvent = createSequencesResetOnHearingDaysEvent(hearingId);
            final Stream<Object> appliedCourtRoomEvent = apply(Stream.of(CourtRoomAssignedToHearing.courtRoomAssignedToHearing()
                    .withCourtRoomId(courtRoomId)
                    .withHearingId(hearingId)
                    .build()));
            return Stream.concat(appliedCourtRoomEvent, apply(Stream.of(sequencesResetOnHearingDaysEvent)));
        } else if (hasChanged(this.courtRoomId, courtRoomId)) {
            final SequencesResetOnHearingDays sequencesResetOnHearingDaysEvent = createSequencesResetOnHearingDaysEvent(hearingId);
            final Stream<Object> appliedCourtRoomEvent = apply(Stream.of(courtRoomChangedForHearing()
                    .withCourtRoomId(courtRoomId)
                    .withHearingId(hearingId)
                    .build()));
            return Stream.concat(appliedCourtRoomEvent, apply(Stream.of(sequencesResetOnHearingDaysEvent)));
        } else {
            LOGGER.info("Incoming court room id {} is the same as current court room id {} for hearing with id {} - Ignore", courtRoomId, this.courtRoomId, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> removeCourtRoom(final UUID hearingId) {
        if (currentlyAssigned(this.courtRoomId)) {
            final SequencesResetOnHearingDays sequencesResetOnHearingDaysEvent = createSequencesResetOnHearingDaysEvent(hearingId);
            final Stream<Object> appliedCourtRoomEvent = apply(Stream.of(CourtRoomRemovedFromHearing.courtRoomRemovedFromHearing()
                    .withHearingId(hearingId)
                    .build()));
            return Stream.concat(appliedCourtRoomEvent, apply(Stream.of(sequencesResetOnHearingDaysEvent)));
        } else {
            LOGGER.info("No court room is currently assigned for hearing with id {} so cannot be removed - Ignore", hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> assignHearingDays(LocalDate startDate, LocalDate endDate, List<LocalDate> nonSittingDays, List<NonDefaultDay> nonDefaultDays, LocalTime defaultStartTime, Integer defaultDuration, UUID hearingId) {
        if (notCurrentlyAssigned(this.hearingDays)) {
            LOGGER.error("HearingDays for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
            return Stream.empty();
        }

        final List<uk.gov.justice.listing.events.HearingDay> hearingDaysChangedForHearing
                = HearingDaysCalculator.calculate(startDate, endDate, nonSittingDays, nonDefaultDays, defaultStartTime, defaultDuration);

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


    public Stream<Object> applyAllocationRules() {
        if (canAllocate()) {
            return onAllocationEvents();
        } else if (canUnallocate()) {
            return onUnallocationEvents();
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> updateDefendants(UUID caseId, List<uk.gov.moj.cpp.listing.domain.Defendant> defendants) {
        if (!isHearingInThePast()) {
            final List<Object> events = defendants.stream()
                    .filter(this::thisHearingContainsDefendant)
                    .map(defendant -> defendantDetailsUpdatedEvent(caseId, defendant))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> updateCaseMarkers(UUID caseId, List<uk.gov.moj.cpp.listing.domain.CaseMarker> caseMarkers) {
        if (!isHearingInThePast()) {
            final NewCaseMarkerUpdated newCaseMarkerUpdated = caseMarkerUpdateEvent(caseId, caseMarkers);
            return apply(Stream.of(newCaseMarkerUpdated));
        }
        return Stream.empty();
    }

    private NewCaseMarkerUpdated caseMarkerUpdateEvent(final UUID caseId, final List<CaseMarker> cm) {
        return NewCaseMarkerUpdated.newCaseMarkerUpdated()
                .withCaseId(caseId)
                .withHearingId(this.hearingId)
                .withCaseMarkers(NewDomainToEventConverter.convertCaseMarkersListToMarkers(cm))
                .build();
    }

    public Stream<Object> updateOffences(UUID caseId, UUID defendantId, List<uk.gov.moj.cpp.listing.domain.Offence> offences) {
        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(offence -> thisHearingContainsDefendantAndOffence(caseId, defendantId, offence.getId()))
                    .map(offence -> offenceUpdatedEvent(caseId, defendantId, offence))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> deleteOffences(UUID caseId, UUID defendantId, List<SimpleOffence> offences) {
        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(simpleOffence -> thisHearingContainsDefendantAndOffence(caseId, defendantId, simpleOffence.getId()))
                    .map(simpleOffence -> this.offenceDeletedEvent(caseId, simpleOffence))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> addOffences(UUID caseId, UUID defendantId, List<uk.gov.moj.cpp.listing.domain.Offence> offences) {
        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(offence -> thisHearingContainsDefendant(caseId, defendantId))
                    .map(offence -> offenceAddedEvent(caseId, defendantId, offence))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> sequenceHearingDays(SequenceHearing sequenceHearing) {
        if (notCurrentlyAssigned(this.hearingDays)) {
            LOGGER.error("HearingDays for hearing with id {} is not assigned. Should have been assigned when first listed", hearingId);
            return Stream.empty();
        }

        Map<LocalDate, Integer> hearingDaysSequencesMap = sequenceHearing.getHearingDays().stream()
                .collect(Collectors.toMap(HearingDay::getHearingDate, HearingDay::getSequence));
        final boolean sequenceChanged = hasSequenceChanged(hearingDaysSequencesMap);

        List<uk.gov.justice.listing.events.HearingDay> updatedHearingDays = this.hearingDays.stream()
                .map(d -> new uk.gov.justice.listing.events.HearingDay.Builder()
                        .withDurationMinutes(d.getDurationMinutes())
                        .withEndTime(d.getEndTime())
                        .withHearingDate(d.getHearingDate())
                        .withSequence(hearingDaysSequencesMap.containsKey(d.getHearingDate())
                                ? hearingDaysSequencesMap.get(d.getHearingDate())
                                : d.getSequence())
                        .withStartTime(d.getStartTime())
                        .build()
                ).collect(toList());
        // Only raise allocation events if sequence changed
        if (sequenceChanged) {
            this.hearingDays = convertHearingDaysToDomain(updatedHearingDays);
            return Stream.concat(applyAllocationRules(), apply(Stream.of(HearingDaysSequenced.hearingDaysSequenced()
                    .withHearingId(this.hearingId)
                    .withHearingDays(updatedHearingDays)
                    .build())));
        }
        LOGGER.info("Sequence not changed for hearing id {}", hearingId);
        return apply(Stream.of(HearingDaysSequenced.hearingDaysSequenced()
                .withHearingId(this.hearingId)
                .withHearingDays(updatedHearingDays)
                .build()));
    }

    public Stream<Object> addCourtApplication(UUID hearingId, CourtApplication courtApplication) {
        return apply(Stream.of(CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(hearingId)
                .withCourtApplication(NewDomainToEventConverter.buildCourtApplications(courtApplication))
                .build()));
    }

    public Stream<Object> updateCourtApplication(UUID hearingId, CourtApplication courtApplication) {
        if (!isHearingInThePast()) {
            return apply(Stream.of(CourtApplicationUpdatedForHearing.courtApplicationUpdatedForHearing()
                    .withHearingId(hearingId)
                    .withCourtApplication(NewDomainToEventConverter.buildCourtApplications(courtApplication))
                    .build()));
        }
        return Stream.empty();
    }

    public Stream<Object> addDefendantsForCourtProceedings(UUID caseId, List<uk.gov.moj.cpp.listing.domain.Defendant> defendants) {
        if (!isHearingInThePast()) {
            final List<Object> events = defendants.stream()
                    .map(defendant -> defendantsAddedForCourtProceedings(caseId, defendant))
                    .collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> restrictDetailsFromCourt(UUID hearingId, RestrictCourtList restrictCourtList) {

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

    private boolean thisHearingContainsDefendant(uk.gov.moj.cpp.listing.domain.Defendant defendant) {
        return isNull(this.prosecutionCaseDefendantOffenceIds) ? Boolean.FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId ->
                        prosecutionCaseDefendantOffenceId.getDefendants().stream()
                                .anyMatch(defendantOffenceId ->
                                        defendantOffenceId.getId().equals(defendant.getId())));
    }

    private boolean thisHearingContainsDefendant(UUID caseId, UUID defendantId) {
        return isNull(this.prosecutionCaseDefendantOffenceIds) ? Boolean.FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId ->
                        prosecutionCaseDefendantOffenceId.getId().equals(caseId) && prosecutionCaseDefendantOffenceId.getDefendants().stream().anyMatch(defendantOffenceIds ->
                                defendantOffenceIds.getId().equals(defendantId))
                );
    }

    private boolean thisHearingContainsDefendantAndOffence(UUID caseId, UUID defendantId, UUID offenceId) {
        return isNull(this.prosecutionCaseDefendantOffenceIds) ? Boolean.FALSE : this.prosecutionCaseDefendantOffenceIds.stream()
                .anyMatch(prosecutionCaseDefendantOffenceId ->
                        prosecutionCaseDefendantOffenceId.getId().equals(caseId) && prosecutionCaseDefendantOffenceId.getDefendants().stream().anyMatch(defendantOffenceIds ->
                                defendantOffenceIds.getId().equals(defendantId) && defendantOffenceIds.getOffences().stream().anyMatch(id ->
                                        id.equals(offenceId)))
                );
    }

    private boolean isHearingInThePast() {
        return this.endDate != null && LocalDate.now().isAfter(this.endDate);
    }

    private Stream<Object> onAllocationEvents() {
        if (allocated) {
            return apply(Stream.of(allocatedHearingUpdatedForListingEvent()));
        }
        return apply(Stream.of(hearingAllocatedForListingEvent()));
    }

    private Stream<Object> onUnallocationEvents() {
        final Stream<Object> appliedBusinessRuleEvents = apply(onUnallocationBusinessRules());
        final HearingUnallocatedForListing unallocateEvent = hearingUnallocatedForListingEvent();
        return Stream.concat(appliedBusinessRuleEvents, apply(Stream.of(unallocateEvent)));
    }

    private Stream<Object> onUnallocationBusinessRules() {
        // Currently no unallocated business rules to apply
        return Stream.empty();
    }

    private boolean canAllocate() {
        return currentlyAssigned(this.hearingLanguage) && currentlyAssigned(this.jurisdictionType)
                && currentlyAssigned(this.courtRoomId) && currentlyAssigned(this.endDate);
    }

    private boolean canUnallocate() {
        return this.allocated && (notCurrentlyAssigned(this.hearingLanguage) || notCurrentlyAssigned(this.courtRoomId)
                || notCurrentlyAssigned(this.jurisdictionType) || notCurrentlyAssigned(this.endDate));
    }

    private boolean notCurrentlyListed() {
        return notCurrentlyAssigned(this.hearingId) && notCurrentlyAssigned(this.type)
                && notCurrentlyAssigned(this.startDate) && notCurrentlyAssigned(this.estimatedMinutes);
    }

    private <T> boolean notCurrentlyAssigned(T hearingData) {
        return hearingData == null;
    }

    private <T> boolean currentlyAssigned(T hearingData) {
        return !notCurrentlyAssigned(hearingData);
    }

    private <T> boolean hasChanged(T currentValue, T newValue) {
        return !Objects.equals(currentValue, newValue);
    }

    // Helper methods to create events


    private HearingAllocatedForListing hearingAllocatedForListingEvent() {

        return hearingAllocatedForListing()
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
                .withProsecutionCaseDefendantsOffenceIds(isNull(this.prosecutionCaseDefendantOffenceIds) ? null : this.prosecutionCaseDefendantOffenceIds.stream()
                        .map(lc -> uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                                .withId(lc.getId())
                                .withDefendants(lc.getDefendants().stream()
                                        .map(this::buildEventDefendantOffenceIds)
                                        .collect(toList()))
                                .build()
                        ).collect(toList()))
                .withCourtApplicationIds(this.confirmedCourtApplicationIds.isEmpty() ? null : this.confirmedCourtApplicationIds)
                .build();
    }


    private uk.gov.justice.listing.events.Type buildHearingType() {
        return uk.gov.justice.listing.events.Type.type()
                .withId(this.type.getId())
                .withDescription(this.type.getDescription())
                .build();
    }

    private AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListingEvent() {

        return allocatedHearingUpdatedForListing()
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
                .withProsecutionCaseDefendantsOffenceIds(isNull(this.prosecutionCaseDefendantOffenceIds) ? null : this.prosecutionCaseDefendantOffenceIds.stream()
                        .map(lc -> uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                                .withId(lc.getId())
                                .withDefendants(lc.getDefendants().stream()
                                        .map(this::buildEventDefendantOffenceIds)
                                        .collect(toList()))
                                .build()
                        ).collect(toList()))
                .withCourtApplicationIds(this.confirmedCourtApplicationIds.isEmpty() ? null : this.confirmedCourtApplicationIds)
                .build();
    }


    private HearingUnallocatedForListing hearingUnallocatedForListingEvent() {
        return hearingUnallocatedForListing()
                .withHearingId(this.hearingId)
                .build();
    }

    private NewDefendantDetailsUpdated defendantDetailsUpdatedEvent(UUID caseId, Defendant defendant) {
        return NewDefendantDetailsUpdated.newDefendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendant(NewDomainToEventConverter.buildNewBaseDefendant(defendant))
                .withHearingId(this.hearingId)
                .build();
    }

    private NewDefendantAddedForCourtProceedings defendantsAddedForCourtProceedings(UUID caseId, Defendant defendant) {
        return NewDefendantAddedForCourtProceedings.newDefendantAddedForCourtProceedings()
                .withCaseId(caseId)
                .withDefendant(NewDomainToEventConverter.buildDefendant(defendant))
                .withHearingId(this.hearingId)
                .build();
    }

    private OffenceUpdated offenceUpdatedEvent(UUID caseId, UUID defendantId, uk.gov.moj.cpp.listing.domain.Offence offence) {
        return OffenceUpdated.offenceUpdated()
                .withOffence(NewDomainToEventConverter.buildOffence(offence))
                .withHearingId(this.hearingId)
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .build();
    }

    private OffenceDeleted offenceDeletedEvent(UUID caseId, SimpleOffence offence) {
        return OffenceDeleted.offenceDeleted()
                .withDefendantId(offence.getDefendantId())
                .withOffenceId(offence.getId())
                .withHearingId(this.hearingId)
                .withCaseId(caseId)
                .build();
    }

    private OffenceAdded offenceAddedEvent(UUID caseId, UUID defendantId, uk.gov.moj.cpp.listing.domain.Offence offence) {
        return OffenceAdded.offenceAdded()
                .withHearingId(this.hearingId)
                .withOffence(NewDomainToEventConverter.buildOffence(offence))
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .build();
    }

    // Methods to apply aggregate state

    private void onNewDefendantDetailsUpdated(NewDefendantDetailsUpdated event) {
        // Do nothing
    }

    private void onOffenceUpdated(OffenceUpdated offenceUpdated) {
        // Do nothing
    }

    private void onCourtApplicationUpdatedForHearing(CourtApplicationUpdatedForHearing courtApplicationUpdatedForHearing) {
        // Do nothing
    }

    private void onOffenceAdded(OffenceAdded offenceAdded) {
        final UUID caseId = offenceAdded.getCaseId();
        final UUID offenceId = offenceAdded.getOffence().getId();
        final UUID defendantId = offenceAdded.getDefendantId();

        final Optional<DefendantOffenceIds> defendantOffences = getDefendantOffenceIds(caseId, defendantId);

        defendantOffences.ifPresent(defendantOffenceIds -> defendantOffenceIds.getOffences().add(offenceId));
    }

    private void onOffenceDeleted(OffenceDeleted offenceDeleted) {
        final UUID caseId = offenceDeleted.getCaseId();
        final UUID offenceId = offenceDeleted.getOffenceId();
        final UUID defendantId = offenceDeleted.getDefendantId();

        final Optional<DefendantOffenceIds> defendantOffences = getDefendantOffenceIds(caseId, defendantId);

        defendantOffences.ifPresent(defendantOffenceIds -> defendantOffenceIds.getOffences().remove(offenceId));
    }


    private Optional<DefendantOffenceIds> getDefendantOffenceIds(final UUID caseId, final UUID defendantId) {
        if (isNull(this.prosecutionCaseDefendantOffenceIds)) {
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

    private void onNewDefendantAddedForCourtProceedings(NewDefendantAddedForCourtProceedings event) {

        final UUID caseId = event.getCaseId();
        final UUID defendantId = event.getDefendant().getId();

        final Optional<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIdsList = this.prosecutionCaseDefendantOffenceIds.stream()
                .filter(prosecutionCaseDefendantOffenceId -> prosecutionCaseDefendantOffenceId.getId().equals(caseId))
                .findFirst();
        prosecutionCaseDefendantOffenceIdsList.ifPresent(caseDefendantOffences ->
                caseDefendantOffences.getDefendants().add(DefendantOffenceIds.defendantOffenceIds().withId(defendantId).
                        withOffences(event.getDefendant().getOffences().stream().map(Offence::getId).collect(toList())).build())
        );

    }

    private void onHearingListed(HearingListed event) {
        LOGGER.info("onHearingListed() event:{}", event);
        final uk.gov.justice.listing.events.Hearing hearing = event.getHearing();
        this.hearingId = hearing.getId();
        this.type = Type.type()
                .withId(hearing.getType().getId())
                .withDescription(hearing.getType().getDescription())
                .build();
        this.startDate = hearing.getStartDate();
        this.endDate = hearing.getEndDate();
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
            this.prosecutionCaseDefendantOffenceIds = hearing.getListedCases().stream()
                    .map(lc -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                            .withId(lc.getId())
                            .withDefendants(lc.getDefendants().stream()
                                    .map(this::buildDomainDefendantOffenceIds)
                                    .collect(toList()))
                            .build()
                    ).collect(toList());
        }
        this.hearingDays = convertHearingDaysToDomain(hearing.getHearingDays());
        this.allocated = Boolean.FALSE;

        if (hearing.getCourtApplications() != null) {
            this.confirmedCourtApplicationIds = hearing.getCourtApplications().stream()
                    .map(uk.gov.justice.listing.events.CourtApplication::getId).collect(toList());
        }
    }

    private List<uk.gov.justice.listing.events.HearingDay> mergeHearingDaySequences(List<uk.gov.justice.listing.events.HearingDay> hearingDaysChangedForHearing, Map<ZonedDateTime, HearingDay> existingHearingDays) {
        return hearingDaysChangedForHearing.stream()
                .map(cd -> uk.gov.justice.listing.events.HearingDay.hearingDay()
                        .withDurationMinutes(cd.getDurationMinutes())
                        .withEndTime(cd.getEndTime())
                        .withSequence(existingHearingDays.containsKey(cd.getStartTime())
                                ? existingHearingDays.get(cd.getStartTime()).getSequence()
                                : 0)
                        .withHearingDate(cd.getHearingDate())
                        .withStartTime(cd.getStartTime())
                        .build())
                .collect(toList());
    }

    private SequencesResetOnHearingDays createSequencesResetOnHearingDaysEvent(UUID hearingId) {
        return SequencesResetOnHearingDays.sequencesResetOnHearingDays()
                .withHearingId(hearingId)
                .build();
    }

    private void onTypeChangedForHearing(TypeChangedForHearing event) {
        this.type = Type.type().withId(event.getType().getId()).withDescription(event.getType().getDescription()).build();
    }

    private void onStartDateChangedForHearing(StartDateChangedForHearing event) {
        this.startDate = LocalDate.parse(event.getStartDate());
    }

    private void onEndDateChangedForHearing(EndDateChangedForHearing event) {
        this.endDate = LocalDate.parse(event.getEndDate());
    }

    private void onNonSittingDaysChangedForHearing(NonSittingDaysChangedForHearing event) {
        this.nonSittingDays = event.getNonSittingDays();
    }

    private void onNonSittingDaysAssignedToHearing(NonSittingDaysAssignedToHearing event) {
        this.nonSittingDays = event.getNonSittingDays();
    }

    private void onNonDefaultDaysAssignedToHearing(NonDefaultDaysAssignedToHearing event) {
        this.nonDefaultDays = convertNonDefaultDaysToDomain(event.getNonDefaultDays());
    }

    private void onHearingLanguageChanged(HearingLanguageChangedForHearing event) {
        this.hearingLanguage = HearingLanguage.valueFor(event.getHearingLanguage().toString())
                .orElseThrow(IllegalArgumentException::new);
    }

    private boolean hasSequenceChanged(final Map<LocalDate, Integer> hearingDaysSequencesMap) {
        return this.hearingDays.stream().anyMatch(d -> ((!hearingDaysSequencesMap.containsKey(d.getHearingDate()))
                || (hearingDaysSequencesMap.containsKey(d.getHearingDate()) && !(hearingDaysSequencesMap.get(d.getHearingDate()).equals(d.getSequence())))));
    }

    private void onNonDefaultDaysChangedForHearing(NonDefaultDaysChangedForHearing event) {
        this.nonDefaultDays = convertNonDefaultDaysToDomain(event.getNonDefaultDays());
    }

    private void onHearingDaysChangedForHearing(HearingDaysChangedForHearing hearingDaysChangedForHearing) {
        this.hearingDays = convertHearingDaysToDomain(hearingDaysChangedForHearing.getHearingDays());
    }

    private void onJudiciaryAssignedToHearing(JudiciaryAssignedToHearing event) {
        this.judiciary = convertToDomain(event.getJudiciary());
    }

    private void onJurisdictionChangedForHearing(JurisdictionChangedForHearing event) {
        this.jurisdictionType = JurisdictionType.valueFor(event.getJurisdictionType().toString())
                .orElseThrow(IllegalArgumentException::new);
    }

    private void onJudiciaryChangedForHearing(JudiciaryChangedForHearing event) {
        this.judiciary = convertToDomain(event.getJudiciary());
    }

    private void onJudiciaryRemovedFromHearing(JudiciaryRemovedFromHearing event) {
        this.judiciary = null;
    }

    private void onCourtRoomAssignedToHearing(CourtRoomAssignedToHearing event) {
        this.courtRoomId = event.getCourtRoomId();
    }

    private void onCourtCentreChangedForHearing(CourtCentreChangedForHearing event) {
        this.courtCentreId = event.getCourtCentreId();
    }

    private void onCourtRoomChangedForHearing(CourtRoomChangedForHearing event) {
        this.courtRoomId = event.getCourtRoomId();
    }

    private void onCourtRoomRemovedFromHearing(CourtRoomRemovedFromHearing event) {
        this.courtRoomId = null;
    }

    private void onSequencesResetOnHearingDays(SequencesResetOnHearingDays event) {
        if (!this.hearingDays.isEmpty()) {
            this.hearingDays = this.hearingDays.stream()
                    .map(cd -> HearingDay.hearingDay()
                            .withDurationMinutes(cd.getDurationMinutes())
                            .withEndTime(cd.getEndTime())
                            .withSequence(0)
                            .withStartTime(cd.getStartTime())
                            .withHearingDate(cd.getHearingDate())
                            .build())
                    .collect(toList());
        }
    }

    private void onEndDateRemovedFromHearing(EndDateRemovedFromHearing event) {
        this.endDate = null;
    }

    private void onHearingDaysSequenced(final HearingDaysSequenced hearingDaysSequenced) {
        this.hearingDays = convertHearingDaysToDomain(hearingDaysSequenced.getHearingDays());
    }

    private void onHearingAllocatedForListing(HearingAllocatedForListing event) {
        this.allocated = Boolean.TRUE;
    }

    private void onAllocatedHearingUpdatedForListing(AllocatedHearingUpdatedForListing event) {
        // Do nothing
    }

    private void onHearingUnallocatedForListing(HearingUnallocatedForListing event) {
        this.allocated = Boolean.FALSE;
    }


    private List<uk.gov.justice.listing.events.NonDefaultDay> convertNonDefaultDaysToEvents(List<uk.gov.moj.cpp.listing.domain.NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> uk.gov.justice.listing.events.NonDefaultDay.nonDefaultDay()
                        .withStartTime(ndd.getStartTime())
                        .withDuration(ndd.getDuration())
                        .build())
                .collect(toList());
    }


    private List<NonDefaultDay> convertNonDefaultDaysToDomain(List<uk.gov.justice.listing.events.NonDefaultDay> nonDefaultDays) {
        return nonDefaultDays.stream()
                .map(ndd -> NonDefaultDay.nonDefaultDay()
                        .withStartTime(ndd.getStartTime())
                        .withDuration(ndd.getDuration())
                        .build())
                .collect(toList());
    }

    private List<HearingDay> convertHearingDaysToDomain(List<uk.gov.justice.listing.events.HearingDay> hearingDays) {
        if (hearingDays.isEmpty()) {
            return emptyList();
        }
        return hearingDays.stream()
                .map(cd -> HearingDay.hearingDay()
                        .withDurationMinutes(cd.getDurationMinutes())
                        .withEndTime(cd.getEndTime())
                        .withSequence(cd.getSequence())
                        .withStartTime(cd.getStartTime())
                        .withHearingDate(cd.getHearingDate())
                        .build())
                .collect(toList());
    }

    private List<uk.gov.justice.listing.events.HearingDay> convertHearingDaysToEvent(List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(cd -> uk.gov.justice.listing.events.HearingDay.hearingDay()
                        .withDurationMinutes(cd.getDurationMinutes())
                        .withEndTime(cd.getEndTime())
                        .withSequence(cd.getSequence())
                        .withStartTime(cd.getStartTime())
                        .withHearingDate(cd.getHearingDate())
                        .build())
                .collect(toList());
    }

    private List<uk.gov.justice.listing.events.JudicialRole> convertToEvents(List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles) {
        return judicialRoles.stream()
                .map(this::buildJudicialRole)
                .collect(toList());
    }

    private List<JudicialRole> convertToDomain(List<uk.gov.justice.listing.events.JudicialRole> judicialRoles) {
        return judicialRoles.stream()
                .map(jr -> JudicialRole.judicialRole()
                        .withJudicialRoleType(uk.gov.moj.cpp.listing.domain.JudicialRoleType.judicialRoleType()
                                .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                                .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId().orElse(null))
                                .build())
                        .withJudicialId(jr.getJudicialId())
                        .withIsDeputy(jr.getIsDeputy())
                        .withIsBenchChairman(jr.getIsBenchChairman())
                        .build())
                .collect(toList());
    }

    private uk.gov.justice.listing.events.JudicialRole buildJudicialRole(JudicialRole jr) {
        return uk.gov.justice.listing.events.JudicialRole.judicialRole()
                .withIsBenchChairman(jr.getIsBenchChairman())
                .withIsDeputy(jr.getIsDeputy())
                .withJudicialId(jr.getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId())
                        .build())
                .build();
    }

    private uk.gov.justice.listing.events.DefendantOffenceIds buildEventDefendantOffenceIds(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds d) {
        return uk.gov.justice.listing.events.DefendantOffenceIds.defendantOffenceIds()
                .withId(d.getId())
                .withOffenceIds(d.getOffences())
                .build();
    }


    private uk.gov.moj.cpp.listing.domain.DefendantOffenceIds buildDomainDefendantOffenceIds(uk.gov.justice.listing.events.Defendant d) {
        return uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                .withId(d.getId())
                .withOffences(d.getOffences().stream()
                        .map(Offence::getId)
                        .collect(toList()))
                .build();
    }

    private List<HearingLanguageNeeds> getHearingLanguageNeedsForAllApplicants(final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds) {
        return courtApplicationPartyListingNeeds.stream().filter(
                v -> v.getHearingLanguageNeeds().isPresent()).map(v -> v.getHearingLanguageNeeds().get()).collect(toList());
    }

}
