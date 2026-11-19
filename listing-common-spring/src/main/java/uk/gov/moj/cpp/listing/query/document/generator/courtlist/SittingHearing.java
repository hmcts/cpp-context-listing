package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;
@SuppressWarnings({"squid:S2384", "squid:S1067"})
public class SittingHearing {
    private String hearingType;
    private String caseNumber;
    private HearingDay hearingDay;
    private List<Defendant> defendants;
    private List<Counsel> prosecutionCounsels;
    private List<Counsel> defenceCounsels;

    public String getHearingType() {
        return hearingType;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public HearingDay getHearingDay() {
        return hearingDay;
    }

    public List<Counsel> getProsecutionCounsels() {
        return prosecutionCounsels;
    }

    public List<Counsel> getDefenceCounsels() {
        return defenceCounsels;
    }

    public static Builder hearing() {
        return new SittingHearing.Builder();
    }


    public static final class Builder {
        private String hearingType;
        private String caseNumber;
        private HearingDay hearingDay;
        private List<Defendant> defendants;
        private List<Counsel> prosecutionCounsels;
        private List<Counsel> defenceCounsels;

        private Builder() {
        }

        public Builder withHearingType(final String hearingType) {
            this.hearingType = hearingType;
            return this;
        }

        public Builder withCaseNumber(final String caseNumber) {
            this.caseNumber = caseNumber;
            return this;
        }

        public Builder withHearingDay(final HearingDay hearingDay) {
            this.hearingDay = hearingDay;
            return this;
        }

        public Builder withDefendants(final List<Defendant> defendants) {
            this.defendants = defendants;
            return this;
        }

        public Builder withProsecutionCounsels(final List<Counsel> prosecutionCounsels) {
            this.prosecutionCounsels = prosecutionCounsels;
            return this;
        }

        public Builder withDefenceCounsels(final List<Counsel> defenceCounsels) {
            this.defenceCounsels = defenceCounsels;
            return this;
        }

        public SittingHearing build() {
            final SittingHearing hearing = new SittingHearing();
            hearing.hearingType = this.hearingType;
            hearing.caseNumber = this.caseNumber;
            hearing.defendants = this.defendants;
            hearing.prosecutionCounsels = this.prosecutionCounsels;
            hearing.defenceCounsels = this.defenceCounsels;
            hearing.hearingDay = hearingDay;
            return hearing;
        }
    }

    @Override
    public String toString() {
        return "Hearing{" +
                "hearingType='" + hearingType + '\'' +
                ", hearingDay='" + hearingDay + '\'' +
                ", caseNumber='" + caseNumber + '\'' +
                ", defendants=" + defendants +
                ", prosecutionCounsels=" + prosecutionCounsels +
                ", defenceCounsels=" + defenceCounsels +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SittingHearing)) {
            return false;
        }
        final SittingHearing hearing = (SittingHearing) o;
        return Objects.equals(hearingType, hearing.hearingType) &&
                Objects.equals(caseNumber, hearing.caseNumber) &&
                Objects.equals(prosecutionCounsels, hearing.prosecutionCounsels) &&
                Objects.equals(defenceCounsels, hearing.defenceCounsels) &&
                Objects.equals(hearingDay, hearing.hearingDay) &&
                Objects.equals(defendants, hearing.defendants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingType, caseNumber, prosecutionCounsels, defenceCounsels, hearingDay, defendants);
    }
}
