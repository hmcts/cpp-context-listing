package uk.gov.moj.cpp.listing.query.view;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.Judge;
import uk.gov.moj.cpp.listing.persistence.repository.JudgeRepository;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.List;

@ServiceComponent(Component.QUERY_VIEW)
public class JudgeQueryView {
    @Inject
    private JudgeRepository repository;

    @Inject
    private Converter<List<Judge>, JsonArray> judgeConverter;

    @Inject
    private Enveloper enveloper;

    @Handles("listing.get.judges")
    public JsonEnvelope findJudges(final JsonEnvelope envelope) {
        final List<Judge> judges = repository.findAll();

        return enveloper.withMetadataFrom(envelope, "listing.get.judges").apply(
                Json.createObjectBuilder()
                        .add("judges", judgeConverter.convert(judges))
                        .build()
        );
    }
}
