package uk.gov.moj.cpp.listing.event.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.persistence.entity.CaseIdentifier;
import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.entity.LinkedCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;
import uk.gov.moj.cpp.listing.persistence.entity.Prosecutor;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import utils.JsonNodeReader;

public class HearingSearchSyncService {
    public static final String LISTED_CASES_FIELD = "listedCases";
    public static final String HEARING_DAYS_FIELD = "hearingDays";
    public static final String COURT_CENTRE_ID = "courtCentreId";
    public static final String COURT_ROOM_ID = "courtRoomId";
    public static final String IS_VACATED_TRIAL = "isVacatedTrial";
    public static final String UNSCHEDULED = "unscheduled";
    public static final String TYPE = "type";
    public static final String ID = "id";
    public static final String JURISDICTION_TYPE = "jurisdictionType";
    public static final String WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    public static final String WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String ALLOCATED = "allocated";
    public static final String COURT_APPLICATIONS = "courtApplications";
    public static final String TYPE_OF_LIST ="typeOfList";

    @Inject
    private HearingRepository hearingRepository;

    public void sync(final UUID hearingId){
        final Hearing hearing = hearingRepository.findBy(hearingId);
        syncEntity(hearing);
    }

    public void syncEntity(final Hearing hearing){
        final JsonNode properties = hearing.getProperties();
        final JsonNodeReader reader = JsonNodeReader.read(hearing.getProperties());
        hearing.setCourtCentreId(reader.getUUID(COURT_CENTRE_ID));
        hearing.setCourtRoomId(reader.getUUID(COURT_ROOM_ID));
        hearing.setIsVacatedTrial(reader.getBoolean(IS_VACATED_TRIAL));
        hearing.setUnscheduled(reader.getBoolean(UNSCHEDULED));
        if(nonNull(reader.get(TYPE))) {
            hearing.setTypeId(reader.get(TYPE).getUUID(ID));
        }
        hearing.setJurisdictionType(reader.getText(JURISDICTION_TYPE));
        hearing.setWeekCommencingStartDate(reader.getDate(WEEK_COMMENCING_START_DATE));
        hearing.setWeekCommencingEndDate(reader.getDate(WEEK_COMMENCING_END_DATE));
        hearing.setStartDate(reader.getDate(START_DATE));
        hearing.setEndDate(reader.getDate(END_DATE));
        hearing.setListedCases(getListedCases(properties.get(LISTED_CASES_FIELD), hearing));
        hearing.setHearingDays(getHearingDays(properties.get(HEARING_DAYS_FIELD), hearing));
        hearing.setCourtApplications(getCourtApplications(properties.get(COURT_APPLICATIONS),hearing));
        hearing.setAllocated(reader.getBoolean(ALLOCATED));
        if(nonNull(reader.get(TYPE_OF_LIST))){
            hearing.setTypeOfListId(reader.get(TYPE_OF_LIST).getUUID(ID));
        }
        hearingRepository.save(hearing);
    }

    private Set<ListedCases> getListedCases(final JsonNode listedCasesNode, final Hearing hearing){
        final Set<ListedCases> result = new HashSet<>();
        final int size = nonNull(listedCasesNode) ? listedCasesNode.size(): 0;

        for (int i = 0; i < size; i++){
            final UUID caseId = UUID.fromString(listedCasesNode.get(i).get("id").asText());
            final JsonNode caseIdentifierNode = listedCasesNode.get(i).get("caseIdentifier");
            final uk.gov.moj.cpp.listing.persistence.entity.CaseIdentifier caseIdentifier = new uk.gov.moj.cpp.listing.persistence.entity.CaseIdentifier();
            caseIdentifier.setAuthorityId(fromString(caseIdentifierNode.get("authorityId").asText()));
            caseIdentifier.setAuthorityCode(caseIdentifierNode.get("authorityCode").asText());
            caseIdentifier.setCaseReference(caseIdentifierNode.get("caseReference").asText());

            result.add(getListedCase(listedCasesNode, hearing, i, caseId, caseIdentifier));
        }

        return result;
    }

    private ListedCases getListedCase(final JsonNode listedCasesNode, final Hearing hearing, final int i, final UUID caseId, final CaseIdentifier caseIdentifier) {
        final Iterator<JsonNode> defendants = listedCasesNode.get(i).has("defendants") ? listedCasesNode.get(i).get("defendants").iterator() : Collections.emptyIterator();
        final Iterator<JsonNode> linkedCases = listedCasesNode.get(i).has("linkedCases") ? listedCasesNode.get(i).get("linkedCases").iterator() : Collections.emptyIterator();

        final ListedCases listedCase = new ListedCases(randomUUID(), caseId, caseIdentifier, null, hearing, null, null);

        if (listedCasesNode.get(i).has("prosecutor")) {
            final JsonNode prosecutorNode = listedCasesNode.get(i).get("prosecutor");
            final Prosecutor prosecutor = new Prosecutor();
            prosecutor.setProsecutorId(fromString(prosecutorNode.get("prosecutorId").asText()));
            prosecutor.setProsecutorCode(prosecutorNode.get("prosecutorCode").asText());
            listedCase.setProsecutor(prosecutor);
        }


        final Set<LinkedCase> allLinkedCases = new HashSet<>();
        final Set<Defendant> allDefendants = new HashSet<>();
        while (linkedCases.hasNext()) {
            final JsonNode linkedCase = linkedCases.next();
            final LinkedCase linkCase = new LinkedCase();
            linkCase.setId(randomUUID());
            linkCase.setCaseId(fromString(linkedCase.get("caseId").asText()));
            linkCase.setCaseUrn(linkedCase.get("caseUrn").asText());
            linkCase.setListedCase(listedCase);

            allLinkedCases.add(linkCase);
        }

        while (defendants.hasNext()) {
            final JsonNode defendantNode = defendants.next();
            final Defendant defendant = new Defendant();
            defendant.setId(randomUUID());
            defendant.setDefendantId(fromString(defendantNode.get("id").asText()));
            defendant.setMasterDefendantId(ofNullable(defendantNode.get("masterDefendantId")).map(JsonNode::asText).map(UUID::fromString).orElse(null));
            defendant.setListedCase(listedCase);

            allDefendants.add(defendant);
        }

        listedCase.setLinkedCases(allLinkedCases);
        listedCase.setDefendants(allDefendants);
        return listedCase;
    }

    private Set<HearingDays> getHearingDays(final JsonNode hearingDaysNode, final Hearing hearing){
        final Set<HearingDays> result = new HashSet<>();
        final int size = nonNull(hearingDaysNode) ? hearingDaysNode.size(): 0;

        for (int i = 0; i < size; i++){
            final JsonNodeReader reader = JsonNodeReader.read(hearingDaysNode.get(i));
            final HearingDays hearingDays = new HearingDays();
            hearingDays.setId(randomUUID());
            hearingDays.setSequence(reader.getInteger("sequence"));
            hearingDays.setStartTime(reader.getZonedDateTime("startTime"));
            hearingDays.setEndTime(reader.getZonedDateTime("endTime"));
            hearingDays.setCourtRoomId(reader.getUUID(COURT_ROOM_ID));
            hearingDays.setHearingDate(reader.getDate("hearingDate"));
            hearingDays.setCourtCentreId(reader.getUUID(COURT_CENTRE_ID));
            hearingDays.setDurationMinutes(reader.getInteger("durationMinutes"));
            hearingDays.setHearing(hearing);
            result.add(hearingDays);
        }

        return result;
    }

    private Set<CourtApplications> getCourtApplications(final JsonNode courtApplicationsNode, final Hearing hearing){
        final Set<CourtApplications> result = new HashSet<>();
        final int size = nonNull(courtApplicationsNode) ? courtApplicationsNode.size(): 0;

        for (int i = 0; i < size; i++){
            final JsonNodeReader reader = JsonNodeReader.read(courtApplicationsNode.get(i));
            final CourtApplications courtApplications = new CourtApplications();
            courtApplications.setId(randomUUID());
            courtApplications.setApplicationId(UUID.fromString(reader.getText("id")));
            courtApplications.setApplicationType(reader.getText("applicationType"));
            courtApplications.setParentApplicationId(reader.getUUID("parentApplicationId"));
            courtApplications.setApplicationReference(reader.getText("applicationReference"));
            courtApplications.setApplicationParticulars(reader.getText("applicationParticulars"));
            courtApplications.setHearing(hearing);
            result.add(courtApplications);
        }

        return result;
    }
}
