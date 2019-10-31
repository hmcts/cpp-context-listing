package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.event.PublishCourtListExportFailed.publishCourtListExportFailed;
import static uk.gov.justice.listing.event.PublishCourtListExportSuccessful.publishCourtListExportSuccessful;
import static uk.gov.justice.listing.event.PublishCourtListProduced.publishCourtListProduced;
import static uk.gov.justice.listing.event.PublishCourtListRequested.publishCourtListRequested;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.event.PublishCourtListExportFailed;
import uk.gov.justice.listing.event.PublishCourtListExportSuccessful;
import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.listing.event.PublishCourtListRequested;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;


public class CourtListAggregate implements Aggregate {

    private UUID courtCentreId;


    public Stream<Object> recordCourtListRequested(final UUID courtCentreId,
                                                   final LocalDate startDate,
                                                   final LocalDate endDate,
                                                   final String courtListType,
                                                   final ZonedDateTime requestedTime) {
        return apply(of(publishCourtListRequested()
                .withCourtCentreId(courtCentreId)
                .withCourtListType(courtListType)
                .withStartDate(startDate.toString())
                .withEndDate(endDate.toString())
                .withRequestedTime(requestedTime)
                .build()));
    }

    public Stream<Object> recordCourtListProduced(final UUID courtCentreId,
                                                  final UUID courtListFileId,
                                                  final String courtListFileName,
                                                  final String courtListType,
                                                  final ZonedDateTime producedTime) {
        return apply(of(publishCourtListProduced()
                .withCourtCentreId(courtCentreId)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withCourtListType(courtListType)
                .withProducedTime(producedTime).build()));
    }

    public Stream<Object> recordCourtListExportSuccessful(final UUID courtCentreId,
                                                          final UUID courtListFileId,
                                                          final String courtListFileName,
                                                          final String courtListType,
                                                          final ZonedDateTime publishedTime) {
        return apply(of(publishCourtListExportSuccessful()
                .withCourtCentreId(courtCentreId)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withCourtListType(courtListType)
                .withPublishedTime(publishedTime).build()));
    }

    public Stream<Object> recordCourtListExportFailed(final UUID courtCentreId,
                                                      final UUID courtListFileId,
                                                      final String courtListFileName,
                                                      final String courtListType,
                                                      final ZonedDateTime failedTime,
                                                      final String errorMessage) {
        return apply(of(publishCourtListExportFailed()
                .withCourtCentreId(courtCentreId)
                .withCourtListFileId(courtListFileId)
                .withCourtListFileName(courtListFileName)
                .withCourtListType(courtListType)
                .withErrorMessage(errorMessage)
                .withFailedTime(failedTime).build()));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(PublishCourtListRequested.class).apply(this::recordCourtListRequested),
                when(PublishCourtListProduced.class).apply(this::recordCourtListProduced),
                when(PublishCourtListExportSuccessful.class).apply(c -> doNothing()),
                when(PublishCourtListExportFailed.class).apply(c -> doNothing())
        );
    }


    private void recordCourtListRequested(final PublishCourtListRequested publishCourtListRequested) {
        this.courtCentreId = publishCourtListRequested.getCourtCentreId();
    }

    private void recordCourtListProduced(final PublishCourtListProduced publishCourtListProduced) {
        this.courtCentreId = publishCourtListProduced.getCourtCentreId();
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

}