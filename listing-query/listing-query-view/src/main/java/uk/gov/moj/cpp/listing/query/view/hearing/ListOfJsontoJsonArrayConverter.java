package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import javax.json.JsonArray;
import java.util.List;

public interface ListOfJsontoJsonArrayConverter extends Converter<List<Hearing>, JsonArray> {

     JsonArray convertHearingResultForAlphabeticalList(final List<Hearing> hearings);

     JsonArray convertHearingResultForPublicList(Hearing hearing);

}
