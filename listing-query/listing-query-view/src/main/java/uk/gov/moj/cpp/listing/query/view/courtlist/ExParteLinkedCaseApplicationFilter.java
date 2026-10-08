package uk.gov.moj.cpp.listing.query.view.courtlist;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes applications linked to an ex-parte civil case that is not itself on the court list.
 * <p>
 * Ex-parte cases on the list, and applications linked to them, are already removed by
 * {@link uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases#filterExParteOffencesFromHearings},
 * which runs first. A linked case off the list is resolved from the hearings it is listed on in
 * Listing; only a case Listing has never listed is fetched from Progression. A case Progression does
 * not have is treated as not ex-parte, while a failed Progression lookup removes the application
 * (fail-closed).
 */
@ApplicationScoped
public class ExParteLinkedCaseApplicationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExParteLinkedCaseApplicationFilter.class);

    private static final String HEARINGS = "hearings";
    private static final String LISTED_CASES = "listedCases";
    private static final String COURT_APPLICATIONS = "courtApplications";
    private static final String DEFENDANTS = "defendants";
    private static final String OFFENCES = "offences";
    private static final String CIVIL_OFFENCE = "civilOffence";
    private static final String IS_EX_PARTE = "isExParte";
    private static final String ID = "id";
    private static final String LINKED_CASE_IDS = "linkedCaseIds";
    private static final String LINKED_CASE_ID = "linkedCaseId";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ProgressionService progressionService;

    public JsonObject removeApplicationsLinkedToExParteCases(final JsonObject rangeSearchResponsePayload, final JsonEnvelope envelope) {
        if (isNull(rangeSearchResponsePayload) || !(rangeSearchResponsePayload.get(HEARINGS) instanceof JsonArray)) {
            return rangeSearchResponsePayload;
        }

        final List<JsonObject> hearings = rangeSearchResponsePayload.getJsonArray(HEARINGS).getValuesAs(JsonObject.class);
        final Set<String> linkedCaseIdsOffTheList = linkedCaseIdsOffTheList(hearings);
        if (linkedCaseIdsOffTheList.isEmpty()) {
            return rangeSearchResponsePayload;
        }
        final Set<String> exParteCaseIds = exParteCaseIdsAmong(linkedCaseIdsOffTheList, envelope);
        if (exParteCaseIds.isEmpty()) {
            return rangeSearchResponsePayload;
        }

        final JsonArrayBuilder hearingsBuilder = createArrayBuilder();
        hearings.forEach(hearing -> {
            final JsonObject filteredHearing = filterHearing(hearing, exParteCaseIds);
            if (nonNull(filteredHearing)) {
                hearingsBuilder.add(filteredHearing);
            }
        });

        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        rangeSearchResponsePayload.forEach((key, value) -> payloadBuilder.add(key, HEARINGS.equals(key) ? hearingsBuilder.build() : value));
        return payloadBuilder.build();
    }

    private static Set<String> linkedCaseIdsOffTheList(final List<JsonObject> hearings) {
        final Set<String> caseIdsOnTheList = new HashSet<>();
        final Set<String> linkedCaseIds = new HashSet<>();
        hearings.forEach(hearing -> {
            objectsIn(hearing, LISTED_CASES).forEach(listedCase -> caseIdsOnTheList.add(listedCase.getString(ID, null)));
            objectsIn(hearing, COURT_APPLICATIONS).forEach(courtApplication -> linkedCaseIds.addAll(linkedCaseIdsOf(courtApplication)));
        });
        linkedCaseIds.removeAll(caseIdsOnTheList);
        return linkedCaseIds;
    }

    private Set<String> exParteCaseIdsAmong(final Set<String> linkedCaseIds, final JsonEnvelope envelope) {
        final Set<String> exParteCaseIds = new HashSet<>();
        final Set<String> caseIdsKnownToListing = new HashSet<>();

        final List<UUID> caseIds = linkedCaseIds.stream().map(UUID::fromString).toList();
        final List<Hearing> hearingsOfLinkedCases = hearingRepository.findHearingsByListedCaseIds(caseIds);
        if (nonNull(hearingsOfLinkedCases)) {
            hearingsOfLinkedCases.stream()
                    .map(Hearing::getProperties)
                    .filter(Objects::nonNull)
                    .forEach(properties -> properties.path(LISTED_CASES).forEach(listedCase -> {
                        final String caseId = listedCase.path(ID).asText(null);
                        if (linkedCaseIds.contains(caseId)) {
                            caseIdsKnownToListing.add(caseId);
                            if (hasExParteOffence(listedCase)) {
                                exParteCaseIds.add(caseId);
                            }
                        }
                    }));
        }

        linkedCaseIds.stream()
                .filter(caseId -> !caseIdsKnownToListing.contains(caseId))
                .filter(caseId -> isExParteInProgression(caseId, envelope))
                .forEach(exParteCaseIds::add);

        if (!exParteCaseIds.isEmpty()) {
            LOGGER.info("Removing applications linked to ex-parte civil cases {} from the court list", exParteCaseIds);
        }
        return exParteCaseIds;
    }

    private boolean isExParteInProgression(final String caseId, final JsonEnvelope envelope) {
        final Optional<ProsecutionCase> prosecutionCase;
        try {
            prosecutionCase = progressionService.findProsecutionCaseByCaseId(envelope, caseId);
        } catch (final RuntimeException e) {
            LOGGER.warn("Unable to resolve ex-parte status of linked case {} from Progression; removing its applications from the court list", caseId, e);
            return true;
        }
        if (prosecutionCase.isEmpty()) {
            LOGGER.info("Linked case {} not found in Progression; treating it as not ex-parte", caseId);
            return false;
        }
        return nonNull(prosecutionCase.get().getDefendants()) && prosecutionCase.get().getDefendants().stream()
                .map(Defendant::getOffences)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .anyMatch(ExParteLinkedCaseApplicationFilter::isExParte);
    }

    private static JsonObject filterHearing(final JsonObject hearing, final Set<String> exParteCaseIds) {
        final List<JsonObject> courtApplications = objectsIn(hearing, COURT_APPLICATIONS);
        final List<JsonObject> keptCourtApplications = courtApplications.stream()
                .filter(courtApplication -> linkedCaseIdsOf(courtApplication).stream().noneMatch(exParteCaseIds::contains))
                .toList();

        if (keptCourtApplications.size() == courtApplications.size()) {
            return hearing;
        }
        if (keptCourtApplications.isEmpty() && objectsIn(hearing, LISTED_CASES).isEmpty()) {
            return null;
        }

        final JsonArrayBuilder applicationsBuilder = createArrayBuilder();
        keptCourtApplications.forEach(applicationsBuilder::add);
        final JsonArray keptCourtApplicationsArray = applicationsBuilder.build();

        final JsonObjectBuilder hearingBuilder = createObjectBuilder();
        hearing.forEach((key, value) -> hearingBuilder.add(key, COURT_APPLICATIONS.equals(key) ? keptCourtApplicationsArray : value));
        return hearingBuilder.build();
    }

    private static boolean hasExParteOffence(final JsonNode listedCase) {
        final List<JsonNode> offences = new ArrayList<>();
        listedCase.path(OFFENCES).forEach(offences::add);
        listedCase.path(DEFENDANTS).forEach(defendant -> defendant.path(OFFENCES).forEach(offences::add));
        return offences.stream().anyMatch(offence -> offence.path(CIVIL_OFFENCE).path(IS_EX_PARTE).asBoolean(false));
    }

    private static boolean isExParte(final Offence offence) {
        return nonNull(offence.getCivilOffence()) && Boolean.TRUE.equals(offence.getCivilOffence().getIsExParte());
    }

    private static Set<String> linkedCaseIdsOf(final JsonObject courtApplication) {
        final Set<String> linkedCaseIds = new HashSet<>();
        if (courtApplication.get(LINKED_CASE_IDS) instanceof JsonArray linkedCaseIdArray) {
            linkedCaseIdArray.stream()
                    .filter(JsonString.class::isInstance)
                    .map(linkedCaseId -> ((JsonString) linkedCaseId).getString())
                    .forEach(linkedCaseIds::add);
        }
        if (courtApplication.get(LINKED_CASE_ID) instanceof JsonString linkedCaseId) {
            linkedCaseIds.add(linkedCaseId.getString());
        }
        return linkedCaseIds;
    }

    private static List<JsonObject> objectsIn(final JsonObject parent, final String arrayName) {
        if (parent.get(arrayName) instanceof JsonArray array) {
            return array.stream()
                    .filter(value -> value.getValueType() == JsonValue.ValueType.OBJECT)
                    .map(JsonObject.class::cast)
                    .toList();
        }
        return List.of();
    }
}
