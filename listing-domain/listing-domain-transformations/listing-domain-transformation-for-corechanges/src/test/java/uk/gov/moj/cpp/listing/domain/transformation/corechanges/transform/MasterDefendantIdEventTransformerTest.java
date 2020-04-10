package uk.gov.moj.cpp.listing.domain.transformation.corechanges.transform;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_DEFENDANTS_TO_BE_UPDATED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_HEARING_LISTED;
import static uk.gov.moj.cpp.listing.domain.transformation.corechanges.ListingEventTransform.EVENT_NEW_DEFENDANT_DETAILS_UPDATED;

import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MasterDefendantIdEventTransformerTest {


    private String file;
    private String eventName;

    public MasterDefendantIdEventTransformerTest(final String file, final String eventName) {
        this.file = file;
        this.eventName = eventName;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"listing.events.defendants-to-be-updated.json", EVENT_DEFENDANTS_TO_BE_UPDATED},
                {"listing.events.hearing-listed.json", EVENT_HEARING_LISTED},
                {"listing.events.new-defendant-details-updated.json", EVENT_NEW_DEFENDANT_DETAILS_UPDATED},
                {"listing.events.case-resulted-defendant-proceedings-updated.json", EVENT_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED},
                {"listing.events.case-update-defendant-proceedings-updated.json", EVENT_CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED},
        });
    }

    @Test
    public void transform() {
        JsonObject oldJsonObject = loadTestFile("master-defendant/old/" + file);
        JsonObject expectedJsonObject = loadTestFile("master-defendant/new/" + file);
        JsonObject resultJsonObject = new MasterDefendantTransformer().transform(
                DefaultJsonMetadata.metadataBuilder().withName(eventName).withId(randomUUID()).build(), oldJsonObject);
        assertThat(expectedJsonObject.toString(), equalTo(resultJsonObject.toString()));
    }

    private JsonObject loadTestFile(String resourceFileName) {
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFileName);
            final JsonReader jsonReader = Json.createReader(is);
            return jsonReader.readObject();

        } catch (Exception ex) {
            throw new RuntimeException("failed to load test file " + resourceFileName, ex);
        }
    }
}
