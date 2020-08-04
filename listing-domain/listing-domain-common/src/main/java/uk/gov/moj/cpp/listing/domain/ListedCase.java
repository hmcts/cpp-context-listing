package uk.gov.moj.cpp.listing.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "pmd:BeanMembersShouldSerialize"})
public class ListedCase {
  private final CaseIdentifier caseIdentifier;

  private final List<Defendant> defendants;

  private final List<CaseMarker> caseMarkers;

  private final UUID id;

  private final Optional<Boolean> shadowListed;

  @SuppressWarnings({"squid:S2384"})
  public ListedCase(final CaseIdentifier caseIdentifier, final List<Defendant> defendants, final List<CaseMarker> caseMarkers, final UUID id, final Optional<Boolean> shadowListed) {
    this.caseIdentifier = caseIdentifier;
    this.defendants = defendants;
    this.caseMarkers = caseMarkers;
    this.id = id;
    this.shadowListed = shadowListed;
  }

  public CaseIdentifier getCaseIdentifier() {
    return caseIdentifier;
  }

  @SuppressWarnings({"squid:S2384"})
  public List<Defendant> getDefendants() {
    return defendants;
  }

  @SuppressWarnings({"squid:S2384"})
  public List<CaseMarker> getCaseMarkers() {
    return caseMarkers;
  }

  public UUID getId() {
    return id;
  }

  public Optional<Boolean> getShadowListed() {
    return shadowListed;
  }

  public static Builder listedCase() {
    return new ListedCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    final ListedCase that = (ListedCase) obj;

    return java.util.Objects.equals(this.caseIdentifier, that.caseIdentifier) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.shadowListed, that.shadowListed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseIdentifier, defendants, caseMarkers, id, shadowListed);
  }

  @Override
  public String toString() {
    return "ListedCase{" +
            "caseIdentifier=" + caseIdentifier +
            ", defendants=" + defendants +
            ", caseMarkers=" + caseMarkers +
            ", id=" + id +
            ", shadowListed=" + shadowListed +
            '}';
  }

  @SuppressWarnings("pmd:BeanMembersShouldSerialize")
  public static class Builder {
    private CaseIdentifier caseIdentifier;

    private List<Defendant> defendants;

    private UUID id;

    private List<CaseMarker> caseMarkers;

    private Optional<Boolean> shadowListed;

    public Builder withCaseIdentifier(final CaseIdentifier caseIdentifier) {
      this.caseIdentifier = caseIdentifier;
      return this;
    }

    @SuppressWarnings({"squid:S2384"})
    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    @SuppressWarnings({"squid:S2384"})
    public Builder withCaseMarkers(final List<CaseMarker> caseMarkers) {
      this.caseMarkers = caseMarkers;
      return this;
    }


    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withShadowListed(final Optional<Boolean> shadowListed){
      this.shadowListed = shadowListed;
      return this;
    }

    public ListedCase build() {
      return new ListedCase(caseIdentifier, defendants, caseMarkers, id, shadowListed);
    }
  }
}
