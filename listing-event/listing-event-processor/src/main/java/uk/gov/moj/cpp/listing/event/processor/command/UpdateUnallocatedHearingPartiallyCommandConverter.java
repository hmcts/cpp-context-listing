package uk.gov.moj.cpp.listing.event.processor.command;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.listing.events.HearingMarkedForPartialUpdate;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class UpdateUnallocatedHearingPartiallyCommandConverter {

    private static final String HEARING_ID_TO_BE_UPDATED = "hearingIdToBeUpdated";
    private static final String OFFENCE_ID = "offenceId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String OFFENCES_TO_REMOVE = "offencesToRemove";
    private static final String DEFENDANTS_TO_REMOVE = "defendantsToRemove";
    private static final String CASE_ID = "caseId";
    private static final String PROSECUTION_CASES_TO_REMOVE = "prosecutionCasesToRemove";

    public JsonObject convertPartialUpdateEventToCommand(final HearingMarkedForPartialUpdate hearingMarkedForPartialUpdateEvent) {

        final JsonObjectBuilder commandBuilder = createObjectBuilder().add(HEARING_ID_TO_BE_UPDATED, hearingMarkedForPartialUpdateEvent.getHearingIdToBeUpdated().toString());

        final JsonArrayBuilder casesArrayBuilder = createArrayBuilder();

        hearingMarkedForPartialUpdateEvent.getProsecutionCases().forEach(pc -> {

            final JsonObjectBuilder caseObjectBuilder = createObjectBuilder();

            final JsonArrayBuilder defendantsArrayBuilder = createArrayBuilder();
            pc.getDefendants().forEach(d -> {

                final JsonObjectBuilder defendantObjectBuilder = createObjectBuilder();
                final JsonArrayBuilder offencesArrayBuilder = createArrayBuilder();

                d.getOffences().forEach(o ->
                        offencesArrayBuilder.add(createObjectBuilder().add(OFFENCE_ID, o.getOffenceId().toString()))
                );

                defendantsArrayBuilder.add(
                        defendantObjectBuilder.add(DEFENDANT_ID, d.getDefendantId().toString())
                                .add(OFFENCES_TO_REMOVE, offencesArrayBuilder.build()));

            });

            casesArrayBuilder.add(caseObjectBuilder.add(CASE_ID, pc.getCaseId().toString())
                    .add(DEFENDANTS_TO_REMOVE, defendantsArrayBuilder.build()));

        });

        return commandBuilder.add(PROSECUTION_CASES_TO_REMOVE, casesArrayBuilder).build();
    }
}