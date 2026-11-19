package uk.gov.moj.cpp.listing.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonEntity {

    JsonNode getProperties();

    void setProperties(JsonNode properties);
}
