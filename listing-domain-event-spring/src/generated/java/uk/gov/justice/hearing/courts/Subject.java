package uk.gov.justice.hearing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Subject implements Serializable {
  private static final long serialVersionUID = 5433828913769985343L;

  private final String defendantFirstName;

  private final UUID defendantId;

  private final String defendantLastName;

  private final String defendantRemandStatus;

  private final String interpreterLanguageNeeds;

  @JsonCreator
  public Subject(final String defendantFirstName, final UUID defendantId, final String defendantLastName, final String defendantRemandStatus, final String interpreterLanguageNeeds) {
    this.defendantFirstName = defendantFirstName;
    this.defendantId = defendantId;
    this.defendantLastName = defendantLastName;
    this.defendantRemandStatus = defendantRemandStatus;
    this.interpreterLanguageNeeds = interpreterLanguageNeeds;
  }

  public String getDefendantFirstName() {
    return defendantFirstName;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public String getDefendantLastName() {
    return defendantLastName;
  }

  public String getDefendantRemandStatus() {
    return defendantRemandStatus;
  }

  public String getInterpreterLanguageNeeds() {
    return interpreterLanguageNeeds;
  }

  public static Builder subject() {
    return new uk.gov.justice.hearing.courts.Subject.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.hearing.courts.Subject that = (uk.gov.justice.hearing.courts.Subject) obj;

    return java.util.Objects.equals(this.defendantFirstName, that.defendantFirstName) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.defendantLastName, that.defendantLastName) &&
    java.util.Objects.equals(this.defendantRemandStatus, that.defendantRemandStatus) &&
    java.util.Objects.equals(this.interpreterLanguageNeeds, that.interpreterLanguageNeeds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantFirstName, defendantId, defendantLastName, defendantRemandStatus, interpreterLanguageNeeds);}

  public static class Builder {
    private String defendantFirstName;

    private UUID defendantId;

    private String defendantLastName;

    private String defendantRemandStatus;

    private String interpreterLanguageNeeds;

    public Builder withDefendantFirstName(final String defendantFirstName) {
      this.defendantFirstName = defendantFirstName;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withDefendantLastName(final String defendantLastName) {
      this.defendantLastName = defendantLastName;
      return this;
    }

    public Builder withDefendantRemandStatus(final String defendantRemandStatus) {
      this.defendantRemandStatus = defendantRemandStatus;
      return this;
    }

    public Builder withInterpreterLanguageNeeds(final String interpreterLanguageNeeds) {
      this.interpreterLanguageNeeds = interpreterLanguageNeeds;
      return this;
    }

    public Builder withValuesFrom(final Subject subject) {
      this.defendantFirstName = subject.getDefendantFirstName();
      this.defendantId = subject.getDefendantId();
      this.defendantLastName = subject.getDefendantLastName();
      this.defendantRemandStatus = subject.getDefendantRemandStatus();
      this.interpreterLanguageNeeds = subject.getInterpreterLanguageNeeds();
      return this;
    }

    public Subject build() {
      return new uk.gov.justice.hearing.courts.Subject(defendantFirstName, defendantId, defendantLastName, defendantRemandStatus, interpreterLanguageNeeds);
    }
  }
}
