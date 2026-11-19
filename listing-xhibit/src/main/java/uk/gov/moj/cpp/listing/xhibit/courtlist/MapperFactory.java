package uk.gov.moj.cpp.listing.xhibit.courtlist;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.xhibit.courtlist.mapper.AbstractCourtListMapper;
import uk.gov.moj.cpp.listing.xhibit.courtlist.mapper.CourtServicesMapper;
import uk.gov.moj.cpp.listing.xhibit.courtlist.mapper.DailyListMapper;
import uk.gov.moj.cpp.listing.xhibit.courtlist.mapper.FirmListMapper;
import uk.gov.moj.cpp.listing.xhibit.courtlist.mapper.WarnedListMapper;
import uk.gov.moj.cpp.listing.xhibit.exception.GenerationFailedException;

import java.util.List;

import jakarta.json.JsonObject;

import org.springframework.stereotype.Component;

/** Picks the XHIBIT mapper for the list type (port of the JEE {@code MapperFactory}). */
@Component
public class MapperFactory {

    private final CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    public MapperFactory(final CommonXhibitReferenceDataService commonXhibitReferenceDataService) {
        this.commonXhibitReferenceDataService = commonXhibitReferenceDataService;
    }

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
        return new CourtServicesMapper(context, commonXhibitReferenceDataService);
    }
}
