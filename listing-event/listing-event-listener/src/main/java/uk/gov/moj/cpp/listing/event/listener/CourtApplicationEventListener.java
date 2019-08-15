package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtApplicationUpdatedForHearing;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Iterables;

@ServiceComponent(Component.EVENT_LISTENER)
public class CourtApplicationEventListener {


    private static final String COURT_APPLICATION_FIELD = "courtApplications";

    @Inject
    private HearingRepository hearingRepository;

    @Handles("listing.events.court-application-added-for-hearing")
    public void courtApplicationAdded(final Envelope<CourtApplicationAddedForHearing> event) {
        final CourtApplicationAddedForHearing addedCourtapplicationToHearing = event.payload();
        final UUID hearingId = addedCourtapplicationToHearing.getHearingId();
        final CourtApplication courtApplication = addedCourtapplicationToHearing.getCourtApplication();

        final TypeReference<List<CourtApplication>> typeRef = new TypeReference<List<CourtApplication>>() {
        };

        using(hearingRepository)
                .find(hearingId)
                .putSubList(COURT_APPLICATION_FIELD, typeRef, getCourtApplicationsAddFunction(courtApplication))
                .save();
    }

    @Handles("listing.events.court-application-updated-for-hearing")
    public void courtApplicationUpdated(final Envelope<CourtApplicationUpdatedForHearing> event) {
        final CourtApplicationUpdatedForHearing courtApplicationUpdatedForHearing = event.payload();
        final UUID hearingId = courtApplicationUpdatedForHearing.getHearingId();
        final CourtApplication courtApplication = courtApplicationUpdatedForHearing.getCourtApplication();

        final TypeReference<List<CourtApplication>> typeRef = new TypeReference<List<CourtApplication>>() {
        };

        using(hearingRepository)
                .find(hearingId)
                .putSubList(COURT_APPLICATION_FIELD, typeRef, getCourtApplicationsFunction(courtApplication))
                .save();
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationsFunction(CourtApplication courtApplication) {
        return courtApplications -> getUpdatedCourtApplications(courtApplication, courtApplications);
    }

    private List<CourtApplication> getUpdatedCourtApplications(CourtApplication updateCourtApplication,
                                                               List<CourtApplication> courtApplications) {

        final CourtApplication origCourtApplication = Iterables.find(courtApplications, ca -> ca.getId()
                .equals(updateCourtApplication.getId()));
        final CourtApplication newCourtApplication = CourtApplication.courtApplication()
                .withApplicant(updateCourtApplication.getApplicant())
                .withRespondents(updateCourtApplication.getRespondents())
                .withApplicationType(updateCourtApplication.getApplicationType())
                .withId(origCourtApplication.getId())
                .withParentApplicationId(updateCourtApplication.getParentApplicationId())
                .withLinkedCaseId(updateCourtApplication.getLinkedCaseId())
                .withRestrictCourtApplicationType(updateCourtApplication.getRestrictCourtApplicationType())
                .withRestrictFromCourtList(updateCourtApplication.getRestrictFromCourtList())
                .withApplicationReference(updateCourtApplication.getApplicationReference())
                .build();

        courtApplications.replaceAll(courtApplication -> courtApplication.getId()
                .equals(newCourtApplication.getId()) ? newCourtApplication : courtApplication);
        return courtApplications;
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationsAddFunction(CourtApplication courtApplication) {
        return courtApplications -> getCourtApplications(courtApplication, courtApplications);
    }

    private List<CourtApplication> getCourtApplications(CourtApplication addCourtApplication,
                                                        List<CourtApplication> courtApplications) {

        courtApplications.add(addCourtApplication);
        return courtApplications;
    }


}
