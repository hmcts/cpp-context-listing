package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067" })
public class ReportingRestrictionData {
  private UUID id;

  private Optional<UUID> judicialResultId;

  private String label;

  private Optional<LocalDate> orderedDate;

  public ReportingRestrictionData(final UUID id, final Optional<UUID> judicialResultId, final String label, final Optional<LocalDate> orderedDate) {
    this.id = id;
    this.judicialResultId = judicialResultId;
    this.label = label;
    this.orderedDate = orderedDate;
  }

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public Optional<UUID> getJudicialResultId() {
    return judicialResultId;
  }

  public void setJudicialResultId(final Optional<UUID> judicialResultId) {
    this.judicialResultId = judicialResultId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  public Optional<LocalDate> getOrderedDate() {
    return orderedDate;
  }

  public void setOrderedDate(final Optional<LocalDate> orderedDate) {
    this.orderedDate = orderedDate;
  }

  public static Builder reportingRestriction() {
    return new ReportingRestrictionData.Builder();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ReportingRestrictionData that = (ReportingRestrictionData) o;
    return Objects.equals(id, that.id) &&
            Objects.equals(judicialResultId, that.judicialResultId) &&
            Objects.equals(label, that.label) &&
            Objects.equals(orderedDate, that.orderedDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, judicialResultId, label, orderedDate);
  }

  @Override
  public String toString() {
    return "ReportingRestriction{" +
            "id=" + id +
            ", judicialResultId=" + judicialResultId +
            ", label='" + label + '\'' +
            ", orderedDate=" + orderedDate +
            '}';
  }

  public static class Builder {
    private UUID id;

    private Optional<UUID> judicialResultId;

    private String label;

    private Optional<LocalDate> orderedDate;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withJudicialResultId(final Optional<UUID> judicialResultId) {
      this.judicialResultId = judicialResultId;
      return this;
    }

    public Builder withLabel(final String label) {
      this.label = label;
      return this;
    }

    public Builder withOrderedDate(final Optional<LocalDate> orderedDate) {
      this.orderedDate = orderedDate;
      return this;
    }

    public ReportingRestrictionData build() {
      return new ReportingRestrictionData(id, judicialResultId, label, orderedDate);
    }
  }
}
