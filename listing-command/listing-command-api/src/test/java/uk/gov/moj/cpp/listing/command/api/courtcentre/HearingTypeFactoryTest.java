package uk.gov.moj.cpp.listing.command.api.courtcentre;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.service.ReferenceDataService;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingTypeFactoryTest {

    private static final String TRIAL_ID = randomUUID().toString();
    private static final String PTPH_ID = randomUUID().toString();
    private static final String NO_FLAG_ID = randomUUID().toString();

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private HearingTypeFactory hearingTypeFactory;

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName("listing.list-next-hearings-v2").build(),
                createObjectBuilder().build());
    }

    private void givenHearingTypes() {
        when(referenceDataService.getHearingTypes(any(JsonEnvelope.class))).thenReturn(envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("referencedata.query.all-hearing-types").build(),
                createObjectBuilder().add("hearingTypes", createArrayBuilder()
                        .add(createObjectBuilder().add("id", TRIAL_ID).add("defaultDurationMin", 360)
                                .add("hearingCode", "TRL").add("trialTypeFlag", true))
                        .add(createObjectBuilder().add("id", PTPH_ID).add("defaultDurationMin", 20)
                                .add("hearingCode", "PTP").add("trialTypeFlag", false))
                        .add(createObjectBuilder().add("id", NO_FLAG_ID).add("defaultDurationMin", 30)
                                .add("hearingCode", "APN"))
                        .build()).build()));
    }

    @Test
    void shouldReturnOnlyHearingTypeIdsFlaggedAsTrial() {
        givenHearingTypes();

        final Set<String> trialIds = hearingTypeFactory.getTrialHearingTypeIds(envelope());

        assertEquals(1, trialIds.size());
        assertTrue(trialIds.contains(TRIAL_ID));
    }

    @Test
    void shouldTreatAbsentTrialTypeFlagAsNotTrial() {
        givenHearingTypes();

        assertFalse(hearingTypeFactory.getTrialHearingTypeIds(envelope()).contains(NO_FLAG_ID));
    }

    @Test
    void shouldTreatFalseTrialTypeFlagAsNotTrial() {
        givenHearingTypes();

        assertFalse(hearingTypeFactory.getTrialHearingTypeIds(envelope()).contains(PTPH_ID));
    }
}
