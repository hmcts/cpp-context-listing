package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Type implements Serializable {
  private static final long serialVersionUID = 7964617314504523615L;

  private final String description;

  private final UUID id;

  private final String welshDescription;

  @JsonCreator
  public Type(final String description, final UUID id, final String welshDescription) {
    this.description = description;
    this.id = id;
    this.welshDescription = welshDescription;
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

  public static Builder type() {
    return new uk.gov.justice.listing.events.Type.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.Type that = (uk.gov.justice.listing.events.Type) obj;

    return java.util.Objects.equals(this.description, that.description) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.welshDescription, that.welshDescription);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(description, id, welshDescription);}

  public static class Builder {
    private String description;

    private UUID id;

    private String welshDescription;

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

    public Builder withValuesFrom(final Type type) {
      this.description = type.getDescription();
      this.id = type.getId();
      this.welshDescription = type.getWelshDescription();
      return this;
    }

    public Type build() {
      return new uk.gov.justice.listing.events.Type(description, id, welshDescription);
    }
  }
}
