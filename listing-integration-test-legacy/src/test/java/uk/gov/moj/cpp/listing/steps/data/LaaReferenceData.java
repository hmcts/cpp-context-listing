package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class LaaReferenceData {
    private final String applicationReference;

    private final Optional<LocalDate> effectiveEndDate;

    private final Optional<LocalDate> effectiveStartDate;

    private final String statusCode;

    private final LocalDate statusDate;

    private final String statusDescription;

    private final UUID statusId;

    public LaaReferenceData(final String applicationReference, final Optional<LocalDate> effectiveEndDate, final Optional<LocalDate> effectiveStartDate, final String statusCode, final LocalDate statusDate, final String statusDescription, final UUID statusId) {
        this.applicationReference = applicationReference;
        this.effectiveEndDate = effectiveEndDate;
        this.effectiveStartDate = effectiveStartDate;
        this.statusCode = statusCode;
        this.statusDate = statusDate;
        this.statusDescription = statusDescription;
        this.statusId = statusId;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public Optional<LocalDate> getEffectiveEndDate() {
        return effectiveEndDate;
    }

    public Optional<LocalDate> getEffectiveStartDate() {
        return effectiveStartDate;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public LocalDate getStatusDate() {
        return statusDate;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public UUID getStatusId() {
        return statusId;
    }

}
