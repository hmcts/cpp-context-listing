package uk.gov.moj.cpp.listing.query.view.service;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.listing.query.view.dto.Permission;
import uk.gov.moj.cpp.listing.query.view.dto.PermissionList;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;


@ApplicationScoped
public class UsersGroupsService {

    public static final String ACTION = "action";
    private static final String ACCESS_TO_STANDALONE_APPLICATION = "Access to Standalone Application";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;


    public List<Permission> getUserPermissionForApplicationTypes(final Metadata metadata) {
        final JsonObject getOrganisationForUserRequest = JsonObjects.createObjectBuilder()
                .add(ACTION, ACCESS_TO_STANDALONE_APPLICATION)
                .build();
        final MetadataBuilder metadataWithActionName = Envelope.metadataFrom(metadata).withName("usersgroups.is-logged-in-user-has-permission-for-action");

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, getOrganisationForUserRequest);
        final Envelope<PermissionList> response = requester.request(requestEnvelope, PermissionList.class);
        if (isNull(response.payload()) || isNull(response.payload().getPermissions())) {
            return emptyList();
        }
        return response.payload().getPermissions();


    }

}

