package uk.gov.moj.cpp.listing.event.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.listing.events.AddedCasesForHearing;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

@ServiceComponent(Component.EVENT_LISTENER)
public class ExtendHearingForHearingListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendHearingForHearingListener.class);
    private HearingRepository hearingRepository;
    private static final String LISTED_CASES_FIELD = "listedCases";

    @Inject
    public ExtendHearingForHearingListener(final HearingRepository hearingRepository) {
        this.hearingRepository = hearingRepository;
    }

    @Handles("listing.events.hearing-deleted")
    public void hearingDeleted(final Envelope<HearingDeleted> event) {

        final HearingDeleted hearingDeleted = event.payload();
        final UUID hearingIdToBeDeleted = hearingDeleted.getHearingIdToBeDeleted();
        final Hearing hearingToBeDeleted = hearingRepository.findBy(hearingIdToBeDeleted);

        if(hearingToBeDeleted != null){
            hearingRepository.remove(hearingToBeDeleted);
            LOGGER.info("Hearing with id {} has been deleted ", hearingIdToBeDeleted);
        }
    }


    @Handles("listing.event.added-cases-for-hearing")
    public void hearingAddedCasesForHearing(final Envelope<AddedCasesForHearing> event) {
        final AddedCasesForHearing addedCasesForHearing = event.payload();
        final UUID hearingId = addedCasesForHearing.getHearingId();
        final List<ListedCase> listedCasesToAdd = addedCasesForHearing.getUnAllocatedListedCases();

        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };

        final Hearing hearing = hearingRepository.findBy(hearingId);

        if(null != hearing.getProperties().get(LISTED_CASES_FIELD)) {
            using(hearingRepository)
                    .find(hearingId)
                    .putSubList(LISTED_CASES_FIELD, typeRef, getListedCasesAddFunction(listedCasesToAdd))
                    .save();
            LOGGER.info("Hearing with id {} has been updated with new listed cases ", hearingId);
        }
    }

    private Function<List<ListedCase>, List<ListedCase>> getListedCasesAddFunction(final List<ListedCase> listedCasesToAdd) {
        return dbListedCases -> getListedCases(listedCasesToAdd, dbListedCases);
    }

    private List<ListedCase> getListedCases(final List<ListedCase> listedCasesToAdd,
                                            final List<ListedCase> dbListedCases) {
        dbListedCases.addAll(listedCasesToAdd);
        return dbListedCases;
    }

}
