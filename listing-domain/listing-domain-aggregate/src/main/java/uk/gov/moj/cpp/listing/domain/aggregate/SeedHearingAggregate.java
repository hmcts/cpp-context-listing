package uk.gov.moj.cpp.listing.domain.aggregate;

import org.apache.commons.collections.CollectionUtils;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.CourtCentreDetails;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.listing.events.NextHearingReplaced;
import uk.gov.justice.listing.events.NextHearingRequested;
import uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested;
import uk.gov.justice.listing.events.UnscheduledNextHearingRequested;
import uk.gov.justice.listing.events.UpdateExistingHearingRequested;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

/**
 * This aggregate streams based on resulted hearing (H1).
 * <p>
 * seededHearingIds - All the hearing ids (H2, H3) of newly created hearings when adjourning. The
 * new hearing ids (H2, H3) will be created from progression context and these hearing ids will be
 * seeded/added in this property.
 * <p>
 * existingHearingIds - All the hearing ids (H4) of already existing hearings when adjourning. This
 * happens when one of the offences is adjourned to already existing hearing (H4) when resulting.
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize", "PMD.NullAssignment", "squid:S3655"})
public class SeedHearingAggregate implements Aggregate {


    private static final long serialVersionUID = 5;
    private Map<String, Set<UUID>> seededHearingIdsMapForHearingDay = new HashMap<>();
    private Map<String, Set<UUID>> existingHearingIdsMapForHearingDay = new HashMap<>();

    private Set<UUID> previousHearingIds = new HashSet<>();
    private Set<UUID> currentHearingIds = new HashSet<>();
    private UUID seedingHearingId;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(NextHearingRequested.class).apply(this::onNextHearingRequested),
                when(DeleteNextHearingRequested.class).apply(this::onDeleteNextHearingRequested),
                when(RemoveOffencesFromExistingHearingRequested.class).apply(this::onRemoveOffencesFromExistingHearingRequested),
                when(UpdateExistingHearingRequested.class).apply(this::onUpdateExistingHearingRequested),
                when(UnscheduledNextHearingRequested.class).apply(this::onUnscheduledNextHearingRequested),
                when(NextHearingReplaced.class).apply(this::onNextHearingReplaced),
                otherwiseDoNothing());
    }

    private void onNextHearingReplaced(NextHearingReplaced nextHearingReplaced) {
        currentHearingIds.add(nextHearingReplaced.getNewHearingId());

    }

    public Stream<Object> requestNextHearings(final List<HearingListingNeeds> hearingListingNeeds, final String hearingDay, final List<CourtCentreDefaults> courtCentreDefaults, final Optional<String> adjournedFromDate, final List<UUID> shadowListedOffences) {
        Stream<Object> events = empty();
        final List<HearingListingNeeds> hearingListingNeedsWithoutDupApplication = removeDuplicateApplicationFromHearing(hearingListingNeeds);

        final List<NextHearingRequested> nextHearingRequestedList =
                hearingListingNeedsWithoutDupApplication.stream()
                        .map(hearingListingNeed -> NextHearingRequested.nextHearingRequested()
                                .withHearing(hearingListingNeed)
                                .withCourtCentreDetails(convertCourtCentreDetails(courtCentreDefaults))
                                .withAdjournedFromDate((adjournedFromDate.orElse(null)))
                                .withShadowListedOffences(shadowListedOffences)
                                .withHearingDay(hearingDay)
                                .build())
                        .collect(toList());

        for (final NextHearingRequested nextHearingRequested : nextHearingRequestedList) {
            events = concat(events, Stream.of(nextHearingRequested));
        }
        if(! previousHearingIds.isEmpty()){
            for (final NextHearingRequested nextHearingRequested : nextHearingRequestedList) {
                events = concat(events, Stream.of(NextHearingReplaced.nextHearingReplaced()
                        .withNewHearingId(nextHearingRequested.getHearing().getId())
                        .withOldHearingIds(this.previousHearingIds.stream().toList())
                        .withSeedingHearingId(this.seedingHearingId)
                        .build()));
            }
        }

        return apply(events);
    }

    private List<HearingListingNeeds> removeDuplicateApplicationFromHearing(List<HearingListingNeeds> hearingListingNeeds) {
        return hearingListingNeeds.stream().map(currentHearingListingNeed -> HearingListingNeeds.hearingListingNeeds()
                .withValuesFrom(currentHearingListingNeed)
                .withCourtApplications(nonNull(currentHearingListingNeed.getCourtApplications())? currentHearingListingNeed.getCourtApplications().stream().distinct().collect(toList()) :null)
                .build()
        ).collect(toList());
    }

    public Stream<Object> requestUpdateExistingHearing(final UUID seedingHearingId, final UUID existingHearingId, final String hearingDay,  final List<ProsecutionCase> prosecutionCases, final List<UUID> shadowListedOffences) {

        return apply(of(UpdateExistingHearingRequested.updateExistingHearingRequested()
                .withHearingId(existingHearingId)
                .withProsecutionCases(prosecutionCases)
                .withShadowListedOffences(shadowListedOffences)
                .withHearingDay(hearingDay)
                .withSeedingHearingId(seedingHearingId)
                .build()));
    }

    public Stream<Object> requestNextUnscheduledHearings(final List<HearingUnscheduledListingNeeds> unscheduledListingNeeds, final String hearingDay, final List<CourtCentreDefaults> courtCentreDefaults) {

        final List<CourtCentreDetails> courtCentreDetails = convertCourtCentreDetails(courtCentreDefaults);

        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();

        for (final HearingUnscheduledListingNeeds hearing : unscheduledListingNeeds) {
            eventStreamBuilder.add(UnscheduledNextHearingRequested.unscheduledNextHearingRequested()
                    .withHearing(hearing)
                    .withHearingDay(hearingDay)
                    .withCourtCentreDetails(courtCentreDetails)
                    .build());
        }

        if(! previousHearingIds.isEmpty()){
            for (final HearingUnscheduledListingNeeds hearing : unscheduledListingNeeds) {
                eventStreamBuilder.add(NextHearingReplaced.nextHearingReplaced()
                        .withNewHearingId(hearing.getId())
                        .withOldHearingIds(this.previousHearingIds.stream().toList())
                        .withSeedingHearingId(this.seedingHearingId)
                        .build());
            }
        }

        return apply(eventStreamBuilder.build());
    }

    public Stream<Object> deleteNextHearings(final UUID seedingHearingId, final String hearingDay) {
        Stream<Object> events = empty();

        final List<DeleteNextHearingRequested> deleteNextHearingRequestedList = createDeleteNextHearingEventsForAllPreviouslySeededNextHearings(seedingHearingId, hearingDay);
        final List<RemoveOffencesFromExistingHearingRequested> removeOffencesFromExistingHearingRequestedList =
                createRemoveOffencesEventForAllExistingHearings(seedingHearingId, hearingDay);

        for (final DeleteNextHearingRequested deleteNextHearingRequested : deleteNextHearingRequestedList) {
            events = concat(events, Stream.of(deleteNextHearingRequested));
        }
        for (final RemoveOffencesFromExistingHearingRequested removeOffencesFromExistingHearingRequested : removeOffencesFromExistingHearingRequestedList) {
            events = concat(events, Stream.of(removeOffencesFromExistingHearingRequested));
        }

        return apply(events);
    }

    private List<CourtCentreDetails> convertCourtCentreDetails(final List<CourtCentreDefaults> courtCentreDefaults) {
        return CollectionUtils.isNotEmpty(courtCentreDefaults) ?
                courtCentreDefaults.stream()
                        .map(this::buildCourtCentreDetails)
                        .collect(toList()) : emptyList();
    }

    private CourtCentreDetails buildCourtCentreDetails(final CourtCentreDefaults c) {
        return CourtCentreDetails.courtCentreDetails()
                .withId(c.getCourtCentreId())
                .withDefaultDuration(c.getDefaultDuration())
                .withDefaultStartTime(c.getDefaultStartTime())
                .build();
    }

    private List<DeleteNextHearingRequested> createDeleteNextHearingEventsForAllPreviouslySeededNextHearings(final UUID seedingHearingId, final String hearingDay) {
        if (seededHearingIdsMapForHearingDay.containsKey(hearingDay)) {
            return seededHearingIdsMapForHearingDay.get(hearingDay).stream()
                    .map(hearingId -> DeleteNextHearingRequested.deleteNextHearingRequested()
                            .withHearingId(hearingId)
                            .withSeedingHearingId(seedingHearingId)
                            .withHearingDay(hearingDay)
                            .build())
                    .collect(toList());
        }
        return emptyList();
    }

    private List<RemoveOffencesFromExistingHearingRequested> createRemoveOffencesEventForAllExistingHearings(final UUID seedingHearingId, final String hearingDay) {
        if (existingHearingIdsMapForHearingDay.containsKey(hearingDay)) {
            return existingHearingIdsMapForHearingDay.get(hearingDay).stream()
                    .map(hearingId -> RemoveOffencesFromExistingHearingRequested.removeOffencesFromExistingHearingRequested()
                            .withHearingId(hearingId)
                            .withSeedingHearingId(seedingHearingId)
                            .build())
                    .collect(toList());
        }
        return emptyList();
    }

    private void onNextHearingRequested(final NextHearingRequested nextHearingRequested) {
        final String hearingDay = nextHearingRequested.getHearingDay();
        if (!seededHearingIdsMapForHearingDay.containsKey(hearingDay)) {
            seededHearingIdsMapForHearingDay.put(hearingDay, new HashSet<>());
        }
        seededHearingIdsMapForHearingDay.get(hearingDay).add(nextHearingRequested.getHearing().getId());
    }

    private void onUpdateExistingHearingRequested(final UpdateExistingHearingRequested updateExistingHearingRequested) {
        final String hearingDay = updateExistingHearingRequested.getHearingDay();
        if (!existingHearingIdsMapForHearingDay.containsKey(hearingDay)) {
            existingHearingIdsMapForHearingDay.put(hearingDay, new HashSet<>());
        }
        existingHearingIdsMapForHearingDay.get(hearingDay).add(updateExistingHearingRequested.getHearingId());
    }

    private void onDeleteNextHearingRequested(final DeleteNextHearingRequested deleteNextHearingRequested) {
        seededHearingIdsMapForHearingDay.remove(deleteNextHearingRequested.getHearingDay());
        seedingHearingId = deleteNextHearingRequested.getSeedingHearingId();
        if(currentHearingIds.contains(deleteNextHearingRequested.getHearingId())){
            previousHearingIds.clear();
            currentHearingIds.clear();
        }
        previousHearingIds.add(deleteNextHearingRequested.getHearingId());
    }

    private void onRemoveOffencesFromExistingHearingRequested(final RemoveOffencesFromExistingHearingRequested removeOffencesFromExistingHearingRequested) {
        existingHearingIdsMapForHearingDay.remove(removeOffencesFromExistingHearingRequested.getHearingId());
    }

    private void onUnscheduledNextHearingRequested(final UnscheduledNextHearingRequested unscheduledNextHearingRequested) {
        final String hearingDay = unscheduledNextHearingRequested.getHearingDay();
        if (!seededHearingIdsMapForHearingDay.containsKey(hearingDay)) {
            seededHearingIdsMapForHearingDay.put(hearingDay, new HashSet<>());
        }
        seededHearingIdsMapForHearingDay.get(hearingDay).add(unscheduledNextHearingRequested.getHearing().getId());
    }
}

