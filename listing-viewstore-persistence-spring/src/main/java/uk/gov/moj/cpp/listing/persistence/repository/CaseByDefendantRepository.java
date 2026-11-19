package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.query.CaseByDefendant;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * The DeltaSpike repository ran the {@code findCaseByDefendantWithCaseId} named native query; the
 * same SQL ({@link CaseByDefendant#QUERY}) is declared here as a native {@code @Query} (§11 pitfall 7).
 */
@Repository
public interface CaseByDefendantRepository extends JpaRepository<CaseByDefendant, UUID> {

    @Query(value = CaseByDefendant.QUERY, nativeQuery = true)
    List<CaseByDefendant> getCasesByDefendantAndHearingDate(@Param("caseIds") final List<UUID> caseIds,
                                                            @Param("defendantIds") final List<UUID> defendantIds,
                                                            @Param("hearingDate") final LocalDate hearingDate);
}
