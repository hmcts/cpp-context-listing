package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.commands.SimpleOffence;
import uk.gov.justice.services.common.converter.Converter;

import java.util.List;

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


