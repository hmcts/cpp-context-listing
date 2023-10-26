package uk.gov.moj.cpp.listing.event.processor.util;

import static com.sun.org.apache.xerces.internal.util.PropertyState.is;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingObjectsListingToCoreConverterTest {

    final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();
    final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @InjectMocks
    private HearingObjectsListingToCoreConverter converter;

    private uk.gov.justice.listing.events.Hearing.Builder hearingBuilder;

    private static final UUID COURT_CENTRE1_ID = randomUUID();
    private static final UUID COURT_CENTRE2_ID = randomUUID();
    private static final UUID COURT_CENTRE3_ID = randomUUID();
    private static final UUID COURT_ROOM1_ID = randomUUID();
    private static final UUID COURT_ROOM2_ID = randomUUID();
    private static final ZonedDateTime NOW = ZonedDateTime.now();

    @Before
    public void setup() {
        converter = new HearingObjectsListingToCoreConverter();
        hearingBuilder = uk.gov.justice.listing.events.Hearing.hearing()
                .withType(Type.type()
                        .withId(randomUUID())
                        .build())
                .withJurisdictionType(JurisdictionType.CROWN);
    }

    @Test
    public void shouldConvertHearingDays() {
        hearingBuilder.withHearingDays(null);
        Hearing convertedHearing = converter.convert(hearingBuilder.build());
        assertNull(convertedHearing.getHearingDays());

        hearingBuilder.withHearingDays(asList(
                getHearingDay(COURT_CENTRE1_ID, COURT_ROOM1_ID, FALSE, 11, 22, NOW)
        ));
        convertedHearing = converter.convert(hearingBuilder.build());
        assertThat(convertedHearing.getHearingDays().size(), equalTo(1));
        assertHearingDay(convertedHearing.getHearingDays().get(0),
                COURT_CENTRE1_ID, COURT_ROOM1_ID, FALSE, 11, 22, NOW, null);

        hearingBuilder.withHearingDays(asList(
                getHearingDay(COURT_CENTRE1_ID, COURT_ROOM1_ID, null, null, 33, NOW),
                getHearingDay(COURT_CENTRE2_ID, COURT_ROOM2_ID, TRUE, 44, null, NOW),
                getHearingDay(COURT_CENTRE3_ID, null, FALSE, 0, 0, null)
        ));
        convertedHearing = converter.convert(hearingBuilder.build());
        assertThat(convertedHearing.getHearingDays().size(), equalTo(3));
        assertHearingDay(convertedHearing.getHearingDays().get(0),
                COURT_CENTRE1_ID, COURT_ROOM1_ID, null, null, 33, NOW, null);
        assertHearingDay(convertedHearing.getHearingDays().get(1),
                COURT_CENTRE2_ID, COURT_ROOM2_ID, TRUE, 44, null, NOW, null);
        assertHearingDay(convertedHearing.getHearingDays().get(2),
                COURT_CENTRE3_ID, null, FALSE, 0, 0, null, null);
    }

    @Test
    public void shouldConvertListingDefendantToCoreDefendant() {
        final JsonObject jsonObject = givenPayload("/test-data/listing.defendant.json");
        final Defendant defendant = jsonObjectToObjectConverter.convert(jsonObject, Defendant.class);
        final uk.gov.justice.core.courts.Defendant coreDefendant = converter.convert(defendant, null);
        assertThat(coreDefendant.getPersonDefendant(), notNullValue());
        assertThat(coreDefendant.getPersonDefendant().getPersonDetails(), notNullValue());
        assertThat(coreDefendant.getPersonDefendant().getPersonDetails().getAddress(), Matchers.is(defendant.getAddress()));
        assertThat(coreDefendant.getPersonDefendant().getPersonDetails().getDateOfBirth(), Matchers.is(defendant.getDateOfBirth()));
        assertThat(coreDefendant.getPersonDefendant().getPersonDetails().getLastName(), Matchers.is(defendant.getLastName()));
        assertThat(coreDefendant.getPersonDefendant().getPersonDetails().getFirstName(), Matchers.is(defendant.getFirstName()));
        assertThat(coreDefendant.getPersonDefendant().getPersonDetails().getFirstName(), Matchers.is(defendant.getFirstName()));
        assertThat(coreDefendant.getId(), Matchers.is(defendant.getId()));
    }

    private uk.gov.justice.listing.events.HearingDay getHearingDay(final UUID courtCentreId, final UUID courtRoomId,
                                                                   final Boolean isCancelled, final Integer listedDurationMinutes,
                                                                   final Integer listingSequence, final ZonedDateTime sittingDay) {
        return uk.gov.justice.listing.events.HearingDay.hearingDay()
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withIsCancelled(isCancelled)
                .withDurationMinutes(listedDurationMinutes)
                .withSequence(listingSequence)
                .withStartTime(sittingDay)
                .build();
    }

    private void assertHearingDay(final HearingDay hearingDay,
                                  final UUID courtCentreId, final UUID courtRoomId,
                                  final Boolean isCancelled, final Integer listedDurationMinutes,
                                  final Integer listingSequence, final ZonedDateTime sittingDay, final Boolean hasSharedResults) {

        assertThat(hearingDay.getCourtCentreId(), equalTo(courtCentreId));
        assertThat(hearingDay.getCourtRoomId(), equalTo(courtRoomId));
        assertThat(hearingDay.getIsCancelled(), equalTo(isCancelled));
        assertThat(hearingDay.getListedDurationMinutes(), equalTo(listedDurationMinutes));
        assertThat(hearingDay.getListingSequence(), equalTo(listingSequence));
        assertThat(hearingDay.getSittingDay(), equalTo(sittingDay));
        assertThat(hearingDay.getHasSharedResults(), equalTo(hasSharedResults));
    }
}