package uk.gov.moj.cpp.listing.domain.aggregate;

import java.io.Serializable;

@SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize", "squid:S2384"})
public class ContactNumber implements Serializable {
    private final String fax;

    private final String home;

    private final String mobile;

    private final String primaryEmail;

    private final String secondaryEmail;

    private final String work;

    public ContactNumber(final String fax, final String home, final String mobile, final String primaryEmail, final String secondaryEmail, final String work) {
        this.fax = fax;
        this.home = home;
        this.mobile = mobile;
        this.primaryEmail = primaryEmail;
        this.secondaryEmail = secondaryEmail;
        this.work = work;
    }

    public String getFax() {
        return fax;
    }

    public String getHome() {
        return home;
    }

    public String getMobile() {
        return mobile;
    }

    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public String getSecondaryEmail() {
        return secondaryEmail;
    }

    public String getWork() {
        return work;
    }

    public static Builder contactNumber() {
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

        final ContactNumber that = (ContactNumber) obj;

        return java.util.Objects.equals(this.fax, that.fax) &&
                java.util.Objects.equals(this.home, that.home) &&
                java.util.Objects.equals(this.mobile, that.mobile) &&
                java.util.Objects.equals(this.primaryEmail, that.primaryEmail) &&
                java.util.Objects.equals(this.secondaryEmail, that.secondaryEmail) &&
                java.util.Objects.equals(this.work, that.work);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(fax, home, mobile, primaryEmail, secondaryEmail, work);
    }

    @Override
    public String toString() {
        return "ContactNumber{" +
                "fax='" + fax + "'," +
                "home='" + home + "'," +
                "mobile='" + mobile + "'," +
                "primaryEmail='" + primaryEmail + "'," +
                "secondaryEmail='" + secondaryEmail + "'," +
                "work='" + work + "'" +
                "}";
    }

    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static class Builder {
        private String fax;

        private String home;

        private String mobile;

        private String primaryEmail;

        private String secondaryEmail;

        private String work;

        public Builder withFax(final String fax) {
            this.fax = fax;
            return this;
        }

        public Builder withHome(final String home) {
            this.home = home;
            return this;
        }

        public Builder withMobile(final String mobile) {
            this.mobile = mobile;
            return this;
        }


        public Builder withPrimaryEmail(final String primaryEmail) {
            this.primaryEmail = primaryEmail;
            return this;
        }

        public Builder withSecondaryEmail(final String secondaryEmail) {
            this.secondaryEmail = secondaryEmail;
            return this;
        }

        public Builder withWork(final String work) {
            this.work = work;
            return this;
        }


        public ContactNumber build() {
            return new ContactNumber(fax, home, mobile, primaryEmail, secondaryEmail, work);
        }
    }
}
