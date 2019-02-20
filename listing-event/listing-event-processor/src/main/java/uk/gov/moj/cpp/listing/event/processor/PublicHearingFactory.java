package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Optional.of;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;

import java.util.UUID;

public class PublicHearingFactory {

    protected CourtCentre buildCourtCentre(UUID courtCentreId, UUID courtRoomId) {

        return CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withRoomId(of(courtRoomId))
                .build();
    }

    protected HearingDay buildHearingDay(uk.gov.justice.listing.events.HearingDay hd) {
        return HearingDay.hearingDay()
                .withListedDurationMinutes(hd.getDurationMinutes())
                .withListingSequence(of(hd.getSequence()))
                .withSittingDay(hd.getStartTime())
                .build();
    }

    protected HearingType buildType(uk.gov.justice.listing.events.Type type) {
        return HearingType.hearingType()
                .withDescription(type.getDescription())
                .withId(type.getId())
                .build();
    }

    protected JudicialRole buildJudicialRole(uk.gov.justice.listing.events.JudicialRole jr) {
        return JudicialRole.judicialRole()
                .withIsDeputy(jr.getIsDeputy())
                .withIsBenchChairman(jr.getIsBenchChairman())
                .withJudicialRoleType(uk.gov.justice.core.courts.JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId())
                        .build())
                .withJudicialId(jr.getJudicialId())
                .build();
    }
}
