package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CrackedIneffectiveTrial implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final String code;

  private final UUID crackedIneffectiveSubReasonId;

  private final String date;

  private final String description;

  private final UUID id;

  private final String type;

  @JsonCreator
  public CrackedIneffectiveTrial(final String code, final UUID crackedIneffectiveSubReasonId, final String date, final String description, final UUID id, final String type) {
    this.code = code;
    this.crackedIneffectiveSubReasonId = crackedIneffectiveSubReasonId;
    this.date = date;
    this.description = description;
    this.id = id;
    this.type = type;
  }

  public String getCode() {
    return code;
  }

  public UUID getCrackedIneffectiveSubReasonId() {
    return crackedIneffectiveSubReasonId;
  }

  public String getDate() {
    return date;
  }

  public String getDescription() {
    return description;
  }

  public UUID getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public static Builder crackedIneffectiveTrial() {
    return new uk.gov.justice.core.courts.CrackedIneffectiveTrial.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CrackedIneffectiveTrial that = (uk.gov.justice.core.courts.CrackedIneffectiveTrial) obj;

    return java.util.Objects.equals(this.code, that.code) &&
    java.util.Objects.equals(this.crackedIneffectiveSubReasonId, that.crackedIneffectiveSubReasonId) &&
    java.util.Objects.equals(this.date, that.date) &&
    java.util.Objects.equals(this.description, that.description) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.type, that.type);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(code, crackedIneffectiveSubReasonId, date, description, id, type);}

  public static class Builder {
    private String code;

    private UUID crackedIneffectiveSubReasonId;

    private String date;

    private String description;

    private UUID id;

    private String type;

    public Builder withCode(final String code) {
      this.code = code;
      return this;
    }

    public Builder withCrackedIneffectiveSubReasonId(final UUID crackedIneffectiveSubReasonId) {
      this.crackedIneffectiveSubReasonId = crackedIneffectiveSubReasonId;
      return this;
    }

    public Builder withDate(final String date) {
      this.date = date;
      return this;
    }

    public Builder withDescription(final String description) {
      this.description = description;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withType(final String type) {
      this.type = type;
      return this;
    }

    public Builder withValuesFrom(final CrackedIneffectiveTrial crackedIneffectiveTrial) {
      this.code = crackedIneffectiveTrial.getCode();
      this.crackedIneffectiveSubReasonId = crackedIneffectiveTrial.getCrackedIneffectiveSubReasonId();
      this.date = crackedIneffectiveTrial.getDate();
      this.description = crackedIneffectiveTrial.getDescription();
      this.id = crackedIneffectiveTrial.getId();
      this.type = crackedIneffectiveTrial.getType();
      return this;
    }

    public CrackedIneffectiveTrial build() {
      return new uk.gov.justice.core.courts.CrackedIneffectiveTrial(code, crackedIneffectiveSubReasonId, date, description, id, type);
    }
  }
}
