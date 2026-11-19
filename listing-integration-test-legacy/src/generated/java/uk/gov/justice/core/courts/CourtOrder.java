package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.courts.JudicialChildResults;

public class CourtOrder implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Boolean canBeSubjectOfBreachProceedings;

  private final Boolean canBeSubjectOfVariationProceedings;

  private final List<CourtOrderOffence> courtOrderOffences;

  private final List<String> defendantIds;

  private final String endDate;

  private final String expiryDate;

  private final UUID id;

  private final Boolean isSJPOrder;

  private final Boolean isUnpaidWork;

  private final List<JudicialChildResults> judicialChildResults;

  private final UUID judicialResultTypeId;

  private final String label;

  private final UUID masterDefendantId;

  private final String orderDate;

  private final CourtCentre orderingCourt;

  private final UUID orderingHearingId;

  private final String startDate;

  @JsonCreator
  public CourtOrder(final Boolean canBeSubjectOfBreachProceedings, final Boolean canBeSubjectOfVariationProceedings, final List<CourtOrderOffence> courtOrderOffences, final List<String> defendantIds, final String endDate, final String expiryDate, final UUID id, final Boolean isSJPOrder, final Boolean isUnpaidWork, final List<JudicialChildResults> judicialChildResults, final UUID judicialResultTypeId, final String label, final UUID masterDefendantId, final String orderDate, final CourtCentre orderingCourt, final UUID orderingHearingId, final String startDate) {
    this.canBeSubjectOfBreachProceedings = canBeSubjectOfBreachProceedings;
    this.canBeSubjectOfVariationProceedings = canBeSubjectOfVariationProceedings;
    this.courtOrderOffences = courtOrderOffences;
    this.defendantIds = defendantIds;
    this.endDate = endDate;
    this.expiryDate = expiryDate;
    this.id = id;
    this.isSJPOrder = isSJPOrder;
    this.isUnpaidWork = isUnpaidWork;
    this.judicialChildResults = judicialChildResults;
    this.judicialResultTypeId = judicialResultTypeId;
    this.label = label;
    this.masterDefendantId = masterDefendantId;
    this.orderDate = orderDate;
    this.orderingCourt = orderingCourt;
    this.orderingHearingId = orderingHearingId;
    this.startDate = startDate;
  }

  public Boolean getCanBeSubjectOfBreachProceedings() {
    return canBeSubjectOfBreachProceedings;
  }

  public Boolean getCanBeSubjectOfVariationProceedings() {
    return canBeSubjectOfVariationProceedings;
  }

  public List<CourtOrderOffence> getCourtOrderOffences() {
    return courtOrderOffences;
  }

  public List<String> getDefendantIds() {
    return defendantIds;
  }

  public String getEndDate() {
    return endDate;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsSJPOrder() {
    return isSJPOrder;
  }

  public Boolean getIsUnpaidWork() {
    return isUnpaidWork;
  }

  public List<JudicialChildResults> getJudicialChildResults() {
    return judicialChildResults;
  }

  public UUID getJudicialResultTypeId() {
    return judicialResultTypeId;
  }

  public String getLabel() {
    return label;
  }

  public UUID getMasterDefendantId() {
    return masterDefendantId;
  }

  public String getOrderDate() {
    return orderDate;
  }

  public CourtCentre getOrderingCourt() {
    return orderingCourt;
  }

  public UUID getOrderingHearingId() {
    return orderingHearingId;
  }

  public String getStartDate() {
    return startDate;
  }

  public static Builder courtOrder() {
    return new uk.gov.justice.core.courts.CourtOrder.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtOrder that = (uk.gov.justice.core.courts.CourtOrder) obj;

    return java.util.Objects.equals(this.canBeSubjectOfBreachProceedings, that.canBeSubjectOfBreachProceedings) &&
    java.util.Objects.equals(this.canBeSubjectOfVariationProceedings, that.canBeSubjectOfVariationProceedings) &&
    java.util.Objects.equals(this.courtOrderOffences, that.courtOrderOffences) &&
    java.util.Objects.equals(this.defendantIds, that.defendantIds) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.expiryDate, that.expiryDate) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isSJPOrder, that.isSJPOrder) &&
    java.util.Objects.equals(this.isUnpaidWork, that.isUnpaidWork) &&
    java.util.Objects.equals(this.judicialChildResults, that.judicialChildResults) &&
    java.util.Objects.equals(this.judicialResultTypeId, that.judicialResultTypeId) &&
    java.util.Objects.equals(this.label, that.label) &&
    java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId) &&
    java.util.Objects.equals(this.orderDate, that.orderDate) &&
    java.util.Objects.equals(this.orderingCourt, that.orderingCourt) &&
    java.util.Objects.equals(this.orderingHearingId, that.orderingHearingId) &&
    java.util.Objects.equals(this.startDate, that.startDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(canBeSubjectOfBreachProceedings, canBeSubjectOfVariationProceedings, courtOrderOffences, defendantIds, endDate, expiryDate, id, isSJPOrder, isUnpaidWork, judicialChildResults, judicialResultTypeId, label, masterDefendantId, orderDate, orderingCourt, orderingHearingId, startDate);}

  public static class Builder {
    private Boolean canBeSubjectOfBreachProceedings;

    private Boolean canBeSubjectOfVariationProceedings;

    private List<CourtOrderOffence> courtOrderOffences;

    private List<String> defendantIds;

    private String endDate;

    private String expiryDate;

    private UUID id;

    private Boolean isSJPOrder;

    private Boolean isUnpaidWork;

    private List<JudicialChildResults> judicialChildResults;

    private UUID judicialResultTypeId;

    private String label;

    private UUID masterDefendantId;

    private String orderDate;

    private CourtCentre orderingCourt;

    private UUID orderingHearingId;

    private String startDate;

    public Builder withCanBeSubjectOfBreachProceedings(final Boolean canBeSubjectOfBreachProceedings) {
      this.canBeSubjectOfBreachProceedings = canBeSubjectOfBreachProceedings;
      return this;
    }

    public Builder withCanBeSubjectOfVariationProceedings(final Boolean canBeSubjectOfVariationProceedings) {
      this.canBeSubjectOfVariationProceedings = canBeSubjectOfVariationProceedings;
      return this;
    }

    public Builder withCourtOrderOffences(final List<CourtOrderOffence> courtOrderOffences) {
      this.courtOrderOffences = courtOrderOffences;
      return this;
    }

    public Builder withDefendantIds(final List<String> defendantIds) {
      this.defendantIds = defendantIds;
      return this;
    }

    public Builder withEndDate(final String endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withExpiryDate(final String expiryDate) {
      this.expiryDate = expiryDate;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withIsSJPOrder(final Boolean isSJPOrder) {
      this.isSJPOrder = isSJPOrder;
      return this;
    }

    public Builder withIsUnpaidWork(final Boolean isUnpaidWork) {
      this.isUnpaidWork = isUnpaidWork;
      return this;
    }

    public Builder withJudicialChildResults(final List<JudicialChildResults> judicialChildResults) {
      this.judicialChildResults = judicialChildResults;
      return this;
    }

    public Builder withJudicialResultTypeId(final UUID judicialResultTypeId) {
      this.judicialResultTypeId = judicialResultTypeId;
      return this;
    }

    public Builder withLabel(final String label) {
      this.label = label;
      return this;
    }

    public Builder withMasterDefendantId(final UUID masterDefendantId) {
      this.masterDefendantId = masterDefendantId;
      return this;
    }

    public Builder withOrderDate(final String orderDate) {
      this.orderDate = orderDate;
      return this;
    }

    public Builder withOrderingCourt(final CourtCentre orderingCourt) {
      this.orderingCourt = orderingCourt;
      return this;
    }

    public Builder withOrderingHearingId(final UUID orderingHearingId) {
      this.orderingHearingId = orderingHearingId;
      return this;
    }

    public Builder withStartDate(final String startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withValuesFrom(final CourtOrder courtOrder) {
      this.canBeSubjectOfBreachProceedings = courtOrder.getCanBeSubjectOfBreachProceedings();
      this.canBeSubjectOfVariationProceedings = courtOrder.getCanBeSubjectOfVariationProceedings();
      this.courtOrderOffences = courtOrder.getCourtOrderOffences();
      this.defendantIds = courtOrder.getDefendantIds();
      this.endDate = courtOrder.getEndDate();
      this.expiryDate = courtOrder.getExpiryDate();
      this.id = courtOrder.getId();
      this.isSJPOrder = courtOrder.getIsSJPOrder();
      this.isUnpaidWork = courtOrder.getIsUnpaidWork();
      this.judicialChildResults = courtOrder.getJudicialChildResults();
      this.judicialResultTypeId = courtOrder.getJudicialResultTypeId();
      this.label = courtOrder.getLabel();
      this.masterDefendantId = courtOrder.getMasterDefendantId();
      this.orderDate = courtOrder.getOrderDate();
      this.orderingCourt = courtOrder.getOrderingCourt();
      this.orderingHearingId = courtOrder.getOrderingHearingId();
      this.startDate = courtOrder.getStartDate();
      return this;
    }

    public CourtOrder build() {
      return new uk.gov.justice.core.courts.CourtOrder(canBeSubjectOfBreachProceedings, canBeSubjectOfVariationProceedings, courtOrderOffences, defendantIds, endDate, expiryDate, id, isSJPOrder, isUnpaidWork, judicialChildResults, judicialResultTypeId, label, masterDefendantId, orderDate, orderingCourt, orderingHearingId, startDate);
    }
  }
}
