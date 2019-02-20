package uk.gov.moj.cpp.listing.domain;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class ListingDefinitions {
  private final Map<String, Object> additionalProperties;

  public ListingDefinitions(final Map<String, Object> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  public static Builder listingDefinitions() {
    return new ListingDefinitions.Builder();
  }

  @Override
  public String toString() {
    return "ListingDefinitions{" +
    	"additionalProperties='" + additionalProperties + "'" +
    "}";
  }

  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperty(final String name, final Object value) {
    additionalProperties.put(name, value);
  }

  public static class Builder {
    private final Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Builder withAdditionalProperty(final String name, final Object value) {
      additionalProperties.put(name, value);
      return this;
    }

    public ListingDefinitions build() {
      return new ListingDefinitions(additionalProperties);
    }
  }
}
