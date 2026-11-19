package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.ProsecutionCase;

public class RelatedHearingUpdatedForAdhocHearing implements Serializable {
  private static final long serialVersionUID = 6292611117311941996L;

  private final UUID hearingId;

  private final List<ProsecutionCase> prosecutionCases;

  private final List<UUID> shadowListedOffences;

  @JsonCreator
  public RelatedHearingUpdatedForAdhocHearing(final UUID hearingId, final List<ProsecutionCase> prosecutionCases, final List<UUID> shadowListedOffences) {
    this.hearingId = hearingId;
    this.prosecutionCases = prosecutionCases;
    this.shadowListedOffences = shadowListedOffences;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<ProsecutionCase> getProsecutionCases() {
    return prosecutionCases;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public static Builder relatedHearingUpdatedForAdhocHearing() {
    return new uk.gov.justice.listing.courts.RelatedHearingUpdatedForAdhocHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.RelatedHearingUpdatedForAdhocHearing that = (uk.gov.justice.listing.courts.RelatedHearingUpdatedForAdhocHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, prosecutionCases, shadowListedOffences);}

  public static class Builder {
    private UUID hearingId;

    private List<ProsecutionCase> prosecutionCases;

    private List<UUID> shadowListedOffences;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withProsecutionCases(final List<ProsecutionCase> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
      return this;
    }

    public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
      this.shadowListedOffences = shadowListedOffences;
      return this;
    }

    public Builder withValuesFrom(final RelatedHearingUpdatedForAdhocHearing relatedHearingUpdatedForAdhocHearing) {
      this.hearingId = relatedHearingUpdatedForAdhocHearing.getHearingId();
      this.prosecutionCases = relatedHearingUpdatedForAdhocHearing.getProsecutionCases();
      this.shadowListedOffences = relatedHearingUpdatedForAdhocHearing.getShadowListedOffences();
      return this;
    }

    public RelatedHearingUpdatedForAdhocHearing build() {
      return new uk.gov.justice.listing.courts.RelatedHearingUpdatedForAdhocHearing(hearingId, prosecutionCases, shadowListedOffences);
    }
  }
}
