package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class TypeOfList implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String description;

  private final UUID id;

  @JsonCreator
  public TypeOfList(final String description, final UUID id) {
    this.description = description;
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public UUID getId() {
    return id;
  }

  public static Builder typeOfList() {
    return new uk.gov.justice.listing.courts.TypeOfList.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.TypeOfList that = (uk.gov.justice.listing.courts.TypeOfList) obj;

    return java.util.Objects.equals(this.description, that.description) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(description, id);}

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

    public Builder withValuesFrom(final TypeOfList typeOfList) {
      this.description = typeOfList.getDescription();
      this.id = typeOfList.getId();
      return this;
    }

    public TypeOfList build() {
      return new uk.gov.justice.listing.courts.TypeOfList(description, id);
    }
  }
}
