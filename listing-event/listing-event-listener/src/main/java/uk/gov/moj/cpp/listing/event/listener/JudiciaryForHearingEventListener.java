package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JudiciaryAssignedToHearing;
import uk.gov.justice.listing.events.JudiciaryAssignmentSource;
import uk.gov.justice.listing.events.JudiciaryChangedForHearing;
import uk.gov.justice.listing.events.JudiciaryRemovedFromHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_LISTENER)
public class JudiciaryForHearingEventListener {

    private static final String JUDICIARY = "judiciary";
    private static final String JUDICIARY_ASSIGNMENT_SOURCE = "judiciaryAssignmentSource";

    private HearingRepository hearingRepository;

    @Inject
    public JudiciaryForHearingEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Handles("listing.events.judiciary-assigned-to-hearing")
    public void judiciaryAssignedToHearing(final Envelope<JudiciaryAssignedToHearing> event) {
        final JudiciaryAssignedToHearing judiciaryAssignedToHearing = event.payload();
        final UUID hearingId = judiciaryAssignedToHearing.getHearingId();
        final List<JudicialRole> judicialRoles = judiciaryAssignedToHearing.getJudiciary();

        if (nonNull(hearingRepository.findBy(hearingId))) {
            final JsonNodeUpdater hearing = using(hearingRepository)
                    .find(hearingId)
                    .putObjectList(JUDICIARY, judicialRoles);

            if (nonNull(judiciaryAssignedToHearing.getJudiciaryAssignmentSource())) {
                hearing.put(JUDICIARY_ASSIGNMENT_SOURCE, judiciaryAssignedToHearing.getJudiciaryAssignmentSource().toString());
            }

            hearing.save();
        }
    }

    @Handles("listing.events.judiciary-changed-for-hearing")
    public void judiciaryChangedForHearing(final Envelope<JudiciaryChangedForHearing> event) {
        final JudiciaryChangedForHearing judiciaryChangedForHearing  = event.payload();
        final UUID hearingId = judiciaryChangedForHearing.getHearingId();
        final List<JudicialRole> judicialRoles = judiciaryChangedForHearing.getJudiciary();

        final JsonNodeUpdater hearing = using(hearingRepository)
                .find(hearingId)
                .putObjectList(JUDICIARY, judicialRoles);

        if (judicialRoles.isEmpty()) {
            hearing.put(JUDICIARY_ASSIGNMENT_SOURCE, JudiciaryAssignmentSource.AUTO.toString());
        } else if (nonNull(judiciaryChangedForHearing.getJudiciaryAssignmentSource())) {
            hearing.put(JUDICIARY_ASSIGNMENT_SOURCE, judiciaryChangedForHearing.getJudiciaryAssignmentSource().toString());
        }

        hearing.save();
    }

    @Handles("listing.events.judiciary-removed-from-hearing")
    public void judiciaryRemovedFromHearing(final Envelope<JudiciaryRemovedFromHearing> event) {
        final JudiciaryRemovedFromHearing judiciaryRemovedFromHearing  = event.payload();
        final UUID hearingId = judiciaryRemovedFromHearing.getHearingId();

        using(hearingRepository)
                .find(hearingId)
                .remove(JUDICIARY)
                .putObjectList(JUDICIARY, new ArrayList<>())
                .put(JUDICIARY_ASSIGNMENT_SOURCE, JudiciaryAssignmentSource.AUTO.toString())
                .save();
    }
}
