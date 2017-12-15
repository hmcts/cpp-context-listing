package uk.gov.moj.cpp.listing.command.api;

import static uk.gov.justice.services.test.utils.core.helper.ServiceComponents.verifyPassThroughCommandHandlerMethod;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListingCommandApiTest {

    private static final String[] PUBLIC_API_COMMANDS = {"sendCaseForListing", "updateHearingForListing"};

    @Test
    public void testIfAllMethodsArePassThrough() throws Exception {
        verifyPassThroughCommandHandlerMethod(ListingCommandApi.class, PUBLIC_API_COMMANDS);
    }
}