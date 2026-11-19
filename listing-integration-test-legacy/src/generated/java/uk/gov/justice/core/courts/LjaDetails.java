package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class LjaDetails implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String ljaCode;

  private final String ljaName;

  private final String welshLjaName;

  @JsonCreator
  public LjaDetails(final String ljaCode, final String ljaName, final String welshLjaName) {
    this.ljaCode = ljaCode;
    this.ljaName = ljaName;
    this.welshLjaName = welshLjaName;
  }

  public String getLjaCode() {
    return ljaCode;
  }

  public String getLjaName() {
    return ljaName;
  }

  public String getWelshLjaName() {
    return welshLjaName;
  }

  public static Builder ljaDetails() {
    return new uk.gov.justice.core.courts.LjaDetails.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.LjaDetails that = (uk.gov.justice.core.courts.LjaDetails) obj;

    return java.util.Objects.equals(this.ljaCode, that.ljaCode) &&
    java.util.Objects.equals(this.ljaName, that.ljaName) &&
    java.util.Objects.equals(this.welshLjaName, that.welshLjaName);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(ljaCode, ljaName, welshLjaName);}

  public static class Builder {
    private String ljaCode;

    private String ljaName;

    private String welshLjaName;

    public Builder withLjaCode(final String ljaCode) {
      this.ljaCode = ljaCode;
      return this;
    }

    public Builder withLjaName(final String ljaName) {
      this.ljaName = ljaName;
      return this;
    }

    public Builder withWelshLjaName(final String welshLjaName) {
      this.welshLjaName = welshLjaName;
      return this;
    }

    public Builder withValuesFrom(final LjaDetails ljaDetails) {
      this.ljaCode = ljaDetails.getLjaCode();
      this.ljaName = ljaDetails.getLjaName();
      this.welshLjaName = ljaDetails.getWelshLjaName();
      return this;
    }

    public LjaDetails build() {
      return new uk.gov.justice.core.courts.LjaDetails(ljaCode, ljaName, welshLjaName);
    }
  }
}
