package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
@SuppressWarnings({"squid:S1948"})
public class CaseOffences implements Serializable {

    private final UUID caseId;

    private final UUID defendantId;

    private final List<Offence> offences;

    public CaseOffences(final UUID caseId, final UUID defendantId, final List<Offence> offences) {
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

    public List<Offence> getOffences() {
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
        final CaseOffences that = (CaseOffences) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(defendantId, that.defendantId) &&
                Objects.equals(offences, that.offences);
    }

    @Override
    public int hashCode() {

        return Objects.hash(caseId, defendantId, offences);
    }

    public static CaseOffencesBuilder createCaseOffencesBuilder() {
        return new CaseOffencesBuilder();
    }

    public static class CaseOffencesBuilder {
        private UUID caseId;
        private UUID defendantId;
        private List<Offence> offences;

        private CaseOffencesBuilder() {
        }

        public CaseOffencesBuilder setCaseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public CaseOffencesBuilder setDefendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public CaseOffencesBuilder setOffences(List<Offence> offences) {
            this.offences = offences;
            return this;
        }

        public CaseOffences build() {
            return new CaseOffences(caseId, defendantId, offences);
        }
    }
}
