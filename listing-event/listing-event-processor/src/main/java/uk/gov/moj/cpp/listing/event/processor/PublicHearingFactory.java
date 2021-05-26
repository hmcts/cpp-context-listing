package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Optional.of;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.listing.events.OrganisationUnit;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.service.ReferenceDataService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
@SuppressWarnings({"squid:S1172", "squid:CommentedOutCodeLine"})
public class PublicHearingFactory {

    @Inject
    ReferenceDataService referenceDataService;

    protected CourtCentre buildCourtCentre(UUID courtCentreId, UUID courtRoomId, final JsonEnvelope envelope) {
        return buildCourtCentre(courtCentreId, Optional.of(courtRoomId), envelope);
    }

    protected CourtCentre buildCourtCentre(UUID courtCentreId, Optional<UUID> courtRoomId, final JsonEnvelope envelope) {
        final OrganisationUnit organisationUnit = referenceDataService.getOrganizationUnitById(courtCentreId, envelope);
        return CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withName(organisationUnit.getOucodeL3Name().orElse(null))
                .withCode(organisationUnit.getOucode().orElse(null))
                .withRoomId(courtRoomId)
                .build();
    }

    protected HearingDay buildHearingDay(uk.gov.justice.listing.events.HearingDay hd) {
        return HearingDay.hearingDay()
                .withListedDurationMinutes(hd.getDurationMinutes())
                .withListingSequence(of(hd.getSequence()))
                .withSittingDay(hd.getStartTime())
                .withCourtCentreId(hd.getCourtCentreId())
                .withCourtRoomId(hd.getCourtRoomId())
                .withIsCancelled(hd.getIsCancelled())
                .build();
    }

    protected HearingType buildType(uk.gov.justice.listing.events.Type type) {
        return HearingType.hearingType()
                .withDescription(type.getDescription())
                .withWelshDescription(type.getWelshDescription())
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
                .withUserId(jr.getUserId())
                .build();
    }
}
