package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.services.common.converter.Converter;

import java.time.LocalDate;
import java.util.List;

public class NonSittingDaysStringToLocalDateConverter implements Converter<List<String>, List<LocalDate>> {

    @Override
    public List<LocalDate> convert(final List<String> source) {
        return isEmpty(source) ? emptyList() : source.stream().map(LocalDate::parse).toList();
    }
}
