package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.JudicialRole;

public class ChangeJudiciaryForHearings implements Serializable {
  private static final long serialVersionUID = -6344990767996753491L;

  private final List<UUID> hearings;

  private final List<JudicialRole> judiciary;

  private final JudiciaryAssignmentSource judiciaryAssignmentSource;

  @JsonCreator
  public ChangeJudiciaryForHearings(final List<UUID> hearings, final List<JudicialRole> judiciary, final JudiciaryAssignmentSource judiciaryAssignmentSource) {
    this.hearings = hearings;
    this.judiciary = judiciary;
    this.judiciaryAssignmentSource = judiciaryAssignmentSource;
  }

  public List<UUID> getHearings() {
    return hearings;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public JudiciaryAssignmentSource getJudiciaryAssignmentSource() {
    return judiciaryAssignmentSource;
  }

  public static Builder changeJudiciaryForHearings() {
    return new uk.gov.justice.listing.courts.ChangeJudiciaryForHearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ChangeJudiciaryForHearings that = (uk.gov.justice.listing.courts.ChangeJudiciaryForHearings) obj;

    return java.util.Objects.equals(this.hearings, that.hearings) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.judiciaryAssignmentSource, that.judiciaryAssignmentSource);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearings, judiciary, judiciaryAssignmentSource);}

  public static class Builder {
    private List<UUID> hearings;

    private List<JudicialRole> judiciary;

    private JudiciaryAssignmentSource judiciaryAssignmentSource;

    public Builder withHearings(final List<UUID> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withJudiciary(final List<JudicialRole> judiciary) {
      this.judiciary = judiciary;
      return this;
    }

    public Builder withJudiciaryAssignmentSource(final JudiciaryAssignmentSource judiciaryAssignmentSource) {
      this.judiciaryAssignmentSource = judiciaryAssignmentSource;
      return this;
    }

    public Builder withValuesFrom(final ChangeJudiciaryForHearings changeJudiciaryForHearings) {
      this.hearings = changeJudiciaryForHearings.getHearings();
      this.judiciary = changeJudiciaryForHearings.getJudiciary();
      this.judiciaryAssignmentSource = changeJudiciaryForHearings.getJudiciaryAssignmentSource();
      return this;
    }

    public ChangeJudiciaryForHearings build() {
      return new uk.gov.justice.listing.courts.ChangeJudiciaryForHearings(hearings, judiciary, judiciaryAssignmentSource);
    }
  }
}
