package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class ReferralReason implements Serializable {
  private static final long serialVersionUID = -2013694293470485741L;

  private final UUID defendantId;

  private final String description;

  private final UUID id;

  private final String welshDescription;

  @JsonCreator
  public ReferralReason(final UUID defendantId, final String description, final UUID id, final String welshDescription) {
    this.defendantId = defendantId;
    this.description = description;
    this.id = id;
    this.welshDescription = welshDescription;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public String getDescription() {
    return description;
  }

  public UUID getId() {
    return id;
  }

  public String getWelshDescription() {
    return welshDescription;
  }

  public static Builder referralReason() {
    return new uk.gov.justice.core.courts.ReferralReason.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ReferralReason that = (uk.gov.justice.core.courts.ReferralReason) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.description, that.description) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.welshDescription, that.welshDescription);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, description, id, welshDescription);}

  public static class Builder {
    private UUID defendantId;

    private String description;

    private UUID id;

    private String welshDescription;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withDescription(final String description) {
      this.description = description;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withWelshDescription(final String welshDescription) {
      this.welshDescription = welshDescription;
      return this;
    }

    public Builder withValuesFrom(final ReferralReason referralReason) {
      this.defendantId = referralReason.getDefendantId();
      this.description = referralReason.getDescription();
      this.id = referralReason.getId();
      this.welshDescription = referralReason.getWelshDescription();
      return this;
    }

    public ReferralReason build() {
      return new uk.gov.justice.core.courts.ReferralReason(defendantId, description, id, welshDescription);
    }
  }
}
