package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class Defendants implements Serializable {
  private static final long serialVersionUID = -7701675490946393616L;

  private final UUID defendantId;

  private final List<Offences> offences;

  @JsonCreator
  public Defendants(final UUID defendantId, final List<Offences> offences) {
    this.defendantId = defendantId;
    this.offences = offences;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public List<Offences> getOffences() {
    return offences;
  }

  public static Builder defendants() {
    return new uk.gov.justice.listing.events.Defendants.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.Defendants that = (uk.gov.justice.listing.events.Defendants) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.offences, that.offences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, offences);}

  public static class Builder {
    private UUID defendantId;

    private List<Offences> offences;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withOffences(final List<Offences> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withValuesFrom(final Defendants defendants) {
      this.defendantId = defendants.getDefendantId();
      this.offences = defendants.getOffences();
      return this;
    }

    public Defendants build() {
      return new uk.gov.justice.listing.events.Defendants(defendantId, offences);
    }
  }
}
