package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class VerdictType implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String category;

  private final String categoryType;

  private final String cjsVerdictCode;

  private final String description;

  private final UUID id;

  private final Integer sequence;

  private final String verdictCode;

  @JsonCreator
  public VerdictType(final String category, final String categoryType, final String cjsVerdictCode, final String description, final UUID id, final Integer sequence, final String verdictCode) {
    this.category = category;
    this.categoryType = categoryType;
    this.cjsVerdictCode = cjsVerdictCode;
    this.description = description;
    this.id = id;
    this.sequence = sequence;
    this.verdictCode = verdictCode;
  }

  public String getCategory() {
    return category;
  }

  public String getCategoryType() {
    return categoryType;
  }

  public String getCjsVerdictCode() {
    return cjsVerdictCode;
  }

  public String getDescription() {
    return description;
  }

  public UUID getId() {
    return id;
  }

  public Integer getSequence() {
    return sequence;
  }

  public String getVerdictCode() {
    return verdictCode;
  }

  public static Builder verdictType() {
    return new uk.gov.justice.core.courts.VerdictType.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.VerdictType that = (uk.gov.justice.core.courts.VerdictType) obj;

    return java.util.Objects.equals(this.category, that.category) &&
    java.util.Objects.equals(this.categoryType, that.categoryType) &&
    java.util.Objects.equals(this.cjsVerdictCode, that.cjsVerdictCode) &&
    java.util.Objects.equals(this.description, that.description) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.sequence, that.sequence) &&
    java.util.Objects.equals(this.verdictCode, that.verdictCode);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(category, categoryType, cjsVerdictCode, description, id, sequence, verdictCode);}

  public static class Builder {
    private String category;

    private String categoryType;

    private String cjsVerdictCode;

    private String description;

    private UUID id;

    private Integer sequence;

    private String verdictCode;

    public Builder withCategory(final String category) {
      this.category = category;
      return this;
    }

    public Builder withCategoryType(final String categoryType) {
      this.categoryType = categoryType;
      return this;
    }

    public Builder withCjsVerdictCode(final String cjsVerdictCode) {
      this.cjsVerdictCode = cjsVerdictCode;
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

    public Builder withSequence(final Integer sequence) {
      this.sequence = sequence;
      return this;
    }

    public Builder withVerdictCode(final String verdictCode) {
      this.verdictCode = verdictCode;
      return this;
    }

    public Builder withValuesFrom(final VerdictType verdictType) {
      this.category = verdictType.getCategory();
      this.categoryType = verdictType.getCategoryType();
      this.cjsVerdictCode = verdictType.getCjsVerdictCode();
      this.description = verdictType.getDescription();
      this.id = verdictType.getId();
      this.sequence = verdictType.getSequence();
      this.verdictCode = verdictType.getVerdictCode();
      return this;
    }

    public VerdictType build() {
      return new uk.gov.justice.core.courts.VerdictType(category, categoryType, cjsVerdictCode, description, id, sequence, verdictCode);
    }
  }
}
