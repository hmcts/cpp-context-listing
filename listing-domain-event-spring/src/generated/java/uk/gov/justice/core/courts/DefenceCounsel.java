package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class DefenceCounsel implements Serializable {
  private static final long serialVersionUID = 641333116650746455L;

  private final List<String> attendanceDays;

  private final List<UUID> defendants;

  private final String firstName;

  private final UUID id;

  private final String lastName;

  private final String middleName;

  private final String status;

  private final String title;

  private final UUID userId;

  @JsonCreator
  public DefenceCounsel(final List<String> attendanceDays, final List<UUID> defendants, final String firstName, final UUID id, final String lastName, final String middleName, final String status, final String title, final UUID userId) {
    this.attendanceDays = attendanceDays;
    this.defendants = defendants;
    this.firstName = firstName;
    this.id = id;
    this.lastName = lastName;
    this.middleName = middleName;
    this.status = status;
    this.title = title;
    this.userId = userId;
  }

  public List<String> getAttendanceDays() {
    return attendanceDays;
  }

  public List<UUID> getDefendants() {
    return defendants;
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

  public UUID getUserId() {
    return userId;
  }

  public static Builder defenceCounsel() {
    return new uk.gov.justice.core.courts.DefenceCounsel.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DefenceCounsel that = (uk.gov.justice.core.courts.DefenceCounsel) obj;

    return java.util.Objects.equals(this.attendanceDays, that.attendanceDays) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.middleName, that.middleName) &&
    java.util.Objects.equals(this.status, that.status) &&
    java.util.Objects.equals(this.title, that.title) &&
    java.util.Objects.equals(this.userId, that.userId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(attendanceDays, defendants, firstName, id, lastName, middleName, status, title, userId);}

  public static class Builder {
    private List<String> attendanceDays;

    private List<UUID> defendants;

    private String firstName;

    private UUID id;

    private String lastName;

    private String middleName;

    private String status;

    private String title;

    private UUID userId;

    public Builder withAttendanceDays(final List<String> attendanceDays) {
      this.attendanceDays = attendanceDays;
      return this;
    }

    public Builder withDefendants(final List<UUID> defendants) {
      this.defendants = defendants;
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

    public Builder withUserId(final UUID userId) {
      this.userId = userId;
      return this;
    }

    public Builder withValuesFrom(final DefenceCounsel defenceCounsel) {
      this.attendanceDays = defenceCounsel.getAttendanceDays();
      this.defendants = defenceCounsel.getDefendants();
      this.firstName = defenceCounsel.getFirstName();
      this.id = defenceCounsel.getId();
      this.lastName = defenceCounsel.getLastName();
      this.middleName = defenceCounsel.getMiddleName();
      this.status = defenceCounsel.getStatus();
      this.title = defenceCounsel.getTitle();
      this.userId = defenceCounsel.getUserId();
      return this;
    }

    public DefenceCounsel build() {
      return new uk.gov.justice.core.courts.DefenceCounsel(attendanceDays, defendants, firstName, id, lastName, middleName, status, title, userId);
    }
  }
}
