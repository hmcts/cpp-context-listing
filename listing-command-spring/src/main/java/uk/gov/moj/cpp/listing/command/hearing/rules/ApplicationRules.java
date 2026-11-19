package uk.gov.moj.cpp.listing.command.hearing.rules;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.listing.command.hearing.rules.EventSwitch.match;
import static uk.gov.moj.cpp.listing.command.hearing.rules.EventSwitch.otherwiseDoNothing;
import static uk.gov.moj.cpp.listing.command.hearing.rules.EventSwitch.when;

import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.listing.events.ApplicationEjectedForHearings;
import uk.gov.justice.listing.events.CourtApplicationAddedToHearing;
import uk.gov.justice.listing.events.CourtApplicationToBeUpdated;
import uk.gov.justice.listing.events.LaaReferenceForApplicationUpdated;
import uk.gov.justice.listing.events.NoHearingFoundForCourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The JEE {@code Application} aggregate's rules, framework-free (MBD refactor P2.3.3, ADR 015),
 * ported verbatim. Its one field, {@code hearingIds} (a list: adding the application to the same
 * hearing twice keeps both entries, as the JEE aggregate did), is kept in
 * {@code application_rules_state} ({@link ApplicationRulesState}).
 */
public class ApplicationRules {

    private UUID applicationId;
    private final List<UUID> hearingIds = new ArrayList<>();

    public ApplicationRules() {
    }

    public ApplicationRules(final UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public List<UUID> getHearingIds() {
        return List.copyOf(hearingIds);
    }

    public Stream<Object> apply(final Stream<Object> events) {
        return events.map(this::apply).collect(toList()).stream();
    }

    public Object apply(final Object event) {
        return match(event).with(
                when(CourtApplicationAddedToHearing.class).apply(this::onCourtApplicationAddedToHearing),
                when(CourtApplicationToBeUpdated.class).apply(e -> onCourtApplicationToBeUpdated()),
                when(ApplicationEjectedForHearings.class).apply(e -> onApplicationEjectedForHearings()),
                otherwiseDoNothing());
    }

    private void onCourtApplicationAddedToHearing(final CourtApplicationAddedToHearing applicationAddedToHearing) {
        hearingIds.add(applicationAddedToHearing.getHearingId());
    }

    public Stream<Object> addToHearing(final UUID applicationId, final UUID hearingId) {
        return apply(Stream.of(new CourtApplicationAddedToHearing(applicationId, hearingId)));
    }

    public Stream<Object> update(final CourtApplication courtApplication) {
        return hearingIds.isEmpty() ? apply(Stream.of(new NoHearingFoundForCourtApplication(NewDomainToEventConverter.buildCourtApplications(courtApplication))))
                : apply(Stream.of(new CourtApplicationToBeUpdated(NewDomainToEventConverter.buildCourtApplications(courtApplication), hearingIds)));
    }

    public Stream<Object> updateLaaReference(final UUID applicationId, final UUID subjectId, final UUID offenceId, final LaaReference laaReference) {
        if (isEmpty(hearingIds)) {
            return empty();
        }

        return apply(Stream.of(
                LaaReferenceForApplicationUpdated.laaReferenceForApplicationUpdated()
                        .withApplicationId(applicationId)
                        .withOffenceId(offenceId)
                        .withSubjectId(subjectId)
                        .withLaaReference(laaReference)
                        .withHearingIds(this.hearingIds)
                        .build()
        ));
    }

    public Stream<Object> ejectApplicationForHearings(final List<UUID> hearingIdForApplicationToBeEjected, final UUID applicationId,
                                                      final Optional<String> removalReason) {

        final Set<UUID> mergedHearingIds = new HashSet<>(this.hearingIds);
        if (Objects.nonNull(hearingIdForApplicationToBeEjected)) {
            if (!hearingIds.isEmpty()) {
                hearingIdForApplicationToBeEjected.retainAll(hearingIds);
            }
            mergedHearingIds.addAll(hearingIdForApplicationToBeEjected);
        }
        final String ejectReason = removalReason.isPresent() ? removalReason.get() : null;

        return mergedHearingIds.isEmpty() ? Stream.empty() : apply(Stream.of(ApplicationEjectedForHearings.applicationEjectedForHearings()
                .withApplicationId(applicationId)
                .withHearingIds(new ArrayList<>(mergedHearingIds))
                .withRemovalReason(ejectReason)
                .build())
        );
    }

    /** The state to keep, or {@code null} for an application nothing was decided on. */
    public ApplicationRulesState exportState() {
        return applicationId == null ? null : new ApplicationRulesState(applicationId, new ArrayList<>(hearingIds));
    }

    /** Restores the kept state (the JEE aggregate rebuilt from its stream). */
    public void restore(final ApplicationRulesState state) {
        this.applicationId = state.applicationId();
        this.hearingIds.clear();
        if (state.hearingIds() != null) {
            this.hearingIds.addAll(state.hearingIds());
        }
    }

    private void onCourtApplicationToBeUpdated() {
        //Do nothing
    }

    private void onApplicationEjectedForHearings() {
        //Do nothing
    }
}
