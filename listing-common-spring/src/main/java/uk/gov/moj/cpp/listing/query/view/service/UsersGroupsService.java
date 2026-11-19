package uk.gov.moj.cpp.listing.query.view.service;

import java.util.UUID;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;
import uk.gov.moj.cpp.listing.domain.json.JsonObjects;

import lombok.RequiredArgsConstructor;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

import uk.gov.moj.cpp.listing.query.view.dto.Permission;
import uk.gov.moj.cpp.listing.query.view.dto.PermissionList;

import java.util.List;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.domain.json.JsonObjects;
import jakarta.json.JsonObject;


@Component
@RequiredArgsConstructor
public class UsersGroupsService {

    public static final String ACTION = "action";
    private static final String ACCESS_TO_STANDALONE_APPLICATION = "Access to Standalone Application";

    private final CppQueryClient cppQueryClient;


    public List<Permission> getUserPermissionForApplicationTypes(final UUID userId) {
        final JsonObject getOrganisationForUserRequest = JsonObjects.createObjectBuilder()
                .add(ACTION, ACCESS_TO_STANDALONE_APPLICATION)
                .build();
        final PermissionList response = cppQueryClient.request("usersgroups.is-logged-in-user-has-permission-for-action",
                getOrganisationForUserRequest, userId, PermissionList.class).orElse(null);
        if (isNull(response) || isNull(response.getPermissions())) {
            return emptyList();
        }
        return response.getPermissions();


    }

}

