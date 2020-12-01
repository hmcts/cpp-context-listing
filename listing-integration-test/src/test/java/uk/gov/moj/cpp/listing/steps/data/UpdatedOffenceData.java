package uk.gov.moj.cpp.listing.steps.data;

import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.ReportingRestriction;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UpdatedOffenceData {

    private final UUID offenceId;
    private final UUID randomOffenceId;
    private final String offenceCode;
    private final String offenceWording;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String statementOfOffenceTitle;
    private final String statementOfOffenceTitleWelsh;
    private final String legislation;
    private final String legislationWelsh;
    private final Optional<LaaReferenceData> laaApplnReference;
    private List<ReportingRestrictionData> reportingRestriction;

    private UpdatedOffenceData(final UUID offenceId, final String offenceCode,
                               final LocalDate startDate, final LocalDate endDate, final String statementOfOffenceTitle,
                               final String statementOfOffenceTitleWelsh, final String offenceWording,
                               final String legislation, final String legislationWelsh, final Optional<LaaReferenceData> laaReferences,
                               final List<ReportingRestrictionData> reportingRestriction) {

        this.endDate = endDate;
        this.offenceCode = offenceCode;
        this.offenceId = offenceId;
        this.laaApplnReference = laaReferences;
        this.randomOffenceId = UUID.randomUUID();
        this.startDate = startDate;
        this.offenceWording = offenceWording;
        this.statementOfOffenceTitleWelsh = statementOfOffenceTitleWelsh;
        this.statementOfOffenceTitle = statementOfOffenceTitle;
        this.legislation = legislation;
        this.legislationWelsh = legislationWelsh;
        this.reportingRestriction = reportingRestriction;
    }

    public static final Builder builder() {
        return new Builder();
    }

    public static final UpdatedOffenceData updateOffenceData(OffenceData offenceData) {
        return builder()
                .withEndDate(offenceData.getEndDate().plusDays(10))
                .withOffenceCode(STRING.next())
                .withOffenceId(offenceData.getOffenceId())
                .withOffenceWording(STRING.next())
                .withStartDate(offenceData.getStartDate().plusDays(10))
                .withStatementOfOffenceTitle(STRING.next())
                .withStatementOfOffenceTitleWelsh(STRING.next())
                .withOffenceWording(STRING.next())
                .withLegislation(STRING.next())
                .withLegislationWelsh(STRING.next())
                .withLaaApplnReference((offenceData.getLaaApplnReference()))
                .withReportingRestriction(offenceData.getReportingRestrictionDataList().subList(1, 2))
                .build();
    }

    public UUID getOffenceId() { return offenceId; }

    public String getOffenceWording() {
        return offenceWording;
    }

    public Optional<LaaReferenceData> getLaaReferences() {
        return laaApplnReference;
    }

    public UUID getRandomOffenceId() { return randomOffenceId; }

    public String getOffenceCode() { return offenceCode; }

    public LocalDate getStartDate() { return startDate; }

    public LocalDate getEndDate() { return endDate; }

    public String getStatementOfOffenceTitle() { return statementOfOffenceTitle; }

    public String getStatementOfOffenceTitleWelsh() { return statementOfOffenceTitleWelsh; }

    public String getLegislation() { return legislation; }

    public String getLegislationWelsh() { return legislationWelsh; }

    public List<ReportingRestrictionData> getReportingRestriction() {
        return reportingRestriction;
    }

    private static class Builder {
        private UUID offenceId;
        private String offenceCode;
        private LocalDate startDate;
        private LocalDate endDate;
        private String statementOfOffenceTitle;
        private String statementOfOffenceTitleWelsh;
        private String offenceWording;
        private String legislation;
        private String legislationWelsh;
        private Optional<LaaReferenceData> laaApplnReference;
        private List<ReportingRestrictionData> reportingRestriction;

        public Builder withOffenceId(final UUID offenceId) {
            this.offenceId = offenceId;
            return this;
        }

        public Builder withOffenceCode(final String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public Builder withStartDate(final LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withEndDate(final LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder withStatementOfOffenceTitle(final String statementOfOffenceTitle) {
            this.statementOfOffenceTitle = statementOfOffenceTitle;
            return this;
        }

        public Builder withStatementOfOffenceTitleWelsh(final String statementOfOffenceTitleWelsh) {
            this.statementOfOffenceTitleWelsh = statementOfOffenceTitleWelsh;
            return this;
        }

        public Builder withOffenceWording(final String offenceWording) {
            this.offenceWording = offenceWording;
            return this;
        }

        public Builder withLegislation(final String legislation) {
            this.legislation = legislation;
            return this;
        }

        public Builder withLaaApplnReference(final Optional<LaaReferenceData> laaApplnReferences) {
            this.laaApplnReference = laaApplnReferences;
            return this;
        }

        public Builder withLegislationWelsh(final String legislationWelsh) {
            this.legislationWelsh = legislationWelsh;
            return this;
        }

        public Builder withReportingRestriction(final List<ReportingRestrictionData> reportingRestriction) {
            this.reportingRestriction = reportingRestriction;
            return this;
        }

        public UpdatedOffenceData build() {
            return new UpdatedOffenceData(offenceId, offenceCode, startDate, endDate,
                    statementOfOffenceTitle, statementOfOffenceTitleWelsh, offenceWording,
                    legislation, legislationWelsh, laaApplnReference, reportingRestriction);
        }
    }
}
