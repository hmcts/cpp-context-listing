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
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class RangeSearchQueryRequestFactoryTest {

    final String START_DATE = "2019-12-16";
    final String EXPECTED_WEEK_COMMENCING_END_DATE = "2019-12-22";

    @SuppressWarnings("squid:S1312")
    @Mock
    private Logger logger;

    @InjectMocks
    private RangeSearchQueryRequestFactory rangeSearchQueryRequestFactory;

    @Spy
    private Enveloper enveloper = createEnveloper();

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(WARN, true),
                Arguments.of(FIRM, true),
                Arguments.of(DRAFT, false),
                Arguments.of(FINAL, false)
        );
    }


    @ParameterizedTest
    @MethodSource("data")
    public void shouldBuildRangeSearchQueryEnvelope(
            final uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType publishCourtListType,
            final boolean shouldUseWeekCommencingQueryParameters) {

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

        final JsonObject queryPayload = rangeSearchQueryEnvelope.payloadAsJsonObject();

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
