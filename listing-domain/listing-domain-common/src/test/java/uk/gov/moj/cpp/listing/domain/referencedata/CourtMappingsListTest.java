package uk.gov.moj.cpp.listing.domain.referencedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import org.junit.jupiter.api.Test;

class CourtMappingsListTest {

    @Test
    void shouldDeserializeReferenceDataCourtMappingsPayload() throws Exception {
        final String json = """
                {
                  "cpXhibitCourtMappings": [
                    {
                      "id": "0a43a1dd-9ba7-33d9-a306-554784e5b116",
                      "oucode": "C04PR00",
                      "crestCourtId": "448",
                      "crestCourtSiteId": "448",
                      "crestCourtSiteName": "PRESTON",
                      "crestCourtName": "PRESTON",
                      "crestCourtShortName": "PREST",
                      "crestCourtFullName": "PRESTON",
                      "crestCourtSiteCode": "C",
                      "courtType": "CROWN_COURT"
                    }
                  ]
                }
                """;

        final CourtMappingsList courtMappingsList = new ObjectMapperProducer().objectMapper()
                .readValue(json, CourtMappingsList.class);

        assertNotNull(courtMappingsList);
        assertEquals(1, courtMappingsList.getCpXhibitCourtMappings().size());
        assertEquals("CROWN_COURT", courtMappingsList.getCpXhibitCourtMappings().get(0).getCourtType());
    }
}
