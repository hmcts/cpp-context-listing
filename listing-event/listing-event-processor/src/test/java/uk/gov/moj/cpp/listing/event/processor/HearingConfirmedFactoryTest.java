package uk.gov.moj.cpp.listing.event.processor;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.HearingAllocatedForListing;
import uk.gov.moj.cpp.listing.event.HearingDate;
import uk.gov.moj.cpp.listing.event.external.HearingConfirmed;
import uk.gov.moj.cpp.listing.event.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.persistence.entity.Defendant;
import uk.gov.moj.cpp.listing.persistence.entity.DefendantBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCaseBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.Offence;
import uk.gov.moj.cpp.listing.persistence.entity.OffenceBuilder;
import uk.gov.moj.cpp.listing.persistence.entity.StatementOfOffence;
import uk.gov.moj.cpp.listing.persistence.entity.StatementOfOffenceBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingConfirmedFactoryTest {
    private static final String FIELD_GENERIC_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_COURT_ROOMS = "courtRooms";

    private static final String FIELD_TITLE = "title";
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_LAST_NAME = "lastName";

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID LISTING_OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final String COURT_ROOM_1_NAME = RandomGenerator.STRING.next();
    private static final UUID COURT_ROOM_1_ID = UUID.randomUUID();
    private static final String COURT_ROOM_2_NAME = RandomGenerator.STRING.next();
    private static final UUID COURT_ROOM_2_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final String TYPE = RandomGenerator.STRING.next();
    private static final Integer ESTIMATED_MINUTES = RandomGenerator.INTEGER.next();
    private static final String BAIL_STATUS = RandomGenerator.STRING.next();
    private static final String DEFENCE_ORGANISATION = RandomGenerator.STRING.next();
    private static final LocalDate DATE = LocalDate.now();
    private static final LocalDate DOB = LocalDate.now().minusYears(45);
    private static final LocalDate CUSTODY_TIME_LIMIT = LocalDate.now().plusYears(5);

    private static final String OFFENCE_CODE = RandomGenerator.STRING.next();
    private static final String LEGISLATION = RandomGenerator.STRING.next();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final LocalDate UPDATED_START_DATE = LocalDate.of(2018,6,1);
    private static final LocalTime UPDATED_START_TIME = LocalTime.of(10,0);
    private static final Boolean NOT_BEFORE = false;
    private static final Boolean ALLOCATED = Boolean.TRUE;
    private static final UUID LISTING_DEFENDANT_ID = UUID.randomUUID();
    private static final String COURT_CENTRE_NAME = RandomGenerator.STRING.next();
    private static final String TITLE = RandomGenerator.STRING.next();
    private static final String FIRST_NAME = RandomGenerator.STRING.next();
    private static final String LAST_NAME = RandomGenerator.STRING.next();
    private static final String URN = RandomGenerator.STRING.next();


    @InjectMocks
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Mock
    private ReferenceDataService referenceDataService;


    @Mock
    private JsonEnvelope envelope;


    @Test
    public void shouldCreateAHearingConfirmed() throws Exception {
        //given
        given(referenceDataService.getCourtCentreById(COURT_CENTRE_ID, envelope)).willReturn(courtCentre());
        given(referenceDataService.getJudgeById(JUDGE_ID, envelope)).willReturn(judge());

        Hearing hearing = hearing();
        HearingAllocatedForListing hearingUpdated = hearingAllocatedForListing();
        ListingCase listingCase = listingCase();

        //when
        HearingConfirmed actual = hearingConfirmedFactory.create(hearing,listingCase, hearingUpdated, envelope);

        //then
        assertThat(actual.getCaseId(), is(listingCase.getCaseId().toString()));
        assertThat(actual.getUrn(), is(listingCase.getUrn()));
        
        uk.gov.moj.cpp.listing.event.external.Hearing scheduledHearing = actual.getHearing();
        assertThat(scheduledHearing.getId(), is(hearing.getId().toString()));
        assertThat(scheduledHearing.getEstimateMinutes(), is(hearingUpdated.getEstimateMinutes()));
        assertThat(scheduledHearing.getStartDateTime(), is("2018-06-01T10:00:00Z"));
        assertThat(scheduledHearing.getType(), is(hearingUpdated.getType()));
        assertThat(scheduledHearing.getCaseId(), is(hearing.getListingCaseId().toString()));
        assertThat(scheduledHearing.getCourtCentreId(), is(hearing.getCourtCentreId().toString()));
        assertThat(scheduledHearing.getCourtCentreName(), is(COURT_CENTRE_NAME));
        assertThat(scheduledHearing.getCourtRoomId(), is(hearingUpdated.getCourtRoomId()));
        assertThat(scheduledHearing.getCourtRoomName(), is(COURT_ROOM_1_NAME));
        assertThat(scheduledHearing.getJudge().getId(), is(hearingUpdated.getJudgeId()));
        assertThat(scheduledHearing.getJudge().getTitle(), is(TITLE));
        assertThat(scheduledHearing.getJudge().getFirstName(), is(FIRST_NAME));
        assertThat(scheduledHearing.getJudge().getLastName(), is(LAST_NAME));

        assertThat(scheduledHearing.getDefendants().size(), is(1));
        assertThat(scheduledHearing.getDefendants(), contains(allOf(
                hasProperty("id", is(DEFENDANT_ID.toString())),
                hasProperty("personId", is(PERSON_ID.toString())),
                hasProperty("firstName", is(FIRST_NAME)),
                hasProperty("lastName", is(LAST_NAME)),
                hasProperty("dateOfBirth", is(DOB)),
                hasProperty("bailStatus", is(BAIL_STATUS)),
                hasProperty("custodyTimeLimit", is(CUSTODY_TIME_LIMIT)),
                hasProperty("defenceOrganisation", is(DEFENCE_ORGANISATION)))));

        List<uk.gov.moj.cpp.listing.domain.Defendant> actualDefendants = scheduledHearing.getDefendants().stream().limit(1).collect(Collectors.toList());
        assertThat(actualDefendants.get(0).getOffences().size(), is(1));
        assertThat(actualDefendants.get(0).getOffences(), contains(allOf(
                hasProperty("id", is(OFFENCE_ID.toString())),
                hasProperty("offenceCode", is(OFFENCE_CODE)),
                hasProperty("startDate", is(DATE)),
                hasProperty("endDate", is(DATE)))));

        uk.gov.moj.cpp.listing.domain.StatementOfOffence statementOfOffence = actualDefendants.get(0).getOffences().get(0).getStatementOfOffence();
        assertThat(statementOfOffence, allOf(
                hasProperty("title", is(TITLE.toString())),
                hasProperty("legislation", is(LEGISLATION))));

    }

    private JsonEnvelope courtCentre() {
        final JsonArrayBuilder courtRooms = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(FIELD_GENERIC_ID, COURT_ROOM_1_ID.toString())
                        .add(FIELD_NAME, COURT_ROOM_1_NAME))
                .add(createObjectBuilder()
                        .add(FIELD_GENERIC_ID, COURT_ROOM_2_ID.toString())
                        .add(FIELD_NAME, COURT_ROOM_2_NAME));

        final JsonObjectBuilder courtCentreJson = createObjectBuilder()
                .add(FIELD_GENERIC_ID, COURT_CENTRE_ID.toString())
                .add(FIELD_NAME, COURT_CENTRE_NAME)
                .add(FIELD_COURT_ROOMS, courtRooms.build());

        return envelopeFrom(metadataWithRandomUUIDAndName(), courtCentreJson.build());
    }

    private JsonEnvelope judge() {
        final JsonObjectBuilder json = createObjectBuilder()
                .add(FIELD_GENERIC_ID, JUDGE_ID.toString())
                .add(FIELD_TITLE, TITLE)
                .add(FIELD_FIRST_NAME, FIRST_NAME)
                .add(FIELD_LAST_NAME, LAST_NAME);

        return envelopeFrom(metadataWithRandomUUIDAndName(), json.build());
    }

    private HearingAllocatedForListing hearingAllocatedForListing() {
        return new HearingAllocatedForListing(HEARING_ID.toString(), TYPE, ESTIMATED_MINUTES,
                JUDGE_ID.toString(), COURT_ROOM_1_ID.toString(), new HearingDate(UPDATED_START_DATE, UPDATED_START_TIME, NOT_BEFORE));
    }

    private ListingCase listingCase() {
        ListingCaseBuilder listingCaseBuilder = new ListingCaseBuilder()
                .setCaseId(CASE_ID)
                .setUrn(URN);
        return listingCaseBuilder.build();
    }

    private Hearing hearing() {
        StatementOfOffence sof = createStatementOfOffence();
        Offence offence = createOffence(sof);
        Defendant defendant = createDefendant(offence);

        return new HearingBuilder()
                .setId(HEARING_ID)
                .setEstimateMinutes(RandomGenerator.INTEGER.next())
                .setCourtCentreId(COURT_CENTRE_ID)
                .setCourtRoomId(COURT_ROOM_ID)
                .setJudgeId(JUDGE_ID)
                .setNotBefore(NOT_BEFORE)
                .setStartDate(START_DATE)
                .setStartTime(START_TIME)
                .setListingCaseId(CASE_ID)
                .setType(TYPE)
                .setAllocated(ALLOCATED)
                .setDefendants(new HashSet<>(Arrays.asList(defendant)))
                .build();
    }


    private Offence createOffence(final StatementOfOffence sof) {
        Offence offence = new OffenceBuilder()
                .setStatementOfOffence(sof)
                .setOffenceCode(OFFENCE_CODE)
                .setListingOffenceId(LISTING_OFFENCE_ID)
                .setOffenceId(OFFENCE_ID)
                .setStartDate(DATE)
                .setEndDate(DATE)
                .build();
        return offence;
    }

    private Defendant createDefendant(final Offence offence) {
        Defendant defandant = new DefendantBuilder()
                .setListingDefendantId(LISTING_DEFENDANT_ID)
                .setDefendantId(DEFENDANT_ID)
                .setPersonId(PERSON_ID)
                .setFirstName(FIRST_NAME)
                .setLastName(LAST_NAME)
                .setDateOfBirth(DOB)
                .setBailStatus(BAIL_STATUS)
                .setCustodyTimeLimit(CUSTODY_TIME_LIMIT)
                .setDefenceOrganisation(DEFENCE_ORGANISATION)
                .setOffences(new HashSet<>(Arrays.asList(offence)))
                .build();
        return defandant;
    }

    private StatementOfOffence createStatementOfOffence() {
        StatementOfOffence statementOfOffence = new StatementOfOffenceBuilder()
                .setLegislation(LEGISLATION)
                .setTitle(TITLE)
                .build();
        return statementOfOffence;
    }
  
}