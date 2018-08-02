package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.Defendant;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class UpdateDefendantsForHearingCommandCollectionConverter implements Converter<DefendantsToBeUpdated, List<UpdateDefendantsForHearingCommand> > {

    @Override
    public List<UpdateDefendantsForHearingCommand>  convert(final DefendantsToBeUpdated event) {

        final List <Defendant> defendants = convertDefendants(event.getDefendants());
        return event.getHearings().stream().map(hearingId ->
                new UpdateDefendantsForHearingCommand(defendants, hearingId)).collect(toList());
    }

    private List<Defendant> convertDefendants(List<uk.gov.justice.listing.events.Defendant> defendants) {
        return defendants.stream().map(defendant -> {
            final String custodyTimeLimitString = defendant.getCustodyTimeLimit().orElse(null);
            final LocalDate custodyTimeLimit = custodyTimeLimitString != null ? LocalDate.parse(custodyTimeLimitString) : null;
            final List<uk.gov.moj.cpp.listing.domain.Offence>  offences = convertOffences();
            return new Defendant(defendant.getId().toString(),
                    defendant.getPersonId().toString(),
                    defendant.getFirstName(),
                    defendant.getLastName(),
                    LocalDate.parse(defendant.getDateOfBirth()),
                    defendant.getBailStatus().toString(),
                    custodyTimeLimit,
                    defendant.getDefenceOrganisation(),
                    offences
            );
        }).collect(toList());
    }

    private List<uk.gov.moj.cpp.listing.domain.Offence> convertOffences() {
        // Offences are not being updated as part of this event flow originating from
        // event public.progression.case-defendant-changed
        return Collections.emptyList();
    }
}
