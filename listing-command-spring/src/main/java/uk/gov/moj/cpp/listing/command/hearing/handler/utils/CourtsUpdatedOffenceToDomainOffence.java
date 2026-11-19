package uk.gov.moj.cpp.listing.command.hearing.handler.utils;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.courts.UpdatedOffences;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.Converter;
import uk.gov.moj.cpp.listing.domain.CaseOffences;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Component("hearingHandlerCourtsUpdatedOffenceToDomainOffence")
public class CourtsUpdatedOffenceToDomainOffence extends CourtsOffenceToDomainOffenceConverter implements Converter<List<UpdatedOffences>,List<CaseOffences>> {

    @Override
    public List<CaseOffences> convert(final List<UpdatedOffences> updatedOffences) {
        return updatedOffences == null ? emptyList() : updatedOffences.stream().map(updatedOffence -> buildCaseOffences(updatedOffence)).collect(toList());
    }

    private CaseOffences buildCaseOffences(UpdatedOffences updatedOffences) {
        final UUID caseId = updatedOffences.getProsecutionCaseId();
        final UUID defendantId = updatedOffences.getDefendantId();

        return createOffences(caseId, defendantId, updatedOffences.getOffences());
    }

}


