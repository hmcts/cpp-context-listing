package uk.gov.moj.cpp.listing.event.external;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class Defendant implements Serializable {

    private final String id;

    private final List<Offence> offences;

    @JsonCreator
    public Defendant(@JsonProperty(value = "id") final String id,
                     @JsonProperty(value = "offences") final List<Offence> offences) {
        this.id = id;
        this.offences = offences;
    }

    public String getId() {
        return id;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    @Override
    public String toString() {
        return "Defendant{" +
                "id='" + id + '\'' +
                ", offences=" + offences +
                '}';
    }
}
