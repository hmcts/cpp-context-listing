package uk.gov.moj.cpp.listing.event.processor;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import uk.gov.justice.services.messaging.JsonObjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsProsecutorUpdatedEventProcessorTest {
    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private HearingQueryView hearingQueryView;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @InjectMocks
    private CpsProsecutorUpdatedEventProcessor processor;

    @Test
    public void shouldUpdateProsecutionCaseWithAssociatedHearings() {

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.events.cps-prosecutor-updated"),
                JsonObjects.createObjectBuilder()
                        .add("prosecutionCaseId", "34d07e81-9770-4d23-af6f-84f1d7571bd3")
                        .add("hearingIds", JsonObjects.createArrayBuilder()
                                .add("a8448a33-68ab-4b9b-84c2-59cee4fe36f4")
                                .add("095d7412-ba76-4a15-942d-566d3aeae7c9")
                                .add("095d7412-ba76-4a15-942d-566d3aeae7c8")
                                .build())
                        .add("caseURN", "test Case URN")
                        .add("prosecutionAuthorityId", "test prosecutionAuthorityId")
                        .add("prosecutionAuthorityReference", "test prosecutionAuthorityReference")
                        .add("prosecutionAuthorityCode", "test prosecutionAuthorityCode")
                        .add("prosecutionAuthorityName", "test prosecutionAuthorityName")
                        .add("address", JsonObjects.createObjectBuilder()
                                .add("address1", "41 Manhattan House")
                                .add("postcode", "MK9 2BQ")
                                .build())
                        .build());

        JsonEnvelope queryEnvelope = envelopeFrom(metadataFrom(event.metadata()).withName("listing.search.hearings"),
                createObjectBuilder()
                        .add("hearings", createArrayBuilder()
                                .add(createObjectBuilder().add("id","a8448a33-68ab-4b9b-84c2-59cee4fe36f4")
                                        .add("allocated", true).build())
                                .add(createObjectBuilder().add("id","095d7412-ba76-4a15-942d-566d3aeae7c9")
                                        .add("allocated", false).build()))
        );

        when(hearingQueryView.searchAllocatedAndUnallocatedHearings(any())).thenReturn(queryEnvelope);

        processor.cpsProsecutorUpdated(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(this.envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.update-cps-prosecutor-with-associated-hearings"));
        assertThat(
                this.envelopeArgumentCaptor.getValue().payloadAsJsonObject(),  payloadIsJson(allOf(
                        withJsonPath("$.prosecutionCaseId", is("34d07e81-9770-4d23-af6f-84f1d7571bd3")),
                        withJsonPath("$.hearingIds[0]", is("a8448a33-68ab-4b9b-84c2-59cee4fe36f4")),
                        withJsonPath("$.hearingIds[1]", is("095d7412-ba76-4a15-942d-566d3aeae7c9")),
                        withoutJsonPath("$.hearingIds[2]"),
                        withJsonPath("$.prosecutionAuthorityId", is("test prosecutionAuthorityId")),
                        withJsonPath("$.prosecutionAuthorityCode", is("test prosecutionAuthorityCode")),
                        withJsonPath("$.*", hasSize(4))
                ))
        );
    }

    @Test
    public void shouldUpdateProsecutionCaseWithoutAssociatedHearings() {

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.events.cps-prosecutor-updated"),
                JsonObjects.createObjectBuilder()
                        .add("prosecutionCaseId", "34d07e81-9770-4d23-af6f-84f1d7571bd3")
                        .add("hearingIds", JsonObjects.createArrayBuilder().build())
                        .add("caseURN", "test Case URN")
                        .add("prosecutionAuthorityId", "test prosecutionAuthorityId")
                        .add("prosecutionAuthorityReference", "test prosecutionAuthorityReference")
                        .add("prosecutionAuthorityCode", "test prosecutionAuthorityCode")
                        .add("prosecutionAuthorityName", "test prosecutionAuthorityName")
                        .add("address", JsonObjects.createObjectBuilder()
                                .add("address1", "41 Manhattan House")
                                .add("postcode", "MK9 2BQ")
                                .build())
                        .build());

        JsonEnvelope queryEnvelope = envelopeFrom(metadataFrom(event.metadata()).withName("listing.search.hearings"),
                createObjectBuilder()
                        .add("hearings", createArrayBuilder()
                                .add(createObjectBuilder().add("id","a8448a33-68ab-4b9b-84c2-59cee4fe36f4")
                                        .add("allocated", true).build())
                                .add(createObjectBuilder().add("id","095d7412-ba76-4a15-942d-566d3aeae7c9")
                                        .add("allocated", false).build()))
        );

        when(hearingQueryView.searchAllocatedAndUnallocatedHearings(any())).thenReturn(queryEnvelope);

        processor.cpsProsecutorUpdated(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(this.envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.update-cps-prosecutor-with-associated-hearings"));
        assertThat(
                this.envelopeArgumentCaptor.getValue().payloadAsJsonObject(),  payloadIsJson(allOf(
                        withJsonPath("$.prosecutionCaseId", is("34d07e81-9770-4d23-af6f-84f1d7571bd3")),
                        withJsonPath("$.hearingIds[0]", is("a8448a33-68ab-4b9b-84c2-59cee4fe36f4")),
                        withJsonPath("$.hearingIds[1]", is("095d7412-ba76-4a15-942d-566d3aeae7c9")),
                        withoutJsonPath("$.hearingIds[2]"),
                        withJsonPath("$.prosecutionAuthorityId", is("test prosecutionAuthorityId")),
                        withJsonPath("$.prosecutionAuthorityCode", is("test prosecutionAuthorityCode")),
                        withJsonPath("$.*", hasSize(4))
                ))
        );
    }
}
