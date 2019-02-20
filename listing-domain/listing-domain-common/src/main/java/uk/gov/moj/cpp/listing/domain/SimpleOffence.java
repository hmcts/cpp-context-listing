package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class SimpleOffence implements Serializable {

    private final UUID id;
    private final UUID defendantId;

    @JsonCreator
    public SimpleOffence(@JsonProperty(value = "id") final UUID id,
                         @JsonProperty(value = "defendantId") final UUID defendantId) {
        this.id = id;
        this.defendantId = defendantId;
    }

    public UUID getId() { return id; }

    public UUID getDefendantId() { return defendantId; }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SimpleOffence that = (SimpleOffence) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(defendantId, that.defendantId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, defendantId);
    }

    @Override
    public String toString() {
        return "BaseOffence{" +
                "id='" + id + '\'' +
                ", defendantId='" + defendantId + '\'' +
                '}';
    }

    public static SimpleOffenceBuilder createSimpleOffenceBuilder() {
        return new SimpleOffenceBuilder();
    }

    public static class SimpleOffenceBuilder {
        private UUID id;
        private UUID defendantId;

        public SimpleOffenceBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public SimpleOffenceBuilder withDefendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public SimpleOffence build() {
            return new SimpleOffence(id, defendantId);
        }
    }
}
