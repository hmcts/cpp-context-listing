package uk.gov.moj.cpp.listing.common.xhibit;


import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class XhibitReferenceDataValidatorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private XhibitReferenceDataValidator xhibitReferenceDataValidator = new XhibitReferenceDataValidator();

    @Test
    public void shouldThrowErrorWhenValidatePayload() {

        expectedException.expect(InvalidReferenceDataException.class);
        expectedException.expectMessage("Invalid value '' for 'lastName' in '{\"firstName\":\"Joe\",\"lastName\":\"\"}'");

        final JsonObject payload = Json.createObjectBuilder()
                .add("firstName", "Joe")
                .add("lastName", "")
                .build();

        xhibitReferenceDataValidator.validate("lastName", payload.getString("lastName"), payload);
    }


    @Test
    public void shouldValidatePayload() {

        final JsonObject payload = Json.createObjectBuilder()
                .add("firstName", "Joe")
                .add("lastName", "White")
                .build();

        xhibitReferenceDataValidator.validate("lastName", payload.getString("lastName"), payload);
    }

    @Test
    public void shouldThrowErrorWhenValidate() {

        expectedException.expect(InvalidReferenceDataException.class);
        expectedException.expectMessage("Invalid value '' for 'lastName'");

        final JsonObject payload = Json.createObjectBuilder()
                .add("firstName", "Joe")
                .add("lastName", "")
                .build();

        xhibitReferenceDataValidator.validate("lastName", payload.getString("lastName"));
    }

    @Test
    public void shouldValidateWhenDataExist() {

        final JsonObject payload = Json.createObjectBuilder()
                .add("firstName", "Joe")
                .add("lastName", "Smith")
                .build();

        xhibitReferenceDataValidator.validate("lastName", payload.getString("lastName"));
    }


    @Test
    public void shouldThrowErrorWhenValidateJsonArrayIsEmpty() {

        expectedException.expect(InvalidReferenceDataException.class);
        expectedException.expectMessage("Invalid object 'lastName' for '[]'");

        final JsonArray jsonArray = Json.createArrayBuilder()
                .build();

        xhibitReferenceDataValidator.validateJsonArray("lastName", jsonArray);
    }

    @Test
    public void shouldValidateJsonArray() {

        final JsonArray jsonArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("firstName", "Joe")
                        .add("lastName", "Smith")

                        .build())
                .build();

        xhibitReferenceDataValidator.validateJsonArray("lastName", jsonArray);
    }
}
