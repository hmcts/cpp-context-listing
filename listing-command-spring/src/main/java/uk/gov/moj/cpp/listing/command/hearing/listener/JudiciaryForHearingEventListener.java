package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JudiciaryAssignedToHearing;
import uk.gov.justice.listing.events.JudiciaryAssignmentSource;
import uk.gov.justice.listing.events.JudiciaryChangedForHearing;
import uk.gov.justice.listing.events.JudiciaryRemovedFromHearing;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Component
public class JudiciaryForHearingEventListener implements ListenerPort {

    private static final String JUDICIARY = "judiciary";
    private static final String JUDICIARY_ASSIGNMENT_SOURCE = "judiciaryAssignmentSource";

    private HearingRepository hearingRepository;
    public JudiciaryForHearingEventListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(JudiciaryAssignedToHearing.class, this::judiciaryAssignedToHearing)
                .on(JudiciaryChangedForHearing.class, this::judiciaryChangedForHearing)
                .on(JudiciaryRemovedFromHearing.class, this::judiciaryRemovedFromHearing);
    }

    public void judiciaryAssignedToHearing(final RaisedEvent<JudiciaryAssignedToHearing> event) {
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
    public void judiciaryChangedForHearing(final RaisedEvent<JudiciaryChangedForHearing> event) {
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
    public void judiciaryRemovedFromHearing(final RaisedEvent<JudiciaryRemovedFromHearing> event) {
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
