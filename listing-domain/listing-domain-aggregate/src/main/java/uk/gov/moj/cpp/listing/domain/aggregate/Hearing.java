package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing.allocatedHearingUpdatedForListing;
import static uk.gov.justice.listing.events.CourtRoomAssignedToHearing.courtRoomAssignedToHearing;
import static uk.gov.justice.listing.events.CourtRoomChangedForHearing.courtRoomChangedForHearing;
import static uk.gov.justice.listing.events.CourtRoomRemovedFromHearing.courtRoomRemovedFromHearing;
import static uk.gov.justice.listing.events.EndDateAssignedToHearing.endDateAssignedToHearing;
import static uk.gov.justice.listing.events.EndDateChangedForHearing.endDateChangedForHearing;
import static uk.gov.justice.listing.events.HearingAllocatedForListing.hearingAllocatedForListing;
import static uk.gov.justice.listing.events.HearingListed.hearingListed;
import static uk.gov.justice.listing.events.HearingUnallocatedForListing.hearingUnallocatedForListing;
import static uk.gov.justice.listing.events.JudgeAssignedToHearing.judgeAssignedToHearing;
import static uk.gov.justice.listing.events.JudgeChangedForHearing.judgeChangedForHearing;
import static uk.gov.justice.listing.events.JudgeRemovedFromHearing.judgeRemovedFromHearing;
import static uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing.nonSittingDaysAssignedToHearing;
import static uk.gov.justice.listing.events.NonSittingDaysChangedForHearing.nonSittingDaysChangedForHearing;
import static uk.gov.justice.listing.events.StartDateChangedForHearing.startDateChangedForHearing;
import static uk.gov.justice.listing.events.StartTimesAssignedToHearing.startTimesAssignedToHearing;
import static uk.gov.justice.listing.events.StartTimesChangedForHearing.startTimesChangedForHearing;
import static uk.gov.justice.listing.events.TypeChangedForHearing.typeChangedForHearing;
import static uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds;
import static uk.gov.moj.cpp.listing.domain.aggregate.DomainToEventConverter.createDefendantsFrom;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.CourtRoomAssignedToHearing;
import uk.gov.justice.listing.events.CourtRoomChangedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.listing.events.DefendantDetailsUpdated;
import uk.gov.justice.listing.events.EndDateAssignedToHearing;
import uk.gov.justice.listing.events.EndDateChangedForHearing;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingDate;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.JudgeAssignedToHearing;
import uk.gov.justice.listing.events.JudgeChangedForHearing;
import uk.gov.justice.listing.events.JudgeRemovedFromHearing;
import uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonSittingDaysChangedForHearing;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.listing.events.StartDateChangedForHearing;
import uk.gov.justice.listing.events.StartTimesAssignedToHearing;
import uk.gov.justice.listing.events.StartTimesChangedForHearing;
import uk.gov.justice.listing.events.TypeChangedForHearing;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.DefendantOffenceIds;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1172", "squid:S2629", "squid:S00107"})
public class Hearing implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hearing.class);

    private static final long serialVersionUID = 2L;

    private static final ZoneId BST = ZoneId.of("Europe/London");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private UUID hearingId;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<LocalDate> nonSittingDays = Collections.emptyList();
    private Integer estimateMinutes;
    private UUID judgeId;
    private UUID courtRoomId;
    private List<ZonedDateTime> startTimes = Collections.emptyList();
    private boolean allocated;
    private UUID caseId;
    private String urn;
    private UUID courtCentreId;
    private List<DefendantOffenceIds> defendantsOffenceIds;


    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(HearingListed.class).apply(this::onHearingListed),
                when(TypeChangedForHearing.class).apply(this::onTypeChangedForHearing),
                when(StartDateChangedForHearing.class).apply(this::onStartDateChangedForHearing),
                when(EndDateChangedForHearing.class).apply(this::onEndDateChangedForHearing),
                when(EndDateAssignedToHearing.class).apply(this::onEndDateAssignedToHearing),
                when(NonSittingDaysChangedForHearing.class).apply(this::onNonSittingDaysChangedForHearing),
                when(NonSittingDaysAssignedToHearing.class).apply(this::onNonSittingDaysAssignedToHearing),
                when(StartTimesChangedForHearing.class).apply(this::onStartTimesChangedForHearing),
                when(StartTimesAssignedToHearing.class).apply(this::onStartTimesAssignedToHearing),
                when(JudgeAssignedToHearing.class).apply(this::onJudgeAssignedToHearing),
                when(JudgeChangedForHearing.class).apply(this::onJudgeChangedForHearing),
                when(JudgeRemovedFromHearing.class).apply(this::onJudgeRemovedFromHearing),
                when(CourtRoomAssignedToHearing.class).apply(this::onCourtRoomAssignedToHearing),
                when(CourtRoomChangedForHearing.class).apply(this::onCourtRoomChangedForHearing),
                when(CourtRoomRemovedFromHearing.class).apply(this::onCourtRoomRemovedFromHearing),
                when(HearingAllocatedForListing.class).apply(this::onHearingAllocatedForListing),
                when(AllocatedHearingUpdatedForListing.class).apply(this::onAllocatedHearingUpdatedForListing),
                when(HearingUnallocatedForListing.class).apply(this::onHearingUnallocatedForListing),
                when(DefendantDetailsUpdated.class).apply(this::onDefendantDetailsUpdated),
                when(OffenceUpdated.class).apply(this::onOffenceUpdated),
                when(OffenceDeleted.class).apply(this::onOffenceDeleted),
                when(OffenceAdded.class).apply(this::onOffenceAdded),
                otherwiseDoNothing());
    }


    public Stream<Object> list(final UUID hearingId, final String type, final LocalDate startDate,
                               final int estimateMinutes, final UUID caseId, final String urn, final UUID courtCentreId,
                               final List<Defendant> defendants, final UUID judgeId, final UUID courtRoomId,
                               final LocalTime startTime, final LocalDate endDate) {
        if (notCurrentlyListed()) {

            return apply(Stream.of(hearingListed()
                    .withHearingId(hearingId).withType(type)
                    .withStartDate(startDate)
                    .withEstimateMinutes(estimateMinutes)
                    .withCaseId(caseId)
                    .withType(type)
                    .withUrn(urn)
                    .withCourtCentreId(courtCentreId)
                    .withDefendants(createDefendantsFrom(defendants))
                    .withJudgeId(judgeId)
                    .withCourtRoomId(courtRoomId)
                    .withStartTimes(startTime != null
                            ? Arrays.asList(ZonedDateTime.of(startDate, startTime, BST).withZoneSameInstant(UTC))
                            : Collections.emptyList())
                    .withEndDate(endDate)
                    .build()));
        } else {
            LOGGER.error(format("Cannot list hearing with id '%s' as it has already been listed", hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> changeType(final String type, final UUID hearingId) {
        if (notCurrentlyAssigned(this.type)) {
            LOGGER.error(format("Type for hearing with id '%s' is not assigned. Should have been assigned when first listed", hearingId));
            return Stream.empty();
        }

        if (hasChanged(this.type, type)) {
            return apply(Stream.of(typeChangedForHearing()
                    .withType(type)
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info(format("Incoming type '%s' is the same as current type '%s' for hearing with id '%s' - Ignore", type, this.type, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> changeStartDate(final LocalDate startDate, final UUID hearingId) {
        if (notCurrentlyAssigned(this.startDate)) {
            LOGGER.error(format("Start date' for hearing with id '%s' is not assigned. Should have been assigned when first listed", hearingId));
            return Stream.empty();
        }

        if (hasChanged(this.startDate, startDate)) {
            return apply(Stream.of(startDateChangedForHearing()
                    .withStartDate(startDate.toString())
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info(format("Incoming start date '%s' is the same as current start date '%s' for hearing with id '%s' - Ignore", startDate, this.startDate, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> assignEndDate(final LocalDate endDate, final UUID hearingId) {
        if (notCurrentlyAssigned(this.endDate)) {
            return apply(Stream.of(endDateAssignedToHearing()
                    .withEndDate(endDate.toString())
                    .withHearingId(hearingId)
                    .build()));
        } else if (hasChanged(this.endDate, endDate)) {
            return apply(Stream.of(endDateChangedForHearing()
                    .withEndDate(endDate.toString())
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info("Incoming endDate {} is the same as current endDate {} for hearing with id {} - Ignore", endDate, this.endDate, hearingId);
            return Stream.empty();
        }
    }

    public Stream<Object> assignStartTimes(final List<ZonedDateTime> startTimes, final UUID hearingId) {
        if (notCurrentlyAssigned(this.startTimes) || this.startTimes.isEmpty()) {
            return apply(Stream.of(startTimesAssignedToHearing()
                    .withStartTimes(startTimes)
                    .withHearingId(hearingId)
                    .build()));
        } else if (hasChanged(this.startTimes, startTimes)) {
            return apply(Stream.of(startTimesChangedForHearing()
                    .withStartTimes(startTimes)
                    .withHearingId(hearingId)
                    .build()
            ));
        } else {
            LOGGER.info("Incoming start times {} is the same as current start times {} for hearing with id {} - Ignore", startTimes, this.startTimes, hearingId);
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

    public Stream<Object> assignJudge(final UUID judgeId, final UUID hearingId) {
        if (notCurrentlyAssigned(this.judgeId)) {
            return apply(Stream.of(judgeAssignedToHearing()
                    .withJudgeId(judgeId)
                    .withHearingId(hearingId)
                    .build()));
        } else if (hasChanged(this.judgeId, judgeId)) {
            return apply(Stream.of(judgeChangedForHearing()
                    .withJudgeId(judgeId)
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info(format("Incoming judge id '%s' is the same as current judge id '%s' for hearing with id '%s' - Ignore", judgeId, this.judgeId, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> removeJudge(final UUID hearingId) {
        if (currentlyAssigned(this.judgeId)) {
            return apply(Stream.of(judgeRemovedFromHearing()
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info(format("No judge is currently assigned for hearing with id '%s' so cannot be removed - Ignore", hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> assignCourtRoom(final UUID courtRoomId, final UUID hearingId) {
        if (notCurrentlyAssigned(this.courtRoomId)) {
            return apply(Stream.of(courtRoomAssignedToHearing()
                    .withCourtRoomId(courtRoomId)
                    .withHearingId(hearingId)
                    .build()));
        } else if (hasChanged(this.courtRoomId, courtRoomId)) {
            return apply(Stream.of(courtRoomChangedForHearing()
                    .withCourtRoomId(courtRoomId)
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info(format("Incoming court room id '%s' is the same as current court room id '%s' for hearing with id '%s' - Ignore", courtRoomId, this.courtRoomId, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> removeCourtRoom(final UUID hearingId) {
        if (currentlyAssigned(this.courtRoomId)) {
            return apply(Stream.of(courtRoomRemovedFromHearing()
                    .withHearingId(hearingId)
                    .build()));
        } else {
            LOGGER.info(format("No court room is currently assigned for hearing with id '%s' so cannot be removed - Ignore", hearingId));
            return Stream.empty();
        }
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

    public Stream<Object> updateDefendants(List<Defendant> defendants) {
        if (!isHearingInThePast()) {
            final List<Object> events = defendants.stream()
                    .filter(this::thisHearingContainsDefendant)
                    .map(this::defendantDetailsUpdatedEvent
            ).collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> updateOffences(List<uk.gov.moj.cpp.listing.domain.Offence> offences) {
        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(this::thisHearingContainsDefendantAndOffence)
                    .map(this::offenceUpdatedEvent
            ).collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> deleteOffences(List<uk.gov.moj.cpp.listing.domain.SimpleOffence> offences) {
        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(this::thisHearingContainsDefendantAndOffence)
                    .map(this::offenceDeletedEvent
            ).collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    public Stream<Object> addOffences(List<uk.gov.moj.cpp.listing.domain.Offence> offences) {
        if (!isHearingInThePast()) {
            final List<Object> events = offences.stream()
                    .filter(this::thisHearingContainsDefendant)
                    .map(this::offenceAddedEvent
            ).collect(toList());
            return apply(events.stream());
        }
        return Stream.empty();
    }

    private boolean thisHearingContainsDefendant(Defendant defendant) {
        return this.defendantsOffenceIds.stream()
                .anyMatch(defendantOffenceId -> defendantOffenceId.getId().toString().equals(defendant.getId()));
    }

    private boolean thisHearingContainsDefendant(uk.gov.moj.cpp.listing.domain.SimpleOffence offence) {
        return this.defendantsOffenceIds.stream()
                .anyMatch(defendantOffenceId -> defendantOffenceId.getId().toString().equals(offence.getDefendantId()));
    }

    private boolean thisHearingContainsDefendantAndOffence(uk.gov.moj.cpp.listing.domain.SimpleOffence offence) {
        return this.defendantsOffenceIds.stream()
                .anyMatch(defendantOffenceId ->
                        defendantOffenceId.getId().toString().equals(offence.getDefendantId())
                                && defendantOffenceId.getOffenceIds().stream().anyMatch(offenceId -> offenceId.toString().equals(offence.getId()))
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
        // Currently no unallocation business rules to apply
        return Stream.empty();
    }

    private boolean canAllocate() {
        return currentlyAssigned(this.judgeId) && currentlyAssigned(this.courtRoomId) && currentlyAssigned(this.endDate);
    }

    private boolean canUnallocate() {
        return this.allocated && (notCurrentlyAssigned(this.judgeId) || notCurrentlyAssigned(this.courtRoomId) || notCurrentlyAssigned(this.endDate));
    }

    private boolean notCurrentlyListed() {
        return notCurrentlyAssigned(this.hearingId) && notCurrentlyAssigned(this.type)
                && notCurrentlyAssigned(this.startDate) && notCurrentlyAssigned(this.estimateMinutes);
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
        final Optional<String> startTime = getStartTime();
        return hearingAllocatedForListing()
                .withHearingId(this.hearingId)
                .withType(this.type)
                .withEstimateMinutes(this.estimateMinutes)
                .withUrn(this.urn)
                .withCaseId(this.caseId)
                .withCourtCentreId(this.courtCentreId)
                .withJudgeId(this.judgeId)
                .withCourtRoomId(this.courtRoomId)
                .withHearingDays(HearingDaysCalculator.calculate(this.startDate, this.endDate, this.nonSittingDays, this.startTimes))
                .withDefendantsOffenceIds(
                        this.defendantsOffenceIds.stream()
                                .map(d -> uk.gov.justice.listing.events.DefendantOffenceIds.defendantOffenceIds()
                                        .withId(d.getId())
                                        .withOffenceIds(d.getOffenceIds())
                                        .build())
                                .collect(toList()))
                .withHearingDate(HearingDate.hearingDate()
                        .withStartDate(this.startDate.toString())
                        .withStartTime(startTime)
                        .build())
                .build();
    }

    private AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListingEvent() {

        return allocatedHearingUpdatedForListing()
                .withHearingId(this.hearingId)
                .withType(this.type)
                .withJudgeId(this.judgeId)
                .withCourtRoomId(this.courtRoomId)
                .withCourtCentreId(courtCentreId)
                .withHearingDays(HearingDaysCalculator.calculate(this.startDate, this.endDate, this.nonSittingDays, this.startTimes))
                .build();
    }

    private HearingUnallocatedForListing hearingUnallocatedForListingEvent() {
        final Optional<String> startTime = getStartTime();

        return hearingUnallocatedForListing()
                .withHearingId(this.hearingId)
                .withType(this.type)
                .withEstimateMinutes(this.estimateMinutes)
                .withJudgeId(this.judgeId)
                .withCourtRoomId(this.courtRoomId)
                .withHearingDate(HearingDate.hearingDate()
                        .withStartDate(this.startDate.toString())
                        .withStartTime(startTime)
                        .build())
                .build();
    }

    private Optional<String> getStartTime() {
        return this.startTimes != null && !this.startTimes.isEmpty() ?
                of(this.startTimes.get(0).withZoneSameInstant(BST).toLocalTime().toString()) : Optional.empty();
    }

    private DefendantDetailsUpdated defendantDetailsUpdatedEvent(Defendant defendant) {
        return DefendantDetailsUpdated.defendantDetailsUpdated()
                .withDefendant(DomainToEventConverter.createBaseDefendantFrom(defendant))
                .withHearingId(this.hearingId)
                .build();
    }

    private OffenceUpdated offenceUpdatedEvent(uk.gov.moj.cpp.listing.domain.Offence offence) {
        return OffenceUpdated.offenceUpdated()
                .withOffence(DomainToEventConverter.createOffenceFrom(offence))
                .withHearingId(this.hearingId)
                .build();
    }

    private OffenceDeleted offenceDeletedEvent(uk.gov.moj.cpp.listing.domain.SimpleOffence offence) {
        return OffenceDeleted.offenceDeleted()
                .withDefendantId(UUID.fromString(offence.getDefendantId()))
                .withOffenceId(UUID.fromString(offence.getId()))
                .withHearingId(this.hearingId)
                .build();
    }

    private OffenceAdded offenceAddedEvent(uk.gov.moj.cpp.listing.domain.Offence offence) {
        return OffenceAdded.offenceAdded()
                .withHearingId(this.hearingId)
                .withOffence(DomainToEventConverter.createOffenceFrom(offence))
                .build();
    }

    // Methods to apply aggregate state

    private void onDefendantDetailsUpdated(DefendantDetailsUpdated event) {
        // Do nothing
    }

    private void onOffenceUpdated(OffenceUpdated offenceUpdated) {
        // Do nothing
    }


    private void onOffenceAdded(OffenceAdded offenceAdded) {
        final UUID offenceId = offenceAdded.getOffence().getId();
        final UUID defendantId = offenceAdded.getOffence().getDefendantId();
        final Optional<DefendantOffenceIds> offences = this.defendantsOffenceIds.stream()
                .filter(defendantOffenceIds -> defendantOffenceIds.getId().equals(defendantId))
                .findFirst();
        offences.ifPresent(defendantOffenceIds -> defendantOffenceIds.getOffenceIds().add(offenceId));
    }

    private void onOffenceDeleted(OffenceDeleted offenceDeleted) {
        final UUID offenceId = offenceDeleted.getOffenceId();
        final UUID defendantId = offenceDeleted.getDefendantId();
        final Optional<DefendantOffenceIds> offences = this.defendantsOffenceIds.stream()
                .filter(defendantOffenceIds -> defendantOffenceIds.getId().equals(defendantId))
                .findFirst();
        offences.ifPresent(defendantOffenceIds -> defendantOffenceIds.getOffenceIds().remove(offenceId));
    }

    private void onHearingListed(HearingListed event) {
        this.hearingId = event.getHearingId();
        this.type = event.getType();
        this.startDate = event.getStartDate();
        this.endDate = event.getEndDate();
        this.estimateMinutes = event.getEstimateMinutes();
        this.allocated = Boolean.FALSE;
        this.caseId = event.getCaseId();
        this.urn = event.getUrn();
        this.judgeId = event.getJudgeId();
        this.courtRoomId = event.getCourtRoomId();
        this.courtCentreId = event.getCourtCentreId();
        this.startTimes = event.getStartTimes();

        this.defendantsOffenceIds = event.getDefendants().stream()
                .map(d -> defendantOffenceIds()
                        .withId(d.getId())
                        .withOffenceIds(d.getOffences().stream()
                                .map(o -> o.getId())
                                .collect(toList()))
                        .build())
                .collect(toList());
    }


    private void onTypeChangedForHearing(TypeChangedForHearing event) {
        this.type = event.getType();
    }

    private void onStartDateChangedForHearing(StartDateChangedForHearing event) {
        this.startDate = LocalDate.parse(event.getStartDate());
    }

    private void onEndDateChangedForHearing(EndDateChangedForHearing event) {
        this.endDate = LocalDate.parse(event.getEndDate());
    }

    private void onEndDateAssignedToHearing(EndDateAssignedToHearing event) {
        this.endDate = LocalDate.parse(event.getEndDate());
    }

    private void onStartTimesChangedForHearing(StartTimesChangedForHearing event) {
        this.startTimes = event.getStartTimes();
    }

    private void onNonSittingDaysChangedForHearing(NonSittingDaysChangedForHearing event) {
        this.nonSittingDays = event.getNonSittingDays();
    }

    private void onNonSittingDaysAssignedToHearing(NonSittingDaysAssignedToHearing event) {
        this.nonSittingDays = event.getNonSittingDays();
    }

    private void onStartTimesAssignedToHearing(StartTimesAssignedToHearing event) {
        this.startTimes = event.getStartTimes();
    }


    private void onJudgeAssignedToHearing(JudgeAssignedToHearing event) {
        this.judgeId = event.getJudgeId();
    }

    private void onJudgeChangedForHearing(JudgeChangedForHearing event) {
        this.judgeId = event.getJudgeId();
    }

    private void onJudgeRemovedFromHearing(JudgeRemovedFromHearing event) {
        this.judgeId = null;
    }

    private void onCourtRoomAssignedToHearing(CourtRoomAssignedToHearing event) {
        this.courtRoomId = event.getCourtRoomId();
    }

    private void onCourtRoomChangedForHearing(CourtRoomChangedForHearing event) {
        this.courtRoomId = event.getCourtRoomId();
    }

    private void onCourtRoomRemovedFromHearing(CourtRoomRemovedFromHearing event) {
        this.courtRoomId = null;
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
}
