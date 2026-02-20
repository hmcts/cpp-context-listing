package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "squid:S00107", "squid:S00121", "squid:S2384"})
public class Defendant {
    private UUID id;
    private String organisationName;

    private String welshOrganisationName;

    private String firstName;

    private String surname;

    private String welshSurname;

    private String dateOfBirth;

    private String age;

    private String nationality;

    private Address address;

    private Set<ReportingRestriction> reportingRestrictions;

    private List<Offence> offences;

    private List<Counsel> prosecutionCounsels;

    private List<Counsel> defenceCounsels;

    private PersonDefendant personDefendant;

    public String getOrganisationName() {
        return organisationName;
    }

    public String getWelshOrganisationName() {
        return welshOrganisationName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSurname() {
        return surname;
    }

    public String getWelshSurname() {
        return welshSurname;
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

    public List<Counsel> getProsecutionCounsels() {
        return prosecutionCounsels;
    }

    public List<Counsel> getDefenceCounsels() {
        return defenceCounsels;
    }

    public UUID getId() {
        return id;
    }

    public Set<ReportingRestriction> getReportingRestrictions() {
        return reportingRestrictions;
    }

    public PersonDefendant getPersonDefendant() {
        return personDefendant;
    }

    /**
     * Convenience accessor for arrest summons number from nested personDefendant.
     */
    public String getArrestSummonsNumber() {
        return personDefendant != null ? personDefendant.getArrestSummonsNumber() : null;
    }

    public static Defendant.Builder defendant() {
        return new Defendant.Builder();
    }

    public static final class Builder {
        private UUID id;
        private String organisationName;
        private String welshOrganisationName;
        private String firstName;
        private String surname;
        private String welshSurname;
        private String dateOfBirth;
        private String age;
        private String nationality;
        private List<Offence> offences;
        private Address address;
        private List<Counsel> prosecutionCounsels;
        private List<Counsel> defenceCounsels;
        private Set<ReportingRestriction> reportingRestrictions;
        private PersonDefendant personDefendant;

        private Builder() {
        }

        public Defendant.Builder withOrganisationName(String organisationName) {
            this.organisationName = organisationName;
            return this;
        }

        public Defendant.Builder withWelshOrganisationName(String welshOrganisationName) {
            this.welshOrganisationName = welshOrganisationName;
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

        public Defendant.Builder withWelshSurname(String welshSurname) {
            this.welshSurname = welshSurname;
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

        public Defendant.Builder withProsecutionCounsels(List<Counsel> prosecutionCounsels) {
            this.prosecutionCounsels = prosecutionCounsels;
            return this;
        }

        public Defendant.Builder withDefenceCounsels(List<Counsel> defenceCounsels) {
            this.defenceCounsels = defenceCounsels;
            return this;
        }

        public Defendant.Builder withAddress(Address address) {
            this.address = address;
            return this;
        }

        public Defendant.Builder withReportingRestrictions(final Set<ReportingRestriction> reportingRestrictions) {
            this.reportingRestrictions = reportingRestrictions;
            return this;
        }

        public Defendant.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Defendant.Builder withPersonDefendant(final PersonDefendant personDefendant) {
            this.personDefendant = personDefendant;
            return this;
        }

        public Defendant build() {
            final Defendant defendant = new Defendant();
            defendant.organisationName = organisationName;
            defendant.welshOrganisationName = welshOrganisationName;
            defendant.firstName = firstName;
            defendant.surname = surname;
            defendant.welshSurname = welshSurname;
            defendant.dateOfBirth = dateOfBirth;
            defendant.age = age;
            defendant.offences = offences;
            defendant.nationality = nationality;
            defendant.address = address;
            defendant.prosecutionCounsels = prosecutionCounsels;
            defendant.defenceCounsels = defenceCounsels;
            defendant.reportingRestrictions = reportingRestrictions;
            defendant.id = id;
            defendant.personDefendant = personDefendant;
            return defendant;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Defendant)) return false;
        final Defendant defendant = (Defendant) o;
        return Objects.equals(organisationName, defendant.organisationName) &&
                Objects.equals(welshOrganisationName, defendant.welshOrganisationName) &&
                Objects.equals(firstName, defendant.firstName) &&
                Objects.equals(surname, defendant.surname) &&
                Objects.equals(welshSurname, defendant.welshSurname) &&
                Objects.equals(dateOfBirth, defendant.dateOfBirth) &&
                Objects.equals(age, defendant.age) &&
                Objects.equals(offences, defendant.offences) &&
                Objects.equals(id, defendant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organisationName, welshOrganisationName, firstName, surname, welshSurname, dateOfBirth, age, offences, id);
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
                ", id=" + id +
                '}';
    }
}
