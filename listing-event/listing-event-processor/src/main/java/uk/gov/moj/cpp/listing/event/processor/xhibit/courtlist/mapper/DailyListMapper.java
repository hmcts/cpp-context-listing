package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

public class DailyListMapper extends AbstractCourtListMapper {

    public DailyListMapper(CourtListGenerationContext context, JsonObject courtListForPublishing, CourtServicesMapper courtServicesMapper) {
        super(context, courtListForPublishing, courtServicesMapper);
    }

    @Override
    public JAXBElement<?> generate() {
        return null;
    }
}
