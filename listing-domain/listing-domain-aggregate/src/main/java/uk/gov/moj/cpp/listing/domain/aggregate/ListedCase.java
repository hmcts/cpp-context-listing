package uk.gov.moj.cpp.listing.domain.aggregate;

import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.Prosecutor;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize", "squid:S2384"})
public class ListedCase implements Serializable {
    private final CaseIdentifier caseIdentifier;

    private final Prosecutor prosecutor;

    private final List<Defendant> defendants;

    private final List<CaseMarker> caseMarkers;

    private final UUID id;

    private final Boolean isCivil;
    private final UUID groupId;
    private final Boolean isGroupMember;
    private final Boolean isGroupMaster;

    public ListedCase(final CaseIdentifier caseIdentifier, final Prosecutor prosecutor, final List<Defendant> defendants, final List<CaseMarker> caseMarkers, final UUID id,
                        final Boolean isCivil, final UUID groupId, final Boolean isGroupMember, final Boolean isGroupMaster) {
        this.caseIdentifier = caseIdentifier;
        this.prosecutor = prosecutor;
        this.defendants = defendants;
        this.caseMarkers = caseMarkers;
        this.id = id;
        this.isCivil = isCivil;
        this.groupId = groupId;
        this.isGroupMember = isGroupMember;
        this.isGroupMaster = isGroupMaster;
    }

    public CaseIdentifier getCaseIdentifier() {
        return caseIdentifier;
    }

    public Prosecutor getProsecutor() { return prosecutor; }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public List<CaseMarker> getCaseMarkers() {
        return caseMarkers;
    }

    public UUID getId() {
        return id;
    }

    public Boolean getIsCivil() {
        return isCivil;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public Boolean getIsGroupMember() {
        return isGroupMember;
    }

    public Boolean getIsGroupMaster() {
        return isGroupMaster;
    }


    public static Builder listedCase() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final ListedCase that = (ListedCase) obj;

        return Objects.equals(this.caseIdentifier, that.caseIdentifier) &&
                Objects.equals(this.defendants, that.defendants) &&
                Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseIdentifier, defendants, id, isCivil, groupId, isGroupMember, isGroupMaster);
    }

    @Override
    public String toString() {
        return "ListedCase{" +
                "caseIdentifier='" + caseIdentifier + "'," +
                "prosecutor='" + prosecutor + "'," +
                "defendants='" + defendants + "'," +
                "id='" + id + "'" +
                "isCivil='" + isCivil + "'" +
                "groupId='" + groupId + "'" +
                "isGroupMember='" + isGroupMember + "'" +
                "isGroupMaster='" + isGroupMaster + "'" +
                "}";
    }

    @SuppressWarnings({"squid:S00107", "squid:S1067", "PMD.BeanMembersShouldSerialize", "squid:S2384", "squid:S2384"})
    public static final class Builder {
        private CaseIdentifier caseIdentifier;

        private Prosecutor prosecutor;

        private List<Defendant> defendants;

        private UUID id;
        private List<CaseMarker> caseMarkers;

        private Boolean isCivil;
        private UUID groupId;
        private Boolean isGroupMember;
        private Boolean isGroupMaster;

        public Builder withCaseIdentifier(final CaseIdentifier caseIdentifier) {
            this.caseIdentifier = caseIdentifier;
            return this;
        }

        public Builder withProsecutor(final Prosecutor val) {
            prosecutor = val;
            return this;
        }

        public Builder withDefendants(final List<Defendant> defendants) {
            this.defendants = defendants;
            return this;
        }

        public Builder withCaseMarkers(final List<CaseMarker> caseMarkers) {
            this.caseMarkers = caseMarkers;
            return this;
        }


        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withIsCivil(final Boolean isCivil) {
            this.isCivil = isCivil;
            return this;
        }

        public Builder withGroupId(final UUID groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder withIsGroupMember(final Boolean isGroupMember) {
            this.isGroupMember = isGroupMember;
            return this;
        }

        public Builder withIsGroupMaster(final Boolean isGroupMaster) {
            this.isGroupMaster = isGroupMaster;
            return this;
        }

        public Builder withValuesFrom(final ListedCase listedCase) {
            this.caseIdentifier = listedCase.getCaseIdentifier();
            this.prosecutor = listedCase.getProsecutor();
            this.defendants = listedCase.getDefendants();
            this.caseMarkers = listedCase.getCaseMarkers();
            this.id = listedCase.getId();
            this.isCivil = listedCase.getIsCivil();
            this.groupId = listedCase.getGroupId();
            this.isGroupMember = listedCase.getIsGroupMember();
            this.isGroupMaster = listedCase.getIsGroupMaster();
            return this;
        }

        public ListedCase build() {
            return new ListedCase(caseIdentifier, prosecutor, defendants, caseMarkers, id,
                    isCivil, groupId, isGroupMember, isGroupMaster);
        }
    }
}
