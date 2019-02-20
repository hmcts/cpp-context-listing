package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.JsonEntity;

import java.io.Serializable;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.deltaspike.data.api.EntityRepository;

public class JsonEntityFinder<E extends JsonEntity, PK extends Serializable> {

    private final EntityRepository<E, PK> entityRepository;

    private JsonEntityFinder(final EntityRepository<E, PK> entityRepository) {
        this.entityRepository = entityRepository;
    }

    public static <E extends JsonEntity, PK extends Serializable> JsonEntityFinder using(
            final EntityRepository<E, PK> entityRepository) {
        return new JsonEntityFinder(entityRepository);
    }

    public JsonNodeUpdater find(final PK entityId) {
        E entity = entityRepository.findBy(entityId);
        ObjectNode properties = (ObjectNode) entity.getProperties();
        return JsonNodeUpdater.createJsonNodeUpdater(properties, newProperties -> {
            entity.setProperties(newProperties);
            entityRepository.save(entity);
        });
    }

    public JsonNodeUpdater find(final PK entityId, final String pathToFind) {
        E entity = entityRepository.findBy(entityId);
        ObjectNode properties = (ObjectNode) entity.getProperties();
        return JsonNodeUpdater.createJsonNodeUpdater(properties, pathToFind, newProperties -> {
            entity.setProperties(newProperties);
            entityRepository.save(entity);
        });
    }
}
