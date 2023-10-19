package uk.gov.moj.cpp.listing.command.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StagingHmiQueryServiceTest {

    @InjectMocks
    private StagingHmiQueryService stagingHmiQueryService;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Test
    public void shouldReturnHmiSessions(){
        final ZonedDateTime sessionStartDate = ZonedDateTime.now();
        final ZonedDateTime sessionEndDate = ZonedDateTime.now().plusHours(1);
        final String typeId = "type";
        final String ouCode = "code";
        final int pageNumber = 0;
        final int pageSize = 10;
        final Optional<UUID> courtRoomId = Optional.of(randomUUID());
        final JsonEnvelope event  = generateEmptyEnvelope();

        when(requester.requestAsAdmin(any())).thenReturn(generateResultEnvelope());

        stagingHmiQueryService.getHmiSessions(sessionStartDate, sessionEndDate, typeId,
                ouCode, pageNumber, pageSize, courtRoomId, event);

        verify(requester).requestAsAdmin(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope result = jsonEnvelopeArgumentCaptor.getValue();

        assertThat(result.metadata().name(), is("staginghmi.query.sessions"));
    }

    private JsonEnvelope generateEmptyEnvelope() {
        return createEnvelope(".", createObjectBuilder().build());
    }

    private JsonEnvelope generateResultEnvelope() {
        return createEnvelope(".", createObjectBuilder()
                .add("hmiSessions", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("courtHouseId", randomUUID().toString())
                                .add("courtRoomId", randomUUID().toString())
                                .add("courtHouseName", "courtHouseName")
                                .add("ouCode", "ouCode")
                                .add("sessionStartTime", LocalDateTime.now().toString())
                                .add("judiciaries", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("judiciaryId", randomUUID().toString())
                                                .add("judiciaryType", "judiciaryType1")
                                                .build())
                                        .build())
                                .build())
                        .build()
                )
                .build());
    }

}