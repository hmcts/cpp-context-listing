package uk.gov.moj.cpp.listing.query.view.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.common.client.CppQueryClient;

import java.util.Optional;
import java.util.UUID;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ported from listing-query-view's ProgressionServiceTest for the methods that survive the P2.1
 * port (the Requester-based typed calls are now CppQueryClient calls returning JSON).
 */
@ExtendWith(MockitoExtension.class)
class ProgressionServiceTest {

    @Mock
    private CppQueryClient cppQueryClient;

    @InjectMocks
    private ProgressionService progressionService;

    @Test
    void shouldGetApplicationDetailsAsTheCallingUser() {
        final UUID userId = randomUUID();
        final UUID applicationId = randomUUID();
        final JsonObject details = createObjectBuilder().add("courtApplication", createObjectBuilder()).build();
        final ArgumentCaptor<JsonObject> params = ArgumentCaptor.forClass(JsonObject.class);
        when(cppQueryClient.request(eq("progression.query.application-only"), params.capture(), eq(userId))).thenReturn(Optional.of(details));

        assertThat(progressionService.getApplicationDetails(userId, applicationId), is(Optional.of(details)));
        assertThat(params.getValue().getString("applicationId"), is(applicationId.toString()));
    }

    @Test
    void shouldFindProsecutionCaseByCaseId() {
        final String caseId = randomUUID().toString();
        final JsonObject prosecutionCase = createObjectBuilder().add("id", caseId).build();
        when(cppQueryClient.requestAsAdmin(eq("progression.query.prosecutioncase"), any()))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCase).build()));

        assertThat(progressionService.findProsecutionCaseByCaseId(caseId), is(Optional.of(prosecutionCase)));
        final ArgumentCaptor<JsonObject> params = ArgumentCaptor.forClass(JsonObject.class);
        verify(cppQueryClient).requestAsAdmin(eq("progression.query.prosecutioncase"), params.capture());
        assertThat(params.getValue().getString("caseId"), is(caseId));
    }

    @Test
    void shouldNotFindProsecutionCaseWhenProgressionReturnsNoPayload() {
        when(cppQueryClient.requestAsAdmin(eq("progression.query.prosecutioncase"), any())).thenReturn(Optional.empty());

        assertThat(progressionService.findProsecutionCaseByCaseId(randomUUID().toString()), is(Optional.empty()));
    }

    @Test
    void shouldNotFindProsecutionCaseWhenPayloadHasNoProsecutionCase() {
        when(cppQueryClient.requestAsAdmin(eq("progression.query.prosecutioncase"), any()))
                .thenReturn(Optional.of(createObjectBuilder().add("other", "x").build()));

        assertThat(progressionService.findProsecutionCaseByCaseId(randomUUID().toString()), is(Optional.empty()));
    }

    @Test
    void shouldPropagateFailureToReachProgressionWhenFindingProsecutionCase() {
        when(cppQueryClient.requestAsAdmin(eq("progression.query.prosecutioncase"), any())).thenThrow(new IllegalStateException("down"));

        assertThrows(IllegalStateException.class, () -> progressionService.findProsecutionCaseByCaseId(randomUUID().toString()));
    }
}
