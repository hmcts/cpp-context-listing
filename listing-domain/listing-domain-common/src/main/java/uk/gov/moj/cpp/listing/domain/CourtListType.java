package uk.gov.moj.cpp.listing.domain;

import static java.util.Arrays.stream;

import java.util.Optional;

public enum CourtListType {
  ALPHABETICAL("ALPHABETICAL", "CourtList", "CourtListEnglishWelsh"),
  PUBLIC("PUBLIC", "PublicStandardList", "BilingualPublicCourtList"),
  STANDARD("STANDARD", "PublicStandardList", null),
  BENCH("BENCH", "PublicStandardList", null),
  JUDGE("JUDGE", "JudgeList", null);


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

    return stream(CourtListType.values()).filter(x -> x.toString().equalsIgnoreCase(value)).findFirst();
  }
}
