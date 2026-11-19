package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.judiciary-assigned-to-hearing")
public class JudiciaryAssignedToHearing implements Serializable {
  private static final long serialVersionUID = -4032135868518979218L;

  private final UUID hearingId;

  private final List<JudicialRole> judiciary;

  private final JudiciaryAssignmentSource judiciaryAssignmentSource;

  @JsonCreator
  public JudiciaryAssignedToHearing(final UUID hearingId, final List<JudicialRole> judiciary, final JudiciaryAssignmentSource judiciaryAssignmentSource) {
    this.hearingId = hearingId;
    this.judiciary = judiciary;
    this.judiciaryAssignmentSource = judiciaryAssignmentSource;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public JudiciaryAssignmentSource getJudiciaryAssignmentSource() {
    return judiciaryAssignmentSource;
  }

  public static Builder judiciaryAssignedToHearing() {
    return new uk.gov.justice.listing.events.JudiciaryAssignedToHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.JudiciaryAssignedToHearing that = (uk.gov.justice.listing.events.JudiciaryAssignedToHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.judiciaryAssignmentSource, that.judiciaryAssignmentSource);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, judiciary, judiciaryAssignmentSource);}

  public static class Builder {
    private UUID hearingId;

    private List<JudicialRole> judiciary;

    private JudiciaryAssignmentSource judiciaryAssignmentSource;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
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

    public Builder withValuesFrom(final JudiciaryAssignedToHearing judiciaryAssignedToHearing) {
      this.hearingId = judiciaryAssignedToHearing.getHearingId();
      this.judiciary = judiciaryAssignedToHearing.getJudiciary();
      this.judiciaryAssignmentSource = judiciaryAssignedToHearing.getJudiciaryAssignmentSource();
      return this;
    }

    public JudiciaryAssignedToHearing build() {
      return new uk.gov.justice.listing.events.JudiciaryAssignedToHearing(hearingId, judiciary, judiciaryAssignmentSource);
    }
  }
}
