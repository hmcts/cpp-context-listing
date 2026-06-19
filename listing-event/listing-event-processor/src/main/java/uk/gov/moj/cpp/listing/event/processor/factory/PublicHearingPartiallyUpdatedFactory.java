package uk.gov.moj.cpp.listing.event.processor.factory;

import uk.gov.justice.listing.courts.Offences;
import uk.gov.justice.listing.events.Defendants;
import uk.gov.justice.listing.events.HearingPartiallyUpdated;
import uk.gov.justice.listing.events.ProsecutionCases;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

@SuppressWarnings("squid:S1168")
@ApplicationScoped
public class PublicHearingPartiallyUpdatedFactory {

    public uk.gov.justice.listing.courts.HearingPartiallyUpdated create(final HearingPartiallyUpdated hearingPartiallyUpdated) {

        return uk.gov.justice.listing.courts.HearingPartiallyUpdated.hearingPartiallyUpdated().
                withHearingIdToBeUpdated(hearingPartiallyUpdated.getHearingIdToBeUpdated()).
                withProsecutionCases(createProsecutionCases(hearingPartiallyUpdated.getProsecutionCases())).
                build();
    }

    private java.util.List<uk.gov.justice.listing.courts.ProsecutionCases> createProsecutionCases(final List<ProsecutionCases> prosecutionCases) {

        if (prosecutionCases == null) {
            return null;
        }

        return prosecutionCases.
                stream().
                map(this::createProsecutionCase).
                collect(Collectors.toList());
    }

    private uk.gov.justice.listing.courts.ProsecutionCases createProsecutionCase(final ProsecutionCases prosecutionCases) {
        return uk.gov.justice.listing.courts.ProsecutionCases.
                prosecutionCases().
                withCaseId(prosecutionCases.getCaseId()).
                withDefendants(
                        createProsecutionDefendants(prosecutionCases.getDefendants())
                ).
                build();
    }

    private List<uk.gov.justice.listing.courts.Defendants> createProsecutionDefendants(final List<Defendants> defendants) {
        if (defendants == null) {
            return null;
        }

        return defendants.
                stream().
                map(defendant -> uk.gov.justice.listing.courts.Defendants.
                        defendants().
                        withDefendantId(defendant.getDefendantId()).
                        withOffences(createOffences(defendant.getOffences())).
                        build()).collect(Collectors.toList());
    }

    private List<Offences> createOffences(final List<uk.gov.justice.listing.events.Offences> offences) {
        if (offences == null) {
            return null;
        }

        return offences.
                stream().
                map(offence -> createOffence(offence.getOffenceId())).
                collect(Collectors.
                        toList());
    }

    private uk.gov.justice.listing.courts.Offences createOffence(final UUID offenceId) {
        return Offences.
                offences().
                withOffenceId(offenceId).
                build();
    }
}
