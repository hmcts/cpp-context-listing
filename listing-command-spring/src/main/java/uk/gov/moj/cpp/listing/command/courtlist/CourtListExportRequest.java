package uk.gov.moj.cpp.listing.command.courtlist;

import uk.gov.justice.listing.event.PublishCourtListType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * What the JEE {@code court-list-export-requested} private event carried (MBD refactor P2.3.1,
 * ADR 015): the input of the export step, handed to {@link CourtListProcessor} after commit.
 *
 * @param courtListId               the deterministic court-list id ({@link CourtListCommands#courtListId})
 * @param courtListJson             the court-list JSON as a string, as sent to the command
 * @param endDate                   may be null (a REST export without one)
 * @param requestedTime             when the export was requested
 * @param sendNotificationToParties may be null (a REST export without one)
 */
public record CourtListExportRequest(UUID courtCentreId,
                                     UUID courtListId,
                                     String courtListJson,
                                     LocalDate endDate,
                                     PublishCourtListType publishCourtListType,
                                     ZonedDateTime requestedTime,
                                     Boolean sendNotificationToParties,
                                     LocalDate startDate) {
}
