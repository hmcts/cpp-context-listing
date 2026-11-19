package uk.gov.moj.cpp.listing.domain.referencedata;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CourtCentreWithRooms {
    private final UUID id;
    private final String oucode;
    private final String lja;
    private final String oucodeL1Code;
    private final String oucodeL1Name;
    private final String oucodeL2Code;
    private final String oucodeL2Name;
    private final String oucodeL3Code;
    private final String oucodeL3Name;
    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String address5;
    private final String postcode;
    private final boolean isWelsh;
    private final String oucodeL3WelshName;
    private final String welshAddress1;
    private final String welshAddress2;
    private final String welshAddress3;
    private final String welshAddress4;
    private final String welshAddress5;
    private final String defaultStartTime;
    private final String defaultDurationHrs;
    private final String courtLocationCode;
    private final String courtId;
    private final List<Courtroom> courtrooms;

    public CourtCentreWithRooms(final String address3, final UUID id, final String oucode, final String lja,final String oucodeL1Code, final String oucodeL1Name,final String oucodeL2Code, final String oucodeL2Name, final String oucodeL3Code, final String oucodeL3Name, final String address1, final String address2, final String address4, final String address5, final String postcode, final boolean isWelsh, final String oucodeL3WelshName, final String welshAddress1, final String welshAddress2, final String welshAddress3, final String welshAddress4, final String welshAddress5, final String defaultStartTime, final String defaultDurationHrs, final String courtLocationCode, final String courtId, final List<Courtroom> courtrooms) {
        this.address3 = address3;
        this.id = id;
        this.oucode = oucode;
        this.lja = lja;
        this.oucodeL1Code = oucodeL1Code;
        this.oucodeL1Name = oucodeL1Name;
        this.oucodeL2Code = oucodeL2Code;
        this.oucodeL2Name = oucodeL2Name;
        this.oucodeL3Code = oucodeL3Code;
        this.oucodeL3Name = oucodeL3Name;
        this.address1 = address1;
        this.address2 = address2;
        this.address4 = address4;
        this.address5 = address5;
        this.postcode = postcode;
        this.isWelsh = isWelsh;
        this.oucodeL3WelshName = oucodeL3WelshName;
        this.welshAddress1 = welshAddress1;
        this.welshAddress2 = welshAddress2;
        this.welshAddress3 = welshAddress3;
        this.welshAddress4 = welshAddress4;
        this.welshAddress5 = welshAddress5;
        this.defaultStartTime = defaultStartTime;
        this.defaultDurationHrs = defaultDurationHrs;
        this.courtLocationCode = courtLocationCode;
        this.courtId = courtId;
        this.courtrooms = courtrooms;
    }

    public UUID getId() {
        return id;
    }

    public String getOucode() {
        return oucode;
    }

    public String getLja() {
        return lja;
    }

    public String getOucodeL1Code() {
        return oucodeL1Code;
    }

    public String getOucodeL1Name() {
        return oucodeL1Name;
    }
    public String getOucodeL2Code() {
        return oucodeL2Code;
    }

    public String getOucodeL2Name() {
        return oucodeL2Name;
    }

    public String getOucodeL3Code() {
        return oucodeL3Code;
    }

    public String getOucodeL3Name() {
        return oucodeL3Name;
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

    public boolean isWelsh() {
        return isWelsh;
    }

    public String getOucodeL3WelshName() {
        return oucodeL3WelshName;
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

    public String getDefaultStartTime() {
        return defaultStartTime;
    }

    public String getDefaultDurationHrs() {
        return defaultDurationHrs;
    }

    public String getCourtLocationCode() {
        return courtLocationCode;
    }

    public String getCourtId() {
        return courtId;
    }

    public List<Courtroom> getCourtrooms() {
        return courtrooms;
    }


    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof final CourtCentreWithRooms that)) return false;
        return isWelsh() == that.isWelsh() &&
                Objects.equals(getId(), that.getId()) &&
                Objects.equals(getOucode(), that.getOucode()) &&
                Objects.equals(getLja(), that.getLja()) &&
                Objects.equals(getOucodeL1Code(), that.getOucodeL1Code()) &&
                Objects.equals(getOucodeL1Name(), that.getOucodeL1Name()) &&
                Objects.equals(getOucodeL2Code(), that.getOucodeL2Code()) &&
                Objects.equals(getOucodeL2Name(), that.getOucodeL2Name()) &&
                Objects.equals(getOucodeL3Code(), that.getOucodeL3Code()) &&
                Objects.equals(getOucodeL3Name(), that.getOucodeL3Name()) &&
                Objects.equals(getAddress1(), that.getAddress1()) &&
                Objects.equals(getAddress2(), that.getAddress2()) &&
                Objects.equals(getAddress3(), that.getAddress3()) &&
                Objects.equals(getAddress4(), that.getAddress4()) &&
                Objects.equals(getAddress5(), that.getAddress5()) &&
                Objects.equals(getPostcode(), that.getPostcode()) &&
                Objects.equals(getOucodeL3WelshName(), that.getOucodeL3WelshName()) &&
                Objects.equals(getWelshAddress1(), that.getWelshAddress1()) &&
                Objects.equals(getWelshAddress2(), that.getWelshAddress2()) &&
                Objects.equals(getWelshAddress3(), that.getWelshAddress3()) &&
                Objects.equals(getWelshAddress4(), that.getWelshAddress4()) &&
                Objects.equals(getWelshAddress5(), that.getWelshAddress5()) &&
                Objects.equals(getDefaultStartTime(), that.getDefaultStartTime()) &&
                Objects.equals(getDefaultDurationHrs(), that.getDefaultDurationHrs()) &&
                Objects.equals(getCourtLocationCode(), that.getCourtLocationCode()) &&
                Objects.equals(getCourtId(), that.getCourtId()) &&
                Objects.equals(getCourtrooms(), that.getCourtrooms());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getOucode(), getLja(),getOucodeL1Code(), getOucodeL1Name(),getOucodeL2Code(), getOucodeL2Name(), getOucodeL3Code(), getOucodeL3Name(), getAddress1(), getAddress2(), getAddress3(), getAddress4(), getAddress5(), getPostcode(), isWelsh(), getOucodeL3WelshName(), getWelshAddress1(), getWelshAddress2(), getWelshAddress3(), getWelshAddress4(), getWelshAddress5(), getDefaultStartTime(), getDefaultDurationHrs(), getCourtLocationCode(), getCourtId(), getCourtrooms());
    }

    @Override
    public String toString() {
        return "CourtCentreWithRooms{" +
                "id=" + id +
                ", oucode='" + oucode + '\'' +
                ", lja='" + lja + '\'' +
                ", oucodeL1Code='" + oucodeL1Code + '\'' +
                ", oucodeL1Name='" + oucodeL1Name + '\'' +
                ", oucodeL2Code='" + oucodeL2Code + '\'' +
                ", oucodeL2Name='" + oucodeL2Name + '\'' +
                ", oucodeL3Code='" + oucodeL3Code + '\'' +
                ", oucodeL3Name='" + oucodeL3Name + '\'' +
                ", address1='" + address1 + '\'' +
                ", address2='" + address2 + '\'' +
                ", address3='" + address3 + '\'' +
                ", address4='" + address4 + '\'' +
                ", address5='" + address5 + '\'' +
                ", postcode='" + postcode + '\'' +
                ", isWelsh=" + isWelsh +
                ", oucodeL3WelshName='" + oucodeL3WelshName + '\'' +
                ", welshAddress1='" + welshAddress1 + '\'' +
                ", welshAddress2='" + welshAddress2 + '\'' +
                ", welshAddress3='" + welshAddress3 + '\'' +
                ", welshAddress4='" + welshAddress4 + '\'' +
                ", welshAddress5='" + welshAddress5 + '\'' +
                ", defaultStartTime='" + defaultStartTime + '\'' +
                ", defaultDurationHrs='" + defaultDurationHrs + '\'' +
                ", courtLocationCode='" + courtLocationCode + '\'' +
                ", courtId='" + courtId + '\'' +
                ", courtrooms=" + courtrooms +
                '}';
    }

    public static Builder courtCentreWithRooms() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private String oucode;
        private String lja;
        private String oucodeL1Code;
        private String oucodeL1Name;
        private String oucodeL2Code;
        private String oucodeL2Name;
        private String oucodeL3Code;
        private String oucodeL3Name;
        private String address1;
        private String address2;
        private String address3;
        private String address4;
        private String address5;
        private String postcode;
        private boolean isWelsh;
        private String oucodeL3WelshName;
        private String welshAddress1;
        private String welshAddress2;
        private String welshAddress3;
        private String welshAddress4;
        private String welshAddress5;
        private String defaultStartTime;
        private String defaultDurationHrs;
        private String courtLocationCode;
        private String courtId;
        private List<Courtroom> courtrooms;

        private Builder() {
        }

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withOucode(String oucode) {
            this.oucode = oucode;
            return this;
        }

        public Builder withLja(String lja) {
            this.lja = lja;
            return this;
        }

        public Builder withOucodeL1Code(String oucodeL1Code) {
            this.oucodeL1Code = oucodeL1Code;
            return this;
        }

        public Builder withOucodeL1Name(String oucodeL1Name) {
            this.oucodeL1Name = oucodeL1Name;
            return this;
        }

        public Builder withOucodeL2Code(String oucodeL2Code) {
            this.oucodeL2Code = oucodeL2Code;
            return this;
        }

        public Builder withOucodeL2Name(String oucodeL2Name) {
            this.oucodeL2Name = oucodeL2Name;
            return this;
        }

        public Builder withOucodeL3Code(String oucodeL3Code) {
            this.oucodeL3Code = oucodeL3Code;
            return this;
        }

        public Builder withOucodeL3Name(String oucodeL3Name) {
            this.oucodeL3Name = oucodeL3Name;
            return this;
        }

        public Builder withAddress1(String address1) {
            this.address1 = address1;
            return this;
        }

        public Builder withAddress2(String address2) {
            this.address2 = address2;
            return this;
        }

        public Builder withAddress3(String address3) {
            this.address3 = address3;
            return this;
        }

        public Builder withAddress4(String address4) {
            this.address4 = address4;
            return this;
        }

        public Builder withAddress5(String address5) {
            this.address5 = address5;
            return this;
        }

        public Builder withPostcode(String postcode) {
            this.postcode = postcode;
            return this;
        }

        public Builder withIsWelsh(boolean isWelsh) {
            this.isWelsh = isWelsh;
            return this;
        }

        public Builder withOucodeL3WelshName(String oucodeL3WelshName) {
            this.oucodeL3WelshName = oucodeL3WelshName;
            return this;
        }

        public Builder withWelshAddress1(String welshAddress1) {
            this.welshAddress1 = welshAddress1;
            return this;
        }

        public Builder withWelshAddress2(String welshAddress2) {
            this.welshAddress2 = welshAddress2;
            return this;
        }

        public Builder withWelshAddress3(String welshAddress3) {
            this.welshAddress3 = welshAddress3;
            return this;
        }

        public Builder withWelshAddress4(String welshAddress4) {
            this.welshAddress4 = welshAddress4;
            return this;
        }

        public Builder withWelshAddress5(String welshAddress5) {
            this.welshAddress5 = welshAddress5;
            return this;
        }

        public Builder withDefaultStartTime(String defaultStartTime) {
            this.defaultStartTime = defaultStartTime;
            return this;
        }

        public Builder withDefaultDurationHrs(String defaultDurationHrs) {
            this.defaultDurationHrs = defaultDurationHrs;
            return this;
        }

        public Builder withCourtLocationCode(String courtLocationCode) {
            this.courtLocationCode = courtLocationCode;
            return this;
        }

        public Builder withCourtId(String courtId) {
            this.courtId = courtId;
            return this;
        }

        public Builder withCourtrooms(List<Courtroom> courtrooms) {
            this.courtrooms = courtrooms;
            return this;
        }

        public CourtCentreWithRooms build() {
            return new CourtCentreWithRooms(address3, id, oucode, lja, oucodeL1Code, oucodeL1Name, oucodeL2Code, oucodeL2Name, oucodeL3Code, oucodeL3Name, address1, address2, address4, address5, postcode, isWelsh, oucodeL3WelshName, welshAddress1, welshAddress2, welshAddress3, welshAddress4, welshAddress5, defaultStartTime, defaultDurationHrs, courtLocationCode, courtId, courtrooms);
        }
    }
}
