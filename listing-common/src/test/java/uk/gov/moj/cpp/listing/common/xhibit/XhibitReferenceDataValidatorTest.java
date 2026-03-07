package uk.gov.moj.cpp.listing.common.xhibit;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class XhibitReferenceDataValidatorTest {

    private XhibitReferenceDataValidator xhibitReferenceDataValidator = new XhibitReferenceDataValidator();

    // Warning - this suppression of warnings is solely to get sonar to pass so the junit5
    // upgrade can be merged. The @SuppressWarnings needs to be removed and the test refactored
    @SuppressWarnings("java:S5778")
    @Test
    public void shouldThrowErrorWhenValidatePayload() {


        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("firstName", "Joe")
                .add("lastName", "")
                .build();

        final InvalidReferenceDataException invalidReferenceDataException = assertThrows(
                InvalidReferenceDataException.class,
                () -> xhibitReferenceDataValidator.validate("lastName", payload.getString("lastName"), payload));

        assertThat(invalidReferenceDataException.getMessage(), is("Invalid value '' for 'lastName' in '{\"firstName\":\"Joe\",\"lastName\":\"\"}'"));
    }


    @Test
    public void shouldThrowErrorWhenValidate() {
        assertThrows(InvalidReferenceDataException.class, () -> xhibitReferenceDataValidator.validate("lastName", ""));
    }


    @Test
    public void shouldValidateWhenDataExist() {


        xhibitReferenceDataValidator.validate("lastName", "Smith");
    }


    @Test
    public void shouldThrowErrorWhenValidateJsonArrayIsEmpty() {
        assertThrows(InvalidReferenceDataException.class, () -> xhibitReferenceDataValidator.validateJsonArray("lastName", JsonObjects.createArrayBuilder().build()));
    }

    @Test
    public void shouldValidateJsonArray() {

        final JsonArray jsonArray = JsonObjects.createArrayBuilder()
                .add(JsonObjects.createObjectBuilder()
                        .add("firstName", "Joe")
                        .add("lastName", "Smith")

                        .build())
                .build();

        xhibitReferenceDataValidator.validateJsonArray("lastName", jsonArray);
    }
}
