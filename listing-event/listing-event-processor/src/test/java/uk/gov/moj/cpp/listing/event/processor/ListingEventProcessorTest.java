package uk.gov.moj.cpp.listing.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.cpp.listing.event.processor.HearingConfirmedFactory.DATE_TIME_FORMAT;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_CASE_SENT_FOR_LISTING;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.PUBLIC_EVENT_HEARING_CONFIRMED;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.listing.Judge;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;
import uk.gov.moj.cpp.listing.event.HearingAllocatedForListing;
import uk.gov.moj.cpp.listing.event.external.HearingConfirmed;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.ListingCaseRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListingEventProcessorTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final String COURT_ROOM_1_NAME = RandomGenerator.STRING.next();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final String TYPE = "TRIAL";
    private static final Integer ESTIMATED_MINUTES = RandomGenerator.INTEGER.next();
    private static final String BAIL_STATUS = RandomGenerator.STRING.next();
    private static final String DEFENCE_ORGANISATION = RandomGenerator.STRING.next();
    private static final LocalDate DATE = LocalDate.now();
    private static final LocalDate DOB = LocalDate.now().minusYears(45);
    private static final LocalDate CUSTODY_TIME_LIMIT = LocalDate.now().plusYears(5);

    private static final String OFFENCE_CODE = RandomGenerator.STRING.next();
    private static final String LEGISLATION = RandomGenerator.STRING.next();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalDate END_DATE = LocalDate.now().plusDays(5);
    private static final LocalTime START_TIME = LocalTime.now();
    private static final LocalDateTime START_DATE_TIME = LocalDateTime.now();
    private static final Boolean NOT_BEFORE = false;
    private static final String COURT_CENTRE_NAME = RandomGenerator.STRING.next();
    private static final String TITLE = RandomGenerator.STRING.next();
    private static final String FIRST_NAME = RandomGenerator.STRING.next();
    private static final String LAST_NAME = RandomGenerator.STRING.next();
    private static final String URN = RandomGenerator.STRING.next();


    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private CaseSentForListing caseSentForListing;

    @Mock
    private HearingAllocatedForListing hearingAllocatedForListing;

    @Mock
    private List<Hearing> hearings;

    @Mock
    private ListingCase listingCase;

    @Mock
    private uk.gov.moj.cpp.listing.persistence.entity.Hearing hearing;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ListingCaseRepository listingCaseRepository;

    @Mock
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @InjectMocks
    private ListingEventProcessor listingEventProcessor;


    @Test
    public void shouldHandleCaseSentForListingEventMessage() throws Exception {
        //Given
        hearings = Arrays.asList(new Hearing(UUID.randomUUID().toString(), new StringGenerator().next(), new StringGenerator().next(), new StringGenerator().next(), null, 15, Arrays.asList(), false));
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        given(jsonObjectConverter.convert(payload, CaseSentForListing.class)).willReturn(caseSentForListing);
        given(enveloper.withMetadataFrom(envelope, PUBLIC_EVENT_CASE_SENT_FOR_LISTING)).willReturn
                (enveloperFunction);
        given(enveloper.withMetadataFrom(envelope, "listing.command.list-hearing")).willReturn
                (enveloperFunction);
        given(enveloperFunction.apply(any(CaseSentForListing.class))).willReturn(finalEnvelope);
        given(enveloperFunction.apply(any(Hearing.class))).willReturn(finalEnvelope);
        given(caseSentForListing.getHearings()).willReturn(hearings);

        final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
                ArgumentCaptor.forClass(JsonEnvelope.class);

        //when
        listingEventProcessor.handleCaseSentForListingMessage(envelope);

        //then
        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
    }

    @Test
    public void shouldHandleHearingAllocatedForListingMessage() throws Exception {
        //given
        final JsonEnvelope event = hearingAllocatedEvent();
        given(jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingAllocatedForListing.class)).willReturn(hearingAllocatedForListing);
        given(hearingAllocatedForListing.getHearingId()).willReturn(HEARING_ID.toString());
        given(hearingRepository.findBy(HEARING_ID)).willReturn(hearing);

        given(hearing.getListingCaseId()).willReturn(CASE_ID);
        given(listingCaseRepository.findBy(CASE_ID)).willReturn(listingCase);
        HearingConfirmed hearingConfirmed = hearingConfirmed();
        given(hearingConfirmedFactory.create(hearing, listingCase, hearingAllocatedForListing,event)).willReturn(hearingConfirmed);


        //when
        listingEventProcessor.handleHearingAllocatedForListingMessage(event);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName(PUBLIC_EVENT_HEARING_CONFIRMED),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                        withJsonPath("$.urn", equalTo(URN)),
                        withJsonPath("$.hearing.id", equalTo(HEARING_ID.toString())),
                        withJsonPath("$.hearing.type", equalTo(TYPE)),
                        withJsonPath("$.hearing.courtCentreId", equalTo(COURT_CENTRE_ID.toString())),
                        withJsonPath("$.hearing.courtCentreName", equalTo(COURT_CENTRE_NAME)),
                        withJsonPath("$.hearing.courtRoomId", equalTo(COURT_ROOM_ID.toString())),
                        withJsonPath("$.hearing.courtRoomName", equalTo(COURT_ROOM_1_NAME)),
                        withJsonPath("$.hearing.startDateTime", equalTo(DATE_TIME_FORMAT.format(START_DATE_TIME))),
                        withJsonPath("$.hearing.notBefore", equalTo(NOT_BEFORE)),
                        withJsonPath("$.hearing.estimateMinutes", equalTo(ESTIMATED_MINUTES)),
                        withJsonPath("$.hearing.judge.id", equalTo(JUDGE_ID.toString())),
                        withJsonPath("$.hearing.judge.title", equalTo(TITLE)),
                        withJsonPath("$.hearing.judge.firstName", equalTo(FIRST_NAME)),
                        withJsonPath("$.hearing.judge.lastName", equalTo(LAST_NAME)),
                        withJsonPath("$.hearing.defendants[0].id", equalTo(DEFENDANT_ID.toString())),
                        withJsonPath("$.hearing.defendants[0].personId", equalTo(PERSON_ID.toString())),
                        withJsonPath("$.hearing.defendants[0].firstName", equalTo(FIRST_NAME)),
                        withJsonPath("$.hearing.defendants[0].lastName", equalTo(LAST_NAME)),
                        withJsonPath("$.hearing.defendants[0].dateOfBirth", equalTo(DOB.toString())),
                        withJsonPath("$.hearing.defendants[0].bailStatus", equalTo(BAIL_STATUS)),
                        withJsonPath("$.hearing.defendants[0].custodyTimeLimit", equalTo(CUSTODY_TIME_LIMIT.toString())),
                        withJsonPath("$.hearing.defendants[0].defenceOrganisation", equalTo(DEFENCE_ORGANISATION)),
                        withJsonPath("$.hearing.defendants[0].offences[0].id", equalTo(OFFENCE_ID.toString())),
                        withJsonPath("$.hearing.defendants[0].offences[0].offenceCode", equalTo(OFFENCE_CODE)),
                        withJsonPath("$.hearing.defendants[0].offences[0].startDate", equalTo(START_DATE.toString())),
                        withJsonPath("$.hearing.defendants[0].offences[0].endDate", equalTo(END_DATE.toString())),
                        withJsonPath("$.hearing.defendants[0].offences[0].statementOfOffence.title", equalTo(TITLE)),
                        withJsonPath("$.hearing.defendants[0].offences[0].statementOfOffence.legislation", equalTo(LEGISLATION))
                        ))).thatMatchesSchema()
        ));
    }

    private JsonEnvelope hearingAllocatedEvent() {

        final JsonObjectBuilder hearingDate = createObjectBuilder()
                .add("startDate", START_DATE.toString())
                .add("startTime", START_TIME.toString())
                .add("notBefore", NOT_BEFORE);

        final JsonObjectBuilder hearingAllocated = createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("type", TYPE)
                .add("estimatedMinutes", ESTIMATED_MINUTES)
                .add("judgeId", JUDGE_ID.toString())
                .add("courtRoomId", COURT_ROOM_ID.toString())
                .add("hearingDate", hearingDate.build());

        return envelopeFrom(metadataWithRandomUUIDAndName(), hearingAllocated.build());
    }

    private HearingConfirmed hearingConfirmed() {

        uk.gov.moj.cpp.listing.domain.StatementOfOffence statementOfOffence = new uk.gov.moj.cpp.listing.domain.StatementOfOffence(TITLE, LEGISLATION);
        uk.gov.moj.cpp.listing.domain.Offence offence = new uk.gov.moj.cpp.listing.domain.Offence(OFFENCE_ID.toString(), OFFENCE_CODE, START_DATE, END_DATE, statementOfOffence);
        uk.gov.moj.cpp.listing.domain.Defendant defendant =
                new uk.gov.moj.cpp.listing.domain.Defendant(DEFENDANT_ID.toString(), PERSON_ID.toString(), FIRST_NAME, LAST_NAME, DOB, BAIL_STATUS, CUSTODY_TIME_LIMIT, DEFENCE_ORGANISATION, Arrays.asList(offence));
        Judge judge = new Judge(JUDGE_ID.toString(), TITLE, FIRST_NAME, LAST_NAME);
        uk.gov.moj.cpp.listing.event.external.Hearing hearing = new uk.gov.moj.cpp.listing.event.external.Hearing
                (HEARING_ID.toString(), TYPE, CASE_ID.toString(), COURT_CENTRE_ID.toString(), COURT_CENTRE_NAME, COURT_ROOM_ID.toString(), COURT_ROOM_1_NAME, judge, DATE_TIME_FORMAT.format(START_DATE_TIME), NOT_BEFORE, ESTIMATED_MINUTES, Arrays.asList(defendant));

        return new HearingConfirmed(CASE_ID.toString(), URN, hearing);
    }

}