package uk.gov.moj.cpp.listing.event.processor;

import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.azure.HearingSlotsService;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotsToJsonStringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AzureListingEventProcessorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String TYPE = "Sentence";
    private static final Integer ESTIMATED_MINUTES = INTEGER.next();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID JUDICIAL_ID = randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final ZonedDateTime START_DATE_TIME = now();
    private static final DateTimeFormatter DATE_TIME_FORMAT = ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String CIRCUIT_JUDGE = "CIRCUIT_JUDGE";
    private static final String TEST_OUTPUT = "sample";

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private SlotsToJsonStringConverter slotsToJsonStringConverter;

    @Mock
    private HearingSlotsService hearingSlotsService;

    @InjectMocks
    private AzureListingEventProcessor azureListingEventProcessor;

    @Test
    public void shouldUpdateSlotsInAzureWhenNonDefaultDaysAssigned() {
        final Envelope<NonDefaultDaysAssignedToHearing> envelope = (Envelope<NonDefaultDaysAssignedToHearing>) mock(Envelope.class);

        final NonDefaultDaysAssignedToHearing hearing = NonDefaultDaysAssignedToHearing.nonDefaultDaysAssignedToHearing()
                .withNonDefaultDays(nonDefaultDays())
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearing);

        given(slotsToJsonStringConverter.convertNonDefaultDaysToJson(HEARING_ID, hearing.getNonDefaultDays())).willReturn(TEST_OUTPUT);

        azureListingEventProcessor.nonDefaultDaysAssignedForHearing(envelope);

        verify(hearingSlotsService).update(TEST_OUTPUT);
    }

    @Test
    public void shouldUpdateSlotsInAzureWhenNonDefaultDaysChanged() {
        final Envelope<NonDefaultDaysChangedForHearing> envelope = (Envelope<NonDefaultDaysChangedForHearing>) mock(Envelope.class);

        final NonDefaultDaysChangedForHearing hearing = NonDefaultDaysChangedForHearing.nonDefaultDaysChangedForHearing()
                .withNonDefaultDays(nonDefaultDays())
                .withHearingId(HEARING_ID)
                .build();
        given(envelope.payload()).willReturn(hearing);

        given(slotsToJsonStringConverter.convertNonDefaultDaysToJson(HEARING_ID, hearing.getNonDefaultDays())).willReturn(TEST_OUTPUT);

        azureListingEventProcessor.nonDefaultDaysChangedForHearing(envelope);

        verify(hearingSlotsService).update(TEST_OUTPUT);
    }

    @Test
    public void shouldUpdateSlotsInAzureWhenHandleWhenHearingConfirmedAfterHearingAllocatedForListingMessage() throws Exception {

        HearingConfirmed hearingConfirmed = hearingConfirmed();

        final JsonEnvelope event = hearingAllocatedEvent();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingConfirmed.class)).willReturn(hearingConfirmed);

        given(slotsToJsonStringConverter.getSlotDetailFromHearingConfirmed(event, hearingConfirmed)).willReturn(TEST_OUTPUT);

        Response response = mock(Response.class);
        given(hearingSlotsService.update(TEST_OUTPUT)).willReturn(response);

        String resp = "sample1";
        when(response.readEntity(String.class)).thenReturn(resp);

        azureListingEventProcessor.handleHearingConfirmedMessage(event);

        verify(hearingSlotsService).update(TEST_OUTPUT);
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
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithRandomUUIDAndName(), hearingAllocated.build());
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
                .withStartTime(START_DATE_TIME)
                .withDuration(of(311))
                .withCourtRoomId(of(34))
                .withCourtScheduleId(of("224686"))
                .withOucode(of("BA09US"))
                .withSession(of("AD"))
                .build();


        return Arrays.asList(nonDefaultDay1, nonDefaultDay2);
    }

    private HearingConfirmed hearingConfirmed() {

        String formattedDateTime = DATE_TIME_FORMAT.format(START_DATE_TIME);

        return HearingConfirmed.hearingConfirmed()
                .withConfirmedHearing(buildHearing(formattedDateTime))
                .build();
    }

    private uk.gov.justice.core.courts.ConfirmedHearing buildHearing(String formattedDateTime) {
        return uk.gov.justice.core.courts.ConfirmedHearing.confirmedHearing()
                .withId(HEARING_ID)
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(formattedDateTime))
                        .withListedDurationMinutes(0)
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(of(COURT_ROOM_ID))
                        .build())
                .withHearingLanguage(of(HearingLanguage.WELSH))
                .withCourtApplicationIds(Arrays.asList(randomUUID()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withType(HearingType.hearingType().withDescription(TYPE).withId(randomUUID()).build())
                .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                        .withJudicialId(JUDICIAL_ID)
                        .withJudicialRoleType(
                                uk.gov.justice.core.courts.JudicialRoleType.judicialRoleType()
                                        .withJudiciaryType(CIRCUIT_JUDGE)
                                        .withJudicialRoleTypeId(Optional.empty())
                                        .build())
                        .build()))
                .withProsecutionCases(Arrays.asList(uk.gov.justice.core.courts.ConfirmedProsecutionCase.confirmedProsecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(Arrays.asList(uk.gov.justice.core.courts.ConfirmedDefendant.confirmedDefendant()
                                .withId(DEFENDANT_ID)
                                .withOffences(Arrays.asList(uk.gov.justice.core.courts.ConfirmedOffence.confirmedOffence().withId(OFFENCE_ID).build()))
                                .build()))
                        .build()))
                .build();
    }

}