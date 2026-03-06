package uk.gov.moj.cpp.listing.event.service;

import static java.util.Arrays.asList;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.COURT_APPLICATIONS;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.COURT_CENTRE_ID;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.COURT_ROOM_ID;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.END_DATE;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.GROUP_ID;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.HEARING_DAYS_FIELD;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.ID;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.IS_CIVIL;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.IS_GROUP_MASTER;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.IS_GROUP_MEMBER;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.IS_GROUP_PROCEEDINGS;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.IS_VACATED_TRIAL;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.LISTED_CASES_FIELD;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.START_DATE;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.TYPE;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.TYPE_OF_LIST;

import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.entity.LinkedCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonWriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingSearchSyncServiceTest {


    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private HearingSearchSyncService hearingSearchSyncService;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    private UUID hearingId = UUID.randomUUID();
    private UUID courtCentreId = UUID.randomUUID();
    private UUID courtRoomId = UUID.randomUUID();
    private UUID typeId = UUID.randomUUID();
    private UUID caseId1 = UUID.randomUUID();
    private UUID caseId2 = UUID.randomUUID();
    private UUID authorityId1 = UUID.randomUUID();
    private UUID authorityId2 = UUID.randomUUID();
    private String authCode1 = "AUTH_CODE_1";
    private String caseRef1 = "case_ref_1";
    private String authCode2 = "AUTH_CODE_2";
    private String caseRef2 = "case_ref_2";
    private String startDate = "2021-03-01";
    private String endDate = "2021-03-02";
    private UUID typeOfListId = UUID.randomUUID();
    private UUID prosecutorId1 = UUID.randomUUID();
    private String prosecutorCode1 = "PROSECUTOR_CODE1";
    private UUID prosecutorId2 = UUID.randomUUID();
    private String prosecutorCode2 = "PROSECUTOR_CODE2";
    private UUID caseId3 = UUID.randomUUID();
    private UUID caseId4 = UUID.randomUUID();
    private UUID caseId5 = UUID.randomUUID();
    private UUID caseId6 = UUID.randomUUID();
    private UUID defendantId1 = UUID.randomUUID();
    private UUID defendantId2 = UUID.randomUUID();
    private UUID defendantId3 = UUID.randomUUID();
    private UUID defendantId4 = UUID.randomUUID();
    private UUID masterDefendantId1 = UUID.randomUUID();
    private UUID masterDefendantId2 = UUID.randomUUID();
    private UUID masterDefendantId3 = UUID.randomUUID();
    private UUID masterDefendantId4 = UUID.randomUUID();
    private UUID applicationId1 = UUID.randomUUID();
    private UUID parentApplicationId1 = UUID.randomUUID();

    @Test
    public void sync() throws IOException {

        when(hearingRepository.findBy(eq(hearingId))).thenReturn(buildHearing(hearingId));
        hearingSearchSyncService.sync(hearingId);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());
        final Hearing actual = hearingArgumentCaptor.getValue();

        assertHearing(actual);

        final Iterator<CourtApplications> iterator = actual.getCourtApplications().iterator();
        CourtApplications courtApplication = iterator.next();
        assertCourtApplications(courtApplication);

        final ListedCases listedCases1 = actual.getListedCases().stream().filter(c -> c.getCaseId().equals(caseId1)).findFirst().orElse(null);
        assertListedCases(listedCases1);

        final List<String> caseUrns1 = listedCases1.getLinkedCases()
                .stream()
                .map(LinkedCase::getCaseUrn)
                .collect(Collectors.toList());

        assertThat(caseUrns1, hasItems("caseUrn1", "caseUrn2"));


        final Iterator<Defendant> iterator3 = listedCases1.getDefendants().iterator();
        Defendant defendant = iterator3.next();
        assertDefendant(defendant);

        defendant = iterator3.next();
        assertDefendant(defendant);


        final ListedCases listedCases2 = actual.getListedCases().stream().filter(c -> c.getCaseId().equals(caseId2)).findFirst().orElse(null);
        assertThat(listedCases2, is(notNullValue()));
        assertThat(listedCases2.getCaseIdentifier().getAuthorityCode(), is(authCode2));
        assertThat(listedCases2.getCaseIdentifier().getCaseReference(), is(caseRef2.toUpperCase(Locale.ENGLISH)));
        assertThat(listedCases2.getProsecutor().getProsecutorId(), is(prosecutorId2));
        assertThat(listedCases2.getProsecutor().getProsecutorCode(), is(prosecutorCode2));
        final List<String> caseUrns2 = listedCases2.getLinkedCases()
                .stream()
                .map(LinkedCase::getCaseUrn)
                .collect(Collectors.toList());
        assertThat(caseUrns2, hasItems("caseUrn3", "caseUrn4"));

        final Iterator<Defendant> iterator4 = listedCases2.getDefendants().iterator();
        defendant = iterator4.next();
        assertDefendant(defendant);

        defendant = iterator4.next();
        assertDefendant(defendant);

        assertThat(actual.getHearingDays().size(), is(2));
        final HearingDays hearingDays1 = actual.getHearingDays().stream().filter(h -> h.getSequence() == 1).findFirst().orElse(null);
        assertThat(hearingDays1, is(notNullValue()));

        final HearingDays hearingDays2 = actual.getHearingDays().stream().filter(h -> h.getSequence() == 2).findFirst().orElse(null);
        assertThat(hearingDays2, is(notNullValue()));
    }

    private static void assertDefendant(final Defendant defendant) {
        assertNotNull(defendant.getId());
        assertNotNull(defendant.getMasterDefendantId());
    }

    private void assertListedCases(final ListedCases listedCases1) {
        assertThat(listedCases1, is(notNullValue()));
        assertThat(listedCases1.getCaseIdentifier().getAuthorityId(), is(authorityId1));
        assertThat(listedCases1.getCaseIdentifier().getAuthorityCode(), is(authCode1));
        assertThat(listedCases1.getCaseIdentifier().getCaseReference(), is(caseRef1.toUpperCase(Locale.ENGLISH)));
        assertThat(listedCases1.getProsecutor().getProsecutorId(), is(prosecutorId1));
        assertThat(listedCases1.getProsecutor().getProsecutorCode(), is(prosecutorCode1));
    }

    private void assertCourtApplications(final CourtApplications courtApplication) {
        assertThat(courtApplication.getApplicationId(), is(applicationId1));
        assertThat(courtApplication.getApplicationType(), is("applicationType"));
        assertThat(courtApplication.getParentApplicationId(), is(parentApplicationId1));
        assertThat(courtApplication.getApplicationReference(), is("APPLICATIONREFERENCE"));
        assertThat(courtApplication.getApplicationParticulars(), is("applicationParticulars"));
        assertThat(courtApplication.getEjected(), is(true));
    }

    private void assertHearing(final Hearing actual) {
        assertThat(actual.getCourtCentreId(), is(courtCentreId));
        assertThat(actual.getCourtRoomId(), is(courtRoomId));
        assertThat(actual.getTypeId(), is(typeId));
        assertThat(actual.getStartDate(), is(LocalDate.parse(startDate)));
        assertThat(actual.getEndDate(), is(LocalDate.parse(endDate)));
        assertThat(actual.getUnscheduled(), is(nullValue()));
        assertThat(actual.getWeekCommencingStartDate(), is(nullValue()));
        assertThat(actual.getWeekCommencingEndDate(), is(nullValue()));
        assertThat(actual.getListedCases().size(), is(2));
    }

    @Test
    public void syncWithGroupCases() throws IOException {
        final UUID groupId = UUID.randomUUID();
        final UUID masterCaseId = UUID.randomUUID();
        final List<UUID> removedCaseIds = asList(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        final List<UUID> memberCaseIds = asList(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(hearingRepository.findBy(eq(hearingId))).thenReturn(buildHearingWithGroupCases(hearingId, groupId, masterCaseId, removedCaseIds, memberCaseIds));
        hearingSearchSyncService.sync(hearingId);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());
        final Hearing actual = hearingArgumentCaptor.getValue();

        assertThat(actual.getListedCases().size(), is(1 + removedCaseIds.size()));
        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).size(), is(1 + removedCaseIds.size()));

        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(0).get(ID).asText(), is(masterCaseId.toString()));
        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(0).get(GROUP_ID).asText(), is(groupId.toString()));
        for (int i = 1; i < (1 + removedCaseIds.size()); i++) {
            assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(i).get(IS_CIVIL).booleanValue(), is(true));
            assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(i).get(GROUP_ID).asText(), is(groupId.toString()));
            assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(i).get(IS_GROUP_MEMBER).booleanValue(), is(false));
            assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(i).get(IS_GROUP_MASTER).booleanValue(), is(false));
        }

        final ListedCases masterCase = actual.getListedCases().stream().filter(lc -> masterCaseId.equals(lc.getCaseId())).findFirst().orElse(null);
        assertThat(masterCase, notNullValue());
    }

    @Test
    public void syncWithGroupCasesWhenCaseRemovedFromGroup() throws IOException {
        final UUID groupId = UUID.randomUUID();
        final UUID masterCaseId = UUID.randomUUID();
        final UUID newGroupMasterCaseId = UUID.randomUUID();
        final List<UUID> previouslyRemovedCaseIds = asList(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        final List<UUID> previouslyMemberCaseIds = asList(UUID.randomUUID(), UUID.randomUUID(), newGroupMasterCaseId);

        final Hearing hearing = buildHearingWithGroupCases(hearingId, groupId, masterCaseId, previouslyRemovedCaseIds, previouslyMemberCaseIds);
        final List<ListedCase> listedCasesToUpdate = getListedCasesToUpdate(groupId, masterCaseId, newGroupMasterCaseId);

        hearingSearchSyncService.syncEntity(hearing, listedCasesToUpdate);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());
        final Hearing actual = hearingArgumentCaptor.getValue();

        assertThat(actual.getListedCases().size(), is(2 + previouslyRemovedCaseIds.size()));
        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).size(), is(2 + previouslyRemovedCaseIds.size()));

        int index = 0;
        for (; index < previouslyRemovedCaseIds.size(); index++) {
            assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(IS_CIVIL).booleanValue(), is(true));
            assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(GROUP_ID).asText(), is(groupId.toString()));
            assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(IS_GROUP_MEMBER).booleanValue(), is(false));
            assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(IS_GROUP_MASTER).booleanValue(), is(false));
        }

        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(ID).asText(), is(masterCaseId.toString()));
        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(GROUP_ID).asText(), is(groupId.toString()));
        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(IS_GROUP_MEMBER).booleanValue(), is(false));
        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(IS_GROUP_MASTER).booleanValue(), is(false));
        index++;

        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(ID).asText(), is(newGroupMasterCaseId.toString()));
        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(GROUP_ID).asText(), is(groupId.toString()));
        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(IS_GROUP_MEMBER).booleanValue(), is(true));
        assertThat(actual.getProperties().get(LISTED_CASES_FIELD).get(index).get(IS_GROUP_MASTER).booleanValue(), is(true));

        index = 0;
        for (; index < previouslyRemovedCaseIds.size(); index++) {
            final UUID removedCaseId = previouslyRemovedCaseIds.get(index);
            final ListedCases previouslyRemovedCase = actual.getListedCases().stream().filter(lc -> removedCaseId.equals(lc.getCaseId())).findFirst().orElse(null);
            assertThat(previouslyRemovedCase, notNullValue());
        }

        final ListedCases previousGroupMasterCase = actual.getListedCases().stream().filter(lc -> masterCaseId.equals(lc.getCaseId())).findFirst().orElse(null);
        assertThat(previousGroupMasterCase, notNullValue());

        final ListedCases newGroupMasterCase = actual.getListedCases().stream().filter(lc -> newGroupMasterCaseId.equals(lc.getCaseId())).findFirst().orElse(null);
        assertThat(newGroupMasterCase, notNullValue());
    }

    private Hearing buildHearing(final UUID hearingId) throws IOException {
        final JsonObject properties = createObjectBuilder()
                .add(COURT_CENTRE_ID, courtCentreId.toString())
                .add(COURT_ROOM_ID, courtRoomId.toString())
                .add(IS_VACATED_TRIAL, "false")
                .add(TYPE, createObjectBuilder().add(ID, typeId.toString()))
                .add(START_DATE, startDate)
                .add(END_DATE, endDate)
                .add(TYPE_OF_LIST, createObjectBuilder().add(ID, typeOfListId.toString()))
                .add(LISTED_CASES_FIELD, createArrayBuilder()
                        .add(createObjectBuilder().add("id", caseId1.toString())
                                .add("prosecutor", createObjectBuilder()
                                        .add("prosecutorId", prosecutorId1.toString())
                                        .add("prosecutorCode", prosecutorCode1))
                                .add("linkedCases", createArrayBuilder()
                                        .add(createObjectBuilder().add("caseId", caseId3.toString()).add("caseUrn", "caseUrn1"))
                                        .add(createObjectBuilder().add("caseId", caseId4.toString()).add("caseUrn", "caseUrn2")))
                                .add("defendants", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", defendantId1.toString()).add("masterDefendantId", masterDefendantId1.toString()))
                                        .add(createObjectBuilder().add("id", defendantId2.toString()).add("masterDefendantId", masterDefendantId2.toString())))
                                .add("caseIdentifier", createObjectBuilder()
                                        .add("authorityId", authorityId1.toString())
                                        .add("authorityCode", authCode1)
                                        .add("caseReference", caseRef1)))
                        .add(createObjectBuilder().add("id", caseId2.toString())
                                .add("prosecutor", createObjectBuilder()
                                        .add("prosecutorId", prosecutorId2.toString())
                                        .add("prosecutorCode", prosecutorCode2))
                                .add("linkedCases", createArrayBuilder()
                                        .add(createObjectBuilder().add("caseId", caseId5.toString()).add("caseUrn", "caseUrn3"))
                                        .add(createObjectBuilder().add("caseId", caseId6.toString()).add("caseUrn", "caseUrn4")))
                                .add("defendants", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", defendantId3.toString()).add("masterDefendantId", masterDefendantId3.toString()))
                                        .add(createObjectBuilder().add("id", defendantId4.toString()).add("masterDefendantId", masterDefendantId4.toString())))
                                .add("caseIdentifier", createObjectBuilder()
                                        .add("authorityId", authorityId2.toString())
                                        .add("authorityCode", authCode2)
                                        .add("caseReference", caseRef2)))
                )
                .add(HEARING_DAYS_FIELD, createArrayBuilder()
                        .add(createObjectBuilder().add("sequence", "1"))
                        .add(createObjectBuilder().add("sequence", "2")))
                .add(COURT_APPLICATIONS, createArrayBuilder()
                        .add(createObjectBuilder().add("id", applicationId1.toString())
                                .add("applicationType", "applicationType")
                                .add("parentApplicationId", parentApplicationId1.toString())
                                .add("applicationReference", "applicationReference")
                                .add("applicationParticulars", "applicationParticulars")
                                .add("isEjected", true)

                        ))
                .build();

        return Hearing.builder()
                .withId(hearingId)
                .withProperties(toJsonNode(properties))
                .build();
    }

    private Hearing buildHearingWithGroupCases(final UUID hearingId, final UUID groupId, final UUID masterCaseId,
                                               final List<UUID> removedCaseIds, final List<UUID> memberCaseIds) throws IOException {
        final JsonObject properties = createObjectBuilder()
                .add(COURT_CENTRE_ID, courtCentreId.toString())
                .add(COURT_ROOM_ID, courtRoomId.toString())
                .add(IS_VACATED_TRIAL, "false")
                .add(TYPE, createObjectBuilder().add(ID, typeId.toString()))
                .add(START_DATE, startDate)
                .add(END_DATE, endDate)
                .add(TYPE_OF_LIST, createObjectBuilder().add(ID, typeOfListId.toString()))
                .add(IS_GROUP_PROCEEDINGS, "true")
                .add(LISTED_CASES_FIELD, getListedCasesWithGroupCases(groupId, masterCaseId, removedCaseIds, memberCaseIds))
                .add(HEARING_DAYS_FIELD, createArrayBuilder()
                        .add(createObjectBuilder().add("sequence", "1"))
                        .add(createObjectBuilder().add("sequence", "2")))
                .build();

        return Hearing.builder()
                .withId(hearingId)
                .withProperties(toJsonNode(properties))
                .build();
    }

    private JsonArray getListedCasesWithGroupCases(final UUID groupId, final UUID masterCaseId, final List<UUID> removedCaseIds, final List<UUID> memberCaseIds) {
        final JsonArrayBuilder builder = createArrayBuilder();

        for (int i = 0; i < (1 + removedCaseIds.size() + memberCaseIds.size()); i++) {
            UUID currentCaseId = i == 0 ? masterCaseId :
                    i < (1 + removedCaseIds.size()) ? removedCaseIds.get(i - 1) : memberCaseIds.get(i - 1 - removedCaseIds.size());
            builder.add(createObjectBuilder()
                    .add("id", currentCaseId.toString())
                    .add("isCivil", true)
                    .add("groupId", groupId.toString())
                    .add("isGroupMember", masterCaseId.equals(currentCaseId) || memberCaseIds.contains(currentCaseId) ? true : false)
                    .add("isGroupMaster", masterCaseId.equals(currentCaseId) ? true : false)
                    .add("caseIdentifier", createObjectBuilder()
                            .add("authorityId", UUID.randomUUID().toString())
                            .add("authorityCode", "AUTH_CODE_".concat(Integer.toString(i + 1)))
                            .add("caseReference", "CASE_REF_".concat(Integer.toString(i + 1)))));
        }

        return builder.build();
    }

    private List<ListedCase> getListedCasesToUpdate(final UUID groupId, final UUID removedCaseId,
                                                    final UUID newGroupMasterCaseId) {
        final List<ListedCase> listedCasesToUpdate = new ArrayList<>();

        listedCasesToUpdate.add(ListedCase.listedCase()
                .withId(removedCaseId)
                .withGroupId(groupId)
                .withIsCivil(Boolean.TRUE)
                .withIsGroupMember(Boolean.FALSE)
                .withIsGroupMaster(Boolean.FALSE)
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityId(UUID.randomUUID())
                        .withAuthorityCode("AUTH_CODE_REMOVED")
                        .withCaseReference("CASE_REF_REMOVED")
                        .build())
                .build());

        if (Objects.nonNull(newGroupMasterCaseId)) {
            listedCasesToUpdate.add(ListedCase.listedCase()
                    .withId(newGroupMasterCaseId)
                    .withGroupId(groupId)
                    .withIsCivil(Boolean.TRUE)
                    .withIsGroupMember(Boolean.TRUE)
                    .withIsGroupMaster(Boolean.TRUE)
                    .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                            .withAuthorityId(UUID.randomUUID())
                            .withAuthorityCode("AUTH_CODE_NEW_GROUP_MASTER")
                            .withCaseReference("CASE_REF_NEW_GROUP_MASTER")
                            .build())
                    .build());
        }

        return listedCasesToUpdate;
    }

    public JsonNode toJsonNode(JsonObject jsonObject) throws IOException {

        // Parse a JsonObject into a JSON string
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = JsonObjects.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        String json = stringWriter.toString();

        // Parse a JSON string into a JsonNode
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);

        return jsonNode;
    }
}