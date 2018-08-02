package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@SuppressWarnings({"squid:S2160"}) // Super class uses unique ID to test for equality
@Entity
@Table(name = "offence")
public class OffenceWithDefendantId extends AbstractOffence {

    @Column(name = "mapped_defendant_id")
    private UUID mappedDefendantId;

    @Column(name = "mapped_hearing_id")
    private UUID mappedHearingId;

    @Embedded
    private StatementOfOffence statementOfOffence;

    public OffenceWithDefendantId() {
        //Required for JPA
    }

    public OffenceWithDefendantId(CompositeOffenceId id,
                                  String offenceCode,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  StatementOfOffence statementOfOffence,
                                  UUID mappedDefendantId,
                                  UUID mappedHearingId) {
        super(id, offenceCode, startDate, endDate);
        this.mappedDefendantId = mappedDefendantId;
        this.mappedHearingId = mappedHearingId;
        this.statementOfOffence = statementOfOffence;
    }

    public UUID getMappedDefendantId() {
        return mappedDefendantId;
    }

    public UUID getMappedHearingId() {
        return mappedHearingId;
    }

    public StatementOfOffence getStatementOfOffence() { return statementOfOffence; }
}

