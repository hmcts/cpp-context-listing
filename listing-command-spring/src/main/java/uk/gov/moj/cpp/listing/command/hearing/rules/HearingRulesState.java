package uk.gov.moj.cpp.listing.command.hearing.rules;

import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.Type;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The command-side state of a hearing that the viewstore does not hold with the meaning the rules
 * need (MBD refactor P2.3.2, ADR 015, implementation notes: the field → storage mapping). Stored in
 * {@code hearing_rules_state} (changeset 033): {@code duplicate}/{@code deleted} as columns (the
 * tombstone: the listener port still deletes the hearing row, so the P2.1 queries never see such a
 * hearing), the rest as jsonb ({@code HearingRulesStateCodec}).
 *
 * <p>Two kinds of field: state that exists only on the command side (offence membership with
 * {@code isNewOffence} and the post-allocation subset, {@code prosecutionCaseDefendants},
 * {@code applicationOffenceIds}, {@code updateSlot}, {@code unAllocatedListedCases},
 * {@code hasAdjournmentDate}, {@code confirmedCourtApplicationIds}), and fields the viewstore has
 * with different rules (the listener's copy vs the rules' copy: {@code type} after an extension,
 * {@code estimatedMinutes} (0/1 → 20 and cancelled days), {@code courtRoomId} and
 * {@code hearingDays} (kept on unallocation; cancelled days and sequence resets kept),
 * {@code nonDefaultDays} and {@code judiciary} (the reduced copies the rules compare with)). The
 * rules decide on these exactly as the JEE aggregate did; the queries keep reading the viewstore.
 */
public record HearingRulesState(
        UUID hearingId,
        boolean duplicate,
        boolean deleted,
        Type type,
        Integer estimatedMinutes,
        UUID courtRoomId,
        List<JudicialRole> judiciary,
        List<NonDefaultDay> nonDefaultDays,
        List<HearingDay> hearingDays,
        List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds,
        Map<UUID, List<UUID>> prosecutionCaseDefendants,
        Map<UUID, List<UUID>> applicationOffenceIds,
        List<UUID> confirmedCourtApplicationIds,
        boolean updateSlot,
        boolean hasAdjournmentDate,
        List<ListedCase> unAllocatedListedCases) {
}
