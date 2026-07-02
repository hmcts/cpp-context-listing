package uk.gov.moj.cpp.listing.command.api.accesscontrol;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.command.api.accesscontrol.PermissionConstants.createChangeHearingToPastDatePermission;
import static uk.gov.moj.cpp.listing.command.api.util.FileUtil.getPayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PermissionConstantsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCreateChangeHearingToPastDatePermission() throws JsonProcessingException {
        JsonNode actual = mapper.readTree(createChangeHearingToPastDatePermission());
        JsonNode expected = mapper.readTree(getPayload("create-change-hearing-to-past-date-permission.json"));
        assertThat(actual, is(expected));
    }
}
