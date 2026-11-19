package uk.gov.justice.listing.courts.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.events.JudicialRole;

public class CourtLists implements Serializable {
  private static final long serialVersionUID = 3881700311939688169L;

  private final UUID courtRoomId;

  private final List<Hearings> hearings;

  private final List<JudicialRole> judiciary;

  private final LocalDate sittingDate;

  private final Boolean weekCommencing;

  @JsonCreator
  public CourtLists(final UUID courtRoomId, final List<Hearings> hearings, final List<JudicialRole> judiciary, final LocalDate sittingDate, final Boolean weekCommencing) {
    this.courtRoomId = courtRoomId;
    this.hearings = hearings;
    this.judiciary = judiciary;
    this.sittingDate = sittingDate;
    this.weekCommencing = weekCommencing;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public List<Hearings> getHearings() {
    return hearings;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public LocalDate getSittingDate() {
    return sittingDate;
  }

  public Boolean getWeekCommencing() {
    return weekCommencing;
  }

  public static Builder courtLists() {
    return new uk.gov.justice.listing.courts.events.CourtLists.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.events.CourtLists that = (uk.gov.justice.listing.courts.events.CourtLists) obj;

    return java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.hearings, that.hearings) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.sittingDate, that.sittingDate) &&
    java.util.Objects.equals(this.weekCommencing, that.weekCommencing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtRoomId, hearings, judiciary, sittingDate, weekCommencing);}

  public static class Builder {
    private UUID courtRoomId;

    private List<Hearings> hearings;

    private List<JudicialRole> judiciary;

    private LocalDate sittingDate;

    private Boolean weekCommencing;

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withHearings(final List<Hearings> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withJudiciary(final List<JudicialRole> judiciary) {
      this.judiciary = judiciary;
      return this;
    }

    public Builder withSittingDate(final LocalDate sittingDate) {
      this.sittingDate = sittingDate;
      return this;
    }

    public Builder withWeekCommencing(final Boolean weekCommencing) {
      this.weekCommencing = weekCommencing;
      return this;
    }

    public Builder withValuesFrom(final CourtLists courtLists) {
      this.courtRoomId = courtLists.getCourtRoomId();
      this.hearings = courtLists.getHearings();
      this.judiciary = courtLists.getJudiciary();
      this.sittingDate = courtLists.getSittingDate();
      this.weekCommencing = courtLists.getWeekCommencing();
      return this;
    }

    public CourtLists build() {
      return new uk.gov.justice.listing.courts.events.CourtLists(courtRoomId, hearings, judiciary, sittingDate, weekCommencing);
    }
  }
}
