package uk.gov.moj.cpp.listing.command.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static uk.gov.justice.services.test.utils.core.helper.ServiceComponents.verifyPassThroughCommandHandlerMethod;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataCommandApiTest {

    @Test
    public void testIfAllMethodsArePassThrough() throws Exception {
        verifyPassThroughCommandHandlerMethod(ReferenceDataCommandApi.class, "addJudge");
        verifyPassThroughCommandHandlerMethod(ReferenceDataCommandApi.class, "addCourtCentre");
        verifyPassThroughCommandHandlerMethod(ReferenceDataCommandApi.class, "addCourtRoom");
    }
}