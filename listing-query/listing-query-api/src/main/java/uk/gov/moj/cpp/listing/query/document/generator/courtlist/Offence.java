package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class Offence {
    private UUID id;
    private String offenceTitle;
    private String welshOffenceTitle;
    private String offenceWording;
    private Integer listingNumber;
    private String offenceCode;
    private String maxPenalty;
    private String alcoholReadingAmount;
    private String convictedOn;
    private String adjournedDate;
    private String adjournedHearingType;

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public String getWelshOffenceTitle() {
        return welshOffenceTitle;
    }

    public UUID getId() {
        return id;
    }

    public Integer getListingNumber() {
        return listingNumber;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getMaxPenalty() {
        return maxPenalty;
    }

    public String getAlcoholReadingAmount() {
        return alcoholReadingAmount;
    }

    public String getConvictedOn() {
        return convictedOn;
    }

    public String getAdjournedDate() {
        return adjournedDate;
    }

    public String getAdjournedHearingType() {
        return adjournedHearingType;
    }

    public static Offence.Builder offence() {
        return new Offence.Builder();
    }


    public static final class Builder {
        private UUID id;
        private String offenceTitle;
        private String welshOffenceTitle;
        private String offenceWording;
        private Integer listingNumber;
        private String offenceCode;
        private String maxPenalty;
        private String alcoholReadingAmount;
        private String convictedOn;
        private String adjournedDate;
        private String adjournedHearingType;

        private Builder() {
        }


        public Builder withOffenceTitle(String offenceTitle) {
            this.offenceTitle = offenceTitle;
            return this;
        }

        public Builder withWelshOffenceTitle(String welshOffenceTitle) {
            this.welshOffenceTitle = welshOffenceTitle;
            return this;
        }


        public Builder withOffenceWording(String offenceWording) {
            this.offenceWording = offenceWording;
            return this;
        }
        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }
        public Builder withListingNumber(Integer listingNumber) {
            this.listingNumber = listingNumber;
            return this;
        }

        public Builder withOffenceCode(String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public Builder withMaxPenalty(String maxPenalty) {
            this.maxPenalty = maxPenalty;
            return this;
        }

        public Builder withAlcoholReadingAmount(String alcoholReadingAmount) {
            this.alcoholReadingAmount = alcoholReadingAmount;
            return this;
        }

        public Builder withConvictedOn(String convictedOn) {
            this.convictedOn = convictedOn;
            return this;
        }

        public Builder withAdjournedDate(String adjournedDate) {
            this.adjournedDate = adjournedDate;
            return this;
        }

        public Builder withAdjournedHearingType(String adjournedHearingType) {
            this.adjournedHearingType = adjournedHearingType;
            return this;
        }

        public Offence build() {
            final Offence offence = new Offence();
            offence.offenceTitle = this.offenceTitle;
            offence.welshOffenceTitle = this.welshOffenceTitle;
            offence.offenceWording = this.offenceWording;
            offence.id = this.id;
            offence.listingNumber = this.listingNumber;
            offence.offenceCode = this.offenceCode;
            offence.maxPenalty = this.maxPenalty;
            offence.alcoholReadingAmount = this.alcoholReadingAmount;
            offence.convictedOn = this.convictedOn;
            offence.adjournedDate = this.adjournedDate;
            offence.adjournedHearingType = this.adjournedHearingType;
            return offence;
        }
    }

    @Override
    public String toString() {
        return "Offence{" +
                "offenceTitle='" + offenceTitle + '\'' +
                ", welshOffenceTitle='" + welshOffenceTitle + '\'' +
                ", offenceWording='" + offenceWording + '\'' +
                ", id='" + id + '\'' +
                ", listingNumber='" + listingNumber + '\'' +
                ", offenceCode='" + offenceCode + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Offence)) return false;
        Offence offence = (Offence) o;
        return Objects.equals(offenceTitle, offence.offenceTitle) &&
                Objects.equals(welshOffenceTitle, offence.welshOffenceTitle) &&
                Objects.equals(offenceWording, offence.offenceWording) &&
                Objects.equals(id, offence.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offenceTitle, welshOffenceTitle, offenceWording, id, listingNumber);
    }
}
