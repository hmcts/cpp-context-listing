package uk.gov.moj.cpp.listing.common.utils;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.common.util.SlotsToJsonStringConverter;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlotsToJsonStringConverterTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final String TYPE = "Sentence";
    private static final Integer ESTIMATED_MINUTES = RandomGenerator.INTEGER.next();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID JUDICIAL_ID = randomUUID();
    private static final LocalDate START_DATE = LocalDate.parse("2019-12-02");
    private static final LocalTime START_TIME = LocalTime.now();
    private static final ZonedDateTime START_DATE_TIME = ZonedDateTime.parse("2019-12-02T11:11:30-05:00");
    private static final String HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";
    public static final String EXPECTED_HEARING_START_TIME = "2019-12-02T11:11:30.000Z";

    @InjectMocks
    private SlotsToJsonStringConverter converter;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Test
    public void shouldTestConvertNonDefaultDaysToJson() {

        final JsonArrayBuilder builder = converter.convertNonDefaultDaysToJson(HEARING_ID, nonDefaultDays());
        final String payload = builder.build().toString();
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
                .assertThat("$[1].courtScheduleId", equalTo("224687"))
                .assertThat("$[0].hearingStartTime", equalTo(EXPECTED_HEARING_START_TIME));
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

    private List<NonDefaultDay> nonDefaultDays() {

        final NonDefaultDay nonDefaultDay1 = NonDefaultDay.nonDefaultDay()
                .withStartTime(START_DATE_TIME)
                .withDuration(Optional.of(1))
                .withCourtRoomId(Optional.of(123))
                .withCourtScheduleId(Optional.of("224686"))
                .withOucode(Optional.of("BA09US"))
                .withSession(Optional.of("AD"))
                .build();

        final NonDefaultDay nonDefaultDay2 = NonDefaultDay.nonDefaultDay()
                .withStartTime(START_DATE_TIME.plusDays(1))
                .withDuration(Optional.of(311))
                .withCourtRoomId(Optional.of(34))
                .withCourtScheduleId(Optional.of("224687"))
                .withOucode(Optional.of("BA09UK"))
                .withSession(Optional.of("AM"))
                .build();


        return Arrays.asList(nonDefaultDay1, nonDefaultDay2);
    }
}