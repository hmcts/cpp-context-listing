package uk.gov.moj.cpp.listing.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.event.JudgeAdded;
import uk.gov.moj.cpp.listing.persistence.entity.Judge;

import java.util.UUID;

public class JudgeConverter implements Converter<JudgeAdded, Judge>{

    @Override
    public Judge convert(final JudgeAdded judgeAdded) {
        return new Judge(UUID.fromString(judgeAdded.getId()), judgeAdded.getTitle(), judgeAdded.getFirstName(), judgeAdded.getLastName());
    }
}
