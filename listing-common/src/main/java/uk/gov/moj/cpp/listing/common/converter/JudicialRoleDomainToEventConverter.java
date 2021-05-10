package uk.gov.moj.cpp.listing.common.converter;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JudicialRoleType;

import java.util.List;

public class JudicialRoleDomainToEventConverter {

    private JudicialRoleDomainToEventConverter() {}

    public static List<JudicialRole> convertToEvents(final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judicialRoles) {
        return judicialRoles.stream()
                .map(JudicialRoleDomainToEventConverter::buildJudicialRole)
                .collect(toList());
    }

    public static JudicialRole buildJudicialRole(final uk.gov.moj.cpp.listing.domain.JudicialRole jr) {
        return JudicialRole.judicialRole()
                .withIsBenchChairman(jr.getIsBenchChairman())
                .withIsDeputy(jr.getIsDeputy())
                .withJudicialId(jr.getJudicialId())
                .withJudicialRoleType(JudicialRoleType.judicialRoleType()
                        .withJudiciaryType(jr.getJudicialRoleType().getJudiciaryType())
                        .withJudicialRoleTypeId(jr.getJudicialRoleType().getJudicialRoleTypeId())
                        .build())
                .withUserId(jr.getUserId())
                .build();
    }
}
