package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class DefendantOffenceIds implements Serializable {
  private static final long serialVersionUID = 1L;


  private final UUID id;

  private final List<OffenceIds> offences;

  public DefendantOffenceIds(final UUID id, final List<OffenceIds> offences) {
    this.id = id;
    this.offences = offences;
  }

  public UUID getId() {
    return id;
  }

  public List<OffenceIds> getOffences() {
    return offences;
  }

  public static Builder defendantOffenceIds() {
    return new DefendantOffenceIds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final DefendantOffenceIds that = (DefendantOffenceIds) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.offences, that.offences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, offences);}

  @Override
  public String toString() {
    return "DefendantOffenceIds{" +
    	"id='" + id + "'," +
    	"offences='" + offences + "'" +
    "}";
  }

  public static class Builder {
    private UUID id;

    private List<OffenceIds> offences;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withOffences(final List<OffenceIds> offences) {
      this.offences = offences;
      return this;
    }

    public DefendantOffenceIds build() {
      return new DefendantOffenceIds(id, offences);
    }
  }
}
