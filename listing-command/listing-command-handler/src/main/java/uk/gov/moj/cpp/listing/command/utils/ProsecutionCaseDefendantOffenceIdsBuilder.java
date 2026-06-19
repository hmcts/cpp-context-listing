package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.moj.cpp.listing.domain.DefendantOffenceIds;
import uk.gov.moj.cpp.listing.domain.OffenceIds;
import uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProsecutionCaseDefendantOffenceIdsBuilder {

    public List<ProsecutionCaseDefendantOffenceIds> buildFromProsecutionCases(final List<ProsecutionCases> prosecutionCases) {
        return Objects.isNull(prosecutionCases) ? Collections.emptyList() :
                prosecutionCases.stream()
                        .map(p -> ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                                .withId(p.getCaseId())
                                .withDefendants(p.getDefendants().stream()
                                        .map(d -> DefendantOffenceIds.defendantOffenceIds()
                                                .withId(d.getDefendantId())
                                                .withOffences(d.getOffences().stream()
                                                        .map(o -> OffenceIds.offenceIds()
                                                                .withId(o.getOffenceId())
                                                                .build()).collect(toList()))
                                                .build()).collect(toList()))
                                .build())
                        .collect(toList());
    }
}
