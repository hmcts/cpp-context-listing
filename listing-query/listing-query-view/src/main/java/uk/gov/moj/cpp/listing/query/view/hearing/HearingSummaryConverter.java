package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.Offence;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HearingSummaryConverter implements Converter<Hearing, HearingSummary> {

    private StartTimesJsonConverter startTimesConverter = new StartTimesJsonConverter();


    private NonSittingDaysJsonConverter nonSittingDaysConverter = new NonSittingDaysJsonConverter();



    @Override
    public HearingSummary convert(final Hearing hearing) {
        final List<ZonedDateTime> startTimes = startTimesConverter.convertStartTimesFrom(hearing.getStartTimes());

        final List<LocalDate> nonSittingDays = nonSittingDaysConverter.convertNonSittingDays(hearing.getNonSittingDays());

        final HearingSummary.HearingSummaryDetails hearingSummaryDetails = new HearingSummary
                .HearingSummaryDetails(hearing.getStartDate(), hearing.getEndDate(), nonSittingDays, startTimes,
                hearing.getCourtCentreId(), hearing.getCourtRoomId(), hearing.getEstimateMinutes(),hearing.getListingCaseId());

        return new HearingSummary(hearing.getId(), hearing.getType(), hearing.getJudgeId(),
                getDefendantSummaries(hearing), hearingSummaryDetails);
    }



    private Set<DefendantSummary> getDefendantSummaries(final Hearing hearing) {
        return hearing.getDefendants().stream()
                .map(d -> new DefendantSummary(d.getId().getHearingId(), d.getId().getDefendantId(), d.getFirstName(),
                        d.getLastName(), d.getBailStatus(), d.getCustodyTimeLimit(),
                        getOffenceSummaries(d.getOffences())))
                .collect(Collectors.toSet());
    }

    private Set<OffenceSummary> getOffenceSummaries(final Set<Offence> offences) {
        return offences.stream()
                .map(o -> new OffenceSummary(o.getId().getOffenceId().toString(), o.getId().getDefendantId().toString(), o.getStatementOfOffence().getTitle()))
                .collect(Collectors.toSet());
    }
}
