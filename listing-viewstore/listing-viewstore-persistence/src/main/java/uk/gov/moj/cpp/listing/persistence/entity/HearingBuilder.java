package uk.gov.moj.cpp.listing.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public class HearingBuilder {
    private UUID id;
    private JsonNode properties;

    public HearingBuilder setId(UUID id) {
        this.id = id;
        return this;
    }

    public HearingBuilder setProperties(JsonNode properties) {
        this.properties = properties;
        return this;
    }

    public Hearing build() {
        return new Hearing(id, properties);
    }
}