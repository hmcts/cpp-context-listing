package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class Type implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String description;

  private final UUID id;

  public Type(final String description, final UUID id) {
    this.description = description;
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public UUID getId() {
    return id;
  }

  public static Builder type() {
    return new Type.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final Type that = (Type) obj;

    return java.util.Objects.equals(this.description, that.description) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(description, id);}

  @Override
  public String toString() {
    return "Type{" +
    	"description='" + description + "'," +
    	"id='" + id + "'" +
    "}";
  }

  public static class Builder {
    private String description;

    private UUID id;

    public Builder withDescription(final String description) {
      this.description = description;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Type build() {
      return new Type(description, id);
    }
  }
}
