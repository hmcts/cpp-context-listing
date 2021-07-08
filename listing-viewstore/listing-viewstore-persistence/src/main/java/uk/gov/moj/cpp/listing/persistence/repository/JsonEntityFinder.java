package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonEntityFinder {

    private final  HearingRepository entityRepository;

    private JsonEntityFinder(final HearingRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    public static JsonEntityFinder using(
            final HearingRepository entityRepository) {
        return new JsonEntityFinder(entityRepository);
    }

    public JsonNodeUpdater find(final UUID entityId) {
        final Hearing entity = entityRepository.findBy(entityId);
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
