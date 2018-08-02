package uk.gov.moj.cpp.listing.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

@JsonInclude(value = Include.NON_NULL)
public class SimpleOffence implements Serializable {

    private final String id;
    private final String defendantId;

    @JsonCreator
    public SimpleOffence(@JsonProperty(value = "id") final String id,
                         @JsonProperty(value = "defendantId") final String defendantId) {
        this.id = id;
        this.defendantId = defendantId;
    }

    public String getId() { return id; }

    public String getDefendantId() { return defendantId; }


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
        private String id;
        private String defendantId;

        public SimpleOffenceBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public SimpleOffenceBuilder setDefendantId(String defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public SimpleOffence build() {
            return new SimpleOffence(id, defendantId);
        }
    }
}
