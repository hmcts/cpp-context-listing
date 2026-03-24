package uk.gov.justice.api.resource;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import static uk.gov.justice.api.resource.SessionAvailabilityValidationQueryParamConstants.*;

@Adapter(Component.QUERY_API)
public class DefaultQueryApiSessionAvailabilityValidationResource implements QueryApiSessionAvailabilityValidationResource {

    @Inject
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Override
    public Response validateSessionAvailability(final JsonObject requestPayload) {
        if (requestPayload == null || requestPayload.isEmpty()) {
            return badRequest("Request payload is required");
        }

        final SessionAvailabilityValidationParams validationParams = new SessionAvailabilityValidationParams(
                requestPayload.getJsonArray(COURT_SCHEDULE_ID_LIST),
                optionalInt(requestPayload, DURATION),
                optionalInt(requestPayload, CONSECUTIVE_DAYS)
        );

        if (validationParams.courtScheduleIdList() == null || validationParams.courtScheduleIdList().isEmpty()) {
            return badRequest("courtScheduleIdList must contain at least one element");
        }

        final JsonObject downStreamRequest;
        if (validationParams.consecutiveDays() != null) {
            if (validationParams.courtScheduleIdList().size() != 1) {
                return badRequest("When consecutiveDays is provided, courtScheduleIdList must contain exactly one element");
            }
            final String courtScheduleId = getCourtScheduleId(validationParams.courtScheduleIdList().get(0));
            if (courtScheduleId == null || courtScheduleId.isBlank()) {
                return badRequest("courtScheduleIdList[0].courtScheduleId is required");
            }
            downStreamRequest = Json.createObjectBuilder()
                    .add(COURT_SCHEDULE_ID, courtScheduleId)
                    .add(CONSECUTIVE_DAYS, validationParams.consecutiveDays())
                    .build();
        } else {
            if (validationParams.duration() == null) {
                return badRequest("duration must be provided when consecutiveDays is not supplied");
            }
            downStreamRequest = Json.createObjectBuilder()
                    .add(COURT_SCHEDULE_ID_LIST, validationParams.courtScheduleIdList())
                    .add(DURATION, validationParams.duration())
                    .build();
        }
        return courtSchedulerServiceAdapter.validateSessionAvailability(downStreamRequest);
    }

    private static Integer optionalInt(final JsonObject jsonObject, final String key) {
        return jsonObject.containsKey(key) && !jsonObject.isNull(key) ? jsonObject.getInt(key) : null;
    }

    private static String getCourtScheduleId(final JsonValue jsonValue) {
        if (!(jsonValue instanceof JsonObject jsonObject) || !jsonObject.containsKey(COURT_SCHEDULE_ID) || jsonObject.isNull(COURT_SCHEDULE_ID)) {
            return null;
        }
        return jsonObject.getString(COURT_SCHEDULE_ID, null);
    }

    private static Response badRequest(final String message) {
        final JsonObject entity = Json.createObjectBuilder()
                .add(VALIDATION_RESULT, Json.createObjectBuilder()
                        .add(VALIDATION_STATUS, FAILURE)
                        .add(VALIDATION_ERROR, message))
                .build();
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }
}
