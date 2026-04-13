package uk.gov.moj.cpp.listing.domain;

import java.util.List;
import java.util.Objects;

public record HearingSlotSearchResponse(String hearingId,
                                        String courtScheduleId,
                                        String courtRoomId,
                                        String sessionStartTime,
                                        Integer duration,
                                        List<JudicialRole> judiciaries,
                                        Boolean isDraft) {
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        final HearingSlotSearchResponse that = (HearingSlotSearchResponse) o;
        return Objects.equals(this.hearingId(), that.hearingId()) && Objects.equals(this.courtScheduleId(),
                that.courtScheduleId()) && Objects.equals(this.courtRoomId(),
                that.courtRoomId()) && Objects.equals(this.sessionStartTime(),
                that.sessionStartTime()) && Objects.equals(this.duration(),
                that.duration()) && Objects.equals(this.judiciaries(),
                that.judiciaries()) && Objects.equals(this.isDraft(),
                that.isDraft());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hearingId(), this.courtScheduleId(), this.courtRoomId(), this.sessionStartTime(),
                this.duration(), this.judiciaries(), this.isDraft());
    }
}
