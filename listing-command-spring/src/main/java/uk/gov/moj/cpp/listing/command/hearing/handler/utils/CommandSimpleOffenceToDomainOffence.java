package uk.gov.moj.cpp.listing.command.hearing.handler.utils;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.SimpleOffence;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.Converter;

import java.util.List;

@org.springframework.stereotype.Component("hearingHandlerCommandSimpleOffenceToDomainOffence")
public class CommandSimpleOffenceToDomainOffence implements Converter<List<SimpleOffence>, List<uk.gov.moj.cpp.listing.domain.SimpleOffence>> {

    public List<uk.gov.moj.cpp.listing.domain.SimpleOffence> convert(final List<SimpleOffence> commandOffences) {

        return commandOffences
                .stream()
                .map(this::convertToDomainOffence)
                .collect(toList());
    }

    private uk.gov.moj.cpp.listing.domain.SimpleOffence convertToDomainOffence(final SimpleOffence commandOffence) {
        return uk.gov.moj.cpp.listing.domain.SimpleOffence.createSimpleOffenceBuilder()
                .withDefendantId(commandOffence.getDefendantId())
                .withId(commandOffence.getId())
                .build();
    }


}


