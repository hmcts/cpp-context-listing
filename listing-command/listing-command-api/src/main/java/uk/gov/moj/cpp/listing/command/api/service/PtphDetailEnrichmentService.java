package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.courts.PtphDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.HearingTypeFactory;
import uk.gov.moj.cpp.listing.domain.PtphDetail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies the seeding hearing's finalised tier / list type / key reason onto the next
 * hearings being listed from it. Tier and list type are a Crown Court PTPH concern, so the
 * hearing context is queried only when at least one of those next hearings is a Crown Court
 * trial. Reference data flags magistrates trial types too, which is why jurisdiction is
 * checked alongside {@code trialTypeFlag} rather than relying on the flag alone.
 */
@ApplicationScoped
public class PtphDetailEnrichmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PtphDetailEnrichmentService.class);

    @Inject
    private HearingTypeFactory hearingTypeFactory;

    @Inject
    private PtphDetailService ptphDetailService;

    /**
     * Scheduled flow — the hearing carrier is in-repo, so the values are stamped onto each
     * trial hearing itself.
     */
    public List<HearingListingNeeds> enrichWithPtphDetail(final List<HearingListingNeeds> hearings,
                                                          final SeedingHearing seedingHearing,
                                                          final JsonEnvelope envelope) {
        if (isNull(hearings) || hearings.isEmpty()) {
            return hearings;
        }

        final Set<String> trialHearingTypeIds = new HashSet<>();
        final Optional<PtphDetail> ptphDetail = resolveForTrials(
                hearings,
                (hearing, ids) -> isCrownTrial(hearing.getJurisdictionType(), hearing.getType(), ids),
                seedingHearing, envelope, trialHearingTypeIds);
        if (ptphDetail.isEmpty()) {
            return hearings;
        }

        final List<HearingListingNeeds> enriched = new ArrayList<>();
        hearings.forEach(hearing -> enriched.add(isCrownTrial(hearing.getJurisdictionType(), hearing.getType(), trialHearingTypeIds)
                ? stamp(hearing, ptphDetail.get())
                : hearing));
        return enriched;
    }

    /**
     * Unscheduled flow — the hearing carrier is the coredomain
     * {@code HearingUnscheduledListingNeeds}, which cannot be extended here, so the values
     * travel as a sibling list keyed by hearing id. Empty when nothing applies.
     */
    public List<PtphDetails> resolvePtphDetails(final List<HearingUnscheduledListingNeeds> hearings,
                                                final SeedingHearing seedingHearing,
                                                final JsonEnvelope envelope) {
        if (isNull(hearings) || hearings.isEmpty()) {
            return emptyList();
        }

        final Set<String> trialHearingTypeIds = new HashSet<>();
        final Optional<PtphDetail> ptphDetail = resolveForTrials(
                hearings,
                (hearing, ids) -> isCrownTrial(hearing.getJurisdictionType(), hearing.getType(), ids),
                seedingHearing, envelope, trialHearingTypeIds);
        if (ptphDetail.isEmpty()) {
            return emptyList();
        }

        final List<PtphDetails> resolved = new ArrayList<>();
        hearings.stream()
                .filter(hearing -> isCrownTrial(hearing.getJurisdictionType(), hearing.getType(), trialHearingTypeIds))
                .forEach(hearing -> {
                    LOGGER.info("Inheriting tier {} and list type {} onto unscheduled trial hearing {}",
                            ptphDetail.get().getTier(), ptphDetail.get().getListType(), hearing.getId());
                    resolved.add(PtphDetails.ptphDetails()
                            .withHearingId(hearing.getId())
                            .withTier(ptphDetail.get().getTier())
                            .withListType(ptphDetail.get().getListType())
                            .withKeyReason(ptphDetail.get().getKeyReason())
                            .build());
                });
        return resolved;
    }

    /**
     * The shared rule for both flows: a seeding hearing id must be present, at least one of
     * the next hearings must be a Crown Court trial — otherwise the hearing context is never
     * called — and the seeding record must be finalised.
     *
     * @param trialHearingTypeIds populated with the trial type ids when they are looked up,
     *                            so the caller can classify the hearings without a second
     *                            reference-data call
     */
    private <T> Optional<PtphDetail> resolveForTrials(final List<T> hearings,
                                                      final BiPredicate<T, Set<String>> isCrownTrial,
                                                      final SeedingHearing seedingHearing,
                                                      final JsonEnvelope envelope,
                                                      final Set<String> trialHearingTypeIds) {
        final UUID seedingHearingId = isNull(seedingHearing) ? null : seedingHearing.getSeedingHearingId();
        if (isNull(seedingHearingId)) {
            return Optional.empty();
        }

        trialHearingTypeIds.addAll(hearingTypeFactory.getTrialHearingTypeIds(envelope));
        if (hearings.stream().noneMatch(hearing -> isCrownTrial.test(hearing, trialHearingTypeIds))) {
            LOGGER.info("No Crown Court trial listed from seeding hearing {}; not querying the hearing context", seedingHearingId);
            return Optional.empty();
        }

        return ptphDetailService.getFinalisedPtphDetail(seedingHearingId, envelope);
    }

    private String typeIdOf(final HearingType type) {
        return nonNull(type) && nonNull(type.getId()) ? type.getId().toString() : null;
    }

    /**
     * Tier and list type are Crown Court PTPH concepts. Reference data's {@code trialTypeFlag}
     * is set on magistrates trial types as well, so the flag alone would inherit a tier onto a
     * magistrates trial — hence the jurisdiction check.
     */
    private boolean isCrownTrial(final JurisdictionType jurisdictionType,
                                 final HearingType type,
                                 final Set<String> trialHearingTypeIds) {
        final String hearingTypeId = typeIdOf(type);
        return CROWN.equals(jurisdictionType)
                && nonNull(hearingTypeId)
                && trialHearingTypeIds.contains(hearingTypeId);
    }

    /**
     * Always overwrites all three fields, so values already present on the inbound
     * command cannot masquerade as hearing-context data.
     */
    private HearingListingNeeds stamp(final HearingListingNeeds hearing, final PtphDetail ptphDetail) {
        LOGGER.info("Inheriting tier {} and list type {} onto trial hearing {}",
                ptphDetail.getTier(), ptphDetail.getListType(), hearing.getId());
        return hearingListingNeeds()
                .withValuesFrom(hearing)
                .withTier(ptphDetail.getTier())
                .withListType(ptphDetail.getListType())
                .withKeyReason(ptphDetail.getKeyReason())
                .build();
    }
}
