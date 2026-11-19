package uk.gov.moj.cpp.listing.command.hearing.processor.command;

import org.springframework.stereotype.Component;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.CaseMarkersToBeUpdated;
import uk.gov.justice.listing.events.Marker;
import uk.gov.moj.cpp.listing.domain.CaseMarker;

import java.util.List;

@Component
public class UpdateCaseMarkersForHearingCommandCollectionConverter {

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
