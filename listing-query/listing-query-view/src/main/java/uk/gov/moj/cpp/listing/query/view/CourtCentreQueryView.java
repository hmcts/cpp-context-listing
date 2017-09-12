package uk.gov.moj.cpp.listing.query.view;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.CourtCentre;
import uk.gov.moj.cpp.listing.persistence.repository.CourtCentreRepository;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.List;

@ServiceComponent(Component.QUERY_VIEW)
public class CourtCentreQueryView {
    @Inject
    private CourtCentreRepository repository;

    @Inject
    private Converter<List<CourtCentre>, JsonArray> courtCentreConverter;

    @Inject
    private Enveloper enveloper;

    @Handles("listing.get.court-centres")
    public JsonEnvelope findCourtCentres(final JsonEnvelope envelope) {
        final List<CourtCentre> courtCentres = repository.findAll();

        return enveloper.withMetadataFrom(envelope, "listing.get.court-centres").apply(
                Json.createObjectBuilder()
                        .add("courtCentres", courtCentreConverter.convert(courtCentres))
                        .build()
        );
    }
}
