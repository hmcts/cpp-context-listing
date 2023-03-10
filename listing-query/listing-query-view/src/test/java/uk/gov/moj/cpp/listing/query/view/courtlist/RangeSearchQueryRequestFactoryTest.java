package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.CROWN;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.DRAFT;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FINAL;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FIRM;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.WARN;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

@RunWith(Parameterized.class)
public class RangeSearchQueryRequestFactoryTest {

    final String START_DATE = "2019-12-16";
    final String EXPECTED_WEEK_COMMENCING_END_DATE = "2019-12-20";

    @Parameterized.Parameter(0)
    public uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType publishCourtListType;

    @Parameterized.Parameter(1)
    public boolean shouldUseWeekCommencingQueryParameters;
    @InjectMocks
    private RangeSearchQueryRequestFactory rangeSearchQueryRequestFactory;
    @Spy
    private Enveloper enveloper = createEnveloper();

    @Parameterized.Parameters(name = "{index}: Test with PublishCourtListType={0}, shouldUseWeekCommencingQueryParameters is:{1} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{{WARN, true}, {FIRM, true}, {DRAFT, false}, {FINAL, false}};
        return Arrays.asList(data);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldBuildRangeSearchQueryEnvelope() {

        String startDate = "2019-12-16";
        String listType = publishCourtListType.toString();
        UUID courtCentreId = UUID.randomUUID();

        final JsonEnvelope courtListQueryEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("event.name"),
                createObjectBuilder()
                        .add("startDate", startDate)
                        .add("listType", listType)
                        .add("courtCentreId", courtCentreId.toString())
                        .build());

        final JsonEnvelope rangeSearchQueryEnvelope = rangeSearchQueryRequestFactory.buildRangeSearchQueryEnvelope(
                courtCentreId,
                publishCourtListType,
                LocalDate.parse(startDate),
                courtListQueryEnvelope);

        verifyQueryParameters(rangeSearchQueryEnvelope.payloadAsJsonObject(), startDate, courtCentreId, publishCourtListType);
    }

    private void verifyQueryParameters(final JsonObject queryPayload, final String startDate, final UUID courtCentreId, final PublishCourtListType publishCourtListType) {

        if (shouldUseWeekCommencingQueryParameters) {
            assertThat(queryPayload.getString("jurisdictionType"), is(CROWN.name()));
            assertThat(queryPayload.getString("weekCommencingStartDate"), is(START_DATE));
            assertThat(queryPayload.getString("weekCommencingEndDate"), is(EXPECTED_WEEK_COMMENCING_END_DATE));
            assertThat(queryPayload.containsKey("startDate"), is(false));
            assertThat(queryPayload.containsKey("endDate"), is(false));
            if(Lists.newArrayList(FIRM, WARN, DRAFT, FINAL).contains(publishCourtListType)){
                assertThat(queryPayload.getBoolean("noPagination"), is(true));
            }
            if(WARN.equals(publishCourtListType)){
                assertThat(queryPayload.getBoolean("noPagination"), is(true));
            }
        } else {
            assertThat(queryPayload.getString("startDate"), is(START_DATE));
            assertThat(queryPayload.getString("endDate"), is(START_DATE));
            assertThat(queryPayload.containsKey("jurisdictionType"), is(false));
            assertThat(queryPayload.containsKey("weekCommencingStartDate"), is(false));
            assertThat(queryPayload.containsKey("weekCommencingEndDate"), is(false));
        }
    }
}
