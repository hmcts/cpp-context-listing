package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class JudicialRole implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String firstName;

  private final Boolean isBenchChairman;

  private final Boolean isDeputy;

  private final UUID judicialId;

  private final JudicialRoleType judicialRoleType;

  private final String lastName;

  private final String middleName;

  private final String title;

  private final UUID userId;

  @JsonCreator
  public JudicialRole(final String firstName, final Boolean isBenchChairman, final Boolean isDeputy, final UUID judicialId, final JudicialRoleType judicialRoleType, final String lastName, final String middleName, final String title, final UUID userId) {
    this.firstName = firstName;
    this.isBenchChairman = isBenchChairman;
    this.isDeputy = isDeputy;
    this.judicialId = judicialId;
    this.judicialRoleType = judicialRoleType;
    this.lastName = lastName;
    this.middleName = middleName;
    this.title = title;
    this.userId = userId;
  }

  public String getFirstName() {
    return firstName;
  }

  public Boolean getIsBenchChairman() {
    return isBenchChairman;
  }

  public Boolean getIsDeputy() {
    return isDeputy;
  }

  public UUID getJudicialId() {
    return judicialId;
  }

  public JudicialRoleType getJudicialRoleType() {
    return judicialRoleType;
  }

  public String getLastName() {
    return lastName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public String getTitle() {
    return title;
  }

  public UUID getUserId() {
    return userId;
  }

  public static Builder judicialRole() {
    return new uk.gov.justice.core.courts.JudicialRole.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.JudicialRole that = (uk.gov.justice.core.courts.JudicialRole) obj;

    return java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.isBenchChairman, that.isBenchChairman) &&
    java.util.Objects.equals(this.isDeputy, that.isDeputy) &&
    java.util.Objects.equals(this.judicialId, that.judicialId) &&
    java.util.Objects.equals(this.judicialRoleType, that.judicialRoleType) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.middleName, that.middleName) &&
    java.util.Objects.equals(this.title, that.title) &&
    java.util.Objects.equals(this.userId, that.userId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(firstName, isBenchChairman, isDeputy, judicialId, judicialRoleType, lastName, middleName, title, userId);}

  public static class Builder {
    private String firstName;

    private Boolean isBenchChairman;

    private Boolean isDeputy;

    private UUID judicialId;

    private JudicialRoleType judicialRoleType;

    private String lastName;

    private String middleName;

    private String title;

    private UUID userId;

    public Builder withFirstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder withIsBenchChairman(final Boolean isBenchChairman) {
      this.isBenchChairman = isBenchChairman;
      return this;
    }

    public Builder withIsDeputy(final Boolean isDeputy) {
      this.isDeputy = isDeputy;
      return this;
    }

    public Builder withJudicialId(final UUID judicialId) {
      this.judicialId = judicialId;
      return this;
    }

    public Builder withJudicialRoleType(final JudicialRoleType judicialRoleType) {
      this.judicialRoleType = judicialRoleType;
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

    public Builder withTitle(final String title) {
      this.title = title;
      return this;
    }

    public Builder withUserId(final UUID userId) {
      this.userId = userId;
      return this;
    }

    public Builder withValuesFrom(final JudicialRole judicialRole) {
      this.firstName = judicialRole.getFirstName();
      this.isBenchChairman = judicialRole.getIsBenchChairman();
      this.isDeputy = judicialRole.getIsDeputy();
      this.judicialId = judicialRole.getJudicialId();
      this.judicialRoleType = judicialRole.getJudicialRoleType();
      this.lastName = judicialRole.getLastName();
      this.middleName = judicialRole.getMiddleName();
      this.title = judicialRole.getTitle();
      this.userId = judicialRole.getUserId();
      return this;
    }

    public JudicialRole build() {
      return new uk.gov.justice.core.courts.JudicialRole(firstName, isBenchChairman, isDeputy, judicialId, judicialRoleType, lastName, middleName, title, userId);
    }
  }
}
