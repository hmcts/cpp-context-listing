package uk.gov.moj.cpp.listing.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.event.CourtRoomAdded;
import uk.gov.moj.cpp.listing.persistence.entity.CourtRoom;

import java.util.UUID;

public class CourtRoomConverter implements Converter<CourtRoomAdded, CourtRoom>{

    @Override
    public CourtRoom convert(final CourtRoomAdded courtRoomAdded) {
        return new CourtRoom(UUID.fromString(courtRoomAdded.getId()), courtRoomAdded.getCourtCentre(), courtRoomAdded.getCourtRoomName());
    }
}
