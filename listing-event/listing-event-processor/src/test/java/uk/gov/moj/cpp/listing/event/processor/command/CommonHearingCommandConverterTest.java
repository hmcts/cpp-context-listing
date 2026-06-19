package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.listing.events.ReportingRestriction;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

public class CommonHearingCommandConverterTest {


    private CommonHearingCommandConverter commonHearingCommandConverter = new CommonHearingCommandConverter();

    @Test
    public void shouldBuildReportingRestrictions() {
        final UUID reportingRestrictionId = randomUUID();
        final ReportingRestriction reportingRestriction = ReportingRestriction.reportingRestriction()
                .withId(reportingRestrictionId)
                .build();
        final List<uk.gov.moj.cpp.listing.domain.ReportingRestriction> result = commonHearingCommandConverter.buildReportingRestrictions(asList(reportingRestriction));

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getId(), is(reportingRestrictionId));

    }
}