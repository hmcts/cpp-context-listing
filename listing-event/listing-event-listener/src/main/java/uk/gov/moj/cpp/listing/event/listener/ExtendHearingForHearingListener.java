package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.AddedCasesForHearing;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Defendants;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.HearingPartiallyUpdated;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.Offences;
import uk.gov.justice.listing.events.ProsecutionCases;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_LISTENER)
public class ExtendHearingForHearingListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendHearingForHearingListener.class);
    private final HearingRepository hearingRepository;
    private static final String LISTED_CASES_FIELD = "listedCases";
    private final JsonObjectToObjectConverter jsonObjectConverter;
    private final ObjectMapper objectMapper;
    private HearingSearchSyncService hearingSearchSyncService;

    private OffenceComparator offenceComparator;

    @Inject
    public ExtendHearingForHearingListener(final HearingRepository hearingRepository, final JsonObjectToObjectConverter jsonObjectConverter, final ObjectMapper objectMapper,
                                           final HearingSearchSyncService hearingSearchSyncService,
                                           final OffenceComparator offenceComparator) {
        this.hearingRepository = hearingRepository;
        this.jsonObjectConverter = jsonObjectConverter;
        this.objectMapper = objectMapper;
        this.hearingSearchSyncService = hearingSearchSyncService;
        this.offenceComparator = offenceComparator;
    }

    @Handles("listing.events.hearing-deleted")
    public void hearingDeleted(final Envelope<HearingDeleted> event) {

        final HearingDeleted hearingDeleted = event.payload();
        final UUID hearingIdToBeDeleted = hearingDeleted.getHearingIdToBeDeleted();
        final Hearing hearingToBeDeleted = hearingRepository.findBy(hearingIdToBeDeleted);

        if (Objects.isNull(hearingToBeDeleted)) {
            LOGGER.error("Hearing with id {} is not found. It could be deleted before the fix for DD-14484. " +
                    "Please re-run the DLQ events if you see this message when doing a replay/catch up.", hearingIdToBeDeleted);
            throw new EntityNotFoundException("Failed to delete hearing " + hearingIdToBeDeleted);
        } else {
            hearingRepository.remove(hearingToBeDeleted);
            LOGGER.info("Hearing with id {} has been deleted ", hearingIdToBeDeleted);
        }
    }

    @Handles("listing.events.hearing-partially-updated")
    public void hearingPartiallyUpdated(final Envelope<HearingPartiallyUpdated> event) {

        final HearingPartiallyUpdated hearingPartiallyUpdated = event.payload();
        final UUID hearingIdToBeUpdated = hearingPartiallyUpdated.getHearingIdToBeUpdated();
        final Hearing hearingToBeUpdated = hearingRepository.findBy(hearingIdToBeUpdated);
        final List<ProsecutionCases> casesToRemove = hearingPartiallyUpdated.getProsecutionCases();

        if (hearingToBeUpdated != null) {

            if (null != hearingToBeUpdated.getProperties().get(LISTED_CASES_FIELD)) {

                final JsonNode jsonNode = hearingToBeUpdated.getProperties().get(LISTED_CASES_FIELD);

                final List<ListedCase> storedListCases = new ArrayList<>();

                //build ListedCase list from db
                jsonNode.forEach(node ->
                        storedListCases.add(extractListedCases(objectMapper, node, hearingIdToBeUpdated.toString()))
                );

                //remove cases in the request from persisted hearing
                removeCasesFromPersistedHearing(casesToRemove, storedListCases);

                using(hearingRepository)
                        .find(hearingIdToBeUpdated)
                        .remove(LISTED_CASES_FIELD)
                        .putObjectList(LISTED_CASES_FIELD, storedListCases)
                        .save();
                LOGGER.info("Hearing with id {} has been updated", hearingIdToBeUpdated);
                LOGGER.info("Hearing with id {} is now as {}", hearingIdToBeUpdated, hearingToBeUpdated);
                hearingSearchSyncService.sync(hearingIdToBeUpdated);

            } else {
                LOGGER.error("Hearing with id {} does not contain any listed cases to update.", hearingIdToBeUpdated);
            }


        } else {
            LOGGER.error("Hearing with id {} not found", hearingIdToBeUpdated);
            throw new EntityNotFoundException("Failed to update hearing " + hearingIdToBeUpdated);
        }
    }

    /**
     * This method handles 'listing.event.added-cases-for-hearing'. The event
     * 'listing.event.added-cases-for-hearing' has been renamed to 'listing.event.cases-added-to-hearing'
     * and is handled in this method {@link ExtendHearingForHearingListener#handleCasesAddedToHearingEvent(Envelope)}
     *
     * @param event listing.event.added-cases-for-hearing
     */
    @Handles("listing.event.added-cases-for-hearing")
    public void hearingAddedCasesForHearing(final Envelope<AddedCasesForHearing> event) {
        final AddedCasesForHearing addedCasesForHearing = event.payload();
        updateHearingWithCases(addedCasesForHearing.getHearingId(), addedCasesForHearing.getUnAllocatedListedCases());
    }

    @Handles("listing.event.cases-added-to-hearing")
    public void handleCasesAddedToHearingEvent(final Envelope<CasesAddedToHearing> event) {
        final CasesAddedToHearing casesAddedToHearing = event.payload();
        updateHearingWithCases(casesAddedToHearing.getHearingId(), casesAddedToHearing.getUnAllocatedListedCases());
    }

    private void updateHearingWithCases(final UUID hearingId, final List<ListedCase> listedCasesToAdd) {
        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };

        final Hearing hearing = hearingRepository.findBy(hearingId);

        if (hearing != null && hearing.getProperties() != null && null != hearing.getProperties().get(LISTED_CASES_FIELD)) {
            using(hearingRepository)
                    .find(hearingId)
                    .putSubList(LISTED_CASES_FIELD, typeRef, getListedCasesAddFunction(listedCasesToAdd))
                    .save();
            LOGGER.info("Hearing with id {} has been updated with new listed cases ", hearingId);
            hearingSearchSyncService.sync(hearingId);
        }
    }

    private Function<List<ListedCase>, List<ListedCase>> getListedCasesAddFunction(final List<ListedCase> listedCasesToAdd) {
        return dbListedCases -> updateListedCases(listedCasesToAdd, dbListedCases);
    }

    private List<ListedCase> updateListedCases(final List<ListedCase> listedCasesToAdd,
                                               final List<ListedCase> dbListedCases) {

        //create map from dbListedCases to compare against given request
        //but we will still modify the dbListedCases list
        final Map<UUID, Map<UUID, List<Offence>>> dbCaseDefendantOffenceMap = dbListedCases.stream()
                .collect(Collectors.toMap(
                        ListedCase::getId,
                        listedCase -> listedCase.getDefendants().stream()
                                .collect(Collectors.toMap(
                                        Defendant::getId,
                                        Defendant::getOffences,
                                        (existing, replacement) -> existing
                                ))
                ));

        listedCasesToAdd.forEach(c -> { //iterate over the requested cases
            if (dbCaseDefendantOffenceMap.containsKey(c.getId())) { //if dbListedCases already has the same case
                c.getDefendants().forEach(d -> {
                    if (dbCaseDefendantOffenceMap.get(c.getId()).containsKey(d.getId())) { //if dbListedCases already has the same defendant
                        compareCases(dbListedCases, c, d);
                    } else { //if dbListedCase already has the case but not the defendant
                        addDefendants(dbListedCases, c, d);
                    }
                });
            } else { // if caseToAdd is not already in dbListedCases, add it directly
                dbListedCases.add(c);
            }
        });

        return dbListedCases;
    }

    /**
     * for each matching case in the db update matching defendant offences if the passed in
     * offence has listing number greater than or equal to the db version or add the passed in offence
     * if there is no version of the offence in db then
     *
     * @param dbListedCases
     * @param listedCase
     * @param defendant
     */
    private void compareCases(final List<ListedCase> dbListedCases, final ListedCase listedCase, final Defendant defendant) {

        dbListedCases.stream().filter(dbListedCase -> dbListedCase.getId().equals(listedCase.getId())).forEach(dbListedCase ->
                dbListedCase.getDefendants().stream().filter(dbListedDefendant -> dbListedDefendant.getId().equals(defendant.getId())).forEach(dbListedDefendant ->
                        defendant.getOffences().forEach(defendantOffence -> {
                                    final Offence latestVersionOfOffence = offenceComparator.getLatestVersionOfOffence(defendantOffence, dbListedDefendant.getOffences());
                                    dbListedDefendant.getOffences().removeIf(dbOffence -> dbOffence.getId().equals(defendantOffence.getId()));
                                    dbListedDefendant.getOffences().add(latestVersionOfOffence);
                                }
                        ))
        );
    }

    private void addDefendants(final List<ListedCase> dbListedCases, final ListedCase listedCase, final Defendant defendant) {

        filterDuplicateOffencesById(defendant.getOffences());
        dbListedCases.stream().filter(dbListedCase -> dbListedCase.getId().equals(listedCase.getId())).forEach(dbListedCase ->
                dbListedCase.getDefendants().add(defendant));
    }

    private  void filterDuplicateOffencesById(final List<uk.gov.justice.listing.events.Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
    }

    private ListedCase extractListedCases(final ObjectMapper objectMapper,
                                          final JsonNode node, final String hearingIdToBeUpdated) {
        final JsonObject hearingJson;
        String valueAsString = "";
        try {
            valueAsString = objectMapper.writeValueAsString(node);
        } catch (final JsonProcessingException jpe) {
            LOGGER.error("Hearing with id {} could not be updated. Could not parse stored hearing json.", hearingIdToBeUpdated, jpe);
        }
        try (final JsonReader reader = Json.createReader(new StringReader(valueAsString))) {
            hearingJson = reader.readObject();
        }
        return jsonObjectConverter.convert(hearingJson, ListedCase.class);
    }

    @SuppressWarnings({"squid:S3776", "squid:S134"})
    private void removeCasesFromPersistedHearing(final List<ProsecutionCases> casesToRemove,
                                                 final List<ListedCase> storedListCases) {
        for (final ProsecutionCases pc : casesToRemove) {//cases in the request
            for (final Defendants defendant : pc.getDefendants()) {//defendants in the request
                for (final Offences offence : defendant.getOffences()) {//offences in the request

                    storedListCases.forEach(lc -> { //traverse persisted cases
                        if (lc.getId().toString().equals(pc.getCaseId().toString())) {
                            lc.getDefendants().forEach(d -> { //traverse persisted defendants
                                if (d.getId().toString().equals(defendant.getDefendantId().toString())) {
                                    d.getOffences().removeIf(o -> o.getId().toString().equals(offence.getOffenceId().toString())); //remove persisted offence from persisted defendant
                                }
                            });//end traverse persisted defendants
                            lc.getDefendants().removeIf(d -> d.getOffences().isEmpty()); //remove persisted defendant if all persisted offences deleted
                        }
                    });//end traverse persisted cases
                    storedListCases.removeIf(lc -> lc.getDefendants().isEmpty());//remove case if all defendants removed
                }
            }
        }
    }

}
