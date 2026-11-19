package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import jakarta.json.JsonArray;
import java.util.List;
import java.util.Map;

public interface ListOfJsontoJsonArrayConverter {

     JsonArray convert(List<Hearing> hearings);

     JsonArray convertHearingResultForAlphabeticalList(final List<Hearing> hearings);

     JsonArray convertHearingResultForPublicList(Hearing hearing);

     JsonArray convertForSearchHearing(final List<Hearing> hearings, final Map<String, String> hearingDayMatchedCriteriaMap);

}
