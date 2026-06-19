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
@Event("listing.events.deleted-hearing-in-staging-hmi")
public class DeletedHearingInStagingHmi implements Serializable {
    private static final long serialVersionUID = 4306298685687288852L;

    private final String cancellationReasonCode;

    private final List<String> caseAndApplicationIds;

    private final UUID courtCentreId;

    private final UUID courtRoomId;

    private final UUID hearingId;

    @JsonCreator
    public DeletedHearingInStagingHmi(final String cancellationReasonCode, final List<String> caseAndApplicationIds, final UUID courtCentreId, final UUID courtRoomId, final UUID hearingId) {
        this.cancellationReasonCode = cancellationReasonCode;
        this.caseAndApplicationIds = caseAndApplicationIds;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
        this.hearingId = hearingId;
    }

    public String getCancellationReasonCode() {
        return cancellationReasonCode;
    }

    public List<String> getCaseAndApplicationIds() {
        return caseAndApplicationIds;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public static Builder deletedHearingInStagingHmi() {
        return new uk.gov.justice.listing.events.DeletedHearingInStagingHmi.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final uk.gov.justice.listing.events.DeletedHearingInStagingHmi that = (uk.gov.justice.listing.events.DeletedHearingInStagingHmi) obj;

        return java.util.Objects.equals(this.cancellationReasonCode, that.cancellationReasonCode) &&
               java.util.Objects.equals(this.caseAndApplicationIds, that.caseAndApplicationIds) &&
               java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
               java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
               java.util.Objects.equals(this.hearingId, that.hearingId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(cancellationReasonCode, caseAndApplicationIds, courtCentreId, courtRoomId, hearingId);}

    public static class Builder {
        private String cancellationReasonCode;

        private List<String> caseAndApplicationIds;

        private UUID courtCentreId;

        private UUID courtRoomId;

        private UUID hearingId;

        public Builder withCancellationReasonCode(final String cancellationReasonCode) {
            this.cancellationReasonCode = cancellationReasonCode;
            return this;
        }

        public Builder withCaseAndApplicationIds(final List<String> caseAndApplicationIds) {
            this.caseAndApplicationIds = caseAndApplicationIds;
            return this;
        }

        public Builder withCourtCentreId(final UUID courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public Builder withCourtRoomId(final UUID courtRoomId) {
            this.courtRoomId = courtRoomId;
            return this;
        }

        public Builder withHearingId(final UUID hearingId) {
            this.hearingId = hearingId;
            return this;
        }

        public Builder withValuesFrom(final DeletedHearingInStagingHmi deletedHearingInStagingHmi) {
            this.cancellationReasonCode = deletedHearingInStagingHmi.getCancellationReasonCode();
            this.caseAndApplicationIds = deletedHearingInStagingHmi.getCaseAndApplicationIds();
            this.courtCentreId = deletedHearingInStagingHmi.getCourtCentreId();
            this.courtRoomId = deletedHearingInStagingHmi.getCourtRoomId();
            this.hearingId = deletedHearingInStagingHmi.getHearingId();
            return this;
        }

        public DeletedHearingInStagingHmi build() {
            return new uk.gov.justice.listing.events.DeletedHearingInStagingHmi(cancellationReasonCode, caseAndApplicationIds, courtCentreId, courtRoomId, hearingId);
        }
    }
}
