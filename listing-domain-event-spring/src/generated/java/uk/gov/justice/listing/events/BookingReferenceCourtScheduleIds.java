package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class BookingReferenceCourtScheduleIds implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final UUID bookingId;

  private final List<UUID> courtScheduleIds;

  @JsonCreator
  public BookingReferenceCourtScheduleIds(final UUID bookingId, final List<UUID> courtScheduleIds) {
    this.bookingId = bookingId;
    this.courtScheduleIds = courtScheduleIds;
  }

  public UUID getBookingId() {
    return bookingId;
  }

  public List<UUID> getCourtScheduleIds() {
    return courtScheduleIds;
  }

  public static Builder bookingReferenceCourtScheduleIds() {
    return new uk.gov.justice.listing.events.BookingReferenceCourtScheduleIds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.BookingReferenceCourtScheduleIds that = (uk.gov.justice.listing.events.BookingReferenceCourtScheduleIds) obj;

    return java.util.Objects.equals(this.bookingId, that.bookingId) &&
    java.util.Objects.equals(this.courtScheduleIds, that.courtScheduleIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(bookingId, courtScheduleIds);}

  public static class Builder {
    private UUID bookingId;

    private List<UUID> courtScheduleIds;

    public Builder withBookingId(final UUID bookingId) {
      this.bookingId = bookingId;
      return this;
    }

    public Builder withCourtScheduleIds(final List<UUID> courtScheduleIds) {
      this.courtScheduleIds = courtScheduleIds;
      return this;
    }

    public Builder withValuesFrom(final BookingReferenceCourtScheduleIds bookingReferenceCourtScheduleIds) {
      this.bookingId = bookingReferenceCourtScheduleIds.getBookingId();
      this.courtScheduleIds = bookingReferenceCourtScheduleIds.getCourtScheduleIds();
      return this;
    }

    public BookingReferenceCourtScheduleIds build() {
      return new uk.gov.justice.listing.events.BookingReferenceCourtScheduleIds(bookingId, courtScheduleIds);
    }
  }
}
