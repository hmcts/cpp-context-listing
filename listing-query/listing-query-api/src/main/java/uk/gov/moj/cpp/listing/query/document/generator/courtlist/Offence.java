package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.Objects;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class Offence {

    private String offenceTitle;
    private String welshOffenceTitle;
    private String offenceWording;

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public String getWelshOffenceTitle() {
        return welshOffenceTitle;
    }

    public static Offence.Builder offence() {
        return new Offence.Builder();
    }


    public static final class Builder {

        private String offenceTitle;
        private String welshOffenceTitle;
        private String offenceWording;

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

        public Offence build() {
            final Offence offence = new Offence();
            offence.offenceTitle = this.offenceTitle;
            offence.welshOffenceTitle = this.welshOffenceTitle;
            offence.offenceWording = this.offenceWording;
            return offence;
        }
    }

    @Override
    public String toString() {
        return "Offence{" +
                "offenceTitle='" + offenceTitle + '\'' +
                ", welshOffenceTitle='" + welshOffenceTitle + '\'' +
                ", offenceWording='" + offenceWording + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Offence)) return false;
        Offence offence = (Offence) o;
        return Objects.equals(offenceTitle, offence.offenceTitle) &&
                Objects.equals(welshOffenceTitle, offence.welshOffenceTitle) &&
                Objects.equals(offenceWording, offence.offenceWording);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offenceTitle, welshOffenceTitle, offenceWording);
    }
}
