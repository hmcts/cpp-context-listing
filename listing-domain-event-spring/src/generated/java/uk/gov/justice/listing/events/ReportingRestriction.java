package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.util.UUID;

public class ReportingRestriction implements Serializable {
  private static final long serialVersionUID = -4253264836934810101L;

  private final UUID id;

  private final UUID judicialResultId;

  private final String label;

  private final LocalDate orderedDate;

  @JsonCreator
  public ReportingRestriction(final UUID id, final UUID judicialResultId, final String label, final LocalDate orderedDate) {
    this.id = id;
    this.judicialResultId = judicialResultId;
    this.label = label;
    this.orderedDate = orderedDate;
  }

  public UUID getId() {
    return id;
  }

  public UUID getJudicialResultId() {
    return judicialResultId;
  }

  public String getLabel() {
    return label;
  }

  public LocalDate getOrderedDate() {
    return orderedDate;
  }

  public static Builder reportingRestriction() {
    return new uk.gov.justice.listing.events.ReportingRestriction.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ReportingRestriction that = (uk.gov.justice.listing.events.ReportingRestriction) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.judicialResultId, that.judicialResultId) &&
    java.util.Objects.equals(this.label, that.label) &&
    java.util.Objects.equals(this.orderedDate, that.orderedDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, judicialResultId, label, orderedDate);}

  public static class Builder {
    private UUID id;

    private UUID judicialResultId;

    private String label;

    private LocalDate orderedDate;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withJudicialResultId(final UUID judicialResultId) {
      this.judicialResultId = judicialResultId;
      return this;
    }

    public Builder withLabel(final String label) {
      this.label = label;
      return this;
    }

    public Builder withOrderedDate(final LocalDate orderedDate) {
      this.orderedDate = orderedDate;
      return this;
    }

    public Builder withValuesFrom(final ReportingRestriction reportingRestriction) {
      this.id = reportingRestriction.getId();
      this.judicialResultId = reportingRestriction.getJudicialResultId();
      this.label = reportingRestriction.getLabel();
      this.orderedDate = reportingRestriction.getOrderedDate();
      return this;
    }

    public ReportingRestriction build() {
      return new uk.gov.justice.listing.events.ReportingRestriction(id, judicialResultId, label, orderedDate);
    }
  }
}
