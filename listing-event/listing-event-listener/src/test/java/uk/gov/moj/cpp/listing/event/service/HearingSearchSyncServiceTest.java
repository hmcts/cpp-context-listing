package uk.gov.moj.cpp.listing.event.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.entity.ListedCases;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.UUID;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.COURT_CENTRE_ID;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.END_DATE;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.HEARING_DAYS_FIELD;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.ID;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.COURT_ROOM_ID;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.IS_VACATED_TRIAL;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.LISTED_CASES_FIELD;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.START_DATE;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.TYPE;
import static uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService.TYPE_OF_LIST;

@RunWith(MockitoJUnitRunner.class)
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
    private String caseRef1 = "CASE_REF_1";
    private String authCode2 = "AUTH_CODE_2";
    private String caseRef2 = "CASE_REF_2";
    private String startDate = "2021-03-01";
    private String endDate = "2021-03-02";
    private UUID typeOfListId = UUID.randomUUID();

    @Test
    public void sync() throws IOException {

        when(hearingRepository.findBy(eq(hearingId))).thenReturn(buildHearing(hearingId));
        hearingSearchSyncService.sync(hearingId);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());
        final Hearing actual = hearingArgumentCaptor.getValue();

        assertThat(actual.getCourtCentreId(), is(courtCentreId));
        assertThat(actual.getCourtRoomId(), is(courtRoomId));
        assertThat(actual.getTypeId(), is(typeId));
        assertThat(actual.getStartDate(), is(LocalDate.parse(startDate)));
        assertThat(actual.getEndDate(), is(LocalDate.parse(endDate)));
        assertThat(actual.getUnscheduled(), is(nullValue()));
        assertThat(actual.getWeekCommencingStartDate(), is(nullValue()));
        assertThat(actual.getWeekCommencingEndDate(), is(nullValue()));
        assertThat(actual.getListedCases().size(), is(2));

        final ListedCases listedCases1 = actual.getListedCases().stream().filter(c -> c.getCaseId().equals(caseId1)).findFirst().orElse(null);
        assertThat(listedCases1, is(notNullValue()));
        assertThat(listedCases1.getCaseIdentifier().getAuthorityId(), is(authorityId1));
        assertThat(listedCases1.getCaseIdentifier().getAuthorityCode(), is(authCode1));
        assertThat(listedCases1.getCaseIdentifier().getCaseReference(), is(caseRef1));

        final ListedCases listedCases2 = actual.getListedCases().stream().filter(c -> c.getCaseId().equals(caseId2)).findFirst().orElse(null);
        assertThat(listedCases2, is(notNullValue()));
        assertThat(listedCases2.getCaseIdentifier().getAuthorityId(), is(authorityId2));
        assertThat(listedCases2.getCaseIdentifier().getAuthorityCode(), is(authCode2));
        assertThat(listedCases2.getCaseIdentifier().getCaseReference(), is(caseRef2));

        assertThat(actual.getHearingDays().size(), is(2));
        final HearingDays hearingDays1 = actual.getHearingDays().stream().filter(h -> h.getSequence() == 1).findFirst().orElse(null);
        assertThat(hearingDays1, is(notNullValue()));

        final HearingDays hearingDays2 = actual.getHearingDays().stream().filter(h -> h.getSequence() == 2).findFirst().orElse(null);
        assertThat(hearingDays2, is(notNullValue()));
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
                                .add("caseIdentifier", createObjectBuilder()
                                        .add("authorityId", authorityId1.toString())
                                        .add("authorityCode", authCode1)
                                        .add("caseReference", caseRef1)))
                        .add(createObjectBuilder().add("id", caseId2.toString())
                                .add("caseIdentifier", createObjectBuilder()
                                        .add("authorityId", authorityId2.toString())
                                        .add("authorityCode", authCode2)
                                        .add("caseReference", caseRef2)))
                )
                .add(HEARING_DAYS_FIELD, createArrayBuilder()
                        .add(createObjectBuilder().add("sequence", "1"))
                        .add(createObjectBuilder().add("sequence", "2")))
                .build();

        return Hearing.builder()
                .withId(hearingId)
                .withProperties(toJsonNode(properties))
                .build();
    }

    public JsonNode toJsonNode(JsonObject jsonObject) throws IOException {

        // Parse a JsonObject into a JSON string
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        String json = stringWriter.toString();

        // Parse a JSON string into a JsonNode
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);

        return jsonNode;
    }
}