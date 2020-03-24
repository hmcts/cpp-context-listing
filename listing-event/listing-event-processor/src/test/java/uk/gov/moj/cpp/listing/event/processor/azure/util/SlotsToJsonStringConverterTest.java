package uk.gov.moj.cpp.listing.event.processor.azure.util;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SlotsToJsonStringConverterTest {

    private static final String REFERENCE_DATA_GET_COURTROOM = "referencedata.query.courtroom";
    private static final UUID CASE_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String TYPE = "Sentence";
    private static final Integer ESTIMATED_MINUTES = RandomGenerator.INTEGER.next();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID JUDICIAL_ID = randomUUID();
    private static final LocalDate START_DATE = LocalDate.parse("2019-12-02");
    private static final LocalTime START_TIME = LocalTime.now();
    private static final ZonedDateTime START_DATE_TIME = ZonedDateTime.parse("2019-12-02T11:11:30-05:00");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String CIRCUIT_JUDGE = "CIRCUIT_JUDGE";
    private static final String HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";

    @Mock
    private ListingReferenceDataService listingReferenceDataService;

    @InjectMocks
    private SlotsToJsonStringConverter converter;

    @Test
    public void getSlotDetailFromHearingConfirmed() {
        final JsonEnvelope event = hearingAllocatedEvent();
        final String ouCode = "B01LY00";
        final int courtRoomId = 2;
        final ZonedDateTime DATE_TIME = ZonedDateTime.parse("2019-12-02T11:11:30-05:00");

        final String expectedZoneDateTime = HearingDayDetailConverter.getMeridian(DATE_TIME);

        final JsonObject jsonObject = getPayloadForCourtRooms(COURT_CENTRE_ID.toString());

        given(listingReferenceDataService.getPayLoadForCourtRoom(event, COURT_CENTRE_ID.toString())).willReturn(envelopeFrom(metadataWithRandomUUID(REFERENCE_DATA_GET_COURTROOM), jsonObject));

        given(listingReferenceDataService.retrieveCourtRoomId(jsonObject, COURT_ROOM_ID, COURT_CENTRE_ID)).willReturn(courtRoomId);

        final HearingConfirmed hearingConfirmed = hearingConfirmed();

        final String slotDetailFromHearingConfirmed = converter.getSlotDetailFromHearingConfirmed(event, hearingConfirmed);

        assertNotNull(slotDetailFromHearingConfirmed);
        with(slotDetailFromHearingConfirmed)
                .assertThat("$[0].courtRoomId", equalTo(courtRoomId))
                .assertThat("$[0].ouCode", equalTo(ouCode))
                .assertThat("$[0].sessionDate", equalTo(START_DATE.toString()))
                .assertThat("$[0].session", equalTo(expectedZoneDateTime))
                .assertThat("$[0].duration", equalTo(10));

    }

    @Test
    public void shouldTestConvertNonDefaultDaysToJson() {

        final String payload = converter.convertNonDefaultDaysToJson(HEARING_ID, nonDefaultDays());
        assertNotNull(payload);
        with(payload)
                .assertThat("$[0].duration", equalTo(1))
                .assertThat("$[0].sessionDate", equalTo(START_DATE_TIME.toLocalDate().toString()))
                .assertThat("$[0].session", equalTo("AD"))
                .assertThat("$[0].courtRoomId", equalTo(123))
                .assertThat("$[0].ouCode", equalTo("BA09US"))
                .assertThat("$[0].hearingId", equalTo(HEARING_ID.toString()))
                .assertThat("$[0].courtScheduleId", equalTo("224686"))
                .assertThat("$[1].duration", equalTo(311))
                .assertThat("$[1].sessionDate", equalTo(START_DATE_TIME.plusDays(1).toLocalDate().toString()))
                .assertThat("$[1].session", equalTo("AM"))
                .assertThat("$[1].courtRoomId", equalTo(34))
                .assertThat("$[1].ouCode", equalTo("BA09UK"))
                .assertThat("$[1].hearingId", equalTo(HEARING_ID.toString()))
                .assertThat("$[1].courtScheduleId", equalTo("224687"));
    }

    private JsonEnvelope hearingAllocatedEvent() {

        final JsonObjectBuilder hearingDate = createObjectBuilder()
                .add("startDate", START_DATE.toString())
                .add("startTime", START_TIME.toString());

        final JsonObjectBuilder hearingAllocated = createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", TYPE)
                .add("estimatedMinutes", ESTIMATED_MINUTES)
                .add("judgeId", JUDICIAL_ID.toString())
                .add("courtCentre", COURT_CENTRE_ID.toString())
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithDefaults().withName(HEARING_ALLOCATED_FOR_LISTING), hearingAllocated);
    }

    private HearingConfirmed hearingConfirmed() {

        final String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);

        return HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildHearing(formattedDateTime))
                .build();
    }

    private uk.gov.justice.core.courts.ConfirmedHearing buildHearing(String formattedDateTime) {
        return uk.gov.justice.core.courts.ConfirmedHearing.confirmedHearing()
                .withId(HEARING_ID)
                .withHearingDays(asList(HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(formattedDateTime))
                        .withListedDurationMinutes(10)
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(of(COURT_ROOM_ID))
                        .build())
                .withHearingLanguage(of(HearingLanguage.WELSH))
                .withCourtApplicationIds(asList(randomUUID()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription(TYPE).withId(randomUUID()).build())
                .withJudiciary(asList(JudicialRole.judicialRole()
                        .withJudicialId(JUDICIAL_ID)
                        .withJudicialRoleType(
                                uk.gov.justice.core.courts.JudicialRoleType.judicialRoleType()
                                        .withJudiciaryType(CIRCUIT_JUDGE)
                                        .withJudicialRoleTypeId(Optional.empty())
                                        .build())
                        .build()))
                .withProsecutionCases(asList(uk.gov.justice.core.courts.ConfirmedProsecutionCase.confirmedProsecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(asList(uk.gov.justice.core.courts.ConfirmedDefendant.confirmedDefendant()
                                .withId(DEFENDANT_ID)
                                .withOffences(asList(uk.gov.justice.core.courts.ConfirmedOffence.confirmedOffence().withId(OFFENCE_ID).build()))
                                .build()))
                        .build()))
                .build();
    }

    private JsonObject getPayloadForCourtRooms(String id) {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("oucode", "B01LY00")
                .add("oucodeL3Name", "South Western (Lavender Hill)")
                .add("oucodeL3WelshName", "welshName_Test")
                .add("courtrooms", getCourtRooms())
                .build();
    }

    private JsonArray getCourtRooms() {
        return createArrayBuilder().add(createObjectBuilder()
                .add("id", COURT_ROOM_ID.toString())
                .add("venueName", "BEXLEY MAGISTRATES' COURT")
                .add("courtroomId", 12)
                .add("courtroomName", "Courtroom 01"))
                .build();
    }

    private List<NonDefaultDay> nonDefaultDays() {

        final NonDefaultDay nonDefaultDay1 = NonDefaultDay.nonDefaultDay()
                .withStartTime(START_DATE_TIME)
                .withDuration(of(1))
                .withCourtRoomId(of(123))
                .withCourtScheduleId(of("224686"))
                .withOucode(of("BA09US"))
                .withSession(of("AD"))
                .build();

        final NonDefaultDay nonDefaultDay2 = NonDefaultDay.nonDefaultDay()
                .withStartTime(START_DATE_TIME.plusDays(1))
                .withDuration(of(311))
                .withCourtRoomId(of(34))
                .withCourtScheduleId(of("224687"))
                .withOucode(of("BA09UK"))
                .withSession(of("AM"))
                .build();


        return Arrays.asList(nonDefaultDay1, nonDefaultDay2);
    }
}