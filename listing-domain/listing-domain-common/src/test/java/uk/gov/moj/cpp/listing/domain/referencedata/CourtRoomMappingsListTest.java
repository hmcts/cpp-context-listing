package uk.gov.moj.cpp.listing.domain.referencedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import org.junit.jupiter.api.Test;

class CourtRoomMappingsListTest {

    @Test
    void shouldDeserializeReferenceDataCourtRoomMappingsPayload() throws Exception {
        final String json = """
                {
                  "cpXhibitCourtRoomMappings": [
                    {
                      "id": "1d0199f8-8812-48a2-b13c-837e1c03ff19",
                      "oucode": "C14DN00",
                      "courtRoomId": 852,
                      "crestCourtId": "420",
                      "crestCourtSiteId": "420",
                      "crestCourtSiteCode": "C",
                      "crestCourtRoomName": "Court 2",
                      "courtType": "CROWN_COURT",
                      "courtRoomUUID": "46b45533-375f-36c0-83e9-8b806855077b",
                      "crestCourtSiteName": "DONCASTER",
                      "crestCourtFullName": "DONCASTER",
                      "crestCourtShortName": "DONCA",
                      "crestCourtSiteUUID": "6c5f4e7f-a684-33e9-8ef9-7493e033ea87"
                    }
                  ]
                }
                """;

        final CourtRoomMappingsList courtRoomMappingsList = new ObjectMapperProducer().objectMapper()
                .readValue(json, CourtRoomMappingsList.class);

        assertNotNull(courtRoomMappingsList);
        assertEquals(1, courtRoomMappingsList.getCpXhibitCourtRoomMappings().size());
        assertEquals("Court 2", courtRoomMappingsList.getCpXhibitCourtRoomMappings().get(0).getCrestCourtRoomName());
        assertEquals("46b45533-375f-36c0-83e9-8b806855077b",
                courtRoomMappingsList.getCpXhibitCourtRoomMappings().get(0).getCourtRoomUUID().toString());
    }
}
