package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class SimpleOffence implements Serializable {
  private static final long serialVersionUID = 5714527378216727770L;

  private final UUID defendantId;

  private final UUID id;

  @JsonCreator
  public SimpleOffence(final UUID defendantId, final UUID id) {
    this.defendantId = defendantId;
    this.id = id;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public UUID getId() {
    return id;
  }

  public static Builder simpleOffence() {
    return new uk.gov.justice.listing.events.SimpleOffence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.SimpleOffence that = (uk.gov.justice.listing.events.SimpleOffence) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, id);}

  public static class Builder {
    private UUID defendantId;

    private UUID id;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withValuesFrom(final SimpleOffence simpleOffence) {
      this.defendantId = simpleOffence.getDefendantId();
      this.id = simpleOffence.getId();
      return this;
    }

    public SimpleOffence build() {
      return new uk.gov.justice.listing.events.SimpleOffence(defendantId, id);
    }
  }
}
