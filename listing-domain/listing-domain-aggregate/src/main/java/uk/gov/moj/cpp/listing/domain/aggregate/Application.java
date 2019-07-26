package uk.gov.moj.cpp.listing.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.CourtApplicationAddedToHearing;
import uk.gov.justice.listing.events.CourtApplicationToBeUpdated;
import uk.gov.justice.listing.events.NoHearingFoundForCourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class Application implements Aggregate {
    private final List<UUID> hearingIds = new ArrayList<>();
    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(CourtApplicationAddedToHearing.class).apply(this::onCourtApplicationAddedToHearing),
                when(CourtApplicationToBeUpdated.class).apply(e-> onCourtApplicationToBeUpdated()),
                otherwiseDoNothing());
    }

    private void onCourtApplicationAddedToHearing(CourtApplicationAddedToHearing applicationAddedToHearing) {
        hearingIds.add(applicationAddedToHearing.getHearingId());
    }

    public Stream<Object> addToHearing(final UUID applicationId, final UUID hearingId) {
        return apply(Stream.of(new CourtApplicationAddedToHearing(applicationId, hearingId)));
    }
    public Stream<Object> update(final CourtApplication courtApplication){
        return hearingIds.isEmpty() ? apply(Stream.of(new NoHearingFoundForCourtApplication(NewDomainToEventConverter.buildCourtApplications(courtApplication)))) : apply(Stream.of(new CourtApplicationToBeUpdated(NewDomainToEventConverter.buildCourtApplications(courtApplication), hearingIds)));
    }
    private void onCourtApplicationToBeUpdated() {
        //Do nothing
    }

}
