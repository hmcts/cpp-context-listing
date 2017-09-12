package uk.gov.moj.cpp.listing.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;

@Event("listing.events.judge-added")
@JsonInclude(value = Include.NON_NULL)
public class JudgeAdded {

    private final String id;
    private final String title;
    private final String firstName;
    private final String lastName;

    public JudgeAdded(@JsonProperty(value = "id") final String id,
                              @JsonProperty(value = "title") final String title,
                              @JsonProperty(value = "firstName") final String firstName,
                              @JsonProperty(value = "lastName") final String lastName) {
        this.id = id;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JudgeAdded that = (JudgeAdded) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, firstName, lastName);
    }
}
