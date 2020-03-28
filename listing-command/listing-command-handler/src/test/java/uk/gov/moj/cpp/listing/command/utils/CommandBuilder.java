package uk.gov.moj.cpp.listing.command.utils;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.StringReader;

public class CommandBuilder {

    private static final String EARLIEST_START_TIME = "2012-12-12T01:02:33Z";
    private static final String LISTED_START_TIME = "2012-11-12T01:02:33Z";

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    public CommandBuilder(JsonObjectToObjectConverter jsonObjectToObjectConverter) {
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
    }

    public HearingListingNeeds buildCommandHearing() {
        JsonObject hearingJsonObject = FileUtil.givenPayload("/test-data/listing.commands.hearing.json");
        return jsonObjectToObjectConverter.convert(hearingJsonObject, HearingListingNeeds.class);
    }

    public HearingListingNeeds buildCommandHearingStandalone() {
        JsonObject hearingJsonObject = FileUtil.givenPayload("/test-data/listing.commands.hearing-standalone.json");
        return jsonObjectToObjectConverter.convert(hearingJsonObject, HearingListingNeeds.class);
    }

    public HearingListingNeeds buildHearingWithListedStartDateTime() {
        String jsonString = FileUtil.givenPayload("/test-data/listing.command.hearing-listed-date-over-earliest-date.json").toString()
                .replace("EARLIEST_START_TIME", EARLIEST_START_TIME)
                .replace("LISTED_START_TIME", LISTED_START_TIME);

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        return jsonObjectToObjectConverter.convert(jsonReader.readObject(), HearingListingNeeds.class);
    }


    public CourtApplication buildCourtApplication() {
        JsonObject courtApplication = FileUtil.givenPayload("/test-data/listing.court-application-applicant-respondent.json");
        return jsonObjectToObjectConverter.convert(courtApplication, CourtApplication.class);
    }

    public HearingListingNeeds buildCommandHearingWithLegalEntity() {
        JsonObject hearingJsonObject = FileUtil.givenPayload("/test-data/listing.court-application-case-legal-entity.json");
        return jsonObjectToObjectConverter.convert(hearingJsonObject, HearingListingNeeds.class);
    }

    public CourtApplication buildCourtApplicationWithLegalEntity() {
        JsonObject courtApplication = FileUtil.givenPayload("/test-data/listing.court-application-applicant-legal-entity.json");
        return jsonObjectToObjectConverter.convert(courtApplication, CourtApplication.class);
    }

    public CourtApplication buildCourtApplicationWithOrganisation() {
        JsonObject courtApplication = FileUtil.givenPayload("/test-data/listing.court-application-applicant-organisation.json");
        return jsonObjectToObjectConverter.convert(courtApplication, CourtApplication.class);
    }
}
