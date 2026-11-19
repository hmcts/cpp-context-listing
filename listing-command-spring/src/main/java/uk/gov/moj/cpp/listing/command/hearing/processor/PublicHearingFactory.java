package uk.gov.moj.cpp.listing.command.hearing.processor;


import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.listing.events.OrganisationUnit;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.command.hearing.processor.service.ReferenceDataService;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
@SuppressWarnings({"squid:S1172", "squid:CommentedOutCodeLine"})
public class PublicHearingFactory {

    @Autowired
    ReferenceDataService referenceDataService;

    protected CourtCentre buildCourtCentre(UUID courtCentreId, UUID courtRoomId, final Envelope envelope) {
        return buildCourtCentre(courtCentreId, Optional.ofNullable(courtRoomId), envelope);
    }

    protected CourtCentre buildCourtCentreWithAdmin(UUID courtCentreId, UUID courtRoomId, final Envelope envelope) {
        return buildCourtCentreWithAdmin(courtCentreId, Optional.ofNullable(courtRoomId), envelope);
    }

    protected CourtCentre buildCourtCentre(UUID courtCentreId, Optional<UUID> courtRoomId, final Envelope envelope) {
        final OrganisationUnit organisationUnit = referenceDataService.getOrganizationUnitById(courtCentreId, envelope);
        return CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withName(organisationUnit.getOucodeL3Name())
                .withCode(organisationUnit.getOucode())
                .withRoomId(courtRoomId.orElse(null))
                .build();
    }

    protected CourtCentre buildCourtCentreWithAdmin(UUID courtCentreId, Optional<UUID> courtRoomId, final Envelope envelope) {
        final OrganisationUnit organisationUnit = referenceDataService.getOrganizationUnitByIdWithAdmin(courtCentreId, envelope);
        return CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withName(organisationUnit.getOucodeL3Name())
                .withCode(organisationUnit.getOucode())
                .withRoomId(courtRoomId.orElse(null))
                .build();
    }

    public HearingDay buildHearingDay(uk.gov.justice.listing.events.HearingDay hd) {
        return HearingDay.hearingDay()
                .withListedDurationMinutes(hd.getDurationMinutes())
                .withListingSequence(hd.getSequence())
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
