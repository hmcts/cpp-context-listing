package uk.gov.moj.cpp.listing.event.listener;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.converter.BaseOffenceConverter;
import uk.gov.moj.cpp.listing.event.converter.OffenceWithDefendantIdConverter;
import uk.gov.moj.cpp.listing.persistence.entity.BaseOffence;
import uk.gov.moj.cpp.listing.persistence.entity.CompositeOffenceId;
import uk.gov.moj.cpp.listing.persistence.entity.OffenceWithDefendantId;
import uk.gov.moj.cpp.listing.persistence.repository.BaseOffenceRepository;
import uk.gov.moj.cpp.listing.persistence.repository.OffenceWithDefendantIdRepository;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DefendantOffencesEventListenerTest {

    @InjectMocks
    private DefendantOffencesEventListener defendantOffencesEventListener;

    @Mock
    private Envelope<OffenceUpdated> offenceUpdatedEnvelope;

    @Mock
    private Envelope<OffenceAdded> offenceAddedEnvelope;

    @Mock
    private Envelope<OffenceDeleted> offenceDeletedEnvelope;

    @Mock
    private BaseOffenceRepository baseOffenceRepository;

    @Mock
    private BaseOffenceConverter baseOffenceConverter;

    @Mock
    private OffenceWithDefendantIdRepository offenceWithDefendantIdRepository;

    @Mock
    private OffenceWithDefendantIdConverter offenceWithDefendantIdConverter;

    @Mock
    private OffenceUpdated offenceUpdated;

    @Mock
    private OffenceAdded offenceAdded;

    @Mock
    private BaseOffence baseOffence;

    @Mock
    private OffenceWithDefendantId offenceWithDefendantId;

    @Mock
    private CompositeOffenceId compositeOffenceId;

    @Test
    public void shouldHandleOffenceUpdatedAndPersistSimpleOffence() {
        given(offenceUpdatedEnvelope.payload()).willReturn(offenceUpdated);
        given(baseOffenceConverter.convert(offenceUpdated)).willReturn(baseOffence);

        defendantOffencesEventListener.offenceUpdated(offenceUpdatedEnvelope);

        verify(baseOffenceConverter).convert(offenceUpdated);
        verify(baseOffenceRepository).save(baseOffence);

    }

    @Test
    public void shouldHandleOffenceAdded() {
        given(offenceAddedEnvelope.payload()).willReturn(offenceAdded);
        given(offenceWithDefendantIdConverter.convert(offenceAdded)).willReturn(offenceWithDefendantId);

        defendantOffencesEventListener.offenceAdded(offenceAddedEnvelope);

        verify(offenceWithDefendantIdConverter).convert(offenceAdded);
        verify(offenceWithDefendantIdRepository).save(offenceWithDefendantId);
    }

    @Test
    public void shouldHandleOffenceDeleteAndDeleteSimpleOffence() {
        final OffenceDeleted offenceDeleted = createOffenceDeleted();
        given(offenceDeletedEnvelope.payload()).willReturn(offenceDeleted);

        defendantOffencesEventListener.offenceDeleted(offenceDeletedEnvelope);

        ArgumentCaptor<CompositeOffenceId> compositeOffenceIdArgumentCaptor = ArgumentCaptor.forClass(CompositeOffenceId.class);
        verify(baseOffenceRepository).removeById(compositeOffenceIdArgumentCaptor.capture());
        CompositeOffenceId compositeOffenceId = compositeOffenceIdArgumentCaptor.getAllValues().get(0);
        assertThat(compositeOffenceId.getDefendantId(), is(offenceDeleted.getDefendantId()));
        assertThat(compositeOffenceId.getOffenceId(), is(offenceDeleted.getOffenceId()));
    }

    private OffenceDeleted createOffenceDeleted() {
        return new OffenceDeleted.Builder()
                    .withOffenceId(UUID.randomUUID())
                    .withDefendantId(UUID.randomUUID())
                    .build();
    }
}