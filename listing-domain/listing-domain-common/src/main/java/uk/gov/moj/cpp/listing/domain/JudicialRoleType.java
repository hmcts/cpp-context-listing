package uk.gov.moj.cpp.listing.domain;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class JudicialRoleType {
    private final Optional<UUID> judicialRoleTypeId;

    private final String judiciaryType;

    public JudicialRoleType(final Optional<UUID> judicialRoleTypeId, final String judiciaryType) {
        this.judicialRoleTypeId = judicialRoleTypeId;
        this.judiciaryType = judiciaryType;
    }

    public Optional<UUID> getJudicialRoleTypeId() {
        return judicialRoleTypeId;
    }

    public String getJudiciaryType() {
        return judiciaryType;
    }

    public static Builder judicialRoleType() {
        return new JudicialRoleType.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()){
            return false;
        }
        final JudicialRoleType that = (JudicialRoleType) obj;

        return java.util.Objects.equals(this.judicialRoleTypeId, that.judicialRoleTypeId) &&
                java.util.Objects.equals(this.judiciaryType, that.judiciaryType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(judicialRoleTypeId, judiciaryType);}

    @Override
    public String toString() {
        return "JudicialRoleType{" +
                "judicialRoleTypeId='" + judicialRoleTypeId + "'," +
                "judiciaryType='" + judiciaryType + "'" +
                "}";
    }

    public static class Builder {
        private Optional<UUID> judicialRoleTypeId;

        private String judiciaryType;

        public Builder withJudicialRoleTypeId(final Optional<UUID> judicialRoleTypeId) {
            this.judicialRoleTypeId = judicialRoleTypeId;
            return this;
        }

        public Builder withJudiciaryType(final String judiciaryType) {
            this.judiciaryType = judiciaryType;
            return this;
        }

        public JudicialRoleType build() {
            return new JudicialRoleType(judicialRoleTypeId, judiciaryType);
        }
    }
}

