package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.type-changed-for-hearing")
public class TypeChangedForHearing implements Serializable {
  private static final long serialVersionUID = -2818974920466990774L;

  private final UUID hearingId;

  private final Type type;

  @JsonCreator
  public TypeChangedForHearing(final UUID hearingId, final Type type) {
    this.hearingId = hearingId;
    this.type = type;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public Type getType() {
    return type;
  }

  public static Builder typeChangedForHearing() {
    return new uk.gov.justice.listing.events.TypeChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.TypeChangedForHearing that = (uk.gov.justice.listing.events.TypeChangedForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.type, that.type);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, type);}

  public static class Builder {
    private UUID hearingId;

    private Type type;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withType(final Type type) {
      this.type = type;
      return this;
    }

    public Builder withValuesFrom(final TypeChangedForHearing typeChangedForHearing) {
      this.hearingId = typeChangedForHearing.getHearingId();
      this.type = typeChangedForHearing.getType();
      return this;
    }

    public TypeChangedForHearing build() {
      return new uk.gov.justice.listing.events.TypeChangedForHearing(hearingId, type);
    }
  }
}
