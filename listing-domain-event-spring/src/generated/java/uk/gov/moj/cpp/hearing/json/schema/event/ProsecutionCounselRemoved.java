package uk.gov.moj.cpp.hearing.json.schema.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class ProsecutionCounselRemoved implements Serializable {
  private static final long serialVersionUID = -2735080683831537622L;

  private final UUID hearingId;

  private final UUID id;

  @JsonCreator
  public ProsecutionCounselRemoved(final UUID hearingId, final UUID id) {
    this.hearingId = hearingId;
    this.id = id;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getId() {
    return id;
  }

  public static Builder prosecutionCounselRemoved() {
    return new uk.gov.moj.cpp.hearing.json.schema.event.ProsecutionCounselRemoved.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.moj.cpp.hearing.json.schema.event.ProsecutionCounselRemoved that = (uk.gov.moj.cpp.hearing.json.schema.event.ProsecutionCounselRemoved) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, id);}

  public static class Builder {
    private UUID hearingId;

    private UUID id;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withValuesFrom(final ProsecutionCounselRemoved prosecutionCounselRemoved) {
      this.hearingId = prosecutionCounselRemoved.getHearingId();
      this.id = prosecutionCounselRemoved.getId();
      return this;
    }

    public ProsecutionCounselRemoved build() {
      return new uk.gov.moj.cpp.hearing.json.schema.event.ProsecutionCounselRemoved(hearingId, id);
    }
  }
}
