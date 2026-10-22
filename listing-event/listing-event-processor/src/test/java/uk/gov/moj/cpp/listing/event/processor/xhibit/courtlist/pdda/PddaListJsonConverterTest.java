package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.pdda;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.DRAFT;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FINAL;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FIRM;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.WARN;

import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.everit.json.schema.FormatValidator;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PddaListJsonConverterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PddaListJsonConverter converter = new PddaListJsonConverter();

    public static Stream<Arguments> expectedXhibitDocuments() {
        return Stream.of(
                Arguments.of("expectedDraftList.xml", DRAFT),
                Arguments.of("expectedDraftListSummerTime.xml", DRAFT),
                Arguments.of("expectedFinalList.xml", FINAL),
                Arguments.of("expectedRestrictedFinalList.xml", FINAL),
                Arguments.of("expectedRestrictedFinalListIeWithMultipleCaseConvertedIntoMultipleHearing.xml", FINAL),
                Arguments.of("expectedCourtApplicationWithSubjectDetailPresentFinalList.xml", FINAL),
                Arguments.of("expectedWarnedList.xml", WARN),
                Arguments.of("expectedSingleWarnedListMapper.xml", WARN),
                Arguments.of("expectedWarnedListWithCaseHidden.xml", WARN),
                Arguments.of("expectedWarnedListWithDifferentHearingTypesInCase.xml", WARN),
                Arguments.of("expectedWarnedListWithMultipleHearingTypesInCase.xml", WARN),
                Arguments.of("expectedInactiveCaseWarnList.xml", WARN),
                Arguments.of("expectedFirmList.xml", FIRM),
                Arguments.of("expectedCourtApplicationWithSubjectDetailAbsentFirmList.xml", FIRM));
    }

    @ParameterizedTest(name = "{0} converts to a valid PDDA {1} document")
    @MethodSource("expectedXhibitDocuments")
    public void shouldConvertXhibitXmlToPddaJsonThatValidatesAgainstTheSchema(final String xmlFile, final PublishCourtListType type) throws IOException {
        final JsonNode json = converter.convert(readXml(xmlFile), type);

        final Schema schema = loadSchema(PddaListJsonConverter.schemaResource(type));
        try {
            schema.validate(new JSONObject(OBJECT_MAPPER.writeValueAsString(json)));
        } catch (final ValidationException e) {
            throw new AssertionError(xmlFile + " failed PDDA validation: " + e.getAllMessages() + "\n" + json.toPrettyString(), e);
        }
    }

    @Test
    public void shouldUnwrapListsAndTypeCourtHouseCodeAsInteger() throws IOException {
        final JsonNode json = converter.convert(readXml("expectedFinalList.xml"), FINAL);

        final JsonNode dailyList = json.get("DailyList");
        assertTrue(dailyList.get("CourtLists").isArray());
        assertTrue(dailyList.get("CourtLists").get(0).get("Sittings").isArray());
        assertTrue(dailyList.get("CourtLists").get(0).get("Sittings").get(0).get("Hearings").isArray());
        assertTrue(dailyList.get("CrownCourt").get("CourtHouseCode").isInt());
        assertEquals(0, dailyList.get("CrownCourt").get("CourtHouseCode").asInt());
    }

    @Test
    public void shouldMoveXmlAttributesIntoProperties() throws IOException {
        final JsonNode json = converter.convert(readXml("expectedFinalList.xml"), FINAL);

        final JsonNode hearing = json.get("DailyList").get("CourtLists").get(0).get("Sittings").get(0).get("Hearings").get(0);
        assertEquals("TRL", hearing.get("HearingDetails").get("HearingType").asText());
        assertEquals("Crown Prosecution Service", hearing.get("Prosecution").get("ProsecutingAuthority").asText());
        final JsonNode charge = hearing.get("Defendants").get(0).get("Charges").get(0);
        assertEquals("OF61131", charge.get("CJSoffenceCode").asText());
        assertTrue(charge.get("IndictmentCountNumber").isInt());
    }

    @Test
    public void shouldWrapSingleElementsIntoArrays() throws IOException {
        final JsonNode json = converter.convert(readXml("expectedFirmList.xml"), FIRM);

        final JsonNode counsel = json.findValue("Counsel");
        assertTrue(counsel.isArray());
        assertTrue(counsel.get(0).get("Solicitor").isArray());
        final JsonNode forenames = json.findValue("CitizenNameForename");
        assertTrue(forenames.isArray());
        assertThat(forenames.size(), is(1));
    }

    @Test
    public void shouldConvertZoneLessTimestampToUtc() throws IOException {
        final JsonNode json = converter.convert(readXml("expectedFirmList.xml"), FIRM);

        // 2026-11-02T10:00:00 London local time (GMT in November)
        assertEquals("2026-11-02T10:00:00Z", json.get("FirmList").get("DocumentID").get("TimeStamp").asText());
    }

    @Test
    public void shouldLeaveOutFirmCourtListsWithoutSittingsAndWithoutSittingDate() throws IOException {
        final JsonNode json = converter.convert(readXml("expectedFirmList.xml"), FIRM);

        for (final JsonNode courtList : json.get("FirmList").get("CourtLists")) {
            assertTrue(courtList.has("SittingDate"));
        }
    }

    @Test
    public void shouldAddCaseNumberCaTHCopiedFromCaseNumberUntilItHasASource() throws IOException {
        final JsonNode json = converter.convert(readXml("expectedFinalList.xml"), FINAL);

        final JsonNode hearing = json.get("DailyList").get("CourtLists").get(0).get("Sittings").get(0).get("Hearings").get(0);
        assertEquals(hearing.get("CaseNumber").asText(), hearing.get("CaseNumberCaTH").asText());
    }

    @Test
    public void shouldDropPropertiesThatAreNotInThePddaSchema() throws IOException {
        final JsonNode json = converter.convert(readXml("expectedFinalList.xml"), FINAL);

        assertFalse(json.get("DailyList").get("DocumentID").has("DocumentName"));
        assertFalse(json.get("DailyList").get("ListHeader").has("ListCategory"));
        assertTrue(json.get("DailyList").get("DocumentID").has("UniqueID"));
    }

    @Test
    public void shouldPutWarnedCasesIntoFixtureArrays() throws IOException {
        final JsonNode json = converter.convert(readXml("expectedWarnedList.xml"), WARN);

        final JsonNode withFixedDate = json.get("WarnedList").get("CourtLists").get(0).get("WithFixedDate");
        assertTrue(withFixedDate.isArray());
        assertThat(withFixedDate.get(0).get("Fixture").get(0).get("Cases").size() > 0, is(true));
        assertThat(withFixedDate.get(0).get("Fixture").get(0).get("Cases").get(0).get("Defendants").size() > 1, is(true));
    }

    // the fixtures are Xhibit XML; the converter takes the DOM tree that the generator marshals from JAXB
    private Element readXml(final String file) throws IOException {
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream("xhibit/" + file)) {
            final String xml = new String(in.readAllBytes(), UTF_8).replace("#TIME_STAMP#", "2026-11-02T10:00:00");
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
        } catch (final ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    private Schema loadSchema(final String resource) throws IOException {
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            final JSONObject schemaJson = new JSONObject(new JSONTokener(in));
            // the validator library knows draft 4 to 6 only; the keywords the PDDA schemas use are common to all
            schemaJson.remove("$schema");
            return SchemaLoader.builder()
                    .schemaJson(schemaJson)
                    .draftV6Support()
                    .addFormatValidator(new FormatValidator() {
                        @Override
                        public Optional<String> validate(final String subject) {
                            return subject.matches("\\d{2}:\\d{2}:\\d{2}") ? Optional.empty() : Optional.of(subject + " is not a valid time");
                        }

                        @Override
                        public String formatName() {
                            return "time";
                        }
                    })
                    .build()
                    .load()
                    .build();
        }
    }
}
