package uk.gov.moj.cpp.listing.command.utils;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.courts.DeletedOffences;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;
import uk.gov.moj.cpp.listing.domain.SimpleOffence;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CourtsDeletedOffenceToDomainCaseSimpleOffence implements Converter<List<DeletedOffences>,List<CaseSimpleOffences>> {

    @Override
    public List<CaseSimpleOffences> convert(List<DeletedOffences> deletedOffences) {
        return deletedOffences == null ? emptyList() : deletedOffences.stream().map(deletedOffence -> buildCaseOffences(deletedOffence)).collect(toList());
    }

    private CaseSimpleOffences buildCaseOffences(DeletedOffences deletedOffences) {
        final UUID caseId = deletedOffences.getProsecutionCaseId();
        final UUID defendantId = deletedOffences.getDefendantId();
        List<SimpleOffence> offences = deletedOffences.getOffences()
                .stream()
                .map(offenceId -> createSimpleOffence(offenceId, defendantId) )
                .collect(Collectors.toList());

        CaseSimpleOffences caseOffences = new CaseSimpleOffences(caseId, defendantId, offences);

        return caseOffences;
    }

    private static SimpleOffence createSimpleOffence(
            UUID offenceId,
            final UUID defendantId) {

        return SimpleOffence.createSimpleOffenceBuilder()
                .withDefendantId(defendantId)
                .withId(offenceId)
                .build();
    }
}


