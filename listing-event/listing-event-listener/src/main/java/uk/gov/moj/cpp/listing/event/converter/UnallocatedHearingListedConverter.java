package uk.gov.moj.cpp.listing.event.converter;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.event.UnallocatedHearingListed;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.DefendantBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;import uk.gov.moj.cpp.listing.persistence.entity.Offence;
import uk.gov.moj.cpp.listing.persistence.entity.OffenceBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.StatementOfOffence;
import uk.gov.moj.cpp.listing.persistence.entity.StatementOfOffenceBuilder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UnallocatedHearingListedConverter implements Converter<UnallocatedHearingListed, Hearing> {

    @Override
    public Hearing convert(final UnallocatedHearingListed event) {

        final Hearing hearing = buildHearing( UUID.fromString(event.getHearingId()), UUID.fromString(event.getCourtCentreId()), event.getType(), event.getStartDate(),
                event.getEstimateMinutes(), UUID.fromString(event.getCaseId()));
        final Set<Defendant> defendants = buildDefendants(event.getDefendants(), hearing);
        hearing.getDefendants().addAll(defendants);
        return hearing;
    }

    Hearing buildHearing(UUID hearingId, UUID courtCentreId, String type, LocalDate startDate,
                         Integer estimateMins, UUID listingCaseId) {
        final HearingBuilder hearingBuilder = new HearingBuilder();

        hearingBuilder.setId(hearingId);
        hearingBuilder.setCourtCentreId(courtCentreId);
        hearingBuilder.setType(type);
        hearingBuilder.setStartDate(startDate);
        hearingBuilder.setEstimateMinutes(estimateMins);
        hearingBuilder.setListingCaseId(listingCaseId);
        hearingBuilder.setAllocated(false);

        return hearingBuilder.build();
    }


    Set<Defendant> buildDefendants(final List<uk.gov.moj.cpp.listing.domain.Defendant> defendantsPartOfEvent,
                                   final Hearing hearing) {
        if (defendantsPartOfEvent == null) {
            return Collections.emptySet();
        }
        return defendantsPartOfEvent.stream()
                .map(defendant -> this.buildDefendant(defendant, hearing))
                .collect(toSet());
    }

    Defendant buildDefendant(final uk.gov.moj.cpp.listing.domain.Defendant defendantPartOfEvent,
                             final Hearing hearing) {
        final DefendantBuilder defendantBuilder = new DefendantBuilder();
        final Set<Offence> offences = buildOffences(defendantPartOfEvent.getOffences());

        defendantBuilder.setListingDefendantId(randomUUID());
        defendantBuilder.setDefendantId(UUID.fromString(defendantPartOfEvent.getId()));
        defendantBuilder.setPersonId(UUID.fromString((defendantPartOfEvent.getPersonId())));
        defendantBuilder.setFirstName(defendantPartOfEvent.getFirstName());
        defendantBuilder.setLastName(defendantPartOfEvent.getLastName());
        defendantBuilder.setBailStatus(defendantPartOfEvent.getBailStatus());
        defendantBuilder.setDateOfBirth(defendantPartOfEvent.getDateOfBirth());
        defendantBuilder.setCustodyTimeLimit(defendantPartOfEvent.getCustodyTimeLimit());
        defendantBuilder.setDefenceOrganisation(defendantPartOfEvent.getDefenceOrganisation());
        defendantBuilder.setOffences(offences);
        defendantBuilder.setHearing(hearing);

        return defendantBuilder.build();
    }

    private Set<Offence> buildOffences(final List<uk.gov.moj.cpp.listing.domain.Offence> offencesPartOfEvent) {
        if (offencesPartOfEvent == null) {
            return Collections.emptySet();
        }
        return offencesPartOfEvent.stream()
                .map(this::buildOffence)
                .collect(toSet());
    }

    private Offence buildOffence(final uk.gov.moj.cpp.listing.domain.Offence offencePartOfPayload) {
        final OffenceBuilder offenceBuilder = new OffenceBuilder();
        final StatementOfOffence statementOfOffence = buildStatementOfOffence
                (offencePartOfPayload.getStatementOfOffence());

        offenceBuilder.setListingOffenceId(randomUUID());
        offenceBuilder.setOffenceId(UUID.fromString(offencePartOfPayload.getId()));
        offenceBuilder.setOffenceCode(offencePartOfPayload.getOffenceCode());
        offenceBuilder.setStartDate(offencePartOfPayload.getStartDate());
        offenceBuilder.setEndDate(offencePartOfPayload.getEndDate());
        offenceBuilder.setStatementOfOffence(statementOfOffence);

        return offenceBuilder.build();
    }

    private StatementOfOffence buildStatementOfOffence(final uk.gov.moj.cpp.listing.domain.StatementOfOffence statementOfOffencePartOfEvent) {
        final StatementOfOffenceBuilder statementOfOffenceBuilder = new StatementOfOffenceBuilder();

        statementOfOffenceBuilder.setLegislation(statementOfOffencePartOfEvent.getLegislation());
        statementOfOffenceBuilder.setTitle(statementOfOffencePartOfEvent.getTitle());

        return statementOfOffenceBuilder.build();
    }


}


