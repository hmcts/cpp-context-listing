package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class BailStatus implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String code;

  private final CustodyTimeLimit custodyTimeLimit;

  private final String description;

  private final UUID id;

  @JsonCreator
  public BailStatus(final String code, final CustodyTimeLimit custodyTimeLimit, final String description, final UUID id) {
    this.code = code;
    this.custodyTimeLimit = custodyTimeLimit;
    this.description = description;
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public CustodyTimeLimit getCustodyTimeLimit() {
    return custodyTimeLimit;
  }

  public String getDescription() {
    return description;
  }

  public UUID getId() {
    return id;
  }

  public static Builder bailStatus() {
    return new uk.gov.justice.core.courts.BailStatus.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.BailStatus that = (uk.gov.justice.core.courts.BailStatus) obj;

    return java.util.Objects.equals(this.code, that.code) &&
    java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
    java.util.Objects.equals(this.description, that.description) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(code, custodyTimeLimit, description, id);}

  public static class Builder {
    private String code;

    private CustodyTimeLimit custodyTimeLimit;

    private String description;

    private UUID id;

    public Builder withCode(final String code) {
      this.code = code;
      return this;
    }

    public Builder withCustodyTimeLimit(final CustodyTimeLimit custodyTimeLimit) {
      this.custodyTimeLimit = custodyTimeLimit;
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

    public Builder withValuesFrom(final BailStatus bailStatus) {
      this.code = bailStatus.getCode();
      this.custodyTimeLimit = bailStatus.getCustodyTimeLimit();
      this.description = bailStatus.getDescription();
      this.id = bailStatus.getId();
      return this;
    }

    public BailStatus build() {
      return new uk.gov.justice.core.courts.BailStatus(code, custodyTimeLimit, description, id);
    }
  }
}
