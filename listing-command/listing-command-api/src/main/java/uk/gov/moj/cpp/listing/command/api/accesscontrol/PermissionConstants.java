package uk.gov.moj.cpp.listing.command.api.accesscontrol;

import static uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission.builder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonPropertyOrder({"object", "action", "key", "keyWithOutSource"})
public final class PermissionConstants {

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private static final String CHANGE_HEARING_TO_PAST_DATE_OBJECT = "Change hearing to past date";
    private static final String LINK_ACTION = "Link";

    private PermissionConstants() {
    }

    public static String createChangeHearingToPastDatePermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject(CHANGE_HEARING_TO_PAST_DATE_OBJECT)
                .withAction(LINK_ACTION)
                .build();

        return objectMapper.writeValueAsString(expectedPermission);
    }

}
