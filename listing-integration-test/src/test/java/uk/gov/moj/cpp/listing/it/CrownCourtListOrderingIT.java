package uk.gov.moj.cpp.listing.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasItems;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDelayForJms;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetProsecutorPoliceFlag;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import com.jayway.jsonpath.JsonPath;
import com.github.tomakehurst.wiremock.client.WireMock;
import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingTypeData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleTypeData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end ordering of a crown court daily list ({@code /dailylistpayload}) for a crown court centre
 * that sits at several CREST court sites. All sites belong to the same crown court (same oucode and
 * CREST court id); the site name is the building the courtrooms are in.
 *
 * Hearings are listed, allocated and sequenced through the real commands in a scrambled order. The
 * generated court list must be ordered:
 * - main CREST site (code A) first, then the remaining sites alphabetically by site name;
 * - courtrooms within a site by CREST courtroom number (Court 2 before Court 10);
 * - hearings within a courtroom by start time, then listing sequence.
 */
public class CrownCourtListOrderingIT extends AbstractIT {

    private static final UUID COURT_CENTRE_ID = fromString("6131bd34-33d9-3d1e-8152-8b5a2084f1bd");
    private static final String OUCODE = "C30DE00";

    private static final Room MAIN_SITE_COURT_2 = new Room(fromString("f9f15b42-6594-38c5-a245-22d752a1e2c5"), 1602);
    private static final Room MAIN_SITE_COURT_10 = new Room(fromString("3d6a1c52-9f4e-4b0a-8a6e-1b2c3d4e5f10"), 1610);
    private static final Room SITE_B_COURT_3 = new Room(fromString("02529e93-35cf-3fc3-8406-d306c5b6bd0c"), 1624);
    private static final Room SITE_C_COURT_1 = new Room(fromString("7c1e5a90-2b3d-4e6f-9a8b-0c1d2e3f4a51"), 1631);

    private static final String DAILY_LIST_MEDIA_TYPE = "application/vnd.listing.search.daily.list.payload+json";
    private static final String SEARCH_HEARINGS_MEDIA_TYPE = "application/vnd.listing.search.hearings+json";
    private static final String SEQUENCE_HEARINGS_MEDIA_TYPE = "application/vnd.listing.command.sequence-hearings+json";
    private static final String REFERENCE_DATA_BASE_URL = "/referencedata-service/query/api/rest/referencedata/";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final HearingTypeData HEARING_TYPE = new HearingTypeData(randomUUID(), "Sentence", "welshSentence");

    private final UUID judicialId = randomUUID();
    private LocalDate hearingDate;

    @BeforeEach
    public void setUp() {
        super.setUp();
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        hearingDate = nextOrSameWorkingDay(ItClock.today());
        stubMultiSiteCrownCourtReferenceData();
        stubGetReferenceDataJudiciaries(judicialId);
    }

    @Test
    void shouldOrderCrownDailyListBySiteThenCourtRoomNumberThenHearingStartTimeAndSequence() {
        // Listed deliberately out of order. Each hearing carries more than one listed case, so a hearing is
        // identified by all of its case references.
        final ListedHearing siteBLate = listAllocateAndSequence("site B 10:30 seq 1", SITE_B_COURT_3, LocalTime.of(10, 30), 1);
        final ListedHearing mainCourt10 = listAllocateAndSequence("main site Court 10", MAIN_SITE_COURT_10, LocalTime.of(10, 0), 1);
        final ListedHearing siteBSecond = listAllocateAndSequence("site B 10:00 seq 2", SITE_B_COURT_3, LocalTime.of(10, 0), 2);
        final ListedHearing siteCCourt1 = listAllocateAndSequence("site C Court 1", SITE_C_COURT_1, LocalTime.of(10, 0), 1);
        final ListedHearing siteBFirst = listAllocateAndSequence("site B 10:00 seq 1", SITE_B_COURT_3, LocalTime.of(10, 0), 1);
        final ListedHearing mainCourt2 = listAllocateAndSequence("main site Court 2", MAIN_SITE_COURT_2, LocalTime.of(10, 0), 1);
        final List<ListedHearing> allHearings = asList(siteBLate, mainCourt10, siteBSecond, siteCCourt1, siteBFirst, mainCourt2);

        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.daily.list.payload"), COURT_CENTRE_ID, hearingDate.toString(), "DRAFT"));

        final ResponseData response = pollWithDelayForJms(requestParams(url, DAILY_LIST_MEDIA_TYPE).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.courtLists[*].crestCourtSite.crestCourtSiteName",
                                        contains("DERBY", "Chesterfield Justice Centre", "MANSFIELD MAGISTRATES' COURT")),
                                withJsonPath("$.courtLists[0].sittings[*].courtRoomName",
                                        contains("Courtroom 02", "Courtroom 10")),
                                withJsonPath("$.courtLists[1].sittings[*].courtRoomName",
                                        contains("Courtroom 01 (Chesterfield)")),
                                withJsonPath("$.courtLists[2].sittings[*].courtRoomName",
                                        contains("Courtroom 03 (Mansfield)")),
                                withJsonPath("$.courtLists[*].sittings[*].hearings[*].caseIdentifier.caseReference",
                                        hasItems(allHearings.stream().map(ListedHearing::firstCaseReference).toArray(String[]::new)))
                        )));

        final String payload = response.getPayload();
        assertThat(hearingsIn(payload, 0, 0, allHearings), contains(mainCourt2.name));
        assertThat(hearingsIn(payload, 0, 1, allHearings), contains(mainCourt10.name));
        assertThat(hearingsIn(payload, 1, 0, allHearings), contains(siteCCourt1.name));
        assertThat(hearingsIn(payload, 2, 0, allHearings), contains(siteBFirst.name, siteBSecond.name, siteBLate.name));
        assertThat(distinctInOrder(JsonPath.read(payload, "$.courtLists[2].sittings[0].hearings[*].startTime")),
                contains(hearingDate + "T10:00", hearingDate + "T10:30"));
    }

    /**
     * The hearings, in output order, of one sitting: each case reference is mapped back to the hearing it was
     * listed on and consecutive entries for the same hearing are collapsed.
     */
    private List<String> hearingsIn(final String payload, final int courtList, final int sitting, final List<ListedHearing> hearings) {
        final List<String> caseReferences = JsonPath.read(payload,
                format("$.courtLists[{0}].sittings[{1}].hearings[*].caseIdentifier.caseReference", courtList, sitting));
        return distinctInOrder(caseReferences.stream()
                .map(caseReference -> hearings.stream()
                        .filter(hearing -> hearing.caseReferences.contains(caseReference))
                        .map(hearing -> hearing.name)
                        .findFirst()
                        .orElse("unexpected case " + caseReference))
                .collect(toList()));
    }

    private static List<String> distinctInOrder(final List<String> values) {
        final List<String> collapsed = new ArrayList<>();
        values.forEach(value -> {
            if (collapsed.isEmpty() || !collapsed.get(collapsed.size() - 1).equals(value)) {
                collapsed.add(value);
            }
        });
        return collapsed;
    }

    /**
     * Lists a new case, allocates its hearing to the given courtroom and start time on {@link #hearingDate}
     * with the shared judge, sequences it, and returns the hearing with all of its case references.
     */
    private ListedHearing listAllocateAndSequence(final String name, final Room room, final LocalTime startTime, final int sequence) {
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final ListedCaseData listedCase = hearingsData.getHearingData().get(0).getListedCases().get(0);
        stubGetProsecutorPoliceFlag(listedCase.getAuthorityId());

        final UpdatedHearingData allocation = allocation(hearingId, room, startTime);
        stubListHearingInCourtSessionsWithMultipleSchedules(allocation);
        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, allocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        verifyHearingAllocated(hearingId, room, startTime);

        sequenceHearing(hearingId, room, sequence);
        return new ListedHearing(name, hearingsData.getHearingData().get(0).getListedCases().stream()
                .map(ListedCaseData::getCaseReference)
                .collect(toList()));
    }

    private UpdatedHearingData allocation(final UUID hearingId, final Room room, final LocalTime startTime) {
        final ZonedDateTime start = ZonedDateTime.of(hearingDate, startTime, ZoneOffset.UTC);
        final NonDefaultDayData hearingDay = new NonDefaultDayData(start.format(DATE_TIME_FORMAT), of(30),
                of(randomUUID().toString()), of(room.courtroomId), of(OUCODE), of("AM"),
                of(COURT_CENTRE_ID.toString()), of(room.id.toString()));
        final List<JudicialRoleData> judiciary = singletonList(new JudicialRoleData(of(true), of(false), judicialId, randomUUID(),
                new JudicialRoleTypeData(Optional.of(randomUUID()), "CIRCUIT_JUDGE")));

        return new UpdatedHearingData(hearingId, COURT_CENTRE_ID, "Derby Crown Court", room.id, HEARING_TYPE,
                hearingDate.toString(), hearingDate.toString(), singletonList(hearingDay), emptyList(), "ENGLISH",
                judiciary, "CROWN", null, null, null, false, null, false, null, null);
    }

    /**
     * Single-day allocation, so the shared UpdateHearingSteps verification (which expects a non-sitting day)
     * does not apply. Checks the hearing is allocated to the courtroom with the requested start time.
     */
    private void verifyHearingAllocated(final UUID hearingId, final Room room, final LocalTime startTime) {
        final String expectedStartTime = ZonedDateTime.of(hearingDate, startTime, ZoneOffset.UTC).format(ZONED_DATE_TIME_FORMAT);
        final String hearing = "$.hearings[?(@.id=='" + hearingId + "')]";
        pollWithDelayForJms(requestParams(searchHearingsUrl(room), SEARCH_HEARINGS_MEDIA_TYPE)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearing + ".courtRoomId", hasItem(room.id.toString())),
                                withJsonPath(hearing + ".judiciary[0].judicialId", hasItem(judicialId.toString())),
                                withJsonPath(hearing + ".hearingDays[0].startTime", hasItem(expectedStartTime)))));
    }

    private String searchHearingsUrl(final Room room) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id.search-date"),
                        ALLOCATED, COURT_CENTRE_ID, room.id, hearingDate.toString()));
    }

    private void sequenceHearing(final UUID hearingId, final Room room, final int sequence) {
        final String request = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(createObjectBuilder()
                        .add("id", hearingId.toString())
                        .add("sequenceHearingDays", createArrayBuilder().add(createObjectBuilder()
                                .add("hearingDate", hearingDate.toString())
                                .add("sequence", sequence)))))
                .build().toString();

        final Response response = restClient.postCommand(
                String.format("%s/%s", getBaseUri(), readConfig().getProperty("listing.command.sequence-hearings")),
                SEQUENCE_HEARINGS_MEDIA_TYPE, request, getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));

        pollWithDelayForJms(requestParams(searchHearingsUrl(room), SEARCH_HEARINGS_MEDIA_TYPE)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath(
                                "$.hearings[?(@.id=='" + hearingId + "')].hearingDays[0].sequence", hasItem(sequence))));
    }

    /**
     * Reference data for a crown court centre with three CREST sites (A = main site, B and C = other buildings),
     * registered at a higher priority than the shared stubs and matched on this court centre only.
     */
    private void stubMultiSiteCrownCourtReferenceData() {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        final String folder = "stub-data/crown-court-list-ordering/";

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_BASE_URL + "courtrooms/" + COURT_CENTRE_ID))
                .atPriority(1)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.referencedata.courtroom+json")
                        .withBody(getPayload(folder + "referencedata.query.courtroom.json"))));

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_BASE_URL + "cp-xhibit-court-mappings"))
                .withQueryParam("ouId", WireMock.equalTo(COURT_CENTRE_ID.toString()))
                .atPriority(1)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.referencedata.query.cp-xhibit-court-mappings+json")
                        .withBody(getPayload(folder + "referencedata.query.cp-xhibit-court-mappings.json"))));

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_BASE_URL + "cp-xhibit-courtroom-mappings"))
                .withQueryParam("ouId", WireMock.equalTo(COURT_CENTRE_ID.toString()))
                .atPriority(1)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.referencedata.query.cp-xhibit-courtroom-mappings+json")
                        .withBody(getPayload(folder + "referencedata.query.cp-xhibit-courtroom-mappings.json"))));
    }

    private static LocalDate nextOrSameWorkingDay(final LocalDate date) {
        LocalDate workingDay = date;
        while (workingDay.getDayOfWeek() == SATURDAY || workingDay.getDayOfWeek() == SUNDAY) {
            workingDay = workingDay.plusDays(1);
        }
        return workingDay;
    }

    private record Room(UUID id, int courtroomId) {
    }

    private record ListedHearing(String name, List<String> caseReferences) {
        private String firstCaseReference() {
            return caseReferences.get(0);
        }
    }
}
