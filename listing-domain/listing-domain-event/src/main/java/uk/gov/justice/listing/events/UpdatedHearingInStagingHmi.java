package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.domain.annotation.Event;

@Deprecated
@Event("listing.events.updated-hearing-in-staging-hmi")
public class UpdatedHearingInStagingHmi implements Serializable {
    private static final long serialVersionUID = 1248325696608131304L;

    private final Hearing hearing;

    @JsonCreator
    public UpdatedHearingInStagingHmi(final Hearing hearing) {
        this.hearing = hearing;
    }

    public Hearing getHearing() {
        return hearing;
    }

    public static Builder updatedHearingInStagingHmi() {
        return new uk.gov.justice.listing.events.UpdatedHearingInStagingHmi.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final uk.gov.justice.listing.events.UpdatedHearingInStagingHmi that = (uk.gov.justice.listing.events.UpdatedHearingInStagingHmi) obj;

        return java.util.Objects.equals(this.hearing, that.hearing);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(hearing);}

    public static class Builder {
        private Hearing hearing;

        public Builder withHearing(final Hearing hearing) {
            this.hearing = hearing;
            return this;
        }

        public Builder withValuesFrom(final UpdatedHearingInStagingHmi updatedHearingInStagingHmi) {
            this.hearing = updatedHearingInStagingHmi.getHearing();
            return this;
        }

        public UpdatedHearingInStagingHmi build() {
            return new uk.gov.justice.listing.events.UpdatedHearingInStagingHmi(hearing);
        }
    }
}
