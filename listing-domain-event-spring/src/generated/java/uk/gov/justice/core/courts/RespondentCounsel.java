package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class RespondentCounsel implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final List<String> attendanceDays;

  private final String firstName;

  private final UUID id;

  private final String lastName;

  private final String middleName;

  private final List<UUID> respondents;

  private final String status;

  private final String title;

  @JsonCreator
  public RespondentCounsel(final List<String> attendanceDays, final String firstName, final UUID id, final String lastName, final String middleName, final List<UUID> respondents, final String status, final String title) {
    this.attendanceDays = attendanceDays;
    this.firstName = firstName;
    this.id = id;
    this.lastName = lastName;
    this.middleName = middleName;
    this.respondents = respondents;
    this.status = status;
    this.title = title;
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

  public List<UUID> getRespondents() {
    return respondents;
  }

  public String getStatus() {
    return status;
  }

  public String getTitle() {
    return title;
  }

  public static Builder respondentCounsel() {
    return new uk.gov.justice.core.courts.RespondentCounsel.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.RespondentCounsel that = (uk.gov.justice.core.courts.RespondentCounsel) obj;

    return java.util.Objects.equals(this.attendanceDays, that.attendanceDays) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.middleName, that.middleName) &&
    java.util.Objects.equals(this.respondents, that.respondents) &&
    java.util.Objects.equals(this.status, that.status) &&
    java.util.Objects.equals(this.title, that.title);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(attendanceDays, firstName, id, lastName, middleName, respondents, status, title);}

  public static class Builder {
    private List<String> attendanceDays;

    private String firstName;

    private UUID id;

    private String lastName;

    private String middleName;

    private List<UUID> respondents;

    private String status;

    private String title;

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

    public Builder withRespondents(final List<UUID> respondents) {
      this.respondents = respondents;
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

    public Builder withValuesFrom(final RespondentCounsel respondentCounsel) {
      this.attendanceDays = respondentCounsel.getAttendanceDays();
      this.firstName = respondentCounsel.getFirstName();
      this.id = respondentCounsel.getId();
      this.lastName = respondentCounsel.getLastName();
      this.middleName = respondentCounsel.getMiddleName();
      this.respondents = respondentCounsel.getRespondents();
      this.status = respondentCounsel.getStatus();
      this.title = respondentCounsel.getTitle();
      return this;
    }

    public RespondentCounsel build() {
      return new uk.gov.justice.core.courts.RespondentCounsel(attendanceDays, firstName, id, lastName, middleName, respondents, status, title);
    }
  }
}
