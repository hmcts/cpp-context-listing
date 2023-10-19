package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.listing.events.CaseMarkersToBeUpdated.caseMarkersToBeUpdated;
import static uk.gov.justice.listing.events.Marker.marker;

import uk.gov.justice.listing.events.CaseMarkersToBeUpdated;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class UpdateCaseMarkersForHearingCommandCollectionConverterTest {

    private UpdateCaseMarkersForHearingCommandCollectionConverter updateCaseMarkersForHearingCommandCollectionConverter = new UpdateCaseMarkersForHearingCommandCollectionConverter();

    @Test
    public void shouldConvert() {
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID markerId = randomUUID();
        final UUID typeId = randomUUID();
        final CaseMarkersToBeUpdated event = caseMarkersToBeUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withHearings(asList(hearingId))
                .withMarkers(asList(marker()
                        .withId(markerId)
                        .withMarkerTypeCode("markerTypeCode")
                        .withMarkerTypeDescription("desc")
                        .withMarkerTypeid(typeId)
                        .build()))
                .build();
        final List<UpdateCaseMarkersForHearingCommand> convert = updateCaseMarkersForHearingCommandCollectionConverter.convert(event);

        assertThat(convert, hasSize(1));
        assertThat(convert.get(0).getHearingId(), is(hearingId));
        assertThat(convert.get(0).getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat(convert.get(0).getCaseMarkers(), hasSize(1));
        assertThat(convert.get(0).getCaseMarkers().get(0).getId(), is(markerId));
        assertThat(convert.get(0).getCaseMarkers().get(0).getMarkerTypeCode(), is("markerTypeCode"));
        assertThat(convert.get(0).getCaseMarkers().get(0).getMarkerTypeDescription(), is("desc"));
        assertThat(convert.get(0).getCaseMarkers().get(0).getMarkerTypeid(), is(typeId));

    }
}