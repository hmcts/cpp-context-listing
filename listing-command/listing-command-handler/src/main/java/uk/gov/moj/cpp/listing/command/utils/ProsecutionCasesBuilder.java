package uk.gov.moj.cpp.listing.command.utils;

import uk.gov.justice.listing.courts.ProsecutionCasesToRemove;
import uk.gov.justice.listing.events.Defendants;
import uk.gov.justice.listing.events.Offences;
import uk.gov.justice.listing.events.ProsecutionCases;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutionCasesBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCasesBuilder.class);

    public List<ProsecutionCases> buildEventProsecutionCase(final Map<UUID, Map<UUID, List<UUID>>> requestCaseMap) {

        final List<ProsecutionCases> prosecutionCasesList = new ArrayList<>();
        // Map<case, Map<defendant, List<offence>>>
        requestCaseMap.forEach((k, v) -> {
            final ProsecutionCases.Builder caseBuilder = ProsecutionCases.prosecutionCases().withCaseId(k);
            final List<Defendants> defendantsList = new ArrayList<>();
            v.forEach((d, o) -> {
                final Defendants.Builder defendantBuilder = Defendants.defendants().withDefendantId(d);
                final List<Offences> offencesList = new ArrayList<>();
                o.forEach(offence ->
                        offencesList.add(Offences.offences().withOffenceId(offence).build())
                );
                defendantsList.add(defendantBuilder.withOffences(offencesList).build());
            });
            prosecutionCasesList.add(caseBuilder.withDefendants(defendantsList).build());
        });

        LOGGER.info("ProsecutionCases to be removed from hearing: {}", prosecutionCasesList);

        return prosecutionCasesList;
    }

    /**
     * Build {@link ProsecutionCases} event object from {@link ProsecutionCasesToRemove} command by transforming:
     * ProsecutionCasesToRemove -> ProsecutionCases
     * DefendantsToRemove -> Defendants
     * OffencesToRemove -> Offences
     */
    public List<ProsecutionCases> buildEventProsecutionCasesToRemove(final List<ProsecutionCasesToRemove> prosecutionCasesToRemove) {

        final List<ProsecutionCases> cases = new ArrayList<>();

        prosecutionCasesToRemove.forEach(pctu -> {

            final ProsecutionCases.Builder casesBuilder = ProsecutionCases.prosecutionCases();
            final List<Defendants> defendants = new ArrayList<>();

            pctu.getDefendantsToRemove().forEach(dtu -> {

                final Defendants.Builder defendantsBuilder = Defendants.defendants();
                final List<Offences> offences = new ArrayList<>();

                dtu.getOffencesToRemove().forEach(otu ->
                        offences.add(Offences.offences().withOffenceId(otu.getOffenceId()).build())
                );

                defendants.add(defendantsBuilder.withOffences(offences).withDefendantId(dtu.getDefendantId()).build());
            });

            cases.add(casesBuilder.withCaseId(pctu.getCaseId()).withDefendants(defendants).build());
        });


        return cases;
    }
}
