package uk.gov.moj.cpp.listing.command.utils;

import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.core.courts.CustodyTimeLimit.custodyTimeLimit;
import static uk.gov.justice.core.courts.Offence.offence;

import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.Offence;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class CourtsOffenceToDomainOffenceTest {


    private CourtsOffenceToDomainOffence courtsOffenceToDomainOffence = new CourtsOffenceToDomainOffence();

    @Test
    public void shouldConvert() {

        final UUID offenceId = randomUUID();

        final CustodyTimeLimit custodyTimeLimit = custodyTimeLimit()
                .withTimeLimit("1")
                .withDaysSpent(1)
                .withIsCtlExtended(true)
                .build();

        final Offence offence = offence()
                .withId(offenceId)
                .withArrestDate("2020-11-12")
                .withCount(1)

                .withCustodyTimeLimit(custodyTimeLimit).build();

        final List<uk.gov.moj.cpp.listing.domain.Offence> convert = courtsOffenceToDomainOffence.convert(asList(offence));
        assertThat(convert, hasSize(1));
        assertThat(convert.get(0).getId(), is(offenceId));
    }
}