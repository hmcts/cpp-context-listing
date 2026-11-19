package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.persistence.entity.CaseIdentifier;
import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.entity.LinkedCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;
import uk.gov.moj.cpp.listing.persistence.entity.Prosecutor;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Builds viewstore rows for the repository tests (entities, saved through the repositories). */
final class ViewstoreFixtures {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private ViewstoreFixtures() {
    }

    static ObjectNode properties(final UUID hearingId, final boolean allocated, final String caseUrn) {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("id", hearingId.toString());
        node.put("allocated", allocated);
        node.putArray("judiciary").addObject().put("judicialId", "4f9d6a3e-6f3e-4d51-9d0d-4a8e2f1c0b11");
        final ObjectNode listedCase = node.putArray("listedCases").addObject();
        listedCase.put("id", randomUUID().toString());
        listedCase.putObject("caseIdentifier").put("caseReference", caseUrn);
        return node;
    }

    /** An allocated, crown, single-day hearing with one listed case (one defendant, one linked case). */
    static Hearing hearing(final UUID courtCentreId, final UUID courtRoomId, final LocalDate date, final boolean allocated,
                           final String caseUrn, final UUID caseId, final UUID masterDefendantId) {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.builder()
                .withId(hearingId)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withIsVacatedTrial(false)
                .withUnscheduled(false)
                .withTypeId(randomUUID())
                .withJurisdictionType("CROWN")
                .withStartDate(date)
                .withEndDate(date)
                .withAllocated(allocated)
                .withEstimatedMinutes(30)
                .withIsPossibleDisqualification(false)
                .withProperties(properties(hearingId, allocated, caseUrn))
                .build();
        hearing.setHearingDays(Set.of(new HearingDays(randomUUID(), 0,
                ZonedDateTime.of(date.atTime(11, 0), ZoneOffset.UTC),
                ZonedDateTime.of(date.atTime(10, 0), ZoneOffset.UTC),
                courtRoomId, date, courtCentreId, 60, hearing)));
        final CaseIdentifier caseIdentifier = new CaseIdentifier();
        caseIdentifier.setCaseReference(caseUrn);
        caseIdentifier.setAuthorityId(randomUUID());
        caseIdentifier.setAuthorityCode("TFL");
        final Prosecutor prosecutor = new Prosecutor();
        prosecutor.setProsecutorId(randomUUID());
        prosecutor.setProsecutorCode("TFL");
        final ListedCases listedCase = new ListedCases(randomUUID(), caseId, caseIdentifier, prosecutor, hearing, Set.of(), Set.of(), false);
        listedCase.setDefendants(Set.of(new Defendant(randomUUID(), randomUUID(), masterDefendantId, listedCase)));
        listedCase.setLinkedCases(Set.of(new LinkedCase(randomUUID(), randomUUID(), "LINKED" + caseUrn, listedCase)));
        hearing.setListedCases(Set.of(listedCase));
        return hearing;
    }

    static CourtApplications application(final Hearing hearing, final UUID applicationId, final UUID parentApplicationId, final String reference) {
        return new CourtApplications(randomUUID(), applicationId, hearing, "APPEAL", parentApplicationId, reference, "particulars", false);
    }
}
