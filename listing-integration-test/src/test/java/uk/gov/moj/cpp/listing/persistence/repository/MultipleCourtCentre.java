package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;

import java.time.LocalDate;
import java.util.UUID;

public class MultipleCourtCentre {
    private final PersistenceTestsIT persistenceTestsIT;
    public UUID courtCentreId;
    public LocalDate startDate;
    public LocalDate endDate;
    public UUID otherCourtCentreId;

    public MultipleCourtCentre(PersistenceTestsIT persistenceTestsIT) {
        this.persistenceTestsIT = persistenceTestsIT;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public UUID getOtherCourtCentreId() {
        return otherCourtCentreId;
    }

    public MultipleCourtCentre invoke() {
        courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();

        otherCourtCentreId = randomUUID();

        startDate = LocalDate.now();
        endDate = LocalDate.now().plusDays(1);

        persistenceTestsIT.givenHearingsWithMultipleCourtCentres(courtCentreId, courtRoomId, otherCourtCentreId, startDate, endDate);
        return this;
    }
}
