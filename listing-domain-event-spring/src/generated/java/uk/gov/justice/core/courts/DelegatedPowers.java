package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class DelegatedPowers implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String firstName;

  private final String lastName;

  private final UUID userId;

  @JsonCreator
  public DelegatedPowers(final String firstName, final String lastName, final UUID userId) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.userId = userId;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public UUID getUserId() {
    return userId;
  }

  public static Builder delegatedPowers() {
    return new uk.gov.justice.core.courts.DelegatedPowers.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DelegatedPowers that = (uk.gov.justice.core.courts.DelegatedPowers) obj;

    return java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.userId, that.userId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(firstName, lastName, userId);}

  public static class Builder {
    private String firstName;

    private String lastName;

    private UUID userId;

    public Builder withFirstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder withLastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder withUserId(final UUID userId) {
      this.userId = userId;
      return this;
    }

    public Builder withValuesFrom(final DelegatedPowers delegatedPowers) {
      this.firstName = delegatedPowers.getFirstName();
      this.lastName = delegatedPowers.getLastName();
      this.userId = delegatedPowers.getUserId();
      return this;
    }

    public DelegatedPowers build() {
      return new uk.gov.justice.core.courts.DelegatedPowers(firstName, lastName, userId);
    }
  }
}
