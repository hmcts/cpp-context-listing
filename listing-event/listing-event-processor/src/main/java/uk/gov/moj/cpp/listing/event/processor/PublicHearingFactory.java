package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Optional.of;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.service.ReferenceDataService;

import java.util.UUID;

import javax.inject.Inject;

public class PublicHearingFactory {

    @Inject
    ReferenceDataService referenceDataService;

    protected CourtCentre buildCourtCentre(UUID courtCentreId, UUID courtRoomId, final JsonEnvelope envelope) {
        return CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withName(referenceDataService.getOrganizationUnitById(courtCentreId, envelope).getOucodeL3Name().orElse(null))
                .withRoomId(of(courtRoomId))
                .build();
    }

    protected HearingDay buildHearingDay(uk.gov.justice.listing.events.HearingDay hd) {
        return new HearingDay(hd.getIsCancelled(), hd.getDurationMinutes(), of(hd.getSequence()), hd.getStartTime());
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
