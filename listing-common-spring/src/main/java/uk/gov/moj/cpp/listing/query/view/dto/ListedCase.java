package uk.gov.moj.cpp.listing.query.view.dto;

import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.CaseMarker;
import uk.gov.moj.cpp.listing.domain.Defendant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ListedCase {
    private final CaseIdentifier caseIdentifier;

    private final List<Defendant> defendants;

    private final UUID id;

    private final List<CaseMarker> markers;

    private final Optional<Boolean> restrictFromCourtList;

    private final List<LinkedCase> linkedCases;

    public ListedCase(final CaseIdentifier caseIdentifier, final List<Defendant> defendants, final UUID id, final List<CaseMarker> markers, final Optional<Boolean> restrictFromCourtList, final List<LinkedCase> linkedCases) {
        this.caseIdentifier = caseIdentifier;
        this.defendants = defendants;
        this.id = id;
        this.markers = markers;
        this.restrictFromCourtList = restrictFromCourtList;
        this.linkedCases= linkedCases;
    }

    public CaseIdentifier getCaseIdentifier() {
        return caseIdentifier;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public List<CaseMarker> getCaseMarker() {
        return markers;
    }

    public UUID getId() {
        return id;
    }

    public Optional<Boolean> getRestrictFromCourtList() {
        return restrictFromCourtList;
    }

    public List<LinkedCase> getLinkedCases() {
        return linkedCases;
    }

    public static Builder listedCase() {
        return new uk.gov.moj.cpp.listing.query.view.dto.ListedCase.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj){
            return true;
        }
        if (obj == null || getClass() != obj.getClass()){
            return false;
        }
        final uk.gov.moj.cpp.listing.query.view.dto.ListedCase that = (uk.gov.moj.cpp.listing.query.view.dto.ListedCase) obj;

        return java.util.Objects.equals(this.caseIdentifier, that.caseIdentifier) &&
                java.util.Objects.equals(this.defendants, that.defendants) &&
                java.util.Objects.equals(this.id, that.id) &&
                java.util.Objects.equals(this.restrictFromCourtList, that.restrictFromCourtList);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(caseIdentifier, defendants, id, markers, restrictFromCourtList);
    }

    @Override
    public String toString() {
        return "ListedCase{" +
                "caseIdentifier='" + caseIdentifier + "'," +
                "defendants='" + defendants + "'," +
                "markers='" + markers + "'," +
                "id='" + id + "'," +
                "restrictFromCourtList='" + restrictFromCourtList + "'" +
                "}";
    }

    public static class Builder {
        private CaseIdentifier caseIdentifier;

        private List<Defendant> defendants;

        private UUID id;

        private List<CaseMarker> markers;

        private Optional<Boolean> restrictFromCourtList;

        private List<LinkedCase> linkedCases;

        public Builder withCaseIdentifier(final CaseIdentifier caseIdentifier) {
            this.caseIdentifier = caseIdentifier;
            return this;
        }

        public Builder withDefendants(final List<Defendant> defendants) {
            this.defendants = defendants;
            return this;
        }

        public Builder withMarker(final List<CaseMarker> markers) {
            this.markers = markers;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withRestrictFromCourtList(final Optional<Boolean> restrictFromCourtList) {
            this.restrictFromCourtList = restrictFromCourtList;
            return this;
        }

        public Builder withLinkedCases(final List<LinkedCase> linkedCases) {
            this.linkedCases = linkedCases;
            return this;
        }

        public ListedCase build() {
            return new uk.gov.moj.cpp.listing.query.view.dto.ListedCase(caseIdentifier, defendants, id, markers, restrictFromCourtList, linkedCases);
        }
    }
}