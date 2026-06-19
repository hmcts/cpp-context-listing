package uk.gov.moj.cpp.listing.domain.aggregate;


import java.io.Serializable;

@SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize", "squid:S2384"})
public class Organisation implements Serializable {
    private final Address address;

    private final ContactNumber contact;

    private final String incorporationNumber;

    private final String name;

    private final String registeredCharityNumber;

    public Organisation(final Address address, final ContactNumber contact, final String incorporationNumber, final String name, final String registeredCharityNumber) {
        this.address = address;
        this.contact = contact;
        this.incorporationNumber = incorporationNumber;
        this.name = name;
        this.registeredCharityNumber = registeredCharityNumber;
    }

    public Address getAddress() {
        return address;
    }

    public ContactNumber getContact() {
        return contact;
    }

    public String getIncorporationNumber() {
        return incorporationNumber;
    }

    public String getName() {
        return name;
    }

    public String getRegisteredCharityNumber() {
        return registeredCharityNumber;
    }

    public static Builder organisation() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Organisation that = (Organisation) obj;

        return java.util.Objects.equals(this.address, that.address) &&
                java.util.Objects.equals(this.contact, that.contact) &&
                java.util.Objects.equals(this.incorporationNumber, that.incorporationNumber) &&
                java.util.Objects.equals(this.name, that.name) &&
                java.util.Objects.equals(this.registeredCharityNumber, that.registeredCharityNumber);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(address, contact, incorporationNumber, name, registeredCharityNumber);
    }

    @Override
    public String toString() {
        return "Organisation{" +
                "address='" + address + "'," +
                "contact='" + contact + "'," +
                "incorporationNumber='" + incorporationNumber + "'," +
                "name='" + name + "'," +
                "registeredCharityNumber='" + registeredCharityNumber + "'" +
                "}";
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static class Builder {
        private Address address;

        private ContactNumber contact;

        private String incorporationNumber;

        private String name;

        private String registeredCharityNumber;

        public Builder withAddress(final Address address) {
            this.address = address;
            return this;
        }

        public Builder withContact(final ContactNumber contact) {
            this.contact = contact;
            return this;
        }


        public Builder withIncorporationNumber(final String incorporationNumber) {
            this.incorporationNumber = incorporationNumber;
            return this;
        }


        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withRegisteredCharityNumber(final String registeredCharityNumber) {
            this.registeredCharityNumber = registeredCharityNumber;
            return this;
        }


        public Organisation build() {
            return new Organisation(address, contact, incorporationNumber, name, registeredCharityNumber);
        }
    }
}
