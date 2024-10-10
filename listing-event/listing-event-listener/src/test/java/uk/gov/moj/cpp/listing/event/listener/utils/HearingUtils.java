package uk.gov.moj.cpp.listing.event.listener.utils;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Defendants;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.Offences;
import uk.gov.justice.listing.events.ProsecutionCases;
import uk.gov.moj.cpp.listing.domain.ApplicantRespondent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

public class HearingUtils {

    public static final UUID HEARING_ID = randomUUID();
    public static final UUID CASE_ID1 = randomUUID();
    public static final UUID CASE_ID2 = randomUUID();
    public static final UUID DEF_ID1 = randomUUID();
    public static final UUID DEF_ID2 = randomUUID();
    public static final UUID OFF_ID1 = randomUUID();
    public static final UUID OFF_ID2 = randomUUID();
    public static final UUID OFF_ID3 = randomUUID();
    public static final UUID OFF_ID4 = randomUUID();
    public static final UUID OFF_ID5 = randomUUID();
    public static final UUID OFF_ID6 = randomUUID();
    public static final UUID DEF_ID3 = randomUUID();
    public static final UUID DEF_ID4 = randomUUID();


    public static String getStringFromResource(final String path) throws IOException {
        final InputStream inputStream = HearingUtils.class.getClassLoader().getResourceAsStream(path);
        assertThat(inputStream, notNullValue());
        return IOUtils.toString(inputStream);
    }

    public static List<ListedCase> createListedCases(final UUID caseId1, final UUID caseId2, final UUID defId1, final UUID defId2, final UUID offId1, final UUID offId2, final UUID offId3) {
        return Arrays.asList(ListedCase.listedCase()
                        .withId(caseId1)
                        .withDefendants(Arrays.asList(Defendant.defendant().withId(defId1)
                                .withOffences(Arrays.asList(Offence.offence().withId(offId1).build(),
                                        Offence.offence().withId(offId2).build()))
                                .build()))
                        .build(),
                ListedCase.listedCase()
                        .withId(caseId2)
                        .withDefendants(Arrays.asList(Defendant.defendant().withId(defId2)
                                .withOffences(Arrays.asList(Offence.offence().withId(offId3).build()))
                                .build()))
                        .build());
    }

    public static List<ProsecutionCases> buildEventProsecutionCases() {
        return Arrays.asList(ProsecutionCases.prosecutionCases()
                .withCaseId(CASE_ID1)
                .withDefendants(Arrays.asList(Defendants.defendants()
                        .withDefendantId(DEF_ID1)
                        .withOffences(Arrays.asList(Offences.offences().withOffenceId(OFF_ID1).build()))
                        .build()))
                .build());
    }

    public static List<ProsecutionCases> buildEventProsecutionCasesForSplit() {
        return Arrays.asList(ProsecutionCases.prosecutionCases()
                .withCaseId(CASE_ID1)
                .withDefendants(Arrays.asList(Defendants.defendants()
                        .withDefendantId(DEF_ID4)
                        .withOffences(Arrays.asList(Offences.offences().withOffenceId(OFF_ID1).build(),
                                Offences.offences().withOffenceId(OFF_ID2).build()))
                        .build()))
                .build());
    }

    public static List<ProsecutionCases> buildEventProsecutionCase2() {
        return Arrays.asList(ProsecutionCases.prosecutionCases()
                .withCaseId(CASE_ID2)
                .withDefendants(Arrays.asList(Defendants.defendants()
                        .withDefendantId(DEF_ID2)
                        .withOffences(Arrays.asList(Offences.offences().withOffenceId(OFF_ID2).build(), Offences.offences().withOffenceId(OFF_ID3).build()))
                        .build()))
                .build());
    }

    public static List<ProsecutionCases> buildEventProsecutionCaseForMultiple() {
        return Arrays.asList(ProsecutionCases.prosecutionCases()
                .withCaseId(CASE_ID1)
                .withDefendants(Arrays.asList(Defendants.defendants()
                        .withDefendantId(DEF_ID1)
                        .withOffences(Arrays.asList(Offences.offences().withOffenceId(OFF_ID1).build()))
                        .build()))
                .build(),
                ProsecutionCases.prosecutionCases()
                        .withCaseId(CASE_ID2)
                        .withDefendants(Arrays.asList(Defendants.defendants()
                                .withDefendantId(DEF_ID2)
                                .withOffences(Arrays.asList(Offences.offences().withOffenceId(OFF_ID2).build()))
                                .build()))
                        .build());
    }


    public static List<ProsecutionCases> buildEventProsecutionCaseForMultipleDefendants() {
        return Arrays.asList(ProsecutionCases.prosecutionCases()
                        .withCaseId(CASE_ID1)
                        .withDefendants(Arrays.asList(Defendants.defendants()
                                .withDefendantId(DEF_ID1)
                                .withOffences(Arrays.asList(Offences.offences().withOffenceId(OFF_ID1).build(), Offences.offences().withOffenceId(OFF_ID2).build()))
                                .build(),
                                Defendants.defendants()
                                        .withDefendantId(DEF_ID2)
                                        .withOffences(Arrays.asList(Offences.offences().withOffenceId(OFF_ID3).build(), Offences.offences().withOffenceId(OFF_ID4).build()))
                                        .build()))
                        .build());
    }

    public static List<ProsecutionCases> buildEventProsecutionCases(final UUID offenceID) {
        return Arrays.asList(ProsecutionCases.prosecutionCases()
                .withCaseId(CASE_ID1)
                .withDefendants(Arrays.asList(Defendants.defendants()
                        .withDefendantId(DEF_ID1)
                        .withOffences(Arrays.asList(Offences.offences().withOffenceId(offenceID).build()))
                        .build()))
                .build());
    }

    public static JsonNode buildHearingEntity() throws IOException {
        final String hearingString = getStringFromResource("hearing.json")
                .replaceAll("HEARING_ID1", HEARING_ID.toString())
                .replaceAll("CASE_ID1", CASE_ID1.toString())
                .replaceAll("CASE_ID2", CASE_ID2.toString())
                .replaceAll("DEF_ID1", DEF_ID1.toString())
                .replaceAll("DEF_ID2", DEF_ID2.toString())
                .replaceAll("OFF_ID1", OFF_ID1.toString())
                .replaceAll("OFF_ID2", OFF_ID2.toString())
                .replaceAll("OFF_ID3", OFF_ID3.toString())
                .replaceAll("OFF_I43", OFF_ID4.toString());

        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(hearingString);
    }

    public static JsonNode buildHearingEntityForMultiDefendants() throws IOException {
        final String hearingString = getStringFromResource("hearing-entity-mutli-defendant.json")
                .replaceAll("HEARING_ID", HEARING_ID.toString())
                .replaceAll("CASE_ID", CASE_ID1.toString())
                .replaceAll("DEFENDANT_ID1", DEF_ID1.toString())
                .replaceAll("DEFENDANT_ID2", DEF_ID2.toString())
                .replaceAll("DEFENDANT_ID3", DEF_ID3.toString())
                .replaceAll("OFFENCE_ID1", OFF_ID1.toString())
                .replaceAll("OFFENCE_ID2", OFF_ID2.toString())
                .replaceAll("OFFENCE_ID3", OFF_ID3.toString())
                .replaceAll("OFFENCE_ID4", OFF_ID4.toString())
                .replaceAll("OFFENCE_ID5", OFF_ID5.toString())
                .replaceAll("OFFENCE_ID6", OFF_ID6.toString());



        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(hearingString);
    }

    public static JsonNode buildHearingEntityProperties() throws IOException {
        final String hearingString = getStringFromResource("hearing-payload-coming-from-db-with-defence-counsels.json")
                .replaceAll("HEARING_ID", HEARING_ID.toString())
                .replaceAll("CASE_ID", CASE_ID1.toString())
                .replaceAll("DEFENDANT_ID1", DEF_ID1.toString())
                .replaceAll("DEFENDANT_ID2", DEF_ID2.toString())
                .replaceAll("OFFENCE_ID1", OFF_ID1.toString())
                .replaceAll("OFFENCE_ID2", OFF_ID2.toString());
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(hearingString);
    }

    public static JsonNode buildHearingEntityWithSingleDefMultiOffenceProperties() throws IOException {
        final String hearingString = getStringFromResource("hearing-payload-coming-from-db.json")
                .replaceAll("HEARING_ID", HEARING_ID.toString())
                .replaceAll("CASE_ID", CASE_ID1.toString())
                .replaceAll("DEFENDANT_ID1", DEF_ID1.toString())
                .replaceAll("OFFENCE_ID1", OFF_ID1.toString())
                .replaceAll("OFFENCE_ID2", OFF_ID2.toString())
                .replaceAll("OFFENCE_ID3", OFF_ID3.toString());

        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(hearingString);
    }
}
