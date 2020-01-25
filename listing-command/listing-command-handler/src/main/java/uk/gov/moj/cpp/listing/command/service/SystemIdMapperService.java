package uk.gov.moj.cpp.listing.command.service;

import static java.util.UUID.randomUUID;

import uk.gov.justice.listing.commands.PublishCourtListType;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.ResultCode;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SystemIdMapperService {

    private static final String SOURCE_TYPE = "COURT_LIST_PK";

    private static final String TARGET_TYPE = "COURT_LIST_ID";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public UUID getCourtListId(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {

        final String courtListPrimaryKey = String.format("%s/%s/%s", courtCentreId.toString(), publishCourtListType.name(), startDate.toString());

        final Optional<SystemIdMapping> mapping = getSystemIdMappingFor(courtListPrimaryKey);
        if (mapping.isPresent()) {
            return mapping.get().getTargetId();
        }

        final UUID newCourtListId = randomUUID();
        final AdditionResponse additionResponse = attemptAddMappingForCourtList(newCourtListId, courtListPrimaryKey);
        if (additionResponse.isSuccess()) {
            return newCourtListId;
        }

        return getSystemIdMappingFor(courtListPrimaryKey)
                .orElseThrow(() -> new IllegalStateException("Error generating court list id"))
                .getTargetId();
    }

    private Optional<SystemIdMapping> getSystemIdMappingFor(final String sourceId) {
        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();
        if (contextSystemUserId.isPresent()) {

            return systemIdMapperClient.findBy(sourceId, SOURCE_TYPE, TARGET_TYPE, contextSystemUserId.get());
        }
        return Optional.empty();
    }

    private AdditionResponse attemptAddMappingForCourtList(final UUID courtListId, final String courtListPrimaryKey) {
        final SystemIdMap systemIdMap = new SystemIdMap(courtListPrimaryKey, SOURCE_TYPE, courtListId, TARGET_TYPE);

        final Optional<UUID> contextSystemUserId = systemUserProvider.getContextSystemUserId();

        if (contextSystemUserId.isPresent()) {
            return systemIdMapperClient.add(systemIdMap, contextSystemUserId.get());
        }

        return new AdditionResponse(courtListId, ResultCode.CONFLICT, Optional.of("system id mapper service failed to add court list mapping"));
    }
}
