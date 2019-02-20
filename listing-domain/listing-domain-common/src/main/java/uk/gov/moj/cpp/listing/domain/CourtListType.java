package uk.gov.moj.cpp.listing.domain;

import java.util.Optional;

public enum CourtListType {
  ALPHABETICAL("ALPHABETICAL", "CourtList", "CourtListEnglishWelsh"),
  PUBLIC("PUBLIC", "PublicStandardList", "BilingualPublicCourtList"),
  STANDARD("STANDARD", "PublicStandardList", null);

  private final String value;
  private final String templateName;
  private final String welshTemplateName;

  CourtListType(String value, String templateName, String welshTemplateName) {
    this.value = value;
    this.templateName = templateName;
    this.welshTemplateName = welshTemplateName;
  }

  public String getTemplateName() {
    return templateName;
  }

  public String getWelshTemplateName() {
    return welshTemplateName;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<CourtListType> valueFor(final String value) {
    if(ALPHABETICAL.value.equalsIgnoreCase(value)) { return Optional.of(ALPHABETICAL); }
    if(PUBLIC.value.equalsIgnoreCase(value)) { return Optional.of(PUBLIC); }
    if(STANDARD.value.equalsIgnoreCase(value)) { return Optional.of(STANDARD); }
    return Optional.empty();
  }
}
