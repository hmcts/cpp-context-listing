package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CourtListDailyList implements Serializable {
  private static final long serialVersionUID = 5557103930060730330L;

  private final UUID courtCentreId;

  private final UUID courtListId;

  private final String dailyListDocument;

  private final PublishCourtListType publishCourtListType;

  @JsonCreator
  public CourtListDailyList(final UUID courtCentreId, final UUID courtListId, final String dailyListDocument, final PublishCourtListType publishCourtListType) {
    this.courtCentreId = courtCentreId;
    this.courtListId = courtListId;
    this.dailyListDocument = dailyListDocument;
    this.publishCourtListType = publishCourtListType;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtListId() {
    return courtListId;
  }

  public String getDailyListDocument() {
    return dailyListDocument;
  }

  public PublishCourtListType getPublishCourtListType() {
    return publishCourtListType;
  }

  public static Builder courtListDailyList() {
    return new uk.gov.justice.listing.courts.CourtListDailyList.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.CourtListDailyList that = (uk.gov.justice.listing.courts.CourtListDailyList) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtListId, that.courtListId) &&
    java.util.Objects.equals(this.dailyListDocument, that.dailyListDocument) &&
    java.util.Objects.equals(this.publishCourtListType, that.publishCourtListType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtListId, dailyListDocument, publishCourtListType);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtListId;

    private String dailyListDocument;

    private PublishCourtListType publishCourtListType;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtListId(final UUID courtListId) {
      this.courtListId = courtListId;
      return this;
    }

    public Builder withDailyListDocument(final String dailyListDocument) {
      this.dailyListDocument = dailyListDocument;
      return this;
    }

    public Builder withPublishCourtListType(final PublishCourtListType publishCourtListType) {
      this.publishCourtListType = publishCourtListType;
      return this;
    }

    public Builder withValuesFrom(final CourtListDailyList courtListDailyList) {
      this.courtCentreId = courtListDailyList.getCourtCentreId();
      this.courtListId = courtListDailyList.getCourtListId();
      this.dailyListDocument = courtListDailyList.getDailyListDocument();
      this.publishCourtListType = courtListDailyList.getPublishCourtListType();
      return this;
    }

    public CourtListDailyList build() {
      return new uk.gov.justice.listing.courts.CourtListDailyList(courtCentreId, courtListId, dailyListDocument, publishCourtListType);
    }
  }
}
