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

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public class CourtListAggregate implements Aggregate {

    private UUID documentId;


    public Stream<Object> recordCourtListRequested(final UUID documentId) {
        return apply(of(publishCourtListRequested()
                .withDocumentId(documentId).build()));
    }

    public Stream<Object> recordCourtListProduced(final String documentName) {
        return apply(of(publishCourtListProduced()
                .withDocumentId(documentId)
                .withDocumentName(documentName).build()));
    }

    public Stream<Object> recordCourtListExportSuccessful(final String documentName,
                                                          final ZonedDateTime publishedTime) {
        return apply(of(publishCourtListExportSuccessful()
                .withDocumentId(documentId)
                .withDocumentName(documentName)
                .withPublishedTime(publishedTime).build()));
    }

    public Stream<Object> recordCourtListExportFailed(final ZonedDateTime publishFailedTime,
                                                      final String documentName,
                                                      final String errorMessage) {
        return apply(of(publishCourtListExportFailed()
                .withDocumentId(documentId)
                .withDocumentName(documentName)
                .withErrorMessage(errorMessage)
                .withFailedTime(publishFailedTime).build()
        ));
    }

    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(PublishCourtListRequested.class).apply(this::recordCourtListRequested),
                when(PublishCourtListProduced.class).apply(this::recordCourtListProduced),
                when(PublishCourtListExportSuccessful.class).apply(c -> doNothing()),
                when(PublishCourtListExportFailed.class).apply(c -> doNothing())
        );
    }


    private void recordCourtListRequested(final PublishCourtListRequested publishCourtListRequested) {
        this.documentId = publishCourtListRequested.getDocumentId();
    }

    private void recordCourtListProduced(final PublishCourtListProduced publishCourtListProduced) {
        this.documentId = publishCourtListProduced.getDocumentId();
    }

    public UUID getDocumentId() {
        return documentId;
    }

}