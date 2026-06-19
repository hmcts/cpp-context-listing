package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.stream.Stream.empty;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.domain.aggregate.Aggregate;
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

public class Application implements Aggregate {

    private static final long serialVersionUID = 201L;
    private final List<UUID> hearingIds = new ArrayList<>();

    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(CourtApplicationAddedToHearing.class).apply(this::onCourtApplicationAddedToHearing),
                when(CourtApplicationToBeUpdated.class).apply(e -> onCourtApplicationToBeUpdated()),
                when(ApplicationEjectedForHearings.class).apply(e -> onApplicationEjectedForHearings()),
                otherwiseDoNothing());
    }

    private void onCourtApplicationAddedToHearing(CourtApplicationAddedToHearing applicationAddedToHearing) {
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

    public Stream<Object> ejectApplicationForHearings
            (List<UUID> hearingIdForApplicationToBeEjected, UUID
                    applicationId, Optional<String> removalReason) {

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

    private void onCourtApplicationToBeUpdated() {
        //Do nothing
    }

    private void onApplicationEjectedForHearings() {
        //Do nothing
    }


}
