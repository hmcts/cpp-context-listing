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
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;

import uk.gov.justice.api.resource.DefaultQueryApiUpdateHearingSlotsResource;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingLanguage;
import uk.gov.justice.listing.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.processor.azure.util.SlotCriteriaConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private SlotCriteriaConverter slotCriteriaConverter;

    @Mock
    private DefaultQueryApiUpdateHearingSlotsResource defaultQueryApiUpdateHearingSlotsResource;

    @InjectMocks
    private AzureListingEventProcessor azureListingEventProcessor;

    @Test
    public void shouldUpdateSlotsInAzureWhenHandleWhenHearingConfirmedAfterHearingAllocatedForListingMessage() throws Exception {

        HearingConfirmed hearingConfirmed = hearingConfirmed();
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingConfirmed.class)).willReturn(hearingConfirmed);

        final String value = "sample";
        given(slotCriteriaConverter.getSlotDetailFromHearingConfirmed(event, hearingConfirmed)). willReturn(value);

        Response response = mock(Response.class);
        given(defaultQueryApiUpdateHearingSlotsResource.updateHearingSlots(value)).willReturn(response);

        String resp = "sample1";
        when(response.readEntity(String.class)).thenReturn(resp);

        //when
        azureListingEventProcessor.handleHearingConfirmedMessage(event);

        //then
        verify(defaultQueryApiUpdateHearingSlotsResource).updateHearingSlots(value);
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