package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.RangeSearchQuery;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A non-ex-parte case adjourned into the hearing of an application linked to an ex-parte case: the
 * whole hearing is left off every Crown list type, run through the real ex-parte filters.
 */
@ExtendWith(MockitoExtension.class)
class CourtListServiceExParteSharedHearingTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();

    @Mock
    private RangeSearchQueryRequestFactory rangeSearchQueryRequestFactory;

    @Mock
    private RangeSearchConverter rangeSearchConverter;

    @Mock
    private RangeSearchQuery rangeSearchQuery;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private CourtListService courtListService;

    private final JsonEnvelope queryEnvelope = mock(JsonEnvelope.class);

    @BeforeEach
    void setUpRealExParteFilters() {
        final ExParteLinkedCaseApplicationFilter exParteLinkedCaseApplicationFilter = new ExParteLinkedCaseApplicationFilter();
        setField(exParteLinkedCaseApplicationFilter, "hearingRepository", hearingRepository);
        setField(exParteLinkedCaseApplicationFilter, "progressionService", progressionService);
        setField(courtListService, "exParteLinkedCaseApplicationFilter", exParteLinkedCaseApplicationFilter);
        setField(courtListService, "hearingJsonListConverterFilterEjectCases", new HearingJsonListConverterFilterEjectCases());
    }

    @ParameterizedTest
    @EnumSource(value = PublishCourtListType.class, names = {"WARN", "FIRM", "DRAFT", "FINAL"})
    void shouldLeaveSharedHearingOffTheListWhenTheExParteCaseIsNotOnIt(final PublishCourtListType publishCourtListType) throws Exception {
        final UUID exParteCaseId = randomUUID();
        final String otherHearingId = randomUUID().toString();
        when(hearingRepository.findHearingsByListedCaseIds(List.of(exParteCaseId)))
                .thenReturn(List.of(new Hearing(randomUUID(), OBJECT_MAPPER.readTree(
                        createObjectBuilder().add("listedCases", createArrayBuilder().add(listedCase(exParteCaseId, true))).build().toString()))));
        givenRangeSearchReturns(publishCourtListType,
                sharedHearing(exParteCaseId),
                caseHearing(otherHearingId, listedCase(randomUUID(), false)));

        assertThat(hearingIdsOnTheCourtList(publishCourtListType), contains(otherHearingId));
    }

    @ParameterizedTest
    @EnumSource(value = PublishCourtListType.class, names = {"WARN", "FIRM", "DRAFT", "FINAL"})
    void shouldLeaveSharedHearingOffTheListWhenTheExParteCaseIsOnIt(final PublishCourtListType publishCourtListType) {
        final UUID exParteCaseId = randomUUID();
        final String otherHearingId = randomUUID().toString();
        givenRangeSearchReturns(publishCourtListType,
                caseHearing(randomUUID().toString(), listedCase(exParteCaseId, true)),
                sharedHearing(exParteCaseId),
                caseHearing(otherHearingId, listedCase(randomUUID(), false)));

        final List<String> hearingIds = hearingIdsOnTheCourtList(publishCourtListType);

        assertThat(hearingIds.size(), org.hamcrest.Matchers.is(2));
        assertThat(hearingIds.get(1), org.hamcrest.Matchers.is(otherHearingId));
    }

    private void givenRangeSearchReturns(final PublishCourtListType publishCourtListType, final JsonObject... hearings) {
        final JsonEnvelope rangeSearchQueryEnvelope = mock(JsonEnvelope.class);
        final JsonEnvelope rangeSearchResponse = mock(JsonEnvelope.class);
        final javax.json.JsonArrayBuilder hearingsBuilder = createArrayBuilder();
        for (final JsonObject hearing : hearings) {
            hearingsBuilder.add(hearing);
        }
        when(rangeSearchQueryRequestFactory.buildRangeSearchQueryEnvelope(COURT_CENTRE_ID, publishCourtListType, START_DATE, queryEnvelope)).thenReturn(rangeSearchQueryEnvelope);
        when(rangeSearchQuery.rangeSearchHearings(rangeSearchQueryEnvelope)).thenReturn(rangeSearchResponse);
        when(rangeSearchResponse.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("hearings", hearingsBuilder).build());
    }

    private List<String> hearingIdsOnTheCourtList(final PublishCourtListType publishCourtListType) {
        courtListService.retrieveUnPublishedCourtList(COURT_CENTRE_ID, publishCourtListType, START_DATE, "", queryEnvelope);

        final ArgumentCaptor<JsonObject> filteredPayload = ArgumentCaptor.forClass(JsonObject.class);
        verify(rangeSearchConverter).generateCourtListQueryPayload(eq(COURT_CENTRE_ID), filteredPayload.capture(), any(), any());
        return filteredPayload.getValue().getJsonArray("hearings").getValuesAs(JsonObject.class)
                .stream().map(hearing -> hearing.getString("id")).toList();
    }

    private static JsonObject sharedHearing(final UUID exParteCaseId) {
        return createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("listedCases", createArrayBuilder().add(listedCase(randomUUID(), false)))
                .add("courtApplications", createArrayBuilder().add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("linkedCaseIds", createArrayBuilder().add(exParteCaseId.toString()))))
                .build();
    }

    private static JsonObject caseHearing(final String hearingId, final JsonObject listedCase) {
        return createObjectBuilder()
                .add("id", hearingId)
                .add("listedCases", createArrayBuilder().add(listedCase))
                .build();
    }

    private static JsonObject listedCase(final UUID caseId, final boolean exParte) {
        return createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", createArrayBuilder().add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("offences", createArrayBuilder().add(createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("civilOffence", createObjectBuilder().add("isExParte", exParte))))))
                .build();
    }
}
