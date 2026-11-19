package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class CourtApplicationPartyCounsel implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final UUID applicationId;

  private final List<UUID> applicationRespondents;

  private final List<String> attendanceDays;

  private final String firstName;

  private final UUID id;

  private final String lastName;

  private final String middleName;

  private final String status;

  private final String title;

  @JsonCreator
  public CourtApplicationPartyCounsel(final UUID applicationId, final List<UUID> applicationRespondents, final List<String> attendanceDays, final String firstName, final UUID id, final String lastName, final String middleName, final String status, final String title) {
    this.applicationId = applicationId;
    this.applicationRespondents = applicationRespondents;
    this.attendanceDays = attendanceDays;
    this.firstName = firstName;
    this.id = id;
    this.lastName = lastName;
    this.middleName = middleName;
    this.status = status;
    this.title = title;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public List<UUID> getApplicationRespondents() {
    return applicationRespondents;
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

  public static Builder courtApplicationPartyCounsel() {
    return new uk.gov.justice.core.courts.CourtApplicationPartyCounsel.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtApplicationPartyCounsel that = (uk.gov.justice.core.courts.CourtApplicationPartyCounsel) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.applicationRespondents, that.applicationRespondents) &&
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
    return java.util.Objects.hash(applicationId, applicationRespondents, attendanceDays, firstName, id, lastName, middleName, status, title);}

  public static class Builder {
    private UUID applicationId;

    private List<UUID> applicationRespondents;

    private List<String> attendanceDays;

    private String firstName;

    private UUID id;

    private String lastName;

    private String middleName;

    private String status;

    private String title;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withApplicationRespondents(final List<UUID> applicationRespondents) {
      this.applicationRespondents = applicationRespondents;
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

    public Builder withValuesFrom(final CourtApplicationPartyCounsel courtApplicationPartyCounsel) {
      this.applicationId = courtApplicationPartyCounsel.getApplicationId();
      this.applicationRespondents = courtApplicationPartyCounsel.getApplicationRespondents();
      this.attendanceDays = courtApplicationPartyCounsel.getAttendanceDays();
      this.firstName = courtApplicationPartyCounsel.getFirstName();
      this.id = courtApplicationPartyCounsel.getId();
      this.lastName = courtApplicationPartyCounsel.getLastName();
      this.middleName = courtApplicationPartyCounsel.getMiddleName();
      this.status = courtApplicationPartyCounsel.getStatus();
      this.title = courtApplicationPartyCounsel.getTitle();
      return this;
    }

    public CourtApplicationPartyCounsel build() {
      return new uk.gov.justice.core.courts.CourtApplicationPartyCounsel(applicationId, applicationRespondents, attendanceDays, firstName, id, lastName, middleName, status, title);
    }
  }
}
