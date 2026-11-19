package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class ContactNumber implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String fax;

  private final String home;

  private final String mobile;

  private final String primaryEmail;

  private final String secondaryEmail;

  private final String work;

  @JsonCreator
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
    return new uk.gov.justice.core.courts.ContactNumber.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ContactNumber that = (uk.gov.justice.core.courts.ContactNumber) obj;

    return java.util.Objects.equals(this.fax, that.fax) &&
    java.util.Objects.equals(this.home, that.home) &&
    java.util.Objects.equals(this.mobile, that.mobile) &&
    java.util.Objects.equals(this.primaryEmail, that.primaryEmail) &&
    java.util.Objects.equals(this.secondaryEmail, that.secondaryEmail) &&
    java.util.Objects.equals(this.work, that.work);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(fax, home, mobile, primaryEmail, secondaryEmail, work);}

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

    public Builder withValuesFrom(final ContactNumber contactNumber) {
      this.fax = contactNumber.getFax();
      this.home = contactNumber.getHome();
      this.mobile = contactNumber.getMobile();
      this.primaryEmail = contactNumber.getPrimaryEmail();
      this.secondaryEmail = contactNumber.getSecondaryEmail();
      this.work = contactNumber.getWork();
      return this;
    }

    public ContactNumber build() {
      return new uk.gov.justice.core.courts.ContactNumber(fax, home, mobile, primaryEmail, secondaryEmail, work);
    }
  }
}
