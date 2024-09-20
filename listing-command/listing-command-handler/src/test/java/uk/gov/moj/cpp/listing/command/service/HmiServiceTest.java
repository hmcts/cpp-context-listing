package uk.gov.moj.cpp.listing.command.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.factory.CourtCentreFactory;
import uk.gov.moj.cpp.staginghmi.common.StagingHmiService;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HmiServiceTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final String OU_CODE = "B06AN00";
    @InjectMocks
    private HmiService hmiService;
    @Mock
    private StagingHmiService stagingHmiService;
    @Mock
    private CourtCentreFactory courtCentreFactory;

    @Test
    public void shouldGetHmiEnabledWithCourtCentreId() {
        final JsonEnvelope envelope = generateEmptyEnvelope();
        when(stagingHmiService.isHmiListingEnabled(any())).thenReturn(true);
        when(courtCentreFactory.getOrganisationUnit(COURT_CENTRE_ID, envelope))
                .thenReturn(Json.createObjectBuilder().add("oucode", OU_CODE).build());

        boolean isHmiEnabled = hmiService.isHmiEnabled(Hearing.hearing().withCourtCentreId(COURT_CENTRE_ID).build(), envelope);

        assertTrue(isHmiEnabled);
        verify(stagingHmiService).isHmiListingEnabled(any());
        verify(courtCentreFactory).getOrganisationUnit(COURT_CENTRE_ID, envelope);
    }

    @Test
    public void shouldReturnHmiEnabledAsFalseIfHearingIsNull() {
        final JsonEnvelope envelope = generateEmptyEnvelope();
        boolean isHmiEnabled = hmiService.isHmiEnabled(null, envelope);

        assertFalse(isHmiEnabled);
        verify(stagingHmiService, never()).isHmiListingEnabled(any());
        verify(courtCentreFactory, never()).getOrganisationUnit(COURT_CENTRE_ID, envelope);
    }

    @Test
    public void shouldGetIfHmiEnabledWithOuCode() {
        when(stagingHmiService.isHmiListingEnabled(Optional.of(OU_CODE))).thenReturn(true);

        boolean isHmiEnabled = hmiService.isHmiEnabled(OU_CODE);

        assertTrue(isHmiEnabled);
        verify(stagingHmiService).isHmiListingEnabled(any());
    }

    private JsonEnvelope generateEmptyEnvelope() {
        return createEnvelope(".", createObjectBuilder().build());
    }
}
