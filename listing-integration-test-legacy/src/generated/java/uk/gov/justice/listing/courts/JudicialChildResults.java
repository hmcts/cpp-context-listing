package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class JudicialChildResults implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final UUID judicialResultId;

  private final UUID judicialResultTypeId;

  private final String label;

  @JsonCreator
  public JudicialChildResults(final UUID judicialResultId, final UUID judicialResultTypeId, final String label) {
    this.judicialResultId = judicialResultId;
    this.judicialResultTypeId = judicialResultTypeId;
    this.label = label;
  }

  public UUID getJudicialResultId() {
    return judicialResultId;
  }

  public UUID getJudicialResultTypeId() {
    return judicialResultTypeId;
  }

  public String getLabel() {
    return label;
  }

  public static Builder judicialChildResults() {
    return new uk.gov.justice.listing.courts.JudicialChildResults.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.JudicialChildResults that = (uk.gov.justice.listing.courts.JudicialChildResults) obj;

    return java.util.Objects.equals(this.judicialResultId, that.judicialResultId) &&
    java.util.Objects.equals(this.judicialResultTypeId, that.judicialResultTypeId) &&
    java.util.Objects.equals(this.label, that.label);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(judicialResultId, judicialResultTypeId, label);}

  public static class Builder {
    private UUID judicialResultId;

    private UUID judicialResultTypeId;

    private String label;

    public Builder withJudicialResultId(final UUID judicialResultId) {
      this.judicialResultId = judicialResultId;
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

    public Builder withValuesFrom(final JudicialChildResults judicialChildResults) {
      this.judicialResultId = judicialChildResults.getJudicialResultId();
      this.judicialResultTypeId = judicialChildResults.getJudicialResultTypeId();
      this.label = judicialChildResults.getLabel();
      return this;
    }

    public JudicialChildResults build() {
      return new uk.gov.justice.listing.courts.JudicialChildResults(judicialResultId, judicialResultTypeId, label);
    }
  }
}
