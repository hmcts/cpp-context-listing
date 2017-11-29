package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.lang.String.format;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.event.AllocatedHearingUpdatedForListing;
import uk.gov.moj.cpp.listing.event.CourtRoomAssignedToHearing;
import uk.gov.moj.cpp.listing.event.CourtRoomChangedForHearing;
import uk.gov.moj.cpp.listing.event.CourtRoomRemovedFromHearing;
import uk.gov.moj.cpp.listing.event.EstimateMinutesChangedForHearing;
import uk.gov.moj.cpp.listing.event.HearingAllocatedForListing;
import uk.gov.moj.cpp.listing.event.HearingDate;
import uk.gov.moj.cpp.listing.event.HearingEvent;
import uk.gov.moj.cpp.listing.event.HearingUnallocatedForListing;
import uk.gov.moj.cpp.listing.event.JudgeAssignedToHearing;
import uk.gov.moj.cpp.listing.event.JudgeChangedForHearing;
import uk.gov.moj.cpp.listing.event.JudgeRemovedFromHearing;
import uk.gov.moj.cpp.listing.event.NotBeforeSelectedForHearing;
import uk.gov.moj.cpp.listing.event.NotBeforeUnselectedForHearing;
import uk.gov.moj.cpp.listing.event.StartDateChangedForHearing;
import uk.gov.moj.cpp.listing.event.StartTimeAssignedToHearing;
import uk.gov.moj.cpp.listing.event.StartTimeChangedForHearing;
import uk.gov.moj.cpp.listing.event.StartTimeRemovedFromHearing;
import uk.gov.moj.cpp.listing.event.TypeChangedForHearing;
import uk.gov.moj.cpp.listing.event.UnallocatedHearingListed;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1172")
public class HearingAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingAggregate.class);

    private static final long serialVersionUID = 1L;

    private String hearingId;
    private String type;
    private LocalDate startDate;
    private Integer estimateMinutes;
    private String judgeId;
    private String courtRoomId;
    private LocalTime startTime;
    private boolean notBefore;
    private boolean allocated;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(UnallocatedHearingListed.class).apply(this::onUnallocatedHearingListed),
                when(TypeChangedForHearing.class).apply(this::onTypeChangedForHearing),
                when(StartDateChangedForHearing.class).apply(this::onStartDateChangedForHearing),
                when(EstimateMinutesChangedForHearing.class).apply(this::onEstimatedMinutesChangedForHearing),
                when(StartTimeAssignedToHearing.class).apply(this::onStartTimeAssignedForHearing),
                when(StartTimeChangedForHearing.class).apply(this::onStartTimeChangedForHearing),
                when(StartTimeRemovedFromHearing.class).apply(this::onStartTimeRemovedFromHearing),
                when(NotBeforeSelectedForHearing.class).apply(this::onNotBeforeSelectedForHearing),
                when(NotBeforeUnselectedForHearing.class).apply(this::onNotBeforeUnselectedForHearing),
                when(JudgeAssignedToHearing.class).apply(this::onJudgeAssignedToHearing),
                when(JudgeChangedForHearing.class).apply(this::onJudgeChangedForHearing),
                when(JudgeRemovedFromHearing.class).apply(this::onJudgeRemovedFromHearing),
                when(CourtRoomAssignedToHearing.class).apply(this::onCourtRoomAssignedToHearing),
                when(CourtRoomChangedForHearing.class).apply(this::onCourtRoomChangedForHearing),
                when(CourtRoomRemovedFromHearing.class).apply(this::onCourtRoomRemovedFromHearing),
                when(HearingAllocatedForListing.class).apply(this::onHearingAllocatedForListing),
                when(AllocatedHearingUpdatedForListing.class).apply(this::onAllocatedHearingUpdatedForListing),
                when(HearingUnallocatedForListing.class).apply(this::onHearingUnallocatedForListing),
                otherwiseDoNothing());
    }


    public Stream<Object> list(final String hearingId, final String type, final LocalDate startDate,
                               final int estimateMinutes, final String caseId, final String courtCentreId,
                               final List<Defendant> defendants) {
        if (notCurrentlyListed()) {
            return apply(Stream.of(new UnallocatedHearingListed(hearingId, type, startDate, estimateMinutes, caseId, courtCentreId, defendants)));
        } else {
            LOGGER.error(format("Cannot list hearing with id '%s' as it has already been listed", hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> changeType(final String type, final String hearingId) {
        if (notCurrentlyAssigned(this.type)) {
            LOGGER.error(format("Type for hearing with id '%s' is not assigned. Should have been assigned when first listed", hearingId));
            return Stream.empty();
        }

        if (hasChanged(this.type, type)) {
            return applyAndAllocateIfRequired(Stream.of(new TypeChangedForHearing(type, hearingId)));
        } else {
            LOGGER.info(format("Incoming type '%s' is the same as current type '%s' for hearing with id '%s' - Ignore", type, this.type, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> changeStartDate(final LocalDate startDate, final String hearingId) {
        if (notCurrentlyAssigned(this.startDate)) {
            LOGGER.error(format("Start date' for hearing with id '%s' is not assigned. Should have been assigned when first listed", hearingId));
            return Stream.empty();
        }

        if (hasChanged(this.startDate, startDate)) {
            return applyAndAllocateIfRequired(Stream.of(new StartDateChangedForHearing(startDate, hearingId)));
        } else {
            LOGGER.info(format("Incoming start date '%s' is the same as current start date '%s' for hearing with id '%s' - Ignore", startDate, this.startDate, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> changeEstimate(final Integer estimateMinutes, final String hearingId) {
        if (notCurrentlyAssigned(this.estimateMinutes)) {
            LOGGER.error(format("'Estimated minutes' for hearing with id '%s' is not assigned. Should have been assigned when first listed", hearingId));
            return Stream.empty();
        }

        if (hasChanged(this.estimateMinutes, estimateMinutes)) {
            return applyAndAllocateIfRequired(Stream.of(new EstimateMinutesChangedForHearing(estimateMinutes, hearingId)));
        } else {
            LOGGER.info(format("Incoming estimated minutes '%s' is the same as current estimated minutees '%s' for hearing with id '%s' - Ignore", estimateMinutes, this.estimateMinutes, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> assignStartTime(final LocalTime startTime, final String hearingId) {
        if (notCurrentlyAssigned(this.startTime)) {
            return apply(Stream.of(new StartTimeAssignedToHearing(startTime, hearingId)));
        } else if (hasChanged(this.startTime, startTime)) {
            return apply(Stream.of(new StartTimeChangedForHearing(startTime, hearingId)));
        } else {
            LOGGER.info(format("Incoming start time '%s' is the same as current start time '%s' for hearing with id '%s' - Ignore", startTime, this.startTime, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> removeStartTime(final String hearingId) {
        if (currentlyAssigned(this.startTime)) {
            return apply(Stream.of(new StartTimeRemovedFromHearing(hearingId)));
        } else {
            LOGGER.info(format("No start time is currently assigned for hearing with id '%s' so cannot be removed - Ignore", hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> selectNotBefore(final boolean notBefore, final String hearingId) {
        if (notBefore && notCurrentlyAssigned(this.startTime)) {
            LOGGER.error(format("Start time for hearing with id '%s' is not assigned. Cannot select 'not before' without a start time", hearingId));
            return Stream.empty();
        }

        if (hasChanged(this.notBefore, notBefore)) {
            final HearingEvent notBeforeEvent =
                    notBefore ? new NotBeforeSelectedForHearing(hearingId) : new NotBeforeUnselectedForHearing(hearingId);
            return apply(Stream.of(notBeforeEvent));
        } else {
            LOGGER.info(format("Incoming not before='%s' is the same as current not before='%s' for hearing with id '%s' - Ignore", notBefore, this.notBefore, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> assignJudge(final String judgeId, final String hearingId) {
        if (notCurrentlyAssigned(this.judgeId)) {
            return applyAndAllocateIfRequired(Stream.of(new JudgeAssignedToHearing(judgeId, hearingId)));
        } else if (hasChanged(this.judgeId, judgeId)) {
            return applyAndAllocateIfRequired(Stream.of(new JudgeChangedForHearing(judgeId, hearingId)));
        } else {
            LOGGER.info(format("Incoming judge id '%s' is the same as current judge id '%s' for hearing with id '%s' - Ignore", judgeId, this.judgeId, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> removeJudge(final String hearingId) {
        if (currentlyAssigned(this.judgeId)) {
            return applyAndUnallocateIfRequired(Stream.of(new JudgeRemovedFromHearing(hearingId)));
        } else {
            LOGGER.info(format("No judge is currently assigned for hearing with id '%s' so cannot be removed - Ignore", hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> assignCourtRoom(final String courtRoomId, final String hearingId) {
        if (notCurrentlyAssigned(this.courtRoomId)) {
            return applyAndAllocateIfRequired(Stream.of(new CourtRoomAssignedToHearing(courtRoomId, hearingId)));
        } else if (hasChanged(this.courtRoomId, courtRoomId)) {
            return applyAndAllocateIfRequired(Stream.of(new CourtRoomChangedForHearing(courtRoomId, hearingId)));
        } else {
            LOGGER.info(format("Incoming court room id '%s' is the same as current court room id '%s' for hearing with id '%s' - Ignore", courtRoomId, this.courtRoomId, hearingId));
            return Stream.empty();
        }
    }

    public Stream<Object> removeCourtRoom(final String hearingId) {
        if (currentlyAssigned(this.courtRoomId)) {
            return applyAndUnallocateIfRequired(Stream.of(new CourtRoomRemovedFromHearing(hearingId)));
        } else {
            LOGGER.info(format("No court room is currently assigned for hearing with id '%s' so cannot be removed - Ignore", hearingId));
            return Stream.empty();
        }
    }


    private Stream<Object> applyAndAllocateIfRequired(Stream<Object> events) {
        final Stream<Object> appliedEvents = apply(events);
        if (canAllocate()) {
            final HearingEvent allocateEvent = allocated ? allocatedHearingUpdatedForListingEvent() : hearingAllocatedForListingEvent();
            return Stream.concat(appliedEvents, apply(Stream.of(allocateEvent)));
        } else {
            return appliedEvents;
        }
    }

    private Stream<Object> applyAndUnallocateIfRequired(Stream<Object> events) {
        final Stream<Object> appliedEvents = apply(events);
        if (canUnallocate()) {
            return Stream.concat(appliedEvents, apply(Stream.of(hearingUnallocatedForListingEvent())));
        } else {
            return appliedEvents;
        }
    }

    private boolean canAllocate() {
        return currentlyAssigned(this.judgeId) && currentlyAssigned(this.courtRoomId);
    }

    private boolean canUnallocate() {
        return this.allocated && (notCurrentlyAssigned(this.judgeId) || notCurrentlyAssigned(this.courtRoomId));
    }

    private HearingAllocatedForListing hearingAllocatedForListingEvent() {
        return new HearingAllocatedForListing(this.hearingId, this.type,
               this.estimateMinutes, this.judgeId, this.courtRoomId,
                new HearingDate(this.startDate, this.startTime, this.notBefore));
    }

    private AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListingEvent() {
        return new AllocatedHearingUpdatedForListing(this.hearingId, this.type,
               this.estimateMinutes, this.judgeId, this.courtRoomId,
                new HearingDate(this.startDate, this.startTime, this.notBefore));
    }

    private HearingUnallocatedForListing hearingUnallocatedForListingEvent() {
        return new HearingUnallocatedForListing(this.hearingId, this.type,
                this.estimateMinutes, this.judgeId, this.courtRoomId,
                new HearingDate(this.startDate, this.startTime, this.notBefore));
    }


    private boolean notCurrentlyListed() {
        return notCurrentlyAssigned(this.hearingId) && notCurrentlyAssigned(this.type)
                && notCurrentlyAssigned(this.startDate) && notCurrentlyAssigned(this.estimateMinutes);
    }

    private boolean notCurrentlyAssigned(String hearingData) {
        return Strings.isNullOrEmpty(hearingData);
    }

    private boolean currentlyAssigned(String hearingData) {
        return !notCurrentlyAssigned(hearingData);
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


    // Methods to apply aggregate state

    private void onUnallocatedHearingListed(UnallocatedHearingListed event) {
        this.hearingId = event.getHearingId();
        this.type = event.getType();
        this.startDate = event.getStartDate();
        this.estimateMinutes = event.getEstimateMinutes();
        this.allocated = Boolean.FALSE;
    }

    private void onTypeChangedForHearing(TypeChangedForHearing event) {
        this.type = event.getType();
    }

    private void onStartDateChangedForHearing(StartDateChangedForHearing event) {
        this.startDate = event.getStartDate();
    }

    private void onEstimatedMinutesChangedForHearing(EstimateMinutesChangedForHearing event) {
        this.estimateMinutes = event.getEstimateMinutes();
    }

    private void onStartTimeAssignedForHearing(StartTimeAssignedToHearing event) {
        this.startTime = event.getStartTime();
    }

    private void onStartTimeChangedForHearing(StartTimeChangedForHearing event) {
        this.startTime = event.getStartTime();
    }

    private void onStartTimeRemovedFromHearing(StartTimeRemovedFromHearing event) {
        this.startTime = null;
    }

    private void onNotBeforeSelectedForHearing(NotBeforeSelectedForHearing event) {
        this.notBefore = Boolean.TRUE;
    }

    private void onNotBeforeUnselectedForHearing(NotBeforeUnselectedForHearing event) {
        this.notBefore = Boolean.FALSE;
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
