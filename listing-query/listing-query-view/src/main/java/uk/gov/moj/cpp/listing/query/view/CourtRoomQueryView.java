package uk.gov.moj.cpp.listing.query.view;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.CourtRoom;
import uk.gov.moj.cpp.listing.persistence.repository.CourtRoomRepository;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.List;

@ServiceComponent(Component.QUERY_VIEW)
public class CourtRoomQueryView {
    @Inject
    private CourtRoomRepository repository;

    @Inject
    private Converter<List<CourtRoom>, JsonArray> courtCentreConverter;

    @Inject
    private Enveloper enveloper;

    @Handles("listing.get.court-rooms")
    public JsonEnvelope findCourtRooms(final JsonEnvelope envelope) {
        final List<CourtRoom> courtRooms = repository.findAll();

        return enveloper.withMetadataFrom(envelope, "listing.get.court-rooms").apply(
                Json.createObjectBuilder()
                        .add("courtRooms", courtCentreConverter.convert(courtRooms))
                        .build()
        );
    }
}
