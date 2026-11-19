package uk.gov.moj.cpp.listing.query.api.service;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createArrayBuilder;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.common.client.CppQueryClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ported from listing-query-api's ReferenceDataServiceTest (Requester#requestAsAdmin -> CppQueryClient#requestAsAdmin). */
@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

    @Mock
    private CppQueryClient cppQueryClient;

    @Captor
    private ArgumentCaptor<JsonObject> params;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Test
    void shouldGetCourtCentreByIdSuccessfully() {
        final JsonObject returned = createObjectBuilder().add("oucodeL3Name", "x").build();
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.courtroom"), any())).thenReturn(Optional.of(returned));

        final UUID courtCentreId = randomUUID();
        final JsonObject response = referenceDataService.getCourtCentreById(courtCentreId);

        verify(cppQueryClient).requestAsAdmin(eq("referencedata.query.courtroom"), params.capture());
        assertThat(params.getValue(), is(createObjectBuilder().add("id", courtCentreId.toString()).build()));
        assertThat(response, is(returned));
    }

    @Test
    void shouldGetJudiciariesByIdList() {
        final UUID judiciaryId = randomUUID();
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.judiciaries"), any())).thenReturn(Optional.of(createObjectBuilder().build()));

        referenceDataService.getJudiciariesByIdList(asList(judiciaryId));

        verify(cppQueryClient).requestAsAdmin(eq("referencedata.query.judiciaries"), params.capture());
        assertThat(params.getValue().getString("ids"), is(judiciaryId.toString()));
    }

    @Test
    void shouldGetProsecutorById() {
        final JsonObject returned = createObjectBuilder().add("fullName", "CPS").build();
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.prosecutor"), any())).thenReturn(Optional.of(returned));

        final String prosecutorId = randomUUID().toString();
        final Optional<JsonObject> response = referenceDataService.getProsecutorById(prosecutorId);

        verify(cppQueryClient).requestAsAdmin(eq("referencedata.query.prosecutor"), params.capture());
        assertThat(params.getValue(), is(createObjectBuilder().add("id", prosecutorId).build()));
        assertThat(response, is(Optional.of(returned)));
    }

    @Test
    void shouldGetIsHearingLanguageWelsh() {
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.courtroom"), any())).thenReturn(Optional.of(createObjectBuilder().build()));

        final UUID courtCentreId = randomUUID();
        final Optional<Boolean> hearingLanguageWelsh = referenceDataService.isHearingLanguageWelsh(courtCentreId.toString());

        verify(cppQueryClient).requestAsAdmin(eq("referencedata.query.courtroom"), params.capture());
        assertThat(params.getValue(), is(createObjectBuilder().add("id", courtCentreId.toString()).build()));
        assertThat(hearingLanguageWelsh, is(Optional.of(false)));
    }

    @Test
    void getHearingTypesIdWelshDescriptionMap() {
        final String hearingTypeId = randomUUID().toString();
        when(cppQueryClient.requestAsAdmin(eq("referencedata.query.hearing-types"), any())).thenReturn(Optional.of(createObjectBuilder()
                .add("hearingTypes", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", hearingTypeId)
                                .add("welshHearingDescription", "welshtest")))
                .build()));

        final Map<String, String> map = referenceDataService.getHearingTypesIdWelshDescriptionMap();

        assertThat(map.entrySet(), hasSize(1));
        assertThat(map.get(hearingTypeId), is("welshtest"));
    }
}
