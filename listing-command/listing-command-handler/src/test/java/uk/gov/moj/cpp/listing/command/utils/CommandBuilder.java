package uk.gov.moj.cpp.listing.command.utils;

import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CommandBuilder {

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


}
