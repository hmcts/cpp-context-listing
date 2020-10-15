package uk.gov.moj.cpp.listing.query.api.courtcentre.details;


import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067"})
public class CourtCentreDetails {

    private final UUID id;
    private final String courtCentreName;
    private final String welshCourtCentreName;

    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String address5;
    private final String postcode;

    private final String welshAddress1;
    private final String welshAddress2;
    private final String welshAddress3;
    private final String welshAddress4;
    private final String welshAddress5;
    private final Boolean welsh;
    private final String ouCodeL3Name;
    private final Map<UUID, CourtRoomDetails> courtRooms;


    public CourtCentreDetails(UUID id, String courtCentreName, String welshCourtCentreName, String address1, String address2, String address3, String address4, String address5, String postcode,
                              String welshAddress1, String welshAddress2, String welshAddress3, String welshAddress4, String welshAddress5, Map<UUID, CourtRoomDetails> courtRooms, final Boolean welsh, final String ouCodeL3Name) {
        this.id = id;
        this.courtCentreName = courtCentreName;
        this.welshCourtCentreName = welshCourtCentreName;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.address5 = address5;
        this.postcode = postcode;
        this.welshAddress1 = welshAddress1;
        this.welshAddress2 = welshAddress2;
        this.welshAddress3 = welshAddress3;
        this.welshAddress4 = welshAddress4;
        this.welshAddress5 = welshAddress5;
        this.courtRooms = courtRooms;
        this.welsh = welsh;
        this.ouCodeL3Name = ouCodeL3Name;
    }

    public UUID getId() {
        return id;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public String getWelshCourtCentreName() {
        return welshCourtCentreName;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getAddress3() {
        return address3;
    }

    public String getAddress4() {
        return address4;
    }

    public String getAddress5() {
        return address5;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getWelshAddress1() {
        return welshAddress1;
    }

    public String getWelshAddress2() {
        return welshAddress2;
    }

    public String getWelshAddress3() {
        return welshAddress3;
    }

    public String getWelshAddress4() {
        return welshAddress4;
    }

    public String getWelshAddress5() {
        return welshAddress5;
    }

    public Map<UUID, CourtRoomDetails> getCourtRooms() {
        return courtRooms;
    }

    public Boolean isWelsh() {
        return welsh;
    }

    public String getOuCodeL3Name() {
        return ouCodeL3Name;
    }

    public static Builder courtCentreDetails() {
        return new CourtCentreDetails.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final CourtCentreDetails that = (CourtCentreDetails) obj;

        return Objects.equals(this.courtCentreName, that.courtCentreName) &&
                Objects.equals(this.welshCourtCentreName, that.welshCourtCentreName) &&
                Objects.equals(this.address1, that.address1) &&
                Objects.equals(this.address2, that.address2) &&
                Objects.equals(this.address3, that.address3) &&
                Objects.equals(this.address4, that.address4) &&
                Objects.equals(this.address5, that.address5) &&
                Objects.equals(this.postcode, that.postcode) &&
                Objects.equals(this.welshAddress1, that.welshAddress1) &&
                Objects.equals(this.welshAddress2, that.welshAddress2) &&
                Objects.equals(this.welshAddress3, that.welshAddress3) &&
                Objects.equals(this.welshAddress4, that.welshAddress4) &&
                Objects.equals(this.welshAddress5, that.welshAddress5) &&
                Objects.equals(this.courtRooms, that.courtRooms) &&
                Objects.equals(this.welsh, that.welsh) &&
                Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, courtCentreName, welshCourtCentreName, address1, address2, address3, address4, address5,
                postcode, welshAddress1, welshAddress2, welshAddress3, welshAddress4, welshAddress5, courtRooms, welsh);
    }


    public static class Builder {
        private UUID id;
        private String courtCentreName;
        private String welshCourtCentreName;

        private String address1;
        private String address2;
        private String address3;
        private String address4;
        private String address5;
        private String postcode;

        private String welshAddress1;
        private String welshAddress2;
        private String welshAddress3;
        private String welshAddress4;
        private String welshAddress5;
        private String ouCodeL3Name;



        private Map<UUID, CourtRoomDetails> courtRooms;
        private Boolean welsh;

        public Builder withCourtCentreName(final String courtCentreName) {
            this.courtCentreName = courtCentreName;
            return this;
        }

        public Builder withWelshCourtCentreName(final String welshCourtCentreName) {
            this.welshCourtCentreName = welshCourtCentreName;
            return this;
        }

        public Builder withAddress1(final String address1) {
            this.address1 = address1;
            return this;
        }

        public Builder withAddress2(final String address2) {
            this.address2 = address2;
            return this;
        }

        public Builder withAddress3(final String address3) {
            this.address3 = address3;
            return this;
        }

        public Builder withAddress4(final String address4) {
            this.address4 = address4;
            return this;
        }

        public Builder withAddress5(final String address5) {
            this.address5 = address5;
            return this;
        }

        public Builder withPostcode(final String postcode) {
            this.postcode = postcode;
            return this;
        }

        public Builder withWelshAddress1(final String welshAddress1) {
            this.welshAddress1 = welshAddress1;
            return this;
        }

        public Builder withWelshAddress2(final String welshAddress2) {
            this.welshAddress2 = welshAddress2;
            return this;
        }

        public Builder withWelshAddress3(final String welshAddress3) {
            this.welshAddress3 = welshAddress3;
            return this;
        }

        public Builder withWelshAddress4(final String welshAddress4) {
            this.welshAddress4 = welshAddress4;
            return this;
        }

        public Builder withWelshAddress5(final String welshAddress5) {
            this.welshAddress5 = welshAddress5;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }


        public Builder withCourtRooms(final Map<UUID, CourtRoomDetails> courtRooms){
            this.courtRooms = courtRooms;
            return this;
        }
        public Builder withWelsh(final Boolean welsh){
            this.welsh= welsh;
            return this;
        }

        public Builder withOuCodeL3Name(final String ouCodeL3Name) {
            this.ouCodeL3Name = ouCodeL3Name;
            return this;
        }

        public CourtCentreDetails build() {
            return new CourtCentreDetails(id, courtCentreName, welshCourtCentreName, address1, address2, address3, address4, address5,
                    postcode, welshAddress1, welshAddress2, welshAddress3, welshAddress4, welshAddress5, courtRooms, welsh, ouCodeL3Name);
        }

    }
}

