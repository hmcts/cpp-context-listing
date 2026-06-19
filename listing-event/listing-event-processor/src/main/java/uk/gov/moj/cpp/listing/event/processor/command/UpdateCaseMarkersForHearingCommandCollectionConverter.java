package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.CaseMarkersToBeUpdated;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.domain.CaseMarker;

import java.util.List;

public class UpdateCaseMarkersForHearingCommandCollectionConverter implements Converter<CaseMarkersToBeUpdated, List<UpdateCaseMarkersForHearingCommand>> {

    @Override
    public List<UpdateCaseMarkersForHearingCommand> convert(final CaseMarkersToBeUpdated event) {
        final List<CaseMarker> markers = convertCaseMarkers(event.getMarkers());
        return event.getHearings().stream().map(hearingId ->
                new UpdateCaseMarkersForHearingCommand(event.getProsecutionCaseId(), hearingId, markers)).collect(toList());
    }

    private List<CaseMarker> convertCaseMarkers(final List<Marker> markers) {
        return markers.stream().map(marker -> CaseMarker.caseMarker()
                .withId(marker.getId())
                .withMarkerTypeid(marker.getMarkerTypeid())
                .withMarkerTypeDescription(marker.getMarkerTypeDescription())
                .withMarkerTypeCode(marker.getMarkerTypeCode())
                .build()).collect(toList());
    }
}
