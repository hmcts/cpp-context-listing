package uk.gov.moj.cpp.listing.query.view.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.listing.query.view.dto.LinkedApplicationsSummary;
import uk.gov.moj.cpp.listing.query.view.dto.ProsecutionCase;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProgressionServiceTest {

    @InjectMocks
    private ProgressionService progressionService;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Requester requester;

    @Mock
    private UtcClock utcClock;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private uk.gov.justice.core.courts.ProsecutionCase prosecutionCase;


    @Test
    public void shouldGetOrganisationUnitByOuCode1() {
        final UUID caseId = randomUUID();
        final UUID applicationId = randomUUID();

        final LinkedApplicationsSummary linkedApplicationsSummary = LinkedApplicationsSummary.linkedApplicationsSummary().withApplicationId(applicationId).build();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withLinkedApplicationsSummary(asList(linkedApplicationsSummary))
                .build();

        when(requester.requestAsAdmin(any(), eq(ProsecutionCase.class)).payload()).thenReturn(prosecutionCase);
        when(utcClock.now()).thenReturn(ZonedDateTime.now());

        final ProsecutionCase prosecutionCaseDetails = progressionService.getProsecutionCaseDetails(caseId);

        assertThat(prosecutionCaseDetails.getLinkedApplicationsSummary(), hasSize(1));
        assertThat(prosecutionCaseDetails.getLinkedApplicationsSummary().get(0).getApplicationId(), is(applicationId));
    }

    @Test
    public void shouldGetProsecutionCaseByCaseId(){
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUIDAndName(),
                createObjectBuilder().build());
        final String caseId = UUID.randomUUID().toString();
        final Envelope<JsonObject> result = Envelope.envelopeFrom(metadataBuilder().withName("progression.query.prosecutioncase")
                .withId(randomUUID()), createObjectBuilder().add("caseId", caseId)
                .build());
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(result);
        given(jsonObjectToObjectConverter.convert(any(), any())).willReturn(prosecutionCase);
        final uk.gov.justice.core.courts.ProsecutionCase response = progressionService.getProsecutionCaseByCaseId(envelope, caseId);
        assertNotNull(response);
    }

}