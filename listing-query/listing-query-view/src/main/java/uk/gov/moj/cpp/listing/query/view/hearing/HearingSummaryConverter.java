package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.Offence;

import java.util.Set;
import java.util.stream.Collectors;

public class HearingSummaryConverter implements Converter<Hearing, HearingSummary> {

    @Override
    public HearingSummary convert(final Hearing hearing) {
        final HearingSummary.HearingSummaryDetails hearingSummaryDetails = new HearingSummary
                .HearingSummaryDetails(hearing.getStartDate(), hearing.getStartTime(),
                hearing.getCourtCentreId(), hearing.getCourtRoomId(), hearing.getEstimateMinutes(),hearing.getListingCaseId());

        return new HearingSummary(hearing.getId(), hearing.getType(), hearing.getJudgeId(), hearing.getNotBefore(),
                getDefendantSummaries(hearing), hearingSummaryDetails);
    }

    private Set<DefendantSummary> getDefendantSummaries(final Hearing hearing) {
        return hearing.getDefendants().stream()
                .map(d -> new DefendantSummary(d.getListingDefendantId(), d.getFirstName(),
                        d.getLastName(), d.getBailStatus(), d.getCustodyTimeLimit(),
                        getOffenceSummaries(d.getOffences())))
                .collect(Collectors.toSet());
    }

    private Set<OffenceSummary> getOffenceSummaries(final Set<Offence> offences) {
        return offences.stream()
                .map(o -> new OffenceSummary(o.getOffenceId().toString(), o.getStatementOfOffence().getTitle()))
                .collect(Collectors.toSet());
    }
}
