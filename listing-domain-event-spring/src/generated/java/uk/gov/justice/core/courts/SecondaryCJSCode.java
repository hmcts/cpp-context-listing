package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class SecondaryCJSCode implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String cjsCode;

  private final String text;

  @JsonCreator
  public SecondaryCJSCode(final String cjsCode, final String text) {
    this.cjsCode = cjsCode;
    this.text = text;
  }

  public String getCjsCode() {
    return cjsCode;
  }

  public String getText() {
    return text;
  }

  public static Builder secondaryCJSCode() {
    return new uk.gov.justice.core.courts.SecondaryCJSCode.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.SecondaryCJSCode that = (uk.gov.justice.core.courts.SecondaryCJSCode) obj;

    return java.util.Objects.equals(this.cjsCode, that.cjsCode) &&
    java.util.Objects.equals(this.text, that.text);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(cjsCode, text);}

  public static class Builder {
    private String cjsCode;

    private String text;

    public Builder withCjsCode(final String cjsCode) {
      this.cjsCode = cjsCode;
      return this;
    }

    public Builder withText(final String text) {
      this.text = text;
      return this;
    }

    public Builder withValuesFrom(final SecondaryCJSCode secondaryCJSCode) {
      this.cjsCode = secondaryCJSCode.getCjsCode();
      this.text = secondaryCJSCode.getText();
      return this;
    }

    public SecondaryCJSCode build() {
      return new uk.gov.justice.core.courts.SecondaryCJSCode(cjsCode, text);
    }
  }
}
