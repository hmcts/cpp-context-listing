package uk.gov.moj.cpp.listing.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.UUID;



@Entity
@Table(name = "hearing")
@TypeDef(
        name = "jsonb-node",
        typeClass = JsonNodeBinaryType.class
)
public class Hearing implements JsonEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "properties", columnDefinition = "jsonb")
    @Type( type = "jsonb-node" )
    private JsonNode properties;

    public Hearing() {
        // for JPA

    }

    public Hearing(final UUID id, final JsonNode properties) {
        this.id = id;
        this.properties = properties;

    }

    public static HearingBuilder createHearingBuilder() {
        return new HearingBuilder();
    }

    public UUID getId() {
        return id;
    }

    public JsonNode getProperties() {
        return properties;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setProperties(JsonNode properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Hearing hearing = (Hearing) o;

        return id.equals(hearing.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
