package uk.gov.justice.listing.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.event.hearing-counsel-modified")
public class HearingCounselModified implements Serializable {
  private static final long serialVersionUID = 1968620463087905967L;

  private final Action action;

  private final CounselType counselType;

  private final UUID hearingId;

  private final String payload;

  @JsonCreator
  public HearingCounselModified(final Action action, final CounselType counselType, final UUID hearingId, final String payload) {
    this.action = action;
    this.counselType = counselType;
    this.hearingId = hearingId;
    this.payload = payload;
  }

  public Action getAction() {
    return action;
  }

  public CounselType getCounselType() {
    return counselType;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getPayload() {
    return payload;
  }

  public static Builder hearingCounselModified() {
    return new uk.gov.justice.listing.event.HearingCounselModified.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.event.HearingCounselModified that = (uk.gov.justice.listing.event.HearingCounselModified) obj;

    return java.util.Objects.equals(this.action, that.action) &&
    java.util.Objects.equals(this.counselType, that.counselType) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.payload, that.payload);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(action, counselType, hearingId, payload);}

  public static class Builder {
    private Action action;

    private CounselType counselType;

    private UUID hearingId;

    private String payload;

    public Builder withAction(final Action action) {
      this.action = action;
      return this;
    }

    public Builder withCounselType(final CounselType counselType) {
      this.counselType = counselType;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withPayload(final String payload) {
      this.payload = payload;
      return this;
    }

    public Builder withValuesFrom(final HearingCounselModified hearingCounselModified) {
      this.action = hearingCounselModified.getAction();
      this.counselType = hearingCounselModified.getCounselType();
      this.hearingId = hearingCounselModified.getHearingId();
      this.payload = hearingCounselModified.getPayload();
      return this;
    }

    public HearingCounselModified build() {
      return new uk.gov.justice.listing.event.HearingCounselModified(action, counselType, hearingId, payload);
    }
  }
}
