package uk.gov.moj.cpp.listing.command.hearing.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.client.CppQueryClient;
import uk.gov.moj.cpp.listing.domain.referencedata.OrganisationUnit;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Port of the JEE {@code ReferenceDataServiceTest} (MBD refactor P2.3.2): the framework
 * {@code Requester} is {@link CppQueryClient}; admin queries stay admin, the organisation unit is
 * requested as the caller.
 */
class ReferenceDataServiceTest {

    private final CppQueryClient cppQueryClient = mock(CppQueryClient.class);
    private final ReferenceDataService referenceDataService = new ReferenceDataService(cppQueryClient);
    private final UUID userId = randomUUID();
    private final Envelope command = new Envelope(Metadata.builder().withId(randomUUID()).withName("listing.command.list-court-hearing")
            .withUserId(userId.toString()).build(), JsonValue.EMPTY_JSON_OBJECT);

    @Test
    public void shouldGetCourtCentreById() {
        final UUID courtCentreId = randomUUID();
        final JsonObject answer = Json.createObjectBuilder().add("id", courtCentreId.toString()).build();
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.courtroom"), any(JsonObject.class))).thenReturn(Optional.of(answer));

        final Envelope response = referenceDataService.getCourtCentreById(courtCentreId, command);

        assertThat(response.name(), is("referencedata.query.courtroom"));
        assertThat(response.payload(), is(answer));
        final ArgumentCaptor<JsonObject> query = ArgumentCaptor.forClass(JsonObject.class);
        verify(cppQueryClient).requestAsAdmin(eq("referencedata.query.courtroom"), query.capture());
        assertThat(query.getValue().getString("id"), is(courtCentreId.toString()));
    }

    @Test
    public void shouldGetCourtCentreSById() {
        final UUID first = randomUUID();
        final UUID second = randomUUID();
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.courtrooms"), any(JsonObject.class))).thenReturn(Optional.empty());

        final Envelope response = referenceDataService.getCourtCentresById(new LinkedHashSet<>(List.of(first, second)), command);

        assertThat(response.name(), is("referencedata.query.courtrooms"));
        assertThat(response.payload(), is(JsonValue.EMPTY_JSON_OBJECT));
        final ArgumentCaptor<JsonObject> query = ArgumentCaptor.forClass(JsonObject.class);
        verify(cppQueryClient).requestAsAdmin(eq("referencedata.query.courtrooms"), query.capture());
        assertThat(query.getValue().getString("courtCentreId"), is(first + "," + second));
    }

    @Test
    public void shouldGetOrganizationUnitById() {
        final UUID courtCentreId = randomUUID();
        when(cppQueryClient.request(eq("referencedata.query.organisation-unit"), any(JsonObject.class), eq(userId)))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("oucode", "B01LY").build()));

        final OrganisationUnit organisationUnit = referenceDataService.getOrganizationUnitById(courtCentreId, command);

        assertThat(organisationUnit.getOucode(), is("B01LY"));
    }

    @Test
    public void shouldReadThePoliceFlagOfTheProsecutor() {
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.prosecutor"), any(JsonObject.class)))
                .thenReturn(Optional.of(Json.createObjectBuilder().add("policeFlag", true).build()));

        assertThat(referenceDataService.getPoliceFlagForProsecutorId(command, randomUUID().toString()), is(true));
    }
}
