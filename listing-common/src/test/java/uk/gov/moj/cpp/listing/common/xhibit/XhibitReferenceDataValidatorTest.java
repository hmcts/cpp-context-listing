package uk.gov.moj.cpp.listing.common.xhibit;


import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class XhibitReferenceDataValidatorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private XhibitReferenceDataValidator xhibitReferenceDataValidator = new XhibitReferenceDataValidator();

    @Test
    public void shouldValidate() {

        expectedException.expect(InvalidReferenceDataException.class);
        expectedException.expectMessage("Invalid value '' for 'lastName' in '{\"firstName\":\"Joe\",\"lastName\":\"\"}'");

        final JsonObject payload = Json.createObjectBuilder()
                .add("firstName", "Joe")
                .add("lastName", "")
                .build();

        xhibitReferenceDataValidator.validate("lastName", payload.getString("lastName"), payload);
    }
}
