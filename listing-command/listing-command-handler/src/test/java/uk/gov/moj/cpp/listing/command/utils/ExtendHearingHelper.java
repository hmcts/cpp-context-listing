package uk.gov.moj.cpp.listing.command.utils;

import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.CASE_ID1;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.CASE_ID2;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.DEF_ID1;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.DEF_ID2;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.DEF_ID3;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.OFF_ID1;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.OFF_ID2;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.OFF_ID3;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.OFF_ID4;
import static uk.gov.moj.cpp.listing.command.utils.ExtendHearingUtilsTest.UNALLOCATED_HEARING_ID;

import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.StringReader;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class ExtendHearingHelper {

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    public ExtendHearingHelper(final JsonObjectToObjectConverter jsonObjectToObjectConverter) {
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
    }

    public uk.gov.justice.listing.events.Hearing getAllocatedHearingById(final UUID hearingId1, final UUID caseId1, final String urn1) {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.extend-hearing-for-hearing-allocated.json").toString()
                .replace("HEARING_ID1", hearingId1.toString())
                .replace("CASE_ID1", caseId1.toString())
                .replace("CASE_URN1", urn1);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            final JsonEnvelope jsonEnvelope = createEnvelope("listing.command.extend-hearing-for-hearing-enriched", jsonReader.readObject());
            final JsonObject jsonObject = jsonEnvelope.payloadAsJsonObject();

            return jsonObjectToObjectConverter.convert(jsonObject, uk.gov.justice.listing.events.Hearing.class);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public uk.gov.justice.listing.events.Hearing getUnAllocatedHearingById(final UUID hearingId1, final UUID caseId1, final String urn1) {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.extend-hearing-for-hearing-unallocated.json").toString()
                .replace("HEARING_ID1", hearingId1.toString())
                .replace("CASE_ID1", caseId1.toString())
                .replace("CASE_URN1", urn1);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            final JsonEnvelope jsonEnvelope = createEnvelope("listing.command.extend-hearing-for-hearing-enriched", jsonReader.readObject());
            final JsonObject jsonObject = jsonEnvelope.payloadAsJsonObject();

            return jsonObjectToObjectConverter.convert(jsonObject, uk.gov.justice.listing.events.Hearing.class);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public uk.gov.justice.listing.events.Hearing getUnAllocatedHearingById(final UUID hearingId1,
                                                                           final UUID caseId1,
                                                                           final UUID caseId2,
                                                                           final String urn1,
                                                                           final String urn2,
                                                                           final UUID def1,
                                                                           final UUID def2,
                                                                           final UUID def3,
                                                                           final UUID off1,
                                                                           final UUID off2,
                                                                           final UUID off3,
                                                                           final UUID off4) {
        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.extend-hearing-for-hearing-unallocated-partial-extension-sample.json").toString()
                .replace("HEARING_ID1", hearingId1.toString())
                .replace("CASE_ID1", caseId1.toString())
                .replace("CASE_ID2", caseId2.toString())
                .replace("CASE_URN1", urn1)
                .replace("CASE_URN2", urn2)
                .replace("DEF_ID1", def1.toString())
                .replace("DEF_ID2", def2.toString())
                .replace("DEF_ID3", def3.toString())
                .replace("OFF_ID1", off1.toString())
                .replace("OFF_ID2", off2.toString())
                .replace("OFF_ID3", off3.toString())
                .replace("OFF_ID4", off4.toString());
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            final JsonEnvelope jsonEnvelope = createEnvelope("listing.command.extend-hearing-for-hearing-enriched", jsonReader.readObject());
            final JsonObject jsonObject = jsonEnvelope.payloadAsJsonObject();

            return jsonObjectToObjectConverter.convert(jsonObject, uk.gov.justice.listing.events.Hearing.class);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Hearing whenWeHave2Cases3DefendantsAnd4OffencesPersistedInViewStore() {

        final String jsonString = FileUtil.givenPayload("/test-data/listing.command.extend-hearing-for-hearing-unallocated-partial-extension-sample.json").toString()
                .replace("HEARING_ID1", UNALLOCATED_HEARING_ID.toString())
                .replace("CASE_ID1", CASE_ID1.toString())
                .replace("CASE_ID2", CASE_ID2.toString())
                .replace("DEF_ID1", DEF_ID1.toString())
                .replace("DEF_ID2", DEF_ID2.toString())
                .replace("DEF_ID3", DEF_ID3.toString())
                .replace("OFF_ID1", OFF_ID1.toString())
                .replace("OFF_ID2", OFF_ID2.toString())
                .replace("OFF_ID3", OFF_ID3.toString())
                .replace("OFF_ID4", OFF_ID4.toString());

        final Hearing unallocatedHearingPersisted;
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            final JsonEnvelope jsonEnvelope = createEnvelope("listing.command.extend-hearing-for-hearing-enriched", jsonReader.readObject());
            final JsonObject jsonObject = jsonEnvelope.payloadAsJsonObject();
            unallocatedHearingPersisted = jsonObjectToObjectConverter.convert(jsonObject, Hearing.class);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return unallocatedHearingPersisted;
    }


    public static JsonEnvelope getEnvelopeForExtendWholeHearing(final UUID hearingID1, final UUID hearingID2) {
        final String requestBody = "{\"allocatedHearingId\":\"" + hearingID1.toString() + "\",\"unAllocatedHearingId\":\"" + hearingID2.toString() + "\"}";
        final JsonReader jsonReader = Json.createReader(new StringReader(requestBody));
        return createEnvelope("listing.command.extend-hearing-for-hearing-enriched", jsonReader.readObject());
    }

    public static JsonEnvelope getEnvelopeForExtendPartialHearing(final String hearingId1,
                                                            final String hearingId2,
                                                            final String caseId1,
                                                            final String caseId2,
                                                            final String defId1,
                                                            final String defId2,
                                                            final String defId3,
                                                            final String offId1,
                                                            final String offId2,
                                                            final String offId3,
                                                            final String offId4) {
        final String requestBody = FileUtil.givenPayload("/test-data/listing.command.extend-hearing-for-hearing-enriched.json").toString()
                .replaceAll("HEARING_ID1", hearingId1)
                .replaceAll("HEARING_ID2", hearingId2)
                .replaceAll("CASE_ID1", caseId1)
                .replaceAll("CASE_ID2", caseId2)
                .replaceAll("DEF_ID1", defId1)
                .replaceAll("DEF_ID2", defId2)
                .replaceAll("DEF_ID3", defId3)
                .replaceAll("OFF_ID1", offId1)
                .replaceAll("OFF_ID2", offId2)
                .replaceAll("OFF_ID3", offId3)
                .replaceAll("OFF_ID4", offId4);

        final JsonReader jsonReader = Json.createReader(new StringReader(requestBody));
        return createEnvelope("listing.command.extend-hearing-for-hearing-enriched", jsonReader.readObject());
    }
}
