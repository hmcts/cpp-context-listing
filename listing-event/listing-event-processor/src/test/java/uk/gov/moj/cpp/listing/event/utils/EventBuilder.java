package uk.gov.moj.cpp.listing.event.utils;

import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;

import javax.inject.Inject;
import javax.json.JsonObject;

public class EventBuilder {

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    public EventBuilder(JsonObjectToObjectConverter jsonObjectToObjectConverter) {
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
    }

    public HearingListed buildHearingListed() {
        JsonObject jsonObject = FileUtil.givenPayload("/test-data/listing.events.hearing-listed.json");
        return jsonObjectToObjectConverter.convert(jsonObject, HearingListed.class);
    }

    public DefendantsToBeUpdated buildDefendantsToBeUpdated() {
        JsonObject jsonObject = FileUtil.givenPayload("/test-data/listing.events.defendants-to-be-updated.json");
        return jsonObjectToObjectConverter.convert(jsonObject, DefendantsToBeUpdated.class);
    }

    public OffencesToBeUpdated buildOffencesToBeUpdated() {
        JsonObject jsonObject = FileUtil.givenPayload("/test-data/listing.events.offences-to-be-updated.json");
        return jsonObjectToObjectConverter.convert(jsonObject, OffencesToBeUpdated.class);
    }

    public OffencesToBeAdded buildOffencesToBeAdded() {
        JsonObject jsonObject = FileUtil.givenPayload("/test-data/listing.events.offences-to-be-added.json");
        return jsonObjectToObjectConverter.convert(jsonObject, OffencesToBeAdded.class);
    }

    public OffencesToBeDeleted buildOffencesToBeDeleted() {
        JsonObject jsonObject = FileUtil.givenPayload("/test-data/listing.events.offences-to-be-deleted.json");
        return jsonObjectToObjectConverter.convert(jsonObject, OffencesToBeDeleted.class);
    }
}
