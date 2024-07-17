package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "pmd:BeanMembersShouldSerialize"})
public class ProsecutionCaseDefendantOffenceIds implements Serializable {
    private static final long serialVersionUID = 1L;


    private final List<DefendantOffenceIds> defendants;

    private final UUID id;

    private final UUID groupId;

    private final Boolean isCivil;

    private final Boolean isGroupMaster;

    private final Boolean isGroupMember;

    public ProsecutionCaseDefendantOffenceIds(final List<DefendantOffenceIds> defendants, final UUID id, final UUID groupId,
                                              final Boolean isCivil, final Boolean isGroupMaster, final Boolean isGroupMember) {
        this.defendants = defendants;
        this.id = id;
        this.groupId = groupId;
        this.isCivil = isCivil;
        this.isGroupMaster = isGroupMaster;
        this.isGroupMember = isGroupMember;
    }

    public List<DefendantOffenceIds> getDefendants() {
        return defendants;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public Boolean getIsCivil() {
        return isCivil;
    }

    public Boolean getIsGroupMaster() {
        return isGroupMaster;
    }

    public Boolean getIsGroupMember() {
        return isGroupMember;
    }

    public static Builder prosecutionCaseDefendantOffenceIds() {
        return new ProsecutionCaseDefendantOffenceIds.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final ProsecutionCaseDefendantOffenceIds that = (ProsecutionCaseDefendantOffenceIds) obj;

        return java.util.Objects.equals(this.defendants, that.defendants) &&
                java.util.Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(defendants, id);
    }

    @Override
    public String toString() {
        return "ProsecutionCaseDefendantOffenceIds{" +
                "defendants='" + defendants + "'," +
                "id='" + id + "'" +
                "}";
    }

    public static class Builder {
        private List<DefendantOffenceIds> defendants;

        private UUID id;

        private UUID groupId;

        private Boolean isCivil;

        private Boolean isGroupMaster;

        private Boolean isGroupMember;

        public Builder withDefendants(final List<DefendantOffenceIds> defendants) {
            this.defendants = defendants;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withGroupId(final UUID groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder withIsCivil(final Boolean isCivil) {
            this.isCivil = isCivil;
            return this;
        }

        public Builder withIsGroupMaster(final Boolean isGroupMaster) {
            this.isGroupMaster = isGroupMaster;
            return this;
        }

        public Builder withIsGroupMember(final Boolean isGroupMember) {
            this.isGroupMember = isGroupMember;
            return this;
        }

        public ProsecutionCaseDefendantOffenceIds build() {
            return new ProsecutionCaseDefendantOffenceIds(defendants, id, groupId, isCivil,
                    isGroupMaster, isGroupMember);
        }
    }
}
