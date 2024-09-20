package uk.gov.moj.cpp.listing.query.document.generator;

import static java.util.Locale.UK;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createReader;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataCache;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.query.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.api.util.FileUtil;
import uk.gov.moj.cpp.listing.query.document.generator.util.SittingsSorter;

import java.io.StringReader;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JudgeListTemplateAssemblerTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID HEARING_ID2 = randomUUID();
    private static final UUID HEARING_ID3 = randomUUID();
    private static final UUID HEARING_ID4 = randomUUID();
    private static final UUID CASE_ID1 = randomUUID();
    private static final UUID CASE_ID2 = randomUUID();
    private static final UUID CASE_ID3 = randomUUID();
    private static final UUID CASE_ID4 = randomUUID();
    private static final String START_DATE = "2019-01-29";
    private static final String QUERY_ACTION_NAME = "listing.public.list";
    private static final String COURT_CENTRE_NAME = STRING.next();
    private static final String WELSH = "Welsh";
    private static final String ADDRESS_1 = STRING.next();
    private static final String ADDRESS_2 = STRING.next();
    private static final String ADDRESS_3 = STRING.next();
    private static final String ADDRESS_4 = STRING.next();
    private static final String ADDRESS_5 = STRING.next();
    private static final String POSTCODE = STRING.next();
    private static final UUID COURT_ROOM_1_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_2_ID = UUID.randomUUID();
    private static final String COURT_ROOM_NAME_1 = "Room 1";
    private static final String COURT_ROOM_NAME_2 = "Room 2";
    private static final UUID JUDICIARY_ID = UUID.randomUUID();
    private static final UUID JUDICIARY2_ID = UUID.randomUUID();
    private static final UUID JUDICIARY3_ID = UUID.randomUUID();


    @BeforeAll
    public static void beforeClass() {
        //Not needed after AM/PM vs am/pm java.util.Locale issue is resolved in EA-11374
        Locale.setDefault(UK);
    }

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonValueConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Mock
    private CourtCentreFactory courtCentreFactory;

    @Mock
    private ReferenceDataCache referenceDataCache;

    @Spy
    private SittingsSorter sittingsSorter;

    @InjectMocks
    JudgeListTemplateAssembler assembler;


    @Test
    public void shouldReturnOneSittingHearingWhenOneHearingExistForGivenCourtRoomAndHearingDate() {


        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(eq(JUDICIARY_ID)))
                .thenReturn(generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"));

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForJudgeList()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.single.hearing.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }


    @Test
    public void shouldReturnOneSittingWithMultipleHearingWhenMultipleHearingExistForGivenCourtRoomAndHearingDateAndSortByHearingTimeForOneJudgeAndHearingSortByStartTime() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(eq(JUDICIARY_ID)))
                .thenReturn(generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"));

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForJudgeListForSameJudiciaries()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.multiple.hearing.same.judiciary.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }



    @Test
    public void shouldReturnTwoSittingWitheHearingWhenHearingExistForDifferentJudiciariesGivenCourtRoomAndHearingDateAndSortByHearingTimeAndSittingTime() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(any(UUID.class)))
                .thenReturn(
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"),
                        generateJudiciary(JUDICIARY2_ID, "Mary", "Her Majesty Honour", "Judge", "Mrs")
                );

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForDifferentJudgeList()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.multiple.hearing.diff.judiciary.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }


    @Test
    public void shouldReturnTThreeSittingWitheHearingWhenHearingExistForDifferentJudiciariesGivenCourtRoomAndOneHearingNoJudiciaryAllocatedHearingDateAndSortByHearingTimeAndSittingTime() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(any(UUID.class)))
                .thenReturn(
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"),
                        generateJudiciary(JUDICIARY2_ID, "Mary", "Her Majesty Honour", "Judge", "Mr")
                );

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForDifferentJudgeListWithEmptyJudiciary()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.multiple.hearing.diff.judiciary_one_empty.judiciary.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }


    @Test
    public void shouldReturnTThreeSittingWitheHearingWhenHearingExistForDifferentJudiciariesGivenCourtRoomAndMultiHearingNoJudiciaryAllocatedHearingDateAndSortByHearingTimeAndSittingTime() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(any(UUID.class)))
                .thenReturn(
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"),
                        generateJudiciary(JUDICIARY2_ID, "Mary", "Her Majesty Honour", "Judge", "Mr")
                );

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForDifferentJudgeListWithMultiEmptyJudiciary()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.multiple.hearing.diff.judiciary_multi_empty.judiciary.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }


    @Test
    public void shouldReturnOneSittingHearingWhenOneHearingExistForGivenCourtRoomAndHearingDateWithJudiciariesSorting() {


        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(any(UUID.class)))
                .thenReturn(
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "High Court Judge", "Mrs"),
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Magistrate", "Mrs"),
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Recorder", "Recorder1", "Mrs")
                );

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForMultipleJudgeList()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.single.hearing.mutipleJudicaries.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }

    @Test
    public void shouldReturnSittingWithMultiHearingsForMultiCases() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(any(UUID.class)))
                .thenReturn(
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"),
                        generateJudiciary(JUDICIARY2_ID, "Mary", "Her Majesty Honour", "Judge", "Mr")
                );

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForDifferentJudgeListAndMultiCases()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.multiple.cases.in.hearing.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }


    @Test
    public void shouldReturnSittingWithMultiHearingsForMultiCasesWithCounsels() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(any(UUID.class)))
                .thenReturn(
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"),
                        generateJudiciary(JUDICIARY2_ID, "Mary", "Her Majesty Honour", "Judge", "Mr")
                );

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForDifferentJudgeListAndMultiCasesWithCounsels()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.multiple.cases.in.hearing.with.counsels.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }

    @Test
    public void shouldReturnSittingWithHearingsAndIgnoreApplications() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(any(UUID.class)))
                .thenReturn(
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"),
                        generateJudiciary(JUDICIARY2_ID, "Mary", "Her Majesty Honour", "Judge", "Mr")
                );

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForListedCasesAndApplications()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.ignore.applications.in.hearing.with.counsels.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }


    @Test
    public void shouldReturnNoSittingWithHearingsHasOnlyApplications() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForApplications()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.ignore.all.application.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }

    @Test
    public void shouldReturnNoSittingWithHearingsHasLinkecApplications() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(any(UUID.class)))
                .thenReturn(
                        generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"),
                        generateJudiciary(JUDICIARY2_ID, "Mary", "Her Majesty Honour", "Judge", "Mr")
                );

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForListedCasesAndApplicationsAndLinkedApplications()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.ignore.linked.application.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }


    @Test
    public void shouldReturnOnlySameDayHearingSittingForAMultiDayHearing() {

        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForMultiHearingDays()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, "2020-10-09");
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.sameday.hearing.from.multi.hearing.days.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }


    @Test
    public void shouldReturnOneSittingHearingWhenOneHearingExistForGivenCourtRoomAndHearingDateForLegalEntityDefendant() {


        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(eq(JUDICIARY_ID)))
                .thenReturn(generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"));

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForJudgeListWithLegalEntityDefendant()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.single.hearing.with.legal.entity.defendant.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }

    @Test
    public void shouldReturnOneSittingWithMultipleHearingWhenMultipleHearingExistForGivenCourtRoomAndHearingDateAndSortByHearingTimeForOneJudgeAndHearingSortByStartTimeReverseList() {
        when(courtCentreFactory.getCourtCentre(eq(COURT_CENTRE_ID), any(JsonEnvelope.class)))
                .thenReturn(generateCourtCentreDetails());
        when(referenceDataCache.getJudiciariesMapCache(eq(JUDICIARY_ID)))
                .thenReturn(generateJudiciary(JUDICIARY_ID, "Sarah", "Her Majesty", "Court Judge", "Mr"));

        Optional<JsonObject> judgeList = assembler.assemble(buildRequestEnvelope(buildHearingDataForJudgeListForSameJudiciariesReverseList()), COURT_CENTRE_ID.toString(), COURT_ROOM_1_ID.toString(), CourtListType.JUDGE, START_DATE);
        final JsonObject expectedList = returnAsJsonObject("expected/list.of.judgesList.for.multiple.hearing.same.judiciary.reverse.list.json");
        assertThat(judgeList.isPresent(), is(true));
        assertThat(judgeList.get(), equalTo(expectedList));
    }

    private Optional<Judiciary> generateJudiciary(final UUID judiciaryId, final String surName, final String requestedName, final String judiciaryType, final String title) {
        Judiciary.Builder builder = new Judiciary.Builder();
        return Optional.of(builder.withId(judiciaryId).withSurname(surName).withJudiciaryType(judiciaryType).withRequestedName(requestedName).withTitlePrefix(title).build());
    }



    private JsonObject buildHearingDataForJudgeListForSameJudiciaries() {
        String jsonString = FileUtil.getPayload("stubbed.findHearings.multipleHearings.ForSameJudgeListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID.toString() + "\"")
                .replaceAll("HEARING_ID", HEARING_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildHearingDataForJudgeListForSameJudiciariesReverseList() {
        String jsonString = FileUtil.getPayload("stubbed.findHearings.multipleHearings.ForSameJudgeReverseListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("JUDICIARY_ID", JUDICIARY_ID.toString())
                .replaceAll("HEARING_ID", HEARING_ID.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildHearingDataForJudgeList() {
        String jsonString = FileUtil.getPayload("stubbed.hearingRepository.findHearingsForJudgeListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID.toString() + "\"")
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID1.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildHearingDataForJudgeListWithLegalEntityDefendant() {
        String jsonString = FileUtil.getPayload("stubbed.hearingRepository.findHearingsForJudgeListScenario-LegalEntityDefendant.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID.toString() + "\"")
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID1.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }


    private JsonObject buildHearingDataForMultipleJudgeList() {
        String jsonString = FileUtil.getPayload("stubbed.findHearings.ForMultiJudciariesListSortScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"")
                .replaceAll("JUDICIARY_ID1", JUDICIARY2_ID.toString())
                .replaceAll("JUDICIARY_ID2", JUDICIARY3_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID1.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildHearingDataForDifferentJudgeList() {
        String jsonString = FileUtil.getPayload("stubbed.findHearings.multipleHearings.ForDifferentJudgeListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"")
                .replaceAll("JUDICIARY_ID1", JUDICIARY2_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID2.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildHearingDataForDifferentJudgeListWithEmptyJudiciary() {
        String jsonString = FileUtil.getPayload("stubbed.findHearings.multipleHearings.ForDifferentJudge.unallocatedJudiciaries.ListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"")
                .replaceAll("JUDICIARY_ID1", JUDICIARY2_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("HEARING_ID3", HEARING_ID3.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID2.toString())
                .replaceAll("CASE_ID3", CASE_ID3.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildHearingDataForDifferentJudgeListWithMultiEmptyJudiciary() {
        String jsonString = FileUtil.getPayload("stubbed.findHearings.multipleHearings.ForDifferentJudge.multipleUnallocatedJudiciaries.ListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"")
                .replaceAll("JUDICIARY_ID1", JUDICIARY2_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("HEARING_ID3", HEARING_ID3.toString())
                .replaceAll("HEARING_ID4", HEARING_ID4.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID4", CASE_ID4.toString())
                .replaceAll("CASE_ID2", CASE_ID2.toString())
                .replaceAll("CASE_ID3", CASE_ID3.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }


    private JsonObject buildHearingDataForDifferentJudgeListAndMultiCases() {
        String jsonString = FileUtil.getPayload("stubbed.multiCases.multipleHearings.ForDifferentJudge.multipleUnallocatedJudiciaries.ListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"")
                .replaceAll("JUDICIARY_ID1", JUDICIARY2_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID2.toString())
                .replaceAll("CASE_ID3", CASE_ID3.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }


    private JsonObject buildHearingDataForDifferentJudgeListAndMultiCasesWithCounsels() {
        String jsonString = FileUtil.getPayload("stubbed.multiCases.multipleHearings.counsels.ListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"")
                .replaceAll("JUDICIARY_ID1", JUDICIARY2_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID2.toString())
                .replaceAll("CASE_ID3", CASE_ID3.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }


    private JsonObject buildHearingDataForListedCasesAndApplications() {
        String jsonString = FileUtil.getPayload("stubbed.multiCases.multipleHearings.application.ListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"")
                .replaceAll("JUDICIARY_ID1", JUDICIARY2_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID2.toString())
                .replaceAll("CASE_ID3", CASE_ID3.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildHearingDataForApplications() {
        String jsonString = FileUtil.getPayload("stubbed.multipleHearings.only.application.ListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"")
                .replaceAll("JUDICIARY_ID1", JUDICIARY2_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildHearingDataForListedCasesAndApplicationsAndLinkedApplications() {
        String jsonString = FileUtil.getPayload("stubbed.multiCases.multipleHearings.linkedapplication.ListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("\"JUDICIARY_ID\"", "\"" + JUDICIARY_ID + "\"")
                .replaceAll("JUDICIARY_ID1", JUDICIARY2_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID2.toString())
                .replaceAll("CASE_ID3", CASE_ID3.toString())
                .replaceAll("HEARING_ID2", HEARING_ID2.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonObject buildHearingDataForMultiHearingDays() {
        String jsonString = FileUtil.getPayload("stubbed.multipleHearingsDays.ListScenario.json")
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replaceAll("COURT_ROOM_ID", COURT_ROOM_1_ID.toString())
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID3.toString());
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }



    private JsonEnvelope buildRequestEnvelope(JsonObject hearingData) {

        return envelopeFrom(
                metadataOf(randomUUID(), QUERY_ACTION_NAME)
                        .withUserId(UUID.randomUUID().toString())
                        .build(),
                hearingData);
    }

    private JsonObject returnAsJsonObject(final String expectedJsonPath) {
        final String payload = FileUtil.getPayload(expectedJsonPath);
        try (JsonReader jsonReader = createReader(new StringReader(payload))) {
            return jsonReader.readObject();
        }
    }

    private CourtCentreDetails generateCourtCentreDetails() {
        return CourtCentreDetails.courtCentreDetails()
                .withId(COURT_CENTRE_ID)
                .withCourtCentreName(COURT_CENTRE_NAME)
                .withWelshCourtCentreName(WELSH + COURT_CENTRE_NAME)
                .withAddress1(ADDRESS_1)
                .withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3)
                .withAddress4(ADDRESS_4)
                .withAddress5(ADDRESS_5)
                .withWelshAddress1(WELSH + ADDRESS_1)
                .withWelshAddress2(WELSH + ADDRESS_2)
                .withWelshAddress3(WELSH + ADDRESS_3)
                .withWelshAddress4(WELSH + ADDRESS_4)
                .withWelshAddress5(WELSH + ADDRESS_5)
                .withPostcode(POSTCODE)
                .withWelsh(false)
                .withCourtRooms(
                        ImmutableMap.of(
                                COURT_ROOM_1_ID,
                                CourtRoomDetails.courtRoomDetails()
                                        .withId(COURT_ROOM_1_ID)
                                        .withCourtRoomName(COURT_ROOM_NAME_1)
                                        .withWelshCourtRoomName(WELSH + COURT_ROOM_NAME_1)
                                        .build(),
                                COURT_ROOM_2_ID,
                                CourtRoomDetails.courtRoomDetails()
                                        .withId(COURT_ROOM_2_ID)
                                        .withCourtRoomName(COURT_ROOM_NAME_2)
                                        .withWelshCourtRoomName(WELSH + COURT_ROOM_NAME_2)
                                        .build()
                        ))
                .build();
    }
}