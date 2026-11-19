package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.hearing-language-changed-for-hearing")
public class HearingLanguageChangedForHearing implements Serializable {
  private static final long serialVersionUID = -9052745030168084818L;

  private final UUID hearingId;

  private final HearingLanguage hearingLanguage;

  @JsonCreator
  public HearingLanguageChangedForHearing(final UUID hearingId, final HearingLanguage hearingLanguage) {
    this.hearingId = hearingId;
    this.hearingLanguage = hearingLanguage;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public HearingLanguage getHearingLanguage() {
    return hearingLanguage;
  }

  public static Builder hearingLanguageChangedForHearing() {
    return new uk.gov.justice.listing.events.HearingLanguageChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingLanguageChangedForHearing that = (uk.gov.justice.listing.events.HearingLanguageChangedForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, hearingLanguage);}

  public static class Builder {
    private UUID hearingId;

    private HearingLanguage hearingLanguage;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withHearingLanguage(final HearingLanguage hearingLanguage) {
      this.hearingLanguage = hearingLanguage;
      return this;
    }

    public Builder withValuesFrom(final HearingLanguageChangedForHearing hearingLanguageChangedForHearing) {
      this.hearingId = hearingLanguageChangedForHearing.getHearingId();
      this.hearingLanguage = hearingLanguageChangedForHearing.getHearingLanguage();
      return this;
    }

    public HearingLanguageChangedForHearing build() {
      return new uk.gov.justice.listing.events.HearingLanguageChangedForHearing(hearingId, hearingLanguage);
    }
  }
}
