package uk.gov.moj.cpp.listing.command.api;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.listing.command.api.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.command.api.util.FileUtil.givenPayload;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NonDefaultDay;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.DeleteNextHearings;
import uk.gov.justice.listing.courts.ExtendHearingForHearing;
import uk.gov.justice.listing.courts.ListCourtHearingEnriched;
import uk.gov.justice.listing.courts.ListNextHearingsEnrichedV2;
import uk.gov.justice.listing.courts.ListNextHearingsV2;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.listing.courts.ListUnscheduledCourtHearingEnriched;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearings;
import uk.gov.justice.listing.courts.ListUnscheduledNextHearingsEnriched;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.courts.UpdateRelatedHearing;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.api.service.HearingEnrichmentOrchestrator;
import uk.gov.moj.cpp.listing.command.api.service.HearingLookupService;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateException;
import uk.gov.moj.cpp.listing.common.pastdate.MoveHearingToPastDateResult;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JudicialRoleType;
import uk.gov.moj.cpp.listing.domain.Type;
import uk.gov.moj.cpp.listing.domain.VacateTrialEnriched;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListingCommandApiTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Mock
    private Sender sender;
    @Mock
    private JsonEnvelope envelope;
    @Mock
    private JsonObject payload;
    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Mock
    private ObjectToJsonValueConverter objectToJsonValueConverter;
    @Mock
    private CourtCentreFactory courtCentreFactory;
    @Mock
    private UpdateHearingForListing updateHearingForListing;
    @Mock
    private VacateTrialEnriched vacateTrialEnriched;
    @InjectMocks
    private ListingCommandApi listingCommandApi;
    @Mock
    private ExtendHearingForHearing extendHearingForHearing;
    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;
    @Mock
    private HearingSlotsService hearingSlotsService;
    @Mock
    private HearingEnrichmentOrchestrator hearingEnrichmentOrchestrator;
    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;
    @Mock
    private HearingLookupService hearingLookupService;

    private static final Type HEARING_TYPE = Type.type()
            .withId(fromString("6e1bef55-7e13-4615-b3ba-8663f4438e16"))
            .withDescription("Trial")
            .build();

    @Test
    public void shouldEnrichOnlyNonDefaultDaysWithMissingOrZeroDurationAD() {
        final Metadata metadata = metadataWithRandomUUIDAndName().build();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtCentreId1 = UUID.randomUUID();
        final UUID courtCentreId2 = UUID.randomUUID();
        final UUID hearingTypeId = UUID.randomUUID();

        final HearingType mockType = mock(HearingType.class);
        when(mockType.getId()).thenReturn(hearingTypeId);

        final HearingListingNeeds hearingNullDuration = HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId1).build())
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(courtScheduleId1.toString())
                                .withDuration(null)
                                .build()))
                .withType(mockType)
                .build();

        final HearingListingNeeds hearingWithDuration = HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId2).build())
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(courtScheduleId2.toString())
                                .withDuration(20)
                                .build()))
                .withType(mockType)
                .build();

        uk.gov.justice.listing.commands.ListCourtHearing listCourtHearing1 = mock(uk.gov.justice.listing.commands.ListCourtHearing.class);

        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(envelope.metadata()).willReturn(metadata);
        given(jsonObjectConverter.convert(payload, uk.gov.justice.listing.commands.ListCourtHearing.class))
                .willReturn(listCourtHearing1);
        given(listCourtHearing1.getHearings()).willReturn(List.of(hearingNullDuration, hearingWithDuration));

        HearingListingNeeds enrichedHearing = HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(hearingNullDuration.getCourtCentre())
                .withType(hearingNullDuration.getType())
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(courtScheduleId1.toString())
                                .withDuration(15)
                                .build()
                ))
                .build();

        when(hearingEnrichmentOrchestrator.enrichListCourtHearing(any(), any())).thenReturn(List.of(enrichedHearing));

        mockCourtCentres();

        doAnswer(invocation -> {
            ListCourtHearingEnriched enriched = invocation.getArgument(0);
            return new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper())
                    .convert(enriched);
        }).when(objectToJsonValueConverter).convert(any(ListCourtHearingEnriched.class));

        listingCommandApi.handleListCourtHearing(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        Envelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        JsonObject sentPayload = (JsonObject) capturedEnvelope.payload();

        JsonArray hearings = sentPayload.getJsonObject("listCourtHearing").getJsonArray("hearings");
        JsonArray nonDefaultDays = hearings.getJsonObject(0).getJsonArray("nonDefaultDays");
        assertThat(nonDefaultDays.getJsonObject(0).getInt("duration"), is(15));
    }

    @Test
    public void shouldEnrichOnlyNonDefaultDaysWithMissingOrZeroDuration() {
        final Metadata metadata = metadataWithRandomUUIDAndName().build();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtCentreId1 = UUID.randomUUID();
        final UUID courtCentreId2 = UUID.randomUUID();
        final UUID hearingTypeId = UUID.randomUUID();

        final HearingType mockType = mock(HearingType.class);
        when(mockType.getId()).thenReturn(hearingTypeId);

        final HearingListingNeeds hearingNullDuration = HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId1).build())
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(courtScheduleId1.toString())
                                .withDuration(null)
                                .build()))
                .withType(mockType)
                .build();

        final HearingListingNeeds hearingWithDuration = HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId2).build())
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(courtScheduleId2.toString())
                                .withDuration(20)
                                .build()))
                .withType(mockType)
                .build();

        uk.gov.justice.listing.commands.ListCourtHearing listCourtHearing1 = mock(uk.gov.justice.listing.commands.ListCourtHearing.class);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(jsonObjectConverter.convert(payload, uk.gov.justice.listing.commands.ListCourtHearing.class))
                .thenReturn(listCourtHearing1);
        when(listCourtHearing1.getHearings()).thenReturn(List.of(hearingNullDuration, hearingWithDuration));

        HearingListingNeeds enrichedHearing = HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(hearingNullDuration.getCourtCentre())
                .withType(hearingNullDuration.getType())
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(courtScheduleId1.toString())
                                .withDuration(45)
                                .build()
                ))
                .build();

        when(hearingEnrichmentOrchestrator.enrichListCourtHearing(any(), any())).thenReturn(List.of(enrichedHearing));
        mockCourtCentres();

        doAnswer(invocation -> {
            ListCourtHearingEnriched enriched = invocation.getArgument(0);
            return new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper())
                    .convert(enriched);
        }).when(objectToJsonValueConverter).convert(any(ListCourtHearingEnriched.class));

        listingCommandApi.handleListCourtHearing(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        Envelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        JsonObject sentPayload = (JsonObject) capturedEnvelope.payload();

        JsonArray hearings = sentPayload.getJsonObject("listCourtHearing").getJsonArray("hearings");
        JsonArray nonDefaultDays = hearings.getJsonObject(0).getJsonArray("nonDefaultDays");
        assertThat(nonDefaultDays.getJsonObject(0).getInt("duration"), is(45));
    }

    @Test
    public void shouldEnrichOnlyNonDefaultDaysWithMissingOrZeroDurationFromRefData() {
        final Metadata metadata = metadataWithRandomUUIDAndName().build();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtCentreId1 = UUID.randomUUID();
        final UUID courtCentreId2 = UUID.randomUUID();
        final UUID hearingTypeId = UUID.randomUUID();

        final HearingType mockType = mock(HearingType.class);
        when(mockType.getId()).thenReturn(hearingTypeId);

        final HearingListingNeeds hearingNullDuration = HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId1).build())
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(courtScheduleId1.toString())
                                .withDuration(null)
                                .build()))
                .withType(mockType)
                .build();

        final HearingListingNeeds hearingWithDuration = HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId2).build())
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(courtScheduleId2.toString())
                                .withDuration(20)
                                .build()))
                .withType(mockType)
                .build();

        uk.gov.justice.listing.commands.ListCourtHearing listCourtHearing1 = mock(uk.gov.justice.listing.commands.ListCourtHearing.class);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(jsonObjectConverter.convert(payload, uk.gov.justice.listing.commands.ListCourtHearing.class))
                .thenReturn(listCourtHearing1);
        when(listCourtHearing1.getHearings()).thenReturn(List.of(hearingNullDuration, hearingWithDuration));

        HearingListingNeeds enrichedHearing = HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(hearingNullDuration.getCourtCentre())
                .withType(hearingNullDuration.getType())
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(courtScheduleId1.toString())
                                .withDuration(20)
                                .build()
                ))
                .build();

        when(hearingEnrichmentOrchestrator.enrichListCourtHearing(any(), any())).thenReturn(List.of(enrichedHearing));
        mockCourtCentres();

        doAnswer(invocation -> {
            ListCourtHearingEnriched enriched = invocation.getArgument(0);
            return new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper())
                    .convert(enriched);
        }).when(objectToJsonValueConverter).convert(any(ListCourtHearingEnriched.class));

        listingCommandApi.handleListCourtHearing(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        Envelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        JsonObject sentPayload = (JsonObject) capturedEnvelope.payload();

        JsonArray hearings = sentPayload.getJsonObject("listCourtHearing").getJsonArray("hearings");
        JsonArray nonDefaultDays = hearings.getJsonObject(0).getJsonArray("nonDefaultDays");
        assertThat(nonDefaultDays.getJsonObject(0).getInt("duration"), is(20));
    }

    @Test
    public void testIfMethodsArePassThrough() {

        assertThat(ListingCommandApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleChangeJudiciaryForHearings")
                        .thatHandles("listing.command.change-judiciary-for-hearings"))
                .with(method("handleSequenceHearings")
                        .thatHandles("listing.command.sequence-hearings")));
    }

    @Test
    public void shouldUpdateHearingForListingWithProsecutionCases() {

        final JsonObject hearingSlotsResponse = givenPayload("/listing.command.hearingSlots.stub-data.json");

        final List<JudicialRole> judicialRoles = new ArrayList<>();
        ((JsonObject) hearingSlotsResponse
                .getJsonArray("hearingSlots").get(0))
                .getJsonArray("judiciaries")
                .stream()
                .map(JsonObject.class::cast)
                .forEach(judiciaryJsonObject ->
                        judicialRoles.add(JudicialRole.judicialRole()
                                .withIsBenchChairman(of(judiciaryJsonObject.getBoolean("benchChairman")))
                                .withIsDeputy(of(judiciaryJsonObject.getBoolean("deputy")))
                                .withJudicialId(UUID.fromString(judiciaryJsonObject.getString("judiciaryId")))
                                .withJudicialRoleType(
                                        JudicialRoleType.judicialRoleType()
                                                .withJudiciaryType(judiciaryJsonObject.getString("judiciaryType"))
                                                .build())
                                .build())
                );

        //given
        UUID hearingId = randomUUID();
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getCourtRoomId()).willReturn(UUID.randomUUID());
        given(updateHearingForListing.getHearingId()).willReturn(hearingId);

        when(hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(any(), any(), any()))
                .thenReturn(updateHearingForListing);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor = forClass(Envelope.class);

        final UUID caseId = randomUUID();
        final JsonObject prosecutionCases = JsonObjects.createObjectBuilder().add("caseId", caseId.toString()).build();
        final JsonArray prosecutionCasesArray = createArrayBuilder().add(prosecutionCases).build();
        given(payload.getJsonArray("prosecutionCases")).willReturn(prosecutionCasesArray);
        given(jsonObjectConverter.convert(prosecutionCases, ProsecutionCases.class)).willReturn(ProsecutionCases.prosecutionCases().build());
        mockCourtCentres();

        //when
        listingCommandApi.handleUpdateHearingForListing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        verify(jsonObjectConverter).convert(prosecutionCases, ProsecutionCases.class);
    }

    @Test
    public void shouldUpdateHearingForListing() {

        final JsonObject hearingSlotsResponse = givenPayload("/listing.command.hearingSlots.stub-data.json");

        final List<JudicialRole> judicialRoles = new ArrayList<>();
        ((JsonObject) hearingSlotsResponse
                .getJsonArray("hearingSlots").get(0))
                .getJsonArray("judiciaries")
                .stream()
                .map(JsonObject.class::cast)
                .forEach(judiciaryJsonObject ->
                        judicialRoles.add(JudicialRole.judicialRole()
                                .withIsBenchChairman(of(judiciaryJsonObject.getBoolean("benchChairman")))
                                .withIsDeputy(of(judiciaryJsonObject.getBoolean("deputy")))
                                .withJudicialId(UUID.fromString(judiciaryJsonObject.getString("judiciaryId")))
                                .withJudicialRoleType(
                                        JudicialRoleType.judicialRoleType()
                                                .withJudiciaryType(judiciaryJsonObject.getString("judiciaryType"))
                                                .build())
                                .build())
                );

        //given
        UUID hearingId = randomUUID();
        HearingType hearingType = HearingType.hearingType().withId(HEARING_TYPE.getId()).build();
        LocalDate startDate = LocalDate.now();
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateHearingForListing.getCourtRoomId()).willReturn(UUID.randomUUID());
        given(updateHearingForListing.getHearingId()).willReturn(hearingId);

        when(hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(any(), any(), any()))
                .thenReturn(updateHearingForListing);
        final ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor = forClass(Envelope.class);

        final JsonArray prosecutionCasesArray = createArrayBuilder().build();
        given(payload.getJsonArray("prosecutionCases")).willReturn(prosecutionCasesArray);
        mockCourtCentres();

        //when
        listingCommandApi.handleUpdateHearingForListing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        verify(jsonObjectConverter, never()).convert(any(), eq(ProsecutionCases.class));
    }

    @Test
    public void shouldUpdateHearingsForListing() {
        String jsonString = getPayload("listing.command.update-hearings-for-listing.json");
        JsonReader jsonReader = createReader(new StringReader(jsonString));
        final JsonObject hearingsJsonObj = jsonReader.readObject();
        final JsonArray hearingsJsonArr = hearingsJsonObj.getJsonArray("hearings");

        JsonObjectToObjectConverter jsonConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
        ObjectToJsonValueConverter objConverter = new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper());

        final JsonObject hearingJsonObj1 = hearingsJsonArr.getJsonObject(0);
        final UpdateHearingForListing hearing1 = jsonConverter.convert(hearingJsonObj1, UpdateHearingForListing.class);
        final List<ProsecutionCases> prosecutionCases1 =
                hearingJsonObj1.getJsonArray("prosecutionCases").stream().map(
                        p -> jsonConverter.convert((JsonObject) p, ProsecutionCases.class)).toList();

        final JsonObject hearingJsonObj2 = hearingsJsonArr.getJsonObject(1);
        final UpdateHearingForListing hearing2 = jsonConverter.convert(hearingJsonObj2, UpdateHearingForListing.class);
        final List<ProsecutionCases> prosecutionCases2 =
                hearingJsonObj2.getJsonArray("prosecutionCases").stream().map(
                        p -> jsonConverter.convert((JsonObject) p, ProsecutionCases.class)).toList();

        // Mock the court centre factory to return court centre details
        CourtCentreDetails courtCentreDetails = CourtCentreDetails.courtCentreDetails()
                .withDefaultDuration(30)
                .build();
        given(courtCentreFactory.getCourtCentreDetailsById(any(), eq(envelope)))
                .willReturn(Map.of(hearing1.getCourtCentreId(), courtCentreDetails));

        // Mock the enrichment orchestrator
        given(hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(eq(hearing1), eq(envelope), eq(courtCentreDetails)))
                .willReturn(hearing1);
        given(hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(eq(hearing2), eq(envelope), eq(courtCentreDetails)))
                .willReturn(hearing2);

        // Mock the JSON conversions
        given(envelope.payloadAsJsonObject()).willReturn(hearingsJsonObj);
        given(jsonObjectConverter.convert(hearingJsonObj1, UpdateHearingForListing.class)).willReturn(hearing1);
        given(jsonObjectConverter.convert(hearingJsonObj2, UpdateHearingForListing.class)).willReturn(hearing2);
        
        // Mock prosecution cases conversions
        AtomicInteger idx = new AtomicInteger();
        hearingJsonObj1.getJsonArray("prosecutionCases").forEach(
                pc -> given(jsonObjectConverter.convert((JsonObject) pc, ProsecutionCases.class))
                        .willReturn(prosecutionCases1.get(idx.getAndIncrement())));
        idx.set(0);
        hearingJsonObj2.getJsonArray("prosecutionCases").forEach(
                pc -> given(jsonObjectConverter.convert((JsonObject) pc, ProsecutionCases.class))
                        .willReturn(prosecutionCases2.get(idx.getAndIncrement())));

        // Mock the object to JSON conversion
        given(objectToJsonValueConverter.convert(any(UpdateHearingForListingEnriched.class)))
                .willReturn(JsonObjects.createObjectBuilder().add("test", "value").build());

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        
        final ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor = forClass(Envelope.class);
        
        // When
        listingCommandApi.handleUpdateHearingsForListing(envelope);

        // Then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        verify(jsonObjectConverter, times(1)).convert(hearingJsonObj1, UpdateHearingForListing.class);
        verify(jsonObjectConverter, times(1)).convert(hearingJsonObj2, UpdateHearingForListing.class);
        verify(hearingEnrichmentOrchestrator, times(1)).enrichUpdateHearingForListing(hearing1, envelope, courtCentreDetails);
        verify(hearingEnrichmentOrchestrator, times(1)).enrichUpdateHearingForListing(hearing2, envelope, courtCentreDetails);
        
        // Verify prosecution cases were converted
        hearingJsonObj1.getJsonArray("prosecutionCases").forEach(
                pc -> verify(jsonObjectConverter, times(2)).convert((JsonObject) pc, ProsecutionCases.class));
        hearingJsonObj2.getJsonArray("prosecutionCases").forEach(
                pc -> verify(jsonObjectConverter, times(2)).convert((JsonObject) pc, ProsecutionCases.class));
    }


    @Test
    void shouldThrowBadRequestException_WhenStartDateGreaterThanEndDate() {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.minusDays(1);
        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getStartDate()).willReturn(startDate);
        given(updateHearingForListing.getEndDate()).willReturn(endDate);

        //when
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            listingCommandApi.handleUpdateHearingForListing(envelope);
        });
        assertThat(exception.getMessage(), is("startDate must be smaller than endDate"));
    }

    @Test
    void shouldThrowBadRequestException_WhenWeekCommencingStartDateGreaterThanWeekCommencingEndDate() {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.minusDays(1);
        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getWeekCommencingStartDate()).willReturn(startDate);
        given(updateHearingForListing.getWeekCommencingEndDate()).willReturn(endDate);

        //when
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            listingCommandApi.handleUpdateHearingForListing(envelope);
        });
        assertThat(exception.getMessage(), is("Week commencing start date must be smaller than week commencing end date"));
    }

    @Test
    public void shouldVacateTheTrial() {

        //given
        UUID hearingId = randomUUID();
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, VacateTrialEnriched.class)).willReturn(vacateTrialEnriched);
        given(vacateTrialEnriched.getHearingId()).willReturn(hearingId);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor = forClass(Envelope.class);
        //when
        listingCommandApi.handleVacateTrial(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldRejectMoveHearingToPastDateWhenHearingIdUnknown() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();

        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString("hearingId")).willReturn(hearingId.toString());
        given(payload.getString("courtCentreId")).willReturn(courtCentreId.toString());
        given(payload.getString("startDate")).willReturn("2026-05-01");
        given(hearingLookupService.findHearing(hearingId, envelope)).willReturn(Optional.empty());

        final MoveHearingToPastDateException thrown = assertThrows(MoveHearingToPastDateException.class,
                () -> listingCommandApi.handleMoveHearingToPastDate(envelope));

        assertThat(thrown.getHttpStatus(), is(422));
        assertThat(thrown.getErrorCode(), is("HEARING_ID_NOT_FOUND"));
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldMoveMagistratesHearingToPastDateEnrichWithSlotDetailsAndSend() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final UUID courtScheduleId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2026-05-01");

        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString("hearingId")).willReturn(hearingId.toString());
        given(payload.getString("courtCentreId")).willReturn(courtCentreId.toString());
        given(payload.getString("startDate")).willReturn(startDate.toString());
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final JsonObject hearing = Json.createObjectBuilder()
                .add("id", hearingId.toString())
                .add("jurisdictionType", "MAGISTRATES")
                .add("estimatedMinutes", 30)
                .build();
        given(hearingLookupService.findHearing(hearingId, envelope)).willReturn(Optional.of(hearing));

        final MoveHearingToPastDateResult slot = new MoveHearingToPastDateResult(courtScheduleId,
                "9d324f4f-6c3b-451f-ac1e-f459db781153", startDate, "2026-05-01T09:00:00Z", "2026-05-01T17:00:00Z", 30);
        given(courtSchedulerServiceAdapter.moveHearingToPastDate(hearingId, courtCentreId, startDate, 30)).willReturn(slot);

        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);

        listingCommandApi.handleMoveHearingToPastDate(envelope);

        verify(courtSchedulerServiceAdapter).moveHearingToPastDate(hearingId, courtCentreId, startDate, 30);
        verify(sender, times(1)).send(captor.capture());
        final JsonObject sent = (JsonObject) captor.getValue().payload();
        assertThat(sent.getString("hearingId"), is(hearingId.toString()));
        assertThat(sent.getString("jurisdiction"), is("MAGISTRATES"));
        assertThat(sent.getString("courtScheduleId"), is(courtScheduleId.toString()));
        assertThat(sent.getString("sessionDate"), is(startDate.toString()));
        assertThat(sent.getInt("durationInMinutes"), is(30));
    }

    @Test
    public void shouldNotSendWhenCourtschedulerRejectsMagistratesMove() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2999-01-01");

        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString("hearingId")).willReturn(hearingId.toString());
        given(payload.getString("courtCentreId")).willReturn(courtCentreId.toString());
        given(payload.getString("startDate")).willReturn(startDate.toString());

        final JsonObject hearing = Json.createObjectBuilder()
                .add("id", hearingId.toString())
                .add("jurisdictionType", "MAGISTRATES")
                .build();
        given(hearingLookupService.findHearing(hearingId, envelope)).willReturn(Optional.of(hearing));

        final JsonObject body = Json.createObjectBuilder()
                .add("errorCode", "FUTURE_DATE_NOT_ALLOWED")
                .add("message", "Hearings can only be moved to today or an earlier date")
                .build();
        given(courtSchedulerServiceAdapter.moveHearingToPastDate(any(), any(), any(), any()))
                .willThrow(new MoveHearingToPastDateException(422, body, "rejected"));

        final MoveHearingToPastDateException thrown = assertThrows(MoveHearingToPastDateException.class,
                () -> listingCommandApi.handleMoveHearingToPastDate(envelope));
        assertThat(thrown.getHttpStatus(), is(422));
        assertThat(thrown.getErrorCode(), is("FUTURE_DATE_NOT_ALLOWED"));
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldNotSendWhenCourtschedulerReturnsNotFoundForMagistratesMove() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final LocalDate startDate = LocalDate.parse("2026-05-01");

        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString("hearingId")).willReturn(hearingId.toString());
        given(payload.getString("courtCentreId")).willReturn(courtCentreId.toString());
        given(payload.getString("startDate")).willReturn(startDate.toString());

        final JsonObject hearing = Json.createObjectBuilder()
                .add("id", hearingId.toString())
                .add("jurisdictionType", "MAGISTRATES")
                .build();
        given(hearingLookupService.findHearing(hearingId, envelope)).willReturn(Optional.of(hearing));

        given(courtSchedulerServiceAdapter.moveHearingToPastDate(any(), any(), any(), any()))
                .willThrow(new MoveHearingToPastDateException(404, Json.createObjectBuilder().build(), "not found"));

        final MoveHearingToPastDateException thrown = assertThrows(MoveHearingToPastDateException.class,
                () -> listingCommandApi.handleMoveHearingToPastDate(envelope));
        assertThat(thrown.getHttpStatus(), is(404));
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldMoveCrownHearingToPastDateListingSideOnlyWithoutCallingCourtScheduler() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final LocalDate startDate = LocalDate.now().minusDays(1);

        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString("hearingId")).willReturn(hearingId.toString());
        given(payload.getString("courtCentreId")).willReturn(courtCentreId.toString());
        given(payload.getString("startDate")).willReturn(startDate.toString());
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final JsonObject hearing = Json.createObjectBuilder()
                .add("id", hearingId.toString())
                .add("jurisdictionType", "CROWN")
                .build();
        given(hearingLookupService.findHearing(hearingId, envelope)).willReturn(Optional.of(hearing));

        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);

        listingCommandApi.handleMoveHearingToPastDate(envelope);

        verify(courtSchedulerServiceAdapter, never()).moveHearingToPastDate(any(), any(), any(), any());
        verify(sender, times(1)).send(captor.capture());
        final JsonObject sent = (JsonObject) captor.getValue().payload();
        assertThat(sent.getString("hearingId"), is(hearingId.toString()));
        assertThat(sent.getString("jurisdiction"), is("CROWN"));
        assertThat(sent.getString("startDate"), is(startDate.toString()));
        assertThat(sent.containsKey("courtScheduleId"), is(false));
    }

    @Test
    public void shouldRejectCrownMoveToFutureDate() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final LocalDate startDate = LocalDate.now().plusDays(1);

        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString("hearingId")).willReturn(hearingId.toString());
        given(payload.getString("courtCentreId")).willReturn(courtCentreId.toString());
        given(payload.getString("startDate")).willReturn(startDate.toString());

        final JsonObject hearing = Json.createObjectBuilder()
                .add("id", hearingId.toString())
                .add("jurisdictionType", "CROWN")
                .build();
        given(hearingLookupService.findHearing(hearingId, envelope)).willReturn(Optional.of(hearing));

        final MoveHearingToPastDateException thrown = assertThrows(MoveHearingToPastDateException.class,
                () -> listingCommandApi.handleMoveHearingToPastDate(envelope));

        assertThat(thrown.getHttpStatus(), is(422));
        assertThat(thrown.getErrorCode(), is("FUTURE_DATE_NOT_ALLOWED"));
        verify(courtSchedulerServiceAdapter, never()).moveHearingToPastDate(any(), any(), any(), any());
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldListCourtHearing() {

        //given
        uk.gov.justice.listing.commands.ListCourtHearing listCourtHearing1 = mock(uk.gov.justice.listing.commands.ListCourtHearing.class);
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, uk.gov.justice.listing.commands.ListCourtHearing.class)).willReturn(listCourtHearing1);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withNonDefaultDays(List.of(
                        uk.gov.justice.core.courts.NonDefaultDay.nonDefaultDay().withCourtScheduleId("123").build()
                ))
                .withType(mock())
                .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).build())
                .build();

        listingCommandApi.handleListCourtHearing(envelope);

        verify(sender).send(any());
    }


    @Test
    public void shouldIssueListNextHearingsEnrichedCommand() {

        final ListNextHearingsV2 listNextHearings = mock(ListNextHearingsV2.class);
        final UUID seedingHearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();
        final ArgumentCaptor<ListNextHearingsEnrichedV2> payloadCaptor = forClass(ListNextHearingsEnrichedV2.class);

        //given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());
        when(jsonObjectConverter.convert(payload, ListNextHearingsV2.class)).thenReturn(listNextHearings);
        when(listNextHearings.getSeedingHearing()).thenReturn(SeedingHearing.seedingHearing()
                .withSittingDay(sittingDay)
                .withSeedingHearingId(seedingHearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .build());

        final JsonValue finalPayload = mock(JsonValue.class);
        when(objectToJsonValueConverter.convert(payloadCaptor.capture())).thenReturn(finalPayload);

        //when
        listingCommandApi.listNextHearings(envelope);

        //then
        final ListNextHearingsEnrichedV2 value = payloadCaptor.getValue();
        assertThat(value, notNullValue(ListNextHearingsEnrichedV2.class));
        assertThat(value.getListNextHearings(), notNullValue());
        final SeedingHearing seedingHearing = value.getSeedingHearing();
        assertThat(seedingHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(seedingHearing.getSittingDay(), is(sittingDay));

    }

    @Test
    public void shouldPublishCourtList() {

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = forClass(JsonEnvelope.class);

        listingCommandApi.handlePublishCourtList(envelope);

        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldPublishCourtListForCrownCourtsForwardsAsExpected() {

        listingCommandApi.handlePublishCourtListForCrownCourts(envelope);

        verify(sender).send(envelope);
    }

    @Test
    public void shouldCourtListRequestExportAsExpected() {

        listingCommandApi.handleCourtListRequestExport(envelope);

        verify(sender).send(envelope);
    }

    @Test
    public void shouldListUnscheduledCourtHearingAsExcepted() {

        final ListUnscheduledCourtHearing listUnscheduledCourtHearing = mock(ListUnscheduledCourtHearing.class);
        final ArgumentCaptor<ListUnscheduledCourtHearingEnriched> payloadCaptor = forClass(ListUnscheduledCourtHearingEnriched.class);
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        when(jsonObjectConverter.convert(payload, ListUnscheduledCourtHearing.class)).thenReturn(listUnscheduledCourtHearing);

        when(listUnscheduledCourtHearing.getHearings()).thenReturn(createUnscheduledListingNeeds(hearingId1, hearingId2));
        mockCourtCentres();

        final JsonValue finalPayload = mock(JsonValue.class);
        when(objectToJsonValueConverter.convert(payloadCaptor.capture())).thenReturn(finalPayload);

        // run
        listingCommandApi.handleListUnscheduledCourtHearing(envelope);

        // verify
        final ListUnscheduledCourtHearingEnriched value = payloadCaptor.getValue();
        assertThat(value, notNullValue(ListUnscheduledCourtHearingEnriched.class));
        assertThat(value.getCourtCentresDetails().size(), is(2));
        assertThat(value.getCourtCentresDetails().get(0).getDefaultDuration(), anyOf(is(10), is(20)));
        assertThat(value.getHearings().size(), is(2));
        assertThat(value.getHearings().get(0).getId(), is(hearingId1));
        assertThat(value.getHearings().get(1).getId(), is(hearingId2));

    }

    @Test
    public void shouldListUnscheduledNextHearings() {

        final ListUnscheduledNextHearings listUnscheduledNextHearings = mock(ListUnscheduledNextHearings.class);
        final ArgumentCaptor<ListUnscheduledNextHearingsEnriched> payloadCaptor = forClass(ListUnscheduledNextHearingsEnriched.class);
        final UUID seedHearingId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final String sittingDay = LocalDate.now().toString();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        when(jsonObjectConverter.convert(payload, ListUnscheduledNextHearings.class)).thenReturn(listUnscheduledNextHearings);

        when(listUnscheduledNextHearings.getHearings()).thenReturn(createUnscheduledListingNeeds(hearingId1, hearingId2));
        when(listUnscheduledNextHearings.getSeedingHearing()).thenReturn(SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedHearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withSittingDay(sittingDay)
                .build());
        mockCourtCentres();

        final JsonValue finalPayload = mock(JsonValue.class);
        when(objectToJsonValueConverter.convert(payloadCaptor.capture())).thenReturn(finalPayload);

        // run
        listingCommandApi.handleListUnscheduledNextCourtHearings(envelope);

        // verify
        final ListUnscheduledNextHearingsEnriched value = payloadCaptor.getValue();
        assertThat(value, notNullValue(ListUnscheduledNextHearingsEnriched.class));
        assertThat(value.getCourtCentresDetails().size(), is(2));
        assertThat(value.getCourtCentresDetails().get(0).getDefaultDuration(), anyOf(is(10), is(20)));
        assertThat(value.getHearings().size(), is(2));
        assertThat(value.getHearings().get(0).getId(), is(hearingId1));
        assertThat(value.getHearings().get(1).getId(), is(hearingId2));
        final SeedingHearing seedingHearing = value.getSeedingHearing();
        assertThat(seedingHearing.getSeedingHearingId(), is(seedHearingId));
        assertThat(seedingHearing.getSittingDay(), is(sittingDay));

    }

    @Test
    public void shouldDeleteNextHearings() {

        final DeleteNextHearings deleteNextHearings = mock(DeleteNextHearings.class);
        final ArgumentCaptor<DefaultEnvelope> senderJsonEnvelopeCaptor = forClass(DefaultEnvelope.class);
        final UUID seedHearingId = randomUUID();
        final String sittingDay = LocalDate.now().toString();
        final JsonObject seedingHearing = JsonObjects.createObjectBuilder()
                .add("seedHearingId", seedHearingId.toString())
                .add("sittingDay", sittingDay)
                .add("jurisdictionType", "CROWN")
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("seedingHearing")).thenReturn(seedingHearing);

        when(envelope.metadata()).thenReturn(metadataWithRandomUUIDAndName().build());

        // run
        listingCommandApi.handleDeleteNextHearings(envelope);

        // verify
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        final JsonObject jsonPayload = (JsonObject) senderJsonEnvelopeCaptor.getValue().payload();
        assertThat(jsonPayload.getJsonObject("seedingHearing"), is(seedingHearing));

    }


    @Test
    public void shouldCallNonDefaultDayDurationBuilder() {
        final JsonObject hearingSlotsResponse = givenPayload("/listing.command.hearingSlots.stub-data.json");

        final List<JudicialRole> judicialRoles = new ArrayList<>();
        ((JsonObject) hearingSlotsResponse
                .getJsonArray("hearingSlots").get(0))
                .getJsonArray("judiciaries")
                .stream()
                .map(JsonObject.class::cast)
                .forEach(judiciaryJsonObject ->
                        judicialRoles.add(JudicialRole.judicialRole()
                                .withIsBenchChairman(of(judiciaryJsonObject.getBoolean("benchChairman")))
                                .withIsDeputy(of(judiciaryJsonObject.getBoolean("deputy")))
                                .withJudicialId(UUID.fromString(judiciaryJsonObject.getString("judiciaryId")))
                                .withJudicialRoleType(
                                        JudicialRoleType.judicialRoleType()
                                                .withJudiciaryType(judiciaryJsonObject.getString("judiciaryType"))
                                                .build())
                                .build())
                );

        //given
        UUID hearingId = randomUUID();
        HearingType hearingType = HearingType.hearingType().withId(HEARING_TYPE.getId()).build();
        LocalDate startDate = LocalDate.now();
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getCourtRoomId()).willReturn(UUID.randomUUID());
        given(updateHearingForListing.getHearingId()).willReturn(hearingId);
        when(hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(any(), any(), any()))
                .thenReturn(updateHearingForListing);
        mockCourtCentres();

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor = forClass(Envelope.class);

        listingCommandApi.handleUpdateHearingForListing(envelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenJurisdictionTypeIsMagistratesCourtAndCourtRoomIdIsNull() {
        //given
        UUID hearingId = randomUUID();
        HearingType hearingType = HearingType.hearingType().withId(HEARING_TYPE.getId()).build();
        LocalDate startDate = LocalDate.now();
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(updateHearingForListing.getJurisdictionType()).willReturn(MAGISTRATES);
        given(updateHearingForListing.getJurisdictionType()).willReturn(MAGISTRATES);
        given(updateHearingForListing.getHearingId()).willReturn(hearingId);

        when(hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(any(), any(), any()))
                .thenReturn(updateHearingForListing);
        mockCourtCentres();

        assertThrows(BadRequestException.class, () -> listingCommandApi.handleUpdateHearingForListing(envelope));
    }

    @Test
    public void shouldExtendCourtHearing() {

        //given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(payload.getString("hearingId", null))
                .willReturn(randomUUID().toString());
        given(jsonObjectConverter.convert(payload, ExtendHearingForHearing.class))
                .willReturn(extendHearingForHearing);

        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());

        final ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor = forClass(Envelope.class);

        //when
        listingCommandApi.handleExtendHearingForHearing(envelope);

        //then
        verify(jsonObjectConverter, times(1)).convert(payload, ExtendHearingForHearing.class);
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldCreateListingNote() {
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("listing.command.create-listing-note");
        given(envelope.metadata()).willReturn(metadataBuilder.build());

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                forClass(JsonEnvelope.class);
        listingCommandApi.handleCreateNote(envelope);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("listing.command.create-listing-note"));
    }

    @Test
    public void shouldDeleteListingNote() {
        final JsonEnvelope command = mock(JsonEnvelope.class);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("listing.command.handler.delete-listing-note");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = forClass(JsonEnvelope.class);
        listingCommandApi.handleDeleteNote(command);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("listing.command.handler.delete-listing-note"));
    }

    @Test
    public void shouldEditNote() {
        final JsonEnvelope command = mock(JsonEnvelope.class);
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("listing.command.handler.edit-listing-note");
        when(command.metadata()).thenReturn(metadataBuilder.build());
        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor = forClass(JsonEnvelope.class);

        listingCommandApi.handleEditNote(command);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("listing.command.handler.edit-listing-note"));

    }

    @Test
    public void shouldHandleCorrectHearingDaysWithoutCourtCentre() {
        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);

        listingCommandApi.handleCorrectHearingDaysWithoutCourtCentre(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.correct-hearing-days-without-court-centre"));
    }

    @Test
    public void shouldHandleMarkUnallocatedHearingAsDuplicate() {
        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);

        listingCommandApi.handleMarkUnallocatedHearingAsDuplicate(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.mark-unallocated-hearing-as-duplicate"));
    }

    @Test
    public void shouldUpdateRelatedHearing() {
        UpdateRelatedHearing updateRelatedHearing = UpdateRelatedHearing.updateRelatedHearing()
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(randomUUID())
                        .build())
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .build()))
                .build();
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateRelatedHearing.class)).willReturn(updateRelatedHearing);

        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);
        when(payload.getString(anyString())).thenReturn(randomUUID().toString());

        listingCommandApi.updateRelatedHearing(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.update-existing-hearing"));
    }

    @Test
    public void shouldUpdateHearingForListingFromHmi() {

        //given
        UUID hearingId = randomUUID();
        HearingType hearingType = HearingType.hearingType().withId(HEARING_TYPE.getId()).build();
        LocalDate startDate = LocalDate.now();
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, UpdateHearingForListing.class)).willReturn(updateHearingForListing);
        given(envelope.metadata()).willReturn(metadataWithRandomUUIDAndName().build());
        given(updateHearingForListing.getCourtRoomId()).willReturn(UUID.randomUUID());
        given(updateHearingForListing.getHearingId()).willReturn(hearingId);
        when(hearingEnrichmentOrchestrator.enrichUpdateHearingForListing(any(), any(), any()))
                .thenReturn(updateHearingForListing);
        given(courtCentreFactory.getCourtCentre(any(), any())).willReturn(CourtCentreDetails.courtCentreDetails().build());
        mockCourtCentres();

        final ArgumentCaptor<Envelope> senderJsonEnvelopeCaptor = forClass(Envelope.class);

        final JsonArray prosecutionCasesArray = createArrayBuilder().build();
        given(payload.getJsonArray("prosecutionCases")).willReturn(prosecutionCasesArray);

        //when
        listingCommandApi.handleUpdateHearingForListing(envelope);

        //then
        verify(sender, times(1)).send(senderJsonEnvelopeCaptor.capture());
        verify(jsonObjectConverter, never()).convert(any(), eq(ProsecutionCases.class));
    }

    @Test
    public void shouldHandleListingCommandDeleteHearing() {
        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);

        listingCommandApi.handleDeleteHearing(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.delete-hearing"));
    }

    @Test
    public void shouldHandleUpdateHearingAddCaseBdf() {
        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);

        listingCommandApi.handleUpdateHearingAddCaseBdf(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.update-hearing-add-case-bdf"));
    }

    @Test
    public void shouldHandleDeletePreviousHearingsAndCreateNextHearing() {
        final Metadata mockMetadata = MetadataBuilderFactory.metadataWithRandomUUIDAndName().build();
        when(envelope.metadata()).thenReturn(mockMetadata);

        listingCommandApi.handleDeletePreviousHearingsAndCreateNextHearing(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("listing.command.delete-previous-hearings-and-create-next-hearing"));
    }

    private List<HearingUnscheduledListingNeeds> createUnscheduledListingNeeds(final UUID hearingId1, final UUID hearingId2) {
        return Arrays.asList(
                HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre().withId(randomUUID()).build())
                        .withId(hearingId1)
                        .build(),
                HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre().withId(randomUUID()).build())
                        .withId(hearingId2)
                        .build());
    }

    private void mockCourtCentres() {
        when(courtCentreFactory.getCourtCentre(any(), eq(envelope))).thenReturn(
                CourtCentreDetails.courtCentreDetails().withDefaultDuration(10).build(),
                CourtCentreDetails.courtCentreDetails().withDefaultDuration(20).build());
    }


}

