package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper.AbstractCourtListMapper;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper.CourtServicesMapper;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper.DailyListMapper;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper.FirmListMapper;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper.WarnedListMapper;
import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.GenerationFailedException;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.annotations.VisibleForTesting;

public class MapperFactory {

    @Inject
    private XhibitReferenceDataService xhibitReferenceDataService;

    public AbstractCourtListMapper createCourtListMapper(final CourtListGenerationContext context, final List<JsonObject> courtListsJson) {
        switch (context.getParameters().getPublishCourtListType()) {
            case FIRM:
                return new FirmListMapper(context, courtListsJson, createCourtServicesMapper(context));
            case WARN:
                return new WarnedListMapper(context, courtListsJson, createCourtServicesMapper(context));
            case DRAFT:
            case FINAL:
                return new DailyListMapper(context, courtListsJson, createCourtServicesMapper(context));
            default:
                throw new GenerationFailedException("No mapper for " + context.getParameters().getPublishCourtListType());
        }
    }

    private CourtServicesMapper createCourtServicesMapper(final CourtListGenerationContext context) {
        return new CourtServicesMapper(context, xhibitReferenceDataService);
    }

    @VisibleForTesting
    public void setXhibitReferenceDataService(XhibitReferenceDataService xhibitReferenceDataService) {
        this.xhibitReferenceDataService = xhibitReferenceDataService;
    }
}
