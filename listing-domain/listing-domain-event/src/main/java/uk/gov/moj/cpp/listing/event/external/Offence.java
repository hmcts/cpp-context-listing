package uk.gov.moj.cpp.listing.event.external;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class Offence implements Serializable {

    private final String id;

    @JsonCreator
    public Offence(@JsonProperty(value = "id") final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Offence{" +
                "id='" + id + '\'' +
                '}';
    }
}
