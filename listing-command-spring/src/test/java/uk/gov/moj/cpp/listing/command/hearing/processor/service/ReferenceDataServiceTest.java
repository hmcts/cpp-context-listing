package uk.gov.moj.cpp.listing.command.hearing.processor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import uk.gov.justice.listing.events.OrganisationUnit;
import uk.gov.moj.cpp.listing.common.client.CppQueryClient;
import uk.gov.moj.cpp.listing.common.xhibit.exception.InvalidReferenceDataException;

import java.util.Optional;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();

    @Mock
    private CppQueryClient cppQueryClient;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Test
    void theOrganisationUnitIsQueriedAsTheSystemUserById() {
        final OrganisationUnit organisationUnit = OrganisationUnit.organisationUnit().withOucode("B01LY00").withOucodeL3Name("Liverpool").build();
        final JsonObject params = Json.createObjectBuilder().add("id", COURT_CENTRE_ID.toString()).build();
        given(cppQueryClient.requestAsAdmin(eq("referencedata.query.organisation-unit"), eq(params), eq(OrganisationUnit.class)))
                .willReturn(Optional.of(organisationUnit));

        assertThat(referenceDataService.getOrganizationUnitById(COURT_CENTRE_ID, null)).isSameAs(organisationUnit);
    }

    @Test
    void noOrganisationUnitFailsTheStep() {
        given(cppQueryClient.requestAsAdmin(eq("referencedata.query.organisation-unit"), eq(Json.createObjectBuilder().add("id", COURT_CENTRE_ID.toString()).build()),
                eq(OrganisationUnit.class))).willReturn(Optional.empty());

        assertThatThrownBy(() -> referenceDataService.getOrganizationUnitByIdWithAdmin(COURT_CENTRE_ID, null))
                .isInstanceOf(InvalidReferenceDataException.class)
                .hasMessage("Cannot find organisationunit for courtCenter : " + COURT_CENTRE_ID);
    }

    @Test
    void theOucodeComesFromTheCourtroomQuery() {
        given(cppQueryClient.requestAsAdmin(eq("referencedata.query.courtroom"), eq(Json.createObjectBuilder().add("id", COURT_CENTRE_ID.toString()).build())))
                .willReturn(Optional.of(Json.createObjectBuilder().add("oucode", "B01LY00").build()));

        assertThat(referenceDataService.getOucodeFromEnvelope(COURT_CENTRE_ID, null)).isEqualTo("B01LY00");
    }
}
