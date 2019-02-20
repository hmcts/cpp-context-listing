package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class ProsecutionCaseDefendantOffenceIds implements Serializable {
  private static final long serialVersionUID = 1L;


  private final List<DefendantOffenceIds> defendants;

  private final UUID id;

  public ProsecutionCaseDefendantOffenceIds(final List<DefendantOffenceIds> defendants, final UUID id) {
    this.defendants = defendants;
    this.id = id;
  }

  public List<DefendantOffenceIds> getDefendants() {
    return defendants;
  }

  public UUID getId() {
    return id;
  }

  public static Builder prosecutionCaseDefendantOffenceIds() {
    return new ProsecutionCaseDefendantOffenceIds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final ProsecutionCaseDefendantOffenceIds that = (ProsecutionCaseDefendantOffenceIds) obj;

    return java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendants, id);}

  @Override
  public String toString() {
    return "ProsecutionCaseDefendantOffenceIds{" +
    	"defendants='" + defendants + "'," +
    	"id='" + id + "'" +
    "}";
  }

  public static class Builder {
    private List<DefendantOffenceIds> defendants;

    private UUID id;

    public Builder withDefendants(final List<DefendantOffenceIds> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public ProsecutionCaseDefendantOffenceIds build() {
      return new ProsecutionCaseDefendantOffenceIds(defendants, id);
    }
  }
}
