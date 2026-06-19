package uk.gov.moj.cpp.listing.query.view.hearing;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.query.view.dto.ApplicationType;
import uk.gov.moj.cpp.listing.query.view.dto.Permission;
import uk.gov.moj.cpp.listing.query.view.service.UsersGroupsService;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class ApplicationTypeFilter {

    @Inject
    private UsersGroupsService usersGroupsService;


    public List<Hearing> filter(final Metadata queryMetadata, final List<Hearing> hearings) {

        if (notRequireFiltering(hearings)) {
            return hearings;
        }

        final List<Permission> permissions = usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata);

        //If no permission found, don't apply any filtering. (ex: systemUsers)
        if (isEmpty(permissions)) {
            return hearings;
        }

        return hearings.stream()
                .filter(hearing -> hearing.getCourtApplications().stream()
                        .allMatch(application -> permissions.stream()
                                .filter(permission -> ApplicationType.getApplicationTypeTitle(permission.getObject()).equals(application.getApplicationType()))
                                .findFirst()
                                .map(Permission::isHasPermission)
                                .orElse(true))  // If no matching permission found, assume no restriction for the applicationType
                )
                .collect(Collectors.toList());
    }

    public JsonArray filter(final Metadata queryMetadata, final JsonArray hearingsArray) {

        if (notRequireFiltering(hearingsArray)) {
            return hearingsArray;
        }

        final List<Permission> permissions = usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata);

        //If no permission found, don't apply any filtering. (ex: systemUsers)
        if (isEmpty(permissions)) {
            return hearingsArray;
        }

        return hearingsArray.stream()
                .map(JsonObject.class::cast)
                .filter(hearing -> hearing.getJsonArray("courtApplications").stream()
                        .map(JsonObject.class::cast)
                        .allMatch(application -> permissions.stream()
                                .filter(permission -> ApplicationType.getApplicationTypeTitle(permission.getObject())
                                        .equals(application.getString("applicationType")))
                                .findFirst()
                                .map(Permission::isHasPermission)
                                .orElse(true) // No restriction if no permission found
                        )
                )
                .collect(JsonObjects::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();
    }

    private boolean notRequireFiltering(final List<Hearing> hearings) {
        if (isEmpty(hearings)) {
            return true;
        }
        return hearings.stream().allMatch(hearing -> isEmpty(hearing.getCourtApplications()));
    }

    private boolean notRequireFiltering(final JsonArray hearingsArray) {
        if (isEmpty(hearingsArray)) {
            return true;
        }
        return hearingsArray.stream().map(JsonObject.class::cast).noneMatch(jsonObject -> jsonObject.containsKey("courtApplications"));
    }



}
