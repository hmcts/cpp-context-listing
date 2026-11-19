package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CaseSimpleOffences implements Serializable {

    private final UUID caseId;

    private final UUID defendantId;

    private final List<SimpleOffence> offences;

    public CaseSimpleOffences(final UUID caseId, final UUID defendantId, final List<SimpleOffence> offences) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.offences = new ArrayList<>(offences);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<SimpleOffence> getOffences() {
        return new ArrayList<>(offences);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseSimpleOffences that = (CaseSimpleOffences) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(defendantId, that.defendantId) &&
                Objects.equals(offences, that.offences);
    }

    @Override
    public int hashCode() {

        return Objects.hash(caseId, defendantId, offences);
    }

    public static CaseSimpleOffencesBuilder createCaseSimpleOffencesBuilder() {
        return new CaseSimpleOffencesBuilder();
    }

    public static class CaseSimpleOffencesBuilder {
        private UUID caseId;
        private UUID defendantId;
        private List<SimpleOffence> offences;

        private CaseSimpleOffencesBuilder() {
        }

        public CaseSimpleOffencesBuilder setCaseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public CaseSimpleOffencesBuilder setDefendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public CaseSimpleOffencesBuilder setOffences(List<SimpleOffence> offences) {
            this.offences = offences;
            return this;
        }

        public CaseSimpleOffences build() {
            return new CaseSimpleOffences(caseId, defendantId, offences);
        }
    }
}
