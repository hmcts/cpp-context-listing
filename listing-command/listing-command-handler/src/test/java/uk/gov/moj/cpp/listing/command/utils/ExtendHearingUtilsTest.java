package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingHelper.getEnvelopeForExtendPartialHearing;

import uk.gov.justice.listing.courts.Defendants;
import uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched;
import uk.gov.justice.listing.courts.Offences;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.utils.hearing.ExtendHearingUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExtendHearingUtilsTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);
    @InjectMocks
    ExtendHearingHelper extendHearingHelper;

    @InjectMocks
    ExtendHearingUtils extendHearingUtils;

    @Spy
    private ProsecutionCasesBuilder prosecutionCasesBuilder;

    @Mock
    private uk.gov.moj.cpp.listing.domain.aggregate.Hearing hearing;

    public static final UUID ALLOCATED_HEARING_ID = UUID.randomUUID();
    public static final UUID UNALLOCATED_HEARING_ID = UUID.randomUUID();
    public static final UUID CASE_ID1 = randomUUID();
    public static final UUID CASE_ID2 = randomUUID();
    public static final UUID CASE_ID3 = randomUUID();
    public static final UUID DEF_ID1 = randomUUID();
    public static final UUID DEF_ID2 = randomUUID();
    public static final UUID DEF_ID3 = randomUUID();
    public static final UUID DEF_ID4 = randomUUID();
    public static final UUID DEF_ID5 = randomUUID();
    public static final UUID OFF_ID1 = randomUUID();
    public static final UUID OFF_ID2 = randomUUID();
    public static final UUID OFF_ID3 = randomUUID();
    public static final UUID OFF_ID4 = randomUUID();
    public static final UUID OFF_ID5 = randomUUID();
    public static final UUID OFF_ID6 = randomUUID();
    public static final UUID OFF_ID7 = randomUUID();
    public static final UUID OFF_ID8 = randomUUID();
    public static final UUID OFF_ID9 = randomUUID();
    public static final UUID OFF_ID10 = randomUUID();

    private static final UUID EXTRA_OFFENCE_ID = fromString("2c053d44-c823-4554-9b62-fe27e5ad5b3b");

    @Test
    public void shouldBuildRequestedCaseDefendantOffenceMap() {

        final Map<UUID, Map<UUID, List<UUID>>> hearingMap = buildCaseDefendantMapToCompare(false);

        final JsonEnvelope envelopeForExtendPartialHearing = getEnvelopeForExtendPartialHearing(ALLOCATED_HEARING_ID.toString(),
                UNALLOCATED_HEARING_ID.toString(),
                CASE_ID1.toString(),
                CASE_ID2.toString(),
                DEF_ID1.toString(),
                DEF_ID2.toString(),
                DEF_ID3.toString(),
                OFF_ID1.toString(),
                OFF_ID2.toString(),
                OFF_ID3.toString(),
                OFF_ID4.toString());

        final ExtendHearingForHearingEnriched extendHearingForHearingEnriched = jsonObjectConverter.convert(envelopeForExtendPartialHearing.payloadAsJsonObject(), ExtendHearingForHearingEnriched.class);

        final Map<UUID, Map<UUID, List<UUID>>> hearingMapBuilt = extendHearingUtils.buildRequestedCaseDefendantOffenceMap(extendHearingForHearingEnriched.getProsecutionCases(), extendHearingForHearingEnriched.getUnAllocatedHearingId());

        assertThat(hearingMapBuilt, equalTo(hearingMap));
    }

    @Test
    public void shouldBuildPersistedCaseDefendantOffenceMap() {
        final Map<UUID, Map<UUID, List<UUID>>> hearingMap = buildCaseDefendantMapToCompare(true);

        final Hearing unallocatedHearingPersisted = extendHearingHelper.getHearingPersisted();

        final Map<UUID, Map<UUID, List<UUID>>> hearingMapBuilt = extendHearingUtils.buildPersistedCaseDefendantOffenceMap(unallocatedHearingPersisted);

        assertThat(hearingMap, equalTo(hearingMapBuilt));
    }

    @Test
    public void shouldUpdateUnallocatedHearingPartially() {
        final Hearing unallocatedHearingPersisted = extendHearingHelper.getHearingPersisted();
        final Map<UUID, Map<UUID, List<UUID>>> hearingRequestMap = buildCaseDefendantMapToCompare(false);

        final AtomicInteger numberOfOffences = new AtomicInteger(0);
        unallocatedHearingPersisted.getListedCases().forEach(lc -> lc.getDefendants().forEach(d -> d.getOffences().forEach(o -> numberOfOffences.incrementAndGet())));

        final Hearing updatedHearing = extendHearingUtils.updateUnallocatedHearing(unallocatedHearingPersisted, hearingRequestMap);

        final AtomicInteger numberOfOffencesAfterUpdate = new AtomicInteger(0);
        updatedHearing.getListedCases().forEach(lc -> lc.getDefendants().forEach(d -> d.getOffences().forEach(o -> numberOfOffencesAfterUpdate.incrementAndGet())));

        assertThat(numberOfOffencesAfterUpdate.get() < numberOfOffences.get(), equalTo(true));
    }

    @Test
    public void shouldExtractCasesToAllocatePartially() {
        final Hearing unallocatedHearingPersisted = extendHearingHelper.getHearingPersisted();
        final Map<UUID, Map<UUID, List<UUID>>> hearingRequestMap = buildCaseDefendantMapToCompare(false);

        final AtomicInteger numberOfOffences = new AtomicInteger(0);
        unallocatedHearingPersisted.getListedCases().forEach(lc -> lc.getDefendants().forEach(d -> d.getOffences().forEach(o -> numberOfOffences.incrementAndGet())));

        final List<ListedCase> listedCases = extendHearingUtils.extractCasesToMove(unallocatedHearingPersisted.getListedCases(), hearingRequestMap);

        final AtomicInteger numberOfOffencesAfterUpdate = new AtomicInteger(0);
        listedCases.forEach(lc -> lc.getDefendants().forEach(d -> d.getOffences().forEach(o -> numberOfOffencesAfterUpdate.incrementAndGet())));

        assertThat(numberOfOffencesAfterUpdate.get() < numberOfOffences.get(), equalTo(true));
    }

    @Test
    public void shouldGetPartiallyAllocationEventForUpdateHearingWhenStoredDataAndRequestAreDifferent() {
        final Hearing unallocatedHearingPersisted = extendHearingHelper.getHearingPersisted();

        final List<ProsecutionCases> prosecutionCases = Arrays.asList(ProsecutionCases.prosecutionCases()
                .withCaseId(of(CASE_ID1))
                .withDefendants(Arrays.asList(Defendants.defendants()
                        .withDefendantId(of(DEF_ID1))
                        .withOffences(Arrays.asList(Offences.offences()
                                .withOffenceId(OFF_ID1)
                                .build()))
                        .build()))
                .build());
        when(hearing.updateUnallocatedHearingPartially(eq(UNALLOCATED_HEARING_ID), any())).thenReturn(Stream.builder().build());
        final ArgumentCaptor<List> prosecutionCaseCapture = ArgumentCaptor.forClass(List.class);
        extendHearingUtils.createPartiallyAllocationEventForUpdateHearing(hearing, UNALLOCATED_HEARING_ID, prosecutionCases, unallocatedHearingPersisted);

        verify(hearing).updateUnallocatedHearingPartially(eq(UNALLOCATED_HEARING_ID), prosecutionCaseCapture.capture());

        final uk.gov.justice.listing.events.ProsecutionCases case1 = uk.gov.justice.listing.events.ProsecutionCases.prosecutionCases()
                .withCaseId(CASE_ID1)
                .withDefendants(Arrays.asList(uk.gov.justice.listing.events.Defendants.defendants()
                        .withDefendantId(DEF_ID2)
                        .withOffences(Arrays.asList(uk.gov.justice.listing.events.Offences.offences().withOffenceId(OFF_ID2).build(),
                                uk.gov.justice.listing.events.Offences.offences().withOffenceId(OFF_ID3).build()))
                        .build()))
                .build();

        final uk.gov.justice.listing.events.ProsecutionCases case2 = uk.gov.justice.listing.events.ProsecutionCases.prosecutionCases()
                .withCaseId(CASE_ID2)
                .withDefendants(Arrays.asList(uk.gov.justice.listing.events.Defendants.defendants()
                        .withDefendantId(DEF_ID3)
                        .withOffences(Arrays.asList(uk.gov.justice.listing.events.Offences.offences().withOffenceId(OFF_ID4).build(),
                                uk.gov.justice.listing.events.Offences.offences().withOffenceId(EXTRA_OFFENCE_ID).build()))
                        .build()))
                .build();

        assertThat(prosecutionCaseCapture.getValue().contains(case1), is(true));
        assertThat(prosecutionCaseCapture.getValue().contains(case2), is(true));
    }

    @Test
    public void shouldNotGetPartiallyAllocationEventForUpdateHearingWhenStoredDataAndRequestAreEqual() {
        final Hearing unallocatedHearingPersisted = Hearing.hearing()
                .withListedCases(Arrays.asList(ListedCase.listedCase()
                        .withId(CASE_ID1)
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(DEF_ID1)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(OFF_ID1)
                                                .build()))
                                        .build(),
                                Defendant.defendant()
                                        .withId(DEF_ID2)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(OFF_ID2)
                                                .build()))
                                        .build()))
                        .build())).build();


        final List<ProsecutionCases> prosecutionCases = Arrays.asList(ProsecutionCases.prosecutionCases()
                .withCaseId(of(CASE_ID1))
                .withDefendants(Arrays.asList(
                        Defendants.defendants()
                                .withDefendantId(of(DEF_ID1))
                                .withOffences(Arrays.asList(Offences.offences()
                                        .withOffenceId(OFF_ID1)
                                        .build()))
                                .build(),
                        Defendants.defendants()
                                .withDefendantId(of(DEF_ID2))
                                .withOffences(Arrays.asList(Offences.offences()
                                        .withOffenceId(OFF_ID2)
                                        .build()))
                                .build()))
                .build());
        when(hearing.updateUnallocatedHearingPartially(eq(UNALLOCATED_HEARING_ID), any())).thenReturn(Stream.builder().build());

        extendHearingUtils.createPartiallyAllocationEventForUpdateHearing(hearing, UNALLOCATED_HEARING_ID, prosecutionCases, unallocatedHearingPersisted);

        verify(hearing, never()).updateUnallocatedHearingPartially(eq(UNALLOCATED_HEARING_ID), any());
    }

    @Test
    public void shouldRemoveSelectedCaseDefendantsOffencesFromStored() {

        final Map<UUID, Map<UUID, List<UUID>>> storedHearingsMap = new HashMap<>();
        final Map<UUID, Map<UUID, List<UUID>>> selectedHearingsMap = new HashMap<>();
        final Map<UUID, Map<UUID, List<UUID>>> unselectedHearingsMap = new HashMap<>();

        buildStoredHearingsMap(storedHearingsMap);
        buildSelectedHearingsMap(selectedHearingsMap);
        buildUnselectedHearingsMap(unselectedHearingsMap);

        //add offence to both selected and stored maps
        final Map<UUID, List<UUID>> defendantMapCase3ForBothStoredAndSelected = new HashMap<>();
        defendantMapCase3ForBothStoredAndSelected.put(DEF_ID5, new ArrayList<>(Arrays.asList(OFF_ID9, OFF_ID10)));
        storedHearingsMap.put(CASE_ID3, defendantMapCase3ForBothStoredAndSelected);
        selectedHearingsMap.put(CASE_ID3, defendantMapCase3ForBothStoredAndSelected);

        extendHearingUtils.removeSelectedCaseDefendantsOffencesFromStored(storedHearingsMap, selectedHearingsMap);

        assertThat(unselectedHearingsMap.equals(storedHearingsMap), is(true));
    }

    private void buildUnselectedHearingsMap(final Map<UUID, Map<UUID, List<UUID>>> unselectedHearingsMap) {
        final Map<UUID, List<UUID>> defendantMapCase1ForUnSelected = new HashMap<>();
        final Map<UUID, List<UUID>> defendantMapCase2ForUnselected = new HashMap<>();
        defendantMapCase1ForUnSelected.put(DEF_ID2, new ArrayList<>(Arrays.asList(OFF_ID4)));
        defendantMapCase2ForUnselected.put(DEF_ID4, new ArrayList<>(Arrays.asList(OFF_ID7, OFF_ID8)));
        unselectedHearingsMap.put(CASE_ID1, defendantMapCase1ForUnSelected);
        unselectedHearingsMap.put(CASE_ID2, defendantMapCase2ForUnselected);
    }

    private void buildSelectedHearingsMap(final Map<UUID, Map<UUID, List<UUID>>> selectedHearingMap) {
        final Map<UUID, List<UUID>> defendantMapCase1ForSelected = new HashMap<>();
        final Map<UUID, List<UUID>> defendantMapCase2ForSelected = new HashMap<>();
        defendantMapCase1ForSelected.put(DEF_ID1, new ArrayList<>(Arrays.asList(OFF_ID1, OFF_ID2)));
        defendantMapCase1ForSelected.put(DEF_ID2, new ArrayList<>(Arrays.asList(OFF_ID3)));
        defendantMapCase2ForSelected.put(DEF_ID3, new ArrayList<>(Arrays.asList(OFF_ID5, OFF_ID6)));
        selectedHearingMap.put(CASE_ID1, defendantMapCase1ForSelected);
        selectedHearingMap.put(CASE_ID2, defendantMapCase2ForSelected);
    }

    private void buildStoredHearingsMap(final Map<UUID, Map<UUID, List<UUID>>> storedHearingMap) {
        final Map<UUID, List<UUID>> defendantMapCase1ForStored = new HashMap<>();
        final Map<UUID, List<UUID>> defendantMapCase2ForStored = new HashMap<>();
        defendantMapCase1ForStored.put(DEF_ID1, new ArrayList<>(Arrays.asList(OFF_ID1, OFF_ID2)));
        defendantMapCase1ForStored.put(DEF_ID2, new ArrayList<>(Arrays.asList(OFF_ID3, OFF_ID4)));
        defendantMapCase2ForStored.put(DEF_ID3, new ArrayList<>(Arrays.asList(OFF_ID5, OFF_ID6)));
        defendantMapCase2ForStored.put(DEF_ID4, new ArrayList<>(Arrays.asList(OFF_ID7, OFF_ID8)));
        storedHearingMap.put(CASE_ID1, defendantMapCase1ForStored);
        storedHearingMap.put(CASE_ID2, defendantMapCase2ForStored);
    }

    private Map<UUID, Map<UUID, List<UUID>>> buildCaseDefendantMapToCompare(final boolean isForPersisted) {
        final Map<UUID, Map<UUID, List<UUID>>> hearingMap = new HashMap<>();
        final Map<UUID, List<UUID>> defendantMapCase1 = new HashMap<>();
        defendantMapCase1.put(DEF_ID2, Arrays.asList(OFF_ID2, OFF_ID3));
        defendantMapCase1.put(DEF_ID1, Arrays.asList(OFF_ID1));
        hearingMap.put(CASE_ID1, defendantMapCase1);

        final Map<UUID, List<UUID>> defendantMapCase2 = new HashMap<>();
        if (isForPersisted) {
            defendantMapCase2.put(DEF_ID3, Arrays.asList(OFF_ID4, EXTRA_OFFENCE_ID));
        } else {
            defendantMapCase2.put(DEF_ID3, Arrays.asList(OFF_ID4));
        }
        hearingMap.put(CASE_ID2, defendantMapCase2);

        return hearingMap;
    }
}
