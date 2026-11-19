package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import uk.gov.moj.cpp.listing.query.view.courtlist.pojo.SittingKeyByJudiciaries;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

@SuppressWarnings("squid:S2384")
public class Sitting {

    private List<String> judiciaryNames;
    private String sittingTime;
    private List<SittingHearing> hearings;
    private SittingKeyByJudiciaries sittingKey;


    public List<String> getJudiciaryNames() {
        return judiciaryNames;
    }

    public String getSittingTime() {
        return sittingTime;
    }

    public List<SittingHearing> getHearings() {
        return hearings;
    }

    @JsonIgnore
    public SittingKeyByJudiciaries getSittingKey() {
        return sittingKey;
    }

    public static Sitting.Builder sitting() {
        return new Sitting.Builder();
    }

    public static final class Builder {

        private List<String> judiciaryNames;
        private String sittingTime;
        private List<SittingHearing> hearings;
        private SittingKeyByJudiciaries sittingKey;


        private Builder() {
        }

        public Builder withJudiciaryNames(final List<String> judiciaryNames) {
            this.judiciaryNames = judiciaryNames;
            return this;
        }

        public Builder withHearings(final List<SittingHearing> hearings) {
            this.hearings = hearings;
            return this;
        }

        public Builder withSittingTime(final String sittingTime) {
            this.sittingTime = sittingTime;
            return this;
        }

        public Builder withSittingKey(final SittingKeyByJudiciaries sittingKey) {
            this.sittingKey = sittingKey;
            return this;
        }

        public Sitting build() {
            final Sitting sittings = new Sitting();
            sittings.sittingTime = this.sittingTime;
            sittings.hearings = this.hearings;
            sittings.judiciaryNames = this.judiciaryNames;
            sittings.sittingKey = sittingKey;
            return sittings;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Sitting)) {
            return false;
        }
        final Sitting that = (Sitting) o;
        return Objects.equals(sittingTime, that.sittingTime) &&
                Objects.equals(hearings, that.hearings) &&
                Objects.equals(judiciaryNames, that.judiciaryNames) &&
                Objects.equals(sittingKey, that.sittingKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sittingTime, hearings, judiciaryNames, sittingKey);
    }

    @Override
    public String toString() {
        return "Sitting{" +
                "sittingTime='" + sittingTime + '\'' +
                ", hearings='" + hearings + '\'' +
                ", judiciaryNames=" + judiciaryNames +
                '}';
    }
}
