package uk.gov.moj.cpp.listing.command.factory;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.activemq.artemis.utils.JsonLoader.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isNull;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.service.ReferenceDataService;

import java.util.Map;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingTypeFactoryTest {
    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private HearingTypeFactory hearingTypeFactory;

    private static final String HEARING_TYPE_ID = randomUUID().toString();
    private static final int HEARING_TYPE_DURATION = 30;

    @Test
    public void shouldReturnHearingTypes() {

        //given
        given(referenceDataService.getHearingTypes(envelope)).willReturn(finalEnvelope);
        given(finalEnvelope.payloadAsJsonObject()).willReturn(getJsonEnvelope());

        //when
        Map hearingTypesMap = hearingTypeFactory.getHearingTypesIdDurationMap(envelope);

        //then
        assertThat(hearingTypesMap.get(HEARING_TYPE_ID), is(HEARING_TYPE_DURATION));
    }
    @Test
    public void shouldNotReturnHearingTypes() {

        //given
        given(referenceDataService.getHearingTypes(envelope)).willReturn(finalEnvelope);
        given(finalEnvelope.payloadAsJsonObject()).willReturn(getJsonEnvelopeNoRecords());

        //when
        Map hearingTypesMap = hearingTypeFactory.getHearingTypesIdDurationMap(envelope);

        //then
        Assert.assertNull(hearingTypesMap.get(HEARING_TYPE_ID));
    }
    private JsonObject getJsonEnvelope() {
        return createObjectBuilder().add("hearingTypes", createArrayBuilder().add(createObjectBuilder().add("id", HEARING_TYPE_ID)
                .add("defaultDurationMin", HEARING_TYPE_DURATION)
                .build()).build()).build();

    }
    private JsonObject getJsonEnvelopeNoRecords() {
        return createObjectBuilder().add("hearingTypes", createArrayBuilder()).build();

    }

}
