package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class SummonsApprovedOutcome implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Boolean personalService;

  private final String prosecutorCost;

  private final String prosecutorEmailAddress;

  private final Boolean summonsSuppressed;

  @JsonCreator
  public SummonsApprovedOutcome(final Boolean personalService, final String prosecutorCost, final String prosecutorEmailAddress, final Boolean summonsSuppressed) {
    this.personalService = personalService;
    this.prosecutorCost = prosecutorCost;
    this.prosecutorEmailAddress = prosecutorEmailAddress;
    this.summonsSuppressed = summonsSuppressed;
  }

  public Boolean getPersonalService() {
    return personalService;
  }

  public String getProsecutorCost() {
    return prosecutorCost;
  }

  public String getProsecutorEmailAddress() {
    return prosecutorEmailAddress;
  }

  public Boolean getSummonsSuppressed() {
    return summonsSuppressed;
  }

  public static Builder summonsApprovedOutcome() {
    return new uk.gov.justice.core.courts.SummonsApprovedOutcome.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.SummonsApprovedOutcome that = (uk.gov.justice.core.courts.SummonsApprovedOutcome) obj;

    return java.util.Objects.equals(this.personalService, that.personalService) &&
    java.util.Objects.equals(this.prosecutorCost, that.prosecutorCost) &&
    java.util.Objects.equals(this.prosecutorEmailAddress, that.prosecutorEmailAddress) &&
    java.util.Objects.equals(this.summonsSuppressed, that.summonsSuppressed);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(personalService, prosecutorCost, prosecutorEmailAddress, summonsSuppressed);}

  public static class Builder {
    private Boolean personalService;

    private String prosecutorCost;

    private String prosecutorEmailAddress;

    private Boolean summonsSuppressed;

    public Builder withPersonalService(final Boolean personalService) {
      this.personalService = personalService;
      return this;
    }

    public Builder withProsecutorCost(final String prosecutorCost) {
      this.prosecutorCost = prosecutorCost;
      return this;
    }

    public Builder withProsecutorEmailAddress(final String prosecutorEmailAddress) {
      this.prosecutorEmailAddress = prosecutorEmailAddress;
      return this;
    }

    public Builder withSummonsSuppressed(final Boolean summonsSuppressed) {
      this.summonsSuppressed = summonsSuppressed;
      return this;
    }

    public Builder withValuesFrom(final SummonsApprovedOutcome summonsApprovedOutcome) {
      this.personalService = summonsApprovedOutcome.getPersonalService();
      this.prosecutorCost = summonsApprovedOutcome.getProsecutorCost();
      this.prosecutorEmailAddress = summonsApprovedOutcome.getProsecutorEmailAddress();
      this.summonsSuppressed = summonsApprovedOutcome.getSummonsSuppressed();
      return this;
    }

    public SummonsApprovedOutcome build() {
      return new uk.gov.justice.core.courts.SummonsApprovedOutcome(personalService, prosecutorCost, prosecutorEmailAddress, summonsSuppressed);
    }
  }
}
