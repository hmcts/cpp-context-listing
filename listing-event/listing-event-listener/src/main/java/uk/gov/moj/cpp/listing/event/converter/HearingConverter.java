package uk.gov.moj.cpp.listing.event.converter;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.DefendantBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCaseBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.Offence;
import uk.gov.moj.cpp.listing.persistence.entity.OffenceBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.StatementOfOffence;
import uk.gov.moj.cpp.listing.persistence.entity.StatementOfOffenceBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.ListingCaseRepository;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class HearingConverter implements Converter<CaseSentForListing,  Set<Hearing>> {
    @Inject
    private ListingCaseRepository listingCaseRepository;

    @Override
    public Set<Hearing> convert(final CaseSentForListing event) {

        final ListingCase listingCase = findListingCase(event);
        return event.getHearings()
                .stream()
                .map(eventHearing -> createHearing(listingCase, eventHearing))
                .collect(Collectors.toSet());
    }

    private Hearing createHearing(final ListingCase listingCase, final uk.gov.moj.cpp.listing.domain.Hearing eventHearing) {
        final Hearing hearing = buildHearing(eventHearing, listingCase);
        final Set<Defendant> defendants = buildDefendants(eventHearing.getDefendants(), hearing);
        hearing.getDefendants().addAll(defendants);
        return hearing;
    }

    private ListingCase findListingCase(final CaseSentForListing event) {
        ListingCase listingCase = listingCaseRepository.findBy(UUID.fromString(event.getCaseProgressionId()));
        if (listingCase == null) {
            listingCase = buildListingCase(event);
        }
        return listingCase;
    }

    private ListingCase buildListingCase(final CaseSentForListing event) {
        final ListingCaseBuilder listingCaseBuilder = new ListingCaseBuilder();
        listingCaseBuilder.setCaseProgressionId(UUID.fromString(event.getCaseProgressionId()));
        listingCaseBuilder.setUrn(event.getUrn());

        return listingCaseBuilder.build();
    }

    private Hearing buildHearing(final uk.gov.moj.cpp.listing.domain.Hearing hearingPartOfEvent, final ListingCase listingCase) {
        final HearingBuilder hearingBuilder = new HearingBuilder();

        hearingBuilder.setId(UUID.fromString(hearingPartOfEvent.getId()));
        hearingBuilder.setCourtCentreId(UUID.fromString(hearingPartOfEvent.getCourtCentreId()));
        hearingBuilder.setType(hearingPartOfEvent.getType());
        hearingBuilder.setStartDate(hearingPartOfEvent.getStartDate());
        hearingBuilder.setEstimateMinutes(hearingPartOfEvent.getEstimateMinutes());
        hearingBuilder.setListingCase(listingCase);
        hearingBuilder.setAllocated(hearingPartOfEvent.isAllocated());

        return hearingBuilder.build();
    }

    private Set<Defendant> buildDefendants(final List<uk.gov.moj.cpp.listing.domain.Defendant> defendantsPartOfEvent,
                                           final Hearing hearing) {
        if (defendantsPartOfEvent == null) {
            return Collections.emptySet();
        }
        return defendantsPartOfEvent.stream()
                .map(defendant -> this.buildDefendant(defendant, hearing))
                .collect(toSet());
    }

    private Defendant buildDefendant(final uk.gov.moj.cpp.listing.domain.Defendant defendantPartOfEvent,
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
