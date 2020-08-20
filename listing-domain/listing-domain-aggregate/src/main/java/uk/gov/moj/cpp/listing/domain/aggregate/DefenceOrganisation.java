package uk.gov.moj.cpp.listing.domain.aggregate;

import java.io.Serializable;

@SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize", "squid:S2384"})
public class DefenceOrganisation implements Serializable {
    private final String laaContractNumber;

    private final Organisation organisation;

    public DefenceOrganisation(final String laaContractNumber, final Organisation organisation) {
        this.laaContractNumber = laaContractNumber;
        this.organisation = organisation;
    }

    public String getLaaContractNumber() {
        return laaContractNumber;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public static Builder defenceOrganisation() {
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
        final DefenceOrganisation that = (DefenceOrganisation) obj;

        return java.util.Objects.equals(this.laaContractNumber, that.laaContractNumber) &&
                java.util.Objects.equals(this.organisation, that.organisation);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(laaContractNumber, organisation);
    }

    @Override
    public String toString() {
        return "DefenceOrganisation{" +
                "laaContractNumber='" + laaContractNumber + "'," +
                "organisation='" + organisation + "'" +
                "}";
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static class Builder {
        private String laaContractNumber;

        private Organisation organisation;

        public Builder withLaaContractNumber(final String laaContractNumber) {
            this.laaContractNumber = laaContractNumber;
            return this;
        }


        public Builder withOrganisation(final Organisation organisation) {
            this.organisation = organisation;
            return this;
        }

        public DefenceOrganisation build() {
            return new DefenceOrganisation(laaContractNumber, organisation);
        }
    }
}
