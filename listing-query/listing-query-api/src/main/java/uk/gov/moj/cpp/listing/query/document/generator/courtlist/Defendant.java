package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;

@SuppressWarnings({"squid:S1067", "squid:S00107", "squid:S00121"})
public class Defendant {
    private String organisationName;

    private String firstName;

    private String surname;

    private String dateOfBirth;

    private String age;

    private String nationality;

    private Address address;

    private List<Offence> offences;

    public String getOrganisationName() { return organisationName; }

    public String getFirstName() {
        return firstName;
    }

    public String getSurname() {
        return surname;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAge() {
        return age;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public String getNationality() {
        return nationality;
    }

    public Address getAddress() {
        return address;
    }

    public static Defendant.Builder defendant() {
        return new Defendant.Builder();
    }

    public static final class Builder {
        private String organisationName;
        private String firstName;
        private String surname;
        private String dateOfBirth;
        private String age;
        private String nationality;
        private List<Offence> offences;
        private Address address;

        private Builder() {
        }

        public Defendant.Builder withOrganisationName(String organisationName) {
            this.organisationName = organisationName;
            return this;
        }

        public Defendant.Builder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Defendant.Builder withSurname(String surname) {
            this.surname = surname;
            return this;
        }

        public Defendant.Builder withDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Defendant.Builder withAge(String age) {
            this.age = age;
            return this;
        }

        public Defendant.Builder withOffences(List<Offence> offences) {
            this.offences = offences;
            return this;
        }
        public Defendant.Builder withNationality(String nationality) {
            this.nationality = nationality;
            return this;
        }

        public Defendant.Builder withAddress(Address address) {
            this.address = address;
            return this;
        }
        public Defendant build() {
            final Defendant defendant = new Defendant();
            defendant.organisationName = organisationName;
            defendant.firstName = firstName;
            defendant.surname = surname;
            defendant.dateOfBirth = this.dateOfBirth;
            defendant.age = this.age;
            defendant.offences = this.offences;
            defendant.nationality = nationality;
            defendant.address = address;
            return defendant;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Defendant)) return false;
        final Defendant defendant = (Defendant) o;
        return Objects.equals(organisationName, defendant.organisationName) &&
                Objects.equals(firstName, defendant.firstName) &&
                Objects.equals(surname, defendant.surname) &&
                Objects.equals(dateOfBirth, defendant.dateOfBirth) &&
                Objects.equals(age, defendant.age) &&
                Objects.equals(offences, defendant.offences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organisationName, firstName, surname, dateOfBirth, age, offences);
    }

    @Override
    public String toString() {
        return "Defendant{" +
                "organisationName='" + organisationName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", surname='" + surname + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", age='" + age + '\'' +
                ", offences=" + offences +
                '}';
    }
}
