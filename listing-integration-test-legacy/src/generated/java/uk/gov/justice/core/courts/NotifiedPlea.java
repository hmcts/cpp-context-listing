package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class NotifiedPlea implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String notifiedPleaDate;

  private final NotifiedPleaValue notifiedPleaValue;

  private final UUID offenceId;

  @JsonCreator
  public NotifiedPlea(final String notifiedPleaDate, final NotifiedPleaValue notifiedPleaValue, final UUID offenceId) {
    this.notifiedPleaDate = notifiedPleaDate;
    this.notifiedPleaValue = notifiedPleaValue;
    this.offenceId = offenceId;
  }

  public String getNotifiedPleaDate() {
    return notifiedPleaDate;
  }

  public NotifiedPleaValue getNotifiedPleaValue() {
    return notifiedPleaValue;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public static Builder notifiedPlea() {
    return new uk.gov.justice.core.courts.NotifiedPlea.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.NotifiedPlea that = (uk.gov.justice.core.courts.NotifiedPlea) obj;

    return java.util.Objects.equals(this.notifiedPleaDate, that.notifiedPleaDate) &&
    java.util.Objects.equals(this.notifiedPleaValue, that.notifiedPleaValue) &&
    java.util.Objects.equals(this.offenceId, that.offenceId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(notifiedPleaDate, notifiedPleaValue, offenceId);}

  public static class Builder {
    private String notifiedPleaDate;

    private NotifiedPleaValue notifiedPleaValue;

    private UUID offenceId;

    public Builder withNotifiedPleaDate(final String notifiedPleaDate) {
      this.notifiedPleaDate = notifiedPleaDate;
      return this;
    }

    public Builder withNotifiedPleaValue(final NotifiedPleaValue notifiedPleaValue) {
      this.notifiedPleaValue = notifiedPleaValue;
      return this;
    }

    public Builder withOffenceId(final UUID offenceId) {
      this.offenceId = offenceId;
      return this;
    }

    public Builder withValuesFrom(final NotifiedPlea notifiedPlea) {
      this.notifiedPleaDate = notifiedPlea.getNotifiedPleaDate();
      this.notifiedPleaValue = notifiedPlea.getNotifiedPleaValue();
      this.offenceId = notifiedPlea.getOffenceId();
      return this;
    }

    public NotifiedPlea build() {
      return new uk.gov.justice.core.courts.NotifiedPlea(notifiedPleaDate, notifiedPleaValue, offenceId);
    }
  }
}
