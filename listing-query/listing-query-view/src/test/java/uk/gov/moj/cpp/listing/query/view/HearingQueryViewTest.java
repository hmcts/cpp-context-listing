package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingSummary;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingSummaryConverter;

import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingQueryViewTest {


    public static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    public static final boolean ALLOCATED = true;
    public static final String ALLOCATED_QUERY_PARAMETER = "allocated";
    private static final String COURT_CENTRE_QUERY_PARAMETER = "courtCentreId";

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSummaryConverter hearingSummaryConverter;

    @Mock
    private Converter<List<HearingSummary>, JsonArray> jsonConverter;

    @InjectMocks
    private HearingQueryView hearingsQueryView;

    @Test
    public void searchHearings() throws Exception {

        when(hearingRepository.findByAllocatedAndCourtCentreId(ALLOCATED, COURT_CENTRE_ID))
                .thenReturn(hearings());

        HearingSummary hearingSummary = Mockito.mock(HearingSummary.class);
        when(hearingSummaryConverter.convert(any(Hearing.class))).thenReturn
                (hearingSummary);

        when(jsonConverter.convert(anyObject())).thenReturn(jsonObjectForHearingSummary());

        final JsonEnvelope query = envelopeFrom(
                metadataWithRandomUUIDAndName(),
                createObjectBuilder()
                        .add(ALLOCATED_QUERY_PARAMETER, ALLOCATED)
                        .add(COURT_CENTRE_QUERY_PARAMETER, COURT_CENTRE_ID.toString())
                        .build());

        final JsonEnvelope results = hearingsQueryView.searchHearings(query);

        assertThat(results, is(jsonEnvelope(withMetadataEnvelopedFrom(query).withName("listing.search.hearings"),
                payloadIsJson(
                        withJsonPath("$.hearings[0].test", equalTo("test"))
                ))
        ));

    }

    private JsonArray jsonObjectForHearingSummary() {
        return createArrayBuilder()
            .add(createObjectBuilder()
                .add("test", "test")).build();

    }

    private List<Hearing> hearings() {
        return newArrayList(Mockito.mock(Hearing.class), Mockito.mock(Hearing.class));
    }
}