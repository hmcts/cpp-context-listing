package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.courts.AddedOffences;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.CaseOffences;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CourtsAddedOffenceToDomainOffence extends CourtsOffenceToDomainOffenceConverter implements Converter<List<AddedOffences>,List<CaseOffences>> {

    @Override
    public List<CaseOffences> convert(final List<AddedOffences> addedOffences) {
        return addedOffences == null ? emptyList() : addedOffences.stream().map(addedOffence -> buildCaseOffences(addedOffence)).collect(toList());
    }

    private CaseOffences buildCaseOffences(AddedOffences updatedOffences) {
        final UUID caseId = updatedOffences.getProsecutionCaseId();
        final UUID defendantId = updatedOffences.getDefendantId();

        return createOffences(caseId, defendantId, updatedOffences.getOffences());
    }

}


