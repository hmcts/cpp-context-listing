package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.entity.Offence;

import java.util.Set;
import java.util.stream.Collectors;

public class HearingSummaryConverter implements Converter<Hearing, HearingSummary> {

    @Override
    public HearingSummary convert(final Hearing hearing) {
        return new HearingSummary(hearing.getId(), hearing.getStartDateTime(),
                hearing.getEstimateMinutes(), hearing.getType(), getDefendantSummaries(hearing.getListingCase()));
    }

    private Set<DefendantSummary> getDefendantSummaries(final ListingCase listingCase) {
        return listingCase.getDefendants().stream()
                .map(d -> new DefendantSummary(d.getId(), d.getFirstName(), d.getLastName(), d.getBailStatus()
                        , getOffenceSummaries(d.getOffences())))
                .collect(Collectors.toSet());
    }

    private Set<OffenceSummary> getOffenceSummaries(final Set<Offence> offences) {
        return offences.stream()
                .map(o -> new OffenceSummary(o.getId().toString(), o.getStatementOfOffence().getTitle()))
                .collect(Collectors.toSet());
    }
}
