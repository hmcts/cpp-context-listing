package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.util.UUID;

@Entity
//@IdClass(value = uk.gov.moj.cpp.listing.Judge.class)
@Table(name = "judge")
public class Judge {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "title")
    private String title;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;


    public Judge() {
        // for JPA
    }

    public Judge(final UUID id, final String title, final String firstName, final String lastName) {
        this.id = id;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Judge judge = (Judge) o;

        return id.equals(judge.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
