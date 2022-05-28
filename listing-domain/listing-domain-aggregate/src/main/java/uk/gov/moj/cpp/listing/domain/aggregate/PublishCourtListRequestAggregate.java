package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.event.PublishCourtListProduced.publishCourtListProduced;
import static uk.gov.justice.listing.event.PublishCourtListRequested.publishCourtListRequested;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_PRODUCED;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.event.CourtListExportRequested;
import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.listing.event.PublishCourtListRequested;
import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishedCourtListStored;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;


public class PublishCourtListRequestAggregate implements Aggregate {

    private static final long serialVersionUID = -7550132883773956916L;


    private boolean weekCommencing;

    public Stream<Object> recordCourtListRequested(final UUID publishCourtListRequestId,
                                                   final UUID courtCentreId,
                                                   final LocalDate startDate,
                                                   final LocalDate endDate,
                                                   final PublishCourtListType publishCourtListType,
                                                   final ZonedDateTime requestedTime) {


        if (!(startDate.equals(endDate))) {
            this.weekCommencing = true;
        }

        return apply(of(publishCourtListRequested()
                .withPublishCourtListRequestId(publishCourtListRequestId)
                .withCourtCentreId(courtCentreId)
                .withPublishCourtListType(publishCourtListType)
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .withPublishStatus(COURT_LIST_REQUESTED)
                .withWeekCommencing(weekCommencing)
                .withRequestedTime(requestedTime)
                .build()));
    }

    public Stream<Object> recordCourtListProduced(final UUID publishCourtListRequestId,
                                                  final UUID courtCentreId,
                                                  final UUID courtListFileId,
                                                  final String courtListFileName,
                                                  final PublishCourtListType publishCourtListType,
                                                  final ZonedDateTime producedTime,
                                                  final LocalDate publishDate) {


        return apply(of(publishCourtListProduced()
                .withPublishCourtListRequestId(publishCourtListRequestId)
                .withCourtCentreId(courtCentreId)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withPublishCourtListType(publishCourtListType)
                .withPublishStatus(COURT_LIST_PRODUCED)
                .withWeekCommencing(weekCommencing)
                .withPublishDate(publishDate.toString())
                .withProducedTime(producedTime)
                .build()));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(PublishCourtListRequested.class).apply(this::recordCourtListRequested),
                when(PublishCourtListProduced.class).apply(c -> doNothing()),
                when(PublishCourtListExportSuccessful.class).apply(c -> doNothing()),
                when(PublishCourtListExportFailed.class).apply(c -> doNothing()),
                when(PublishedCourtListStored.class).apply(c -> doNothing()),
                when(CourtListExportRequested.class).apply(c -> doNothing())
        );
    }

    private void recordCourtListRequested(final PublishCourtListRequested publishCourtListRequested) {
        this.weekCommencing = publishCourtListRequested.getWeekCommencing();
    }
}

