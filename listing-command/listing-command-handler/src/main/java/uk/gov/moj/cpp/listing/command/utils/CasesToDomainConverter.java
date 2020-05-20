package uk.gov.moj.cpp.listing.command.utils;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.courts.Cases;
import uk.gov.justice.listing.courts.LinkedToCases;
import uk.gov.justice.services.common.converter.Converter;
import java.util.List;

public class CasesToDomainConverter implements Converter<Cases, uk.gov.moj.cpp.listing.domain.Cases> {

    @Override
    public uk.gov.moj.cpp.listing.domain.Cases convert(final Cases cases) {
        return uk.gov.moj.cpp.listing.domain.Cases.cases()
                .withCaseId(cases.getCaseId())
                .withCaseUrn(cases.getCaseUrn())
                .withLinkedToCases(convertLinkedToCases(cases.getLinkedToCases()))
                .build();
    }

    private List<uk.gov.moj.cpp.listing.domain.LinkedToCases> convertLinkedToCases(List<LinkedToCases> linkedToCases) {
        return linkedToCases.stream()
                .map(lc -> uk.gov.moj.cpp.listing.domain.LinkedToCases.linkedToCases()
                .withCaseId(lc.getCaseId())
                .withCaseUrn(lc.getCaseUrn())
                .build())
                .collect(toList());
    }
}