package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class ApplicantCounsel implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final List<UUID> applicants;

  private final List<String> attendanceDays;

  private final String firstName;

  private final UUID id;

  private final String lastName;

  private final String middleName;

  private final String status;

  private final String title;

  @JsonCreator
  public ApplicantCounsel(final List<UUID> applicants, final List<String> attendanceDays, final String firstName, final UUID id, final String lastName, final String middleName, final String status, final String title) {
    this.applicants = applicants;
    this.attendanceDays = attendanceDays;
    this.firstName = firstName;
    this.id = id;
    this.lastName = lastName;
    this.middleName = middleName;
    this.status = status;
    this.title = title;
  }

  public List<UUID> getApplicants() {
    return applicants;
  }

  public List<String> getAttendanceDays() {
    return attendanceDays;
  }

  public String getFirstName() {
    return firstName;
  }

  public UUID getId() {
    return id;
  }

  public String getLastName() {
    return lastName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public String getStatus() {
    return status;
  }

  public String getTitle() {
    return title;
  }

  public static Builder applicantCounsel() {
    return new uk.gov.justice.core.courts.ApplicantCounsel.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ApplicantCounsel that = (uk.gov.justice.core.courts.ApplicantCounsel) obj;

    return java.util.Objects.equals(this.applicants, that.applicants) &&
    java.util.Objects.equals(this.attendanceDays, that.attendanceDays) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.middleName, that.middleName) &&
    java.util.Objects.equals(this.status, that.status) &&
    java.util.Objects.equals(this.title, that.title);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicants, attendanceDays, firstName, id, lastName, middleName, status, title);}

  public static class Builder {
    private List<UUID> applicants;

    private List<String> attendanceDays;

    private String firstName;

    private UUID id;

    private String lastName;

    private String middleName;

    private String status;

    private String title;

    public Builder withApplicants(final List<UUID> applicants) {
      this.applicants = applicants;
      return this;
    }

    public Builder withAttendanceDays(final List<String> attendanceDays) {
      this.attendanceDays = attendanceDays;
      return this;
    }

    public Builder withFirstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder withMiddleName(final String middleName) {
      this.middleName = middleName;
      return this;
    }

    public Builder withStatus(final String status) {
      this.status = status;
      return this;
    }

    public Builder withTitle(final String title) {
      this.title = title;
      return this;
    }

    public Builder withValuesFrom(final ApplicantCounsel applicantCounsel) {
      this.applicants = applicantCounsel.getApplicants();
      this.attendanceDays = applicantCounsel.getAttendanceDays();
      this.firstName = applicantCounsel.getFirstName();
      this.id = applicantCounsel.getId();
      this.lastName = applicantCounsel.getLastName();
      this.middleName = applicantCounsel.getMiddleName();
      this.status = applicantCounsel.getStatus();
      this.title = applicantCounsel.getTitle();
      return this;
    }

    public ApplicantCounsel build() {
      return new uk.gov.justice.core.courts.ApplicantCounsel(applicants, attendanceDays, firstName, id, lastName, middleName, status, title);
    }
  }
}
