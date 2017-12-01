package uk.gov.moj.cpp.listing.domain.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;

import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("squid:S1068")
public class Case implements Aggregate {

    private static final long serialVersionUID = 1L;

    private String caseId;
    private String urn;
    private List<Hearing> hearings;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CaseSentForListing.class).apply(this::onCaseSentForListing),
                otherwiseDoNothing());
    }

    public Stream<Object> sendForListing(final String caseId, final String urn, final List<Hearing> hearings) {
        return apply(Stream.of(new CaseSentForListing(caseId, urn, hearings)));
    }

    // Methods to apply aggregate state

    private void onCaseSentForListing(CaseSentForListing event) {
        this.caseId = event.getCaseId();
        this.urn = event.getUrn();
        this.hearings = event.getHearings();
    }
}
