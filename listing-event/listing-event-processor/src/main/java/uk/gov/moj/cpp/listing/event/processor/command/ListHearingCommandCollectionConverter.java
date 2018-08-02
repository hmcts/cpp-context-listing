package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.CaseSentForListing;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.LocalDates;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ListHearingCommandCollectionConverter implements Converter<CaseSentForListing, List<ListHearingCommand> > {

    @Override
    public List<ListHearingCommand>  convert(final CaseSentForListing event) {

        return event.getHearings().stream().map(hearing -> {
            final List<uk.gov.moj.cpp.listing.domain.Defendant> defendants = convertDefendants(hearing.getDefendants());
            return new ListHearingCommand(
                    hearing.getId().toString(),
                    hearing.getType(),
                    LocalDate.parse(hearing.getStartDate()),
                    hearing.getEndDate(),
                    hearing.getEstimateMinutes(),
                    hearing.getCaseId().toString(),
                    hearing.getCourtCentreId().toString(),
                    getStringOrNull(hearing.getCourtRoomId()),
                    getStringOrNull(hearing.getJudgeId()),
                    getStringOrNull(hearing.getStartTime()),
                    defendants,
                    event.getUrn()

            );
        }).collect(toList());
    }


    private List<uk.gov.moj.cpp.listing.domain.Defendant> convertDefendants(final List<Defendant> defendants) {
        return defendants.stream().map(d -> convertDefendant(d)).collect(Collectors.toList());
    }

    private uk.gov.moj.cpp.listing.domain.Defendant convertDefendant(final Defendant defendant) {
        return new uk.gov.moj.cpp.listing.domain.Defendant(defendant.getId().toString(),defendant.getPersonId().toString(),defendant.getFirstName(),defendant.getLastName(), LocalDate.parse(defendant.getDateOfBirth()),defendant.getBailStatus().toString(), getCustodyTimeLimit(defendant.getCustodyTimeLimit()),defendant.getDefenceOrganisation(), convertOffences(defendant.getOffences()));
    }

    private List<uk.gov.moj.cpp.listing.domain.Offence> convertOffences(final List<Offence> offences) {
        return offences.stream().map(o -> convertOffence(o)).collect(Collectors.toList());
    }

    private uk.gov.moj.cpp.listing.domain.Offence convertOffence(final Offence offence) {
        final LocalDate endDate = offence.getEndDate().map(LocalDates::from).orElse(null);
        return uk.gov.moj.cpp.listing.domain.Offence.createOffenceBuilder()
                .setId(offence.getId().toString())
                .setOffenceCode(offence.getOffenceCode())
                .setStartDate(getLocalDate(offence.getStartDate()))
                .setEndDate(endDate)
                .setStatementOfOffence(convertStatementOfOffence(offence.getStatementOfOffence()))
                .setDefendantId(offence.getDefendantId().toString()).build();
    }


    private uk.gov.moj.cpp.listing.domain.StatementOfOffence convertStatementOfOffence(final StatementOfOffence statementOfOffence) {
        return new uk.gov.moj.cpp.listing.domain.StatementOfOffence(statementOfOffence.getTitle(), statementOfOffence.getLegislation());
    }

    private LocalDate getCustodyTimeLimit(final Optional<String> custodyTimeLimit) {
        if(custodyTimeLimit.isPresent()){
            return LocalDate.parse(custodyTimeLimit.get());
        }
        return null;
    }

    private LocalDate getLocalDate(final String date) {
        if(date!=null){
            return LocalDate.parse(date);
        }
        return null;
    }

    private String getStringOrNull(Optional<String> optional) {
        if(optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    private String getStringOrNull(UUID uuid) {
        if(uuid!=null) {
            return uuid.toString();
        }
        return null;
    }


}
