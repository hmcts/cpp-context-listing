package uk.gov.moj.cpp.listing.command.service;

import static java.util.UUID.nameUUIDFromBytes;

import uk.gov.justice.listing.commands.PublishCourtListType;

import java.time.LocalDate;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UUIDService {

    public UUID getCourtListId(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {

        final String courtCentreIdStr = courtCentreId.toString();
        final String publishCourtListTypeName = publishCourtListType.name();
        final String startDateStr = startDate.toString();

        final String courtListPrimaryKey = String.format("%s/%s/%s", courtCentreIdStr, publishCourtListTypeName, startDateStr);

        return nameUUIDFromBytes(courtListPrimaryKey.getBytes());
    }
}
