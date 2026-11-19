package uk.gov.moj.cpp.listing.command.hearing.handler.utils;

import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.listing.domain.CaseMarker.caseMarker;

import uk.gov.justice.core.courts.Marker;
import uk.gov.moj.cpp.listing.command.hearing.handler.utils.Converter;
import uk.gov.moj.cpp.listing.domain.CaseMarker;

import java.util.List;

@org.springframework.stereotype.Component("hearingHandlerCaseMarkersToDomainConverter")
public class CaseMarkersToDomainConverter implements Converter<List<uk.gov.justice.core.courts.Marker>, List<CaseMarker>> {

    @Override
    public List<CaseMarker> convert(final List<Marker> markers) {
        return markers.stream().map(marker -> caseMarker()
                .withId(marker.getId())
                .withMarkerTypeCode(marker.getMarkerTypeCode())
                .withMarkerTypeDescription(marker.getMarkerTypeDescription())
                .withMarkerTypeid(marker.getMarkerTypeid())
                .build()).collect(toList());
    }
}