package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class HearingType implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String description;

  private final UUID id;

  private final String welshDescription;

  @JsonCreator
  public HearingType(final String description, final UUID id, final String welshDescription) {
    this.description = description;
    this.id = id;
    this.welshDescription = welshDescription;
  }

  public String getDescription() {
    return description;
  }

  public UUID getId() {
    return id;
  }

  public String getWelshDescription() {
    return welshDescription;
  }

  public static Builder hearingType() {
    return new uk.gov.justice.core.courts.HearingType.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.HearingType that = (uk.gov.justice.core.courts.HearingType) obj;

    return java.util.Objects.equals(this.description, that.description) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.welshDescription, that.welshDescription);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(description, id, welshDescription);}

  public static class Builder {
    private String description;

    private UUID id;

    private String welshDescription;

    public Builder withDescription(final String description) {
      this.description = description;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withWelshDescription(final String welshDescription) {
      this.welshDescription = welshDescription;
      return this;
    }

    public Builder withValuesFrom(final HearingType hearingType) {
      this.description = hearingType.getDescription();
      this.id = hearingType.getId();
      this.welshDescription = hearingType.getWelshDescription();
      return this;
    }

    public HearingType build() {
      return new uk.gov.justice.core.courts.HearingType(description, id, welshDescription);
    }
  }
}
