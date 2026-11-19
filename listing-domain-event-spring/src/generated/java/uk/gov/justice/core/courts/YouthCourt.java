package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class YouthCourt implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final Integer courtCode;

  private final String name;

  private final String welshName;

  private final UUID youthCourtId;

  @JsonCreator
  public YouthCourt(final Integer courtCode, final String name, final String welshName, final UUID youthCourtId) {
    this.courtCode = courtCode;
    this.name = name;
    this.welshName = welshName;
    this.youthCourtId = youthCourtId;
  }

  public Integer getCourtCode() {
    return courtCode;
  }

  public String getName() {
    return name;
  }

  public String getWelshName() {
    return welshName;
  }

  public UUID getYouthCourtId() {
    return youthCourtId;
  }

  public static Builder youthCourt() {
    return new uk.gov.justice.core.courts.YouthCourt.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.YouthCourt that = (uk.gov.justice.core.courts.YouthCourt) obj;

    return java.util.Objects.equals(this.courtCode, that.courtCode) &&
    java.util.Objects.equals(this.name, that.name) &&
    java.util.Objects.equals(this.welshName, that.welshName) &&
    java.util.Objects.equals(this.youthCourtId, that.youthCourtId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCode, name, welshName, youthCourtId);}

  public static class Builder {
    private Integer courtCode;

    private String name;

    private String welshName;

    private UUID youthCourtId;

    public Builder withCourtCode(final Integer courtCode) {
      this.courtCode = courtCode;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withWelshName(final String welshName) {
      this.welshName = welshName;
      return this;
    }

    public Builder withYouthCourtId(final UUID youthCourtId) {
      this.youthCourtId = youthCourtId;
      return this;
    }

    public Builder withValuesFrom(final YouthCourt youthCourt) {
      this.courtCode = youthCourt.getCourtCode();
      this.name = youthCourt.getName();
      this.welshName = youthCourt.getWelshName();
      this.youthCourtId = youthCourt.getYouthCourtId();
      return this;
    }

    public YouthCourt build() {
      return new uk.gov.justice.core.courts.YouthCourt(courtCode, name, welshName, youthCourtId);
    }
  }
}
