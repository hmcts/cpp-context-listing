package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.event.PublishCourtListExportFailed.publishCourtListExportFailed;
import static uk.gov.justice.listing.event.PublishCourtListExportSuccessful.publishCourtListExportSuccessful;
import static uk.gov.justice.listing.event.PublishCourtListProduced.publishCourtListProduced;
import static uk.gov.justice.listing.event.PublishCourtListRequested.publishCourtListRequested;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_PRODUCED;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_FAILED;
import static uk.gov.justice.listing.event.PublishStatus.EXPORT_SUCCESSFUL;
import static uk.gov.justice.listing.event.PublishedCourtListStored.publishedCourtListStored;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.listing.event.PublishCourtListRequested;
import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishedCourtListStored;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;


public class PublishCourtListRequestAggregate implements Aggregate {

    private static final long serialVersionUID = -7550132883773956916L;

    private UUID publishCourtListRequestId;
    private UUID courtCentreId;
    private PublishCourtListType publishCourtListType;
    private UUID courtListFileId;
    private String courtListFileName;
    private LocalDate publishDate;
    private boolean weekCommencing;

    public Stream<Object> recordCourtListRequested(final UUID publishCourtListRequestId,
                                                   final UUID courtCentreId,
                                                   final LocalDate startDate,
                                                   final LocalDate endDate,
                                                   final PublishCourtListType publishCourtListType,
                                                   final ZonedDateTime requestedTime) {

        this.publishCourtListRequestId = publishCourtListRequestId;
        this.courtCentreId = courtCentreId;
        this.publishCourtListType = publishCourtListType;

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
                .withWeekCommencing(Optional.of(weekCommencing))
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

        this.courtListFileId = courtListFileId;
        this.courtListFileName = courtListFileName;
        this.publishDate = publishDate;

        return apply(of(publishCourtListProduced()
                .withPublishCourtListRequestId(publishCourtListRequestId)
                .withCourtCentreId(courtCentreId)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withPublishCourtListType(publishCourtListType)
                .withPublishStatus(COURT_LIST_PRODUCED)
                .withWeekCommencing(Optional.of(weekCommencing))
                .withPublishDate(publishDate.toString())
                .withProducedTime(producedTime)
                .build()));
    }

    public Stream<Object> recordCourtListExportSuccessful(final ZonedDateTime publishedTime) {

        return apply(of(publishCourtListExportSuccessful()
                .withPublishCourtListRequestId(publishCourtListRequestId)
                .withCourtCentreId(courtCentreId)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withPublishCourtListType(publishCourtListType)
                .withPublishStatus(EXPORT_SUCCESSFUL)
                .withWeekCommencing(Optional.of(weekCommencing))
                .withPublishDate(publishDate.toString())
                .withPublishedTime(publishedTime)
                .build()));
    }

    public Stream<Object> recordCourtListExportFailed(final ZonedDateTime failedTime,
                                                      final String errorMessage) {
        return apply(of(publishCourtListExportFailed()
                .withPublishCourtListRequestId(publishCourtListRequestId)
                .withCourtCentreId(courtCentreId)
                .withCourtListFileId(Optional.ofNullable(courtListFileId))
                .withCourtListFileName(Optional.ofNullable(courtListFileName))
                .withPublishCourtListType(publishCourtListType)
                .withPublishStatus(EXPORT_FAILED)
                .withFailedTime(failedTime)
                .withErrorMessage(errorMessage)
                .withWeekCommencing(Optional.ofNullable(weekCommencing))
                .build()));
    }

    public Stream<Object> storePublishedCourtList(final UUID courtCentreId,
                                                  final PublishCourtListType publishCourtListType,
                                                  final LocalDate startDate,
                                                  final String courtListJson) {

        return apply(of(publishedCourtListStored()
                .withCourtCentreId(courtCentreId)
                .withPublishCourtListType(publishCourtListType)
                .withStartDate(startDate)
                .withCourtListJson(courtListJson)
                .build()));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(PublishCourtListRequested.class).apply(this::recordCourtListRequested),
                when(PublishCourtListProduced.class).apply(this::recordCourtListProduced),
                when(PublishCourtListExportSuccessful.class).apply(c -> doNothing()),
                when(PublishCourtListExportFailed.class).apply(c -> doNothing()),
                when(PublishedCourtListStored.class).apply(c -> doNothing())
        );
    }

    private void recordCourtListRequested(final PublishCourtListRequested publishCourtListRequested) {
        this.publishCourtListRequestId = publishCourtListRequested.getPublishCourtListRequestId();
        this.courtCentreId = publishCourtListRequested.getCourtCentreId();
        this.publishCourtListType = publishCourtListRequested.getPublishCourtListType();
        this.weekCommencing = publishCourtListRequested.getWeekCommencing().orElse(false);
    }

    private void recordCourtListProduced(final PublishCourtListProduced publishCourtListProduced) {
        this.courtListFileId = publishCourtListProduced.getCourtListFileId();
        this.courtListFileName = publishCourtListProduced.getCourtListFileName();
        this.publishDate = LocalDate.parse(publishCourtListProduced.getPublishDate());
    }
}
