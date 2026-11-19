package uk.gov.moj.cpp.hearing.json.schema.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.DefenceCounsel;

public class DefenceCounselUpdated implements Serializable {
  private static final long serialVersionUID = -5311579648723155256L;

  private final DefenceCounsel defenceCounsel;

  private final UUID hearingId;

  @JsonCreator
  public DefenceCounselUpdated(final DefenceCounsel defenceCounsel, final UUID hearingId) {
    this.defenceCounsel = defenceCounsel;
    this.hearingId = hearingId;
  }

  public DefenceCounsel getDefenceCounsel() {
    return defenceCounsel;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder defenceCounselUpdated() {
    return new uk.gov.moj.cpp.hearing.json.schema.event.DefenceCounselUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.moj.cpp.hearing.json.schema.event.DefenceCounselUpdated that = (uk.gov.moj.cpp.hearing.json.schema.event.DefenceCounselUpdated) obj;

    return java.util.Objects.equals(this.defenceCounsel, that.defenceCounsel) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defenceCounsel, hearingId);}

  public static class Builder {
    private DefenceCounsel defenceCounsel;

    private UUID hearingId;

    public Builder withDefenceCounsel(final DefenceCounsel defenceCounsel) {
      this.defenceCounsel = defenceCounsel;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final DefenceCounselUpdated defenceCounselUpdated) {
      this.defenceCounsel = defenceCounselUpdated.getDefenceCounsel();
      this.hearingId = defenceCounselUpdated.getHearingId();
      return this;
    }

    public DefenceCounselUpdated build() {
      return new uk.gov.moj.cpp.hearing.json.schema.event.DefenceCounselUpdated(defenceCounsel, hearingId);
    }
  }
}
