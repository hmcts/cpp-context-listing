package uk.gov.moj.cpp.hearing.json.schema.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCounsel;

public class ProsecutionCounselUpdated implements Serializable {
  private static final long serialVersionUID = -7964788930062250847L;

  private final UUID hearingId;

  private final ProsecutionCounsel prosecutionCounsel;

  @JsonCreator
  public ProsecutionCounselUpdated(final UUID hearingId, final ProsecutionCounsel prosecutionCounsel) {
    this.hearingId = hearingId;
    this.prosecutionCounsel = prosecutionCounsel;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public ProsecutionCounsel getProsecutionCounsel() {
    return prosecutionCounsel;
  }

  public static Builder prosecutionCounselUpdated() {
    return new uk.gov.moj.cpp.hearing.json.schema.event.ProsecutionCounselUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.moj.cpp.hearing.json.schema.event.ProsecutionCounselUpdated that = (uk.gov.moj.cpp.hearing.json.schema.event.ProsecutionCounselUpdated) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.prosecutionCounsel, that.prosecutionCounsel);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, prosecutionCounsel);}

  public static class Builder {
    private UUID hearingId;

    private ProsecutionCounsel prosecutionCounsel;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withProsecutionCounsel(final ProsecutionCounsel prosecutionCounsel) {
      this.prosecutionCounsel = prosecutionCounsel;
      return this;
    }

    public Builder withValuesFrom(final ProsecutionCounselUpdated prosecutionCounselUpdated) {
      this.hearingId = prosecutionCounselUpdated.getHearingId();
      this.prosecutionCounsel = prosecutionCounselUpdated.getProsecutionCounsel();
      return this;
    }

    public ProsecutionCounselUpdated build() {
      return new uk.gov.moj.cpp.hearing.json.schema.event.ProsecutionCounselUpdated(hearingId, prosecutionCounsel);
    }
  }
}
