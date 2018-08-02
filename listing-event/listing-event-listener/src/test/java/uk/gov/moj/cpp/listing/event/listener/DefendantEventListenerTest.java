package uk.gov.moj.cpp.listing.event.listener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.DefendantDetailsUpdated;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.converter.SimpleDefendantConverter;
import uk.gov.moj.cpp.listing.persistence.entity.SimpleDefendant;
import uk.gov.moj.cpp.listing.persistence.repository.SimpleDefendantRepository;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DefendantEventListenerTest {

    @InjectMocks
    private DefendantEventListener defendantEventListener;

    @Mock
    private Envelope<DefendantDetailsUpdated> defendantDetailsUpdatedEnvelope;

    @Mock
    private SimpleDefendantRepository simpleDefendantRepository;

    @Mock
    private SimpleDefendantConverter simpleDefendantConverter;

    @Mock
    private DefendantDetailsUpdated defendantDetailsUpdated;

    @Mock
    private SimpleDefendant simpleDefendant;

    @Test
    public void shouldHandleDefendantDetailsUpdatedAndPersistSimpleDefendant() throws Exception {
        given(defendantDetailsUpdatedEnvelope.payload()).willReturn(defendantDetailsUpdated);
        given(simpleDefendantConverter.convert(defendantDetailsUpdated)).willReturn(simpleDefendant);

        defendantEventListener.defendantDetailsUpdated(defendantDetailsUpdatedEnvelope);

        verify(simpleDefendantConverter).convert(defendantDetailsUpdated);
        verify(simpleDefendantRepository).save(simpleDefendant);
    }
}