
package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublishCourtListRequestParametersParserTest {

    @InjectMocks
    PublishCourtListRequestParametersParser publishCourtListRequestParametersParser;

    @Test
    public void shouldParse() {

        final UUID publishCourtListRequestId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final LocalDate startDate = LocalDate.of(2019, 11, 4);
        final LocalDate endDate = LocalDate.of(2019, 11, 5);
        final PublishCourtListType publishCourtListType = PublishCourtListType.WARN;
        final ZonedDateTime requestedTime = ZonedDateTime.now(ZoneId.of("UTC").normalized());

        final JsonObject payload = createObjectBuilder()
                .add("publishCourtListRequestId", publishCourtListRequestId.toString())
                .add("courtCentreId", courtCentreId.toString())
                .add("startDate", startDate.toString())
                .add("endDate", endDate.toString())
                .add("publishCourtListType", publishCourtListType.name())
                .add("requestedTime", requestedTime.toString())
                .add("sendNotificationToParties", true)
                .build();

        final Metadata metadata = mock(Metadata.class);
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, payload);

        PublishCourtListRequestParameters parameters = publishCourtListRequestParametersParser.parse(tEnvelope);

        assertThat(parameters.getCourtListId(), is(publishCourtListRequestId));
        assertThat(parameters.getCourtCentreId(), is(courtCentreId));
        assertThat(parameters.getStartDate(), is(startDate));
        assertThat(parameters.getEndDate(), is(endDate));
        assertThat(parameters.getPublishCourtListType(), is(publishCourtListType));
        assertThat(parameters.getRequestedTime(), is(requestedTime));
    }
}
