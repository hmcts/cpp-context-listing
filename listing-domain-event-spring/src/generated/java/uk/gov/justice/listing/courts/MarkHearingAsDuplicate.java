package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class MarkHearingAsDuplicate implements Serializable {
  private static final long serialVersionUID = -6787590095515610234L;

  private final UUID hearingId;

  private final List<UUID> prosecutionCaseIds;

  @JsonCreator
  public MarkHearingAsDuplicate(final UUID hearingId, final List<UUID> prosecutionCaseIds) {
    this.hearingId = hearingId;
    this.prosecutionCaseIds = prosecutionCaseIds;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<UUID> getProsecutionCaseIds() {
    return prosecutionCaseIds;
  }

  public static Builder markHearingAsDuplicate() {
    return new uk.gov.justice.listing.courts.MarkHearingAsDuplicate.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.MarkHearingAsDuplicate that = (uk.gov.justice.listing.courts.MarkHearingAsDuplicate) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.prosecutionCaseIds, that.prosecutionCaseIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, prosecutionCaseIds);}

  public static class Builder {
    private UUID hearingId;

    private List<UUID> prosecutionCaseIds;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withProsecutionCaseIds(final List<UUID> prosecutionCaseIds) {
      this.prosecutionCaseIds = prosecutionCaseIds;
      return this;
    }

    public Builder withValuesFrom(final MarkHearingAsDuplicate markHearingAsDuplicate) {
      this.hearingId = markHearingAsDuplicate.getHearingId();
      this.prosecutionCaseIds = markHearingAsDuplicate.getProsecutionCaseIds();
      return this;
    }

    public MarkHearingAsDuplicate build() {
      return new uk.gov.justice.listing.courts.MarkHearingAsDuplicate(hearingId, prosecutionCaseIds);
    }
  }
}
