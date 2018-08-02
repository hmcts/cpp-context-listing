package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.listing.domain.aggregate.utils.DomainBuilder.buildHearings;

import uk.gov.justice.listing.events.CaseSentForListing;
import uk.gov.moj.cpp.listing.domain.Hearing;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class CaseTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String URN = "92GX685120";

    private Case aCase;

    @Before
    public void setUp() {
        aCase = new Case();
    }

    @Test
    public void shouldEmitCaseSentForListingEventWhenListed() throws Exception {
        //given
        List<Hearing> hearings = buildHearings();

        //when
        List<Object> events = aCase.sendForListing(CASE_ID, URN, hearings).collect(toList());

        //then
        assertThat(events.size(), is(1));

        Object object = events.get(0);
        assertThat(object.getClass() , is(equalTo(CaseSentForListing.class)));
    }


    @After
    public void teardown() {
        try {
            // ensure aggregate is serializable
            SerializationUtils.serialize(aCase);
        } catch (SerializationException e) {
            fail("Aggregate should be serializable");
        }
    }

}
