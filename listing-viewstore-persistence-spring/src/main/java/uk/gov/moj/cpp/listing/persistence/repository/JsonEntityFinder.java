package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonEntityFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonEntityFinder.class);
    private final HearingRepository entityRepository;

    private JsonEntityFinder(final HearingRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    public static JsonEntityFinder using(
            final HearingRepository entityRepository) {
        return new JsonEntityFinder(entityRepository);
    }

    @SuppressWarnings("squid:S2259")
    public JsonNodeUpdater find(final UUID entityId) {
        final Hearing entity = entityRepository.findBy(entityId);
        if (entity == null) {
            LOGGER.warn("==== No hearing found for hearingId {} ... NPE ahead", entityId);
        }

        ObjectNode properties = (ObjectNode) entity.getProperties();
        return JsonNodeUpdater.createJsonNodeUpdater(properties, newProperties -> {
            entity.setProperties(newProperties);
            entityRepository.save(entity);
        });
    }

    public JsonNodeUpdater find(final UUID entityId, final String pathToFind) {
        final Hearing entity = entityRepository.findBy(entityId);
        ObjectNode properties = (ObjectNode) entity.getProperties();
        return JsonNodeUpdater.createJsonNodeUpdater(properties, pathToFind, newProperties -> {
            entity.setProperties(newProperties);
            entityRepository.save(entity);
        });
    }
}
