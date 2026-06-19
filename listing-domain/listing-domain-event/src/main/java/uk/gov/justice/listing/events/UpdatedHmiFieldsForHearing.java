package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Deprecated
@Event("listing.events.updated-hmi-fields-for-hearing")
public class UpdatedHmiFieldsForHearing implements Serializable {
    private static final long serialVersionUID = -7369511719734887602L;

    private final String bookingType;

    private final UUID hearingId;

    private final String priority;

    private final List<String> specialRequirements;

    @JsonCreator
    public UpdatedHmiFieldsForHearing(final String bookingType, final UUID hearingId, final String priority, final List<String> specialRequirements) {
        this.bookingType = bookingType;
        this.hearingId = hearingId;
        this.priority = priority;
        this.specialRequirements = specialRequirements;
    }

    public String getBookingType() {
        return bookingType;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public String getPriority() {
        return priority;
    }

    public List<String> getSpecialRequirements() {
        return specialRequirements;
    }

    public static Builder updatedHmiFieldsForHearing() {
        return new uk.gov.justice.listing.events.UpdatedHmiFieldsForHearing.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final uk.gov.justice.listing.events.UpdatedHmiFieldsForHearing that = (uk.gov.justice.listing.events.UpdatedHmiFieldsForHearing) obj;

        return java.util.Objects.equals(this.bookingType, that.bookingType) &&
               java.util.Objects.equals(this.hearingId, that.hearingId) &&
               java.util.Objects.equals(this.priority, that.priority) &&
               java.util.Objects.equals(this.specialRequirements, that.specialRequirements);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(bookingType, hearingId, priority, specialRequirements);}

    public static class Builder {
        private String bookingType;

        private UUID hearingId;

        private String priority;

        private List<String> specialRequirements;

        public Builder withBookingType(final String bookingType) {
            this.bookingType = bookingType;
            return this;
        }

        public Builder withHearingId(final UUID hearingId) {
            this.hearingId = hearingId;
            return this;
        }

        public Builder withPriority(final String priority) {
            this.priority = priority;
            return this;
        }

        public Builder withSpecialRequirements(final List<String> specialRequirements) {
            this.specialRequirements = specialRequirements;
            return this;
        }

        public Builder withValuesFrom(final UpdatedHmiFieldsForHearing updatedHmiFieldsForHearing) {
            this.bookingType = updatedHmiFieldsForHearing.getBookingType();
            this.hearingId = updatedHmiFieldsForHearing.getHearingId();
            this.priority = updatedHmiFieldsForHearing.getPriority();
            this.specialRequirements = updatedHmiFieldsForHearing.getSpecialRequirements();
            return this;
        }

        public UpdatedHmiFieldsForHearing build() {
            return new uk.gov.justice.listing.events.UpdatedHmiFieldsForHearing(bookingType, hearingId, priority, specialRequirements);
        }
    }
}
