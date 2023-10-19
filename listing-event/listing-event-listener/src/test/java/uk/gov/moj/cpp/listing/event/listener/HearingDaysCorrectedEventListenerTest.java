package uk.gov.moj.cpp.listing.event.listener;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.events.HearingDay.hearingDay;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingDaysCorrectedEventListenerTest {

    private static final LocalDate NOW_DATE = LocalDate.now();
    private final ZonedDateTime NOW_DATE_TIME = new UtcClock().now();
    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Mock
    Hearing hearing;

    @Mock
    ObjectNode properties;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingSearchSyncService hearingSearchSyncService;

    @Captor
    private ArgumentCaptor<JsonNode> hearingDaysCaptor;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @InjectMocks
    private HearingDaysCorrectedEventListener hearingDaysCorrectedEventListener;

    @Test
    public void test() throws JsonProcessingException {
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final List<HearingDay> hearingDays = hearingDays(true);

        HearingDaysWithoutCourtCentreCorrected corrected = HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(hearingDays.stream().map(hearingDay -> hearingDay().withValuesFrom(hearingDay).withCourtRoomId(courtRoomId).withCourtCentreId(courtCentreId).build()).collect(toList()))
                .withId(hearingId)
                .build();
        final Envelope<HearingDaysWithoutCourtCentreCorrected> listenerEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.events.hearing-days-without-court-centre-corrected"),
                corrected);
        final NonDefaultDay nonDefaultDay = new NonDefaultDay(UUID.randomUUID().toString(), 1, UUID.randomUUID().toString(),
                1, "oucode", "roomId", "session", ZonedDateTime.now());
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        nonDefaultDays.add(nonDefaultDay);
        final uk.gov.justice.listing.events.Hearing dbHearingPayload = uk.gov.justice.listing.events.Hearing.hearing().withId(hearingId)
                .withNonDefaultDays(nonDefaultDays).withHearingDays(hearingDays).build();
        when(hearingRepository.findBy(hearingId)).thenReturn(hearing);
        final JsonNode t = objectMapper.valueToTree(dbHearingPayload);
        when(hearing.getProperties()).thenReturn(t);
        hearingDaysCorrectedEventListener.hearingDaysWithoutCourtCentreCorrected(listenerEnvelope);
        verify(hearing).setProperties(hearingDaysCaptor.capture());

        final JsonNode properties = hearingDaysCaptor.getValue();
        assertThat(properties.toString(), isJson(allOf(
                withJsonPath("$.hearingDays", hasSize(1)),
                withJsonPath("$.nonDefaultDays", hasSize(1)),
                withJsonPath("$.hearingDays[0].hearingDate", equalTo(LocalDates.to(NOW_DATE))),
                withJsonPath("$.hearingDays[0].startTime", equalTo(ISO_8601_FORMATTER.format(NOW_DATE_TIME))),
                withJsonPath("$.hearingDays[0].isCancelled", equalTo(false))
        )));
    }

    private List<HearingDay> hearingDays(boolean isAnyCancelled) {
        return Stream.of(
                hearingDay()
                        .withHearingDate(NOW_DATE)
                        .withIsCancelled(false)
                        .withStartTime(NOW_DATE_TIME)
                        .withEndTime(NOW_DATE_TIME.plusMinutes(30))
                        .build(),
                hearingDay()
                        .withHearingDate(NOW_DATE.plusDays(1))
                        .withIsCancelled(isAnyCancelled)
                        .withStartTime(NOW_DATE_TIME.plusDays(1))
                        .withEndTime(NOW_DATE_TIME.plusMinutes(30).plusDays(1))
                        .build()).collect(toList());
    }
}