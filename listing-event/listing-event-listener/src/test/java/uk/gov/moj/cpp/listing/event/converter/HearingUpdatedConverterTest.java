package uk.gov.moj.cpp.listing.event.converter;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.event.HearingPeriod;
import uk.gov.moj.cpp.listing.event.HearingUpdatedForListing;
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
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingUpdatedConverterTest {

    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID LISTING_OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final String TYPE = RandomGenerator.STRING.next();
    private static final String TYPE_2 = RandomGenerator.STRING.next();
    private static final String FIRST_NAME = RandomGenerator.STRING.next();
    private static final String LAST_NAME = "Testing";
    private static final String BAIL_STATUS = RandomGenerator.STRING.next();
    private static final String DEFENCE_ORGANISATION = RandomGenerator.STRING.next();
    private static final LocalDate DATE = LocalDate.now();
    private static final String OFFENCE_CODE = RandomGenerator.STRING.next();
    private static final String LEGISLATION = RandomGenerator.STRING.next();
    private static final String TITLE = RandomGenerator.STRING.next();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID_2 = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID_2 = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalDate START_DATE_2 = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final LocalTime START_TIME_2 = LocalTime.now();
    private static final boolean NOT_BEFORE_TRUE = true;
    private static final boolean NOT_BEFORE_FALSE = false;
    private static final Integer ESTIMATE_MINS = RandomGenerator.INTEGER.next();
    private static final Integer ESTIMATE_MINS_2 = RandomGenerator.INTEGER.next();
    private static final Boolean ALLOCATED = Boolean.TRUE;
    private static final UUID LISTING_DEFENDANT_ID = UUID.randomUUID();


    @InjectMocks
    private HearingUpdatedConverter hearingUpdatedConverter;

    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private HearingRepository hearingRepository;


    @Test
    public void shouldConvertHearingUpdatedForListingForAllPossibleChangesToHearing() throws Exception {
        // Given
        HearingUpdatedForListing hearingUpdatedForListing = createHearingUpdatedForListing();
        Hearing hearingBeforeUpdate = createHearing();
        given(hearingRepository.findBy(anyObject())).willReturn(hearingBeforeUpdate);
                  
        //when
        Hearing actual = hearingUpdatedConverter.convert(hearingUpdatedForListing);

        //then
        assertHearing(actual, hearingBeforeUpdate);
        assertHearingOnAllPossibleChanges(actual, hearingUpdatedForListing);

        assertThat(actual.getDefendants().size(), is(1));
        Defendant defendantBeforeUpdate = hearingBeforeUpdate.getDefendants().toArray(new Defendant[1])[0];
        Defendant actualDefendant = actual.getDefendants().toArray(new Defendant[1])[0];
        assertDefendant(actualDefendant, defendantBeforeUpdate);

        assertThat(actualDefendant.getOffences().size(), is(1));
        Offence offenceBeforeUpdate = defendantBeforeUpdate.getOffences().toArray(new Offence[1])[0];
        Offence actualOffence = actualDefendant.getOffences().toArray(new Offence[1])[0];
        assertOffence(actualOffence, offenceBeforeUpdate);
    }



    private void assertHearingOnAllPossibleChanges(final uk.gov.moj.cpp.listing.persistence.entity.Hearing actual, final HearingUpdatedForListing hearingUpdatedForListing) {
        assertThat(actual.getCourtRoomId().toString(), is(hearingUpdatedForListing.getCourtRoomId()));
        assertThat(actual.getJudgeId().toString(), is(hearingUpdatedForListing.getJudgeId()));
        assertThat(actual.getEstimateMinutes(), is(hearingUpdatedForListing.getEstimateMinutes()));
        assertThat(actual.getId().toString(), is(hearingUpdatedForListing.getHearingId()));
        assertThat(actual.getStartDate(), is(hearingUpdatedForListing.getHearingPeriod().getStartDate()));
        assertThat(actual.getStartTime(), is(hearingUpdatedForListing.getHearingPeriod().getStartTime()));
        assertThat(actual.getNotBefore(), is(hearingUpdatedForListing.getHearingPeriod().getNotBefore()));
        assertThat(actual.getType(), is(hearingUpdatedForListing.getType()));
    }

    private void assertHearing(final uk.gov.moj.cpp.listing.persistence.entity.Hearing actual, final Hearing hearingBeforeUpdate) {
        assertThat(actual.getId(), is(hearingBeforeUpdate.getId()));
        assertThat(actual.getCourtCentreId(), is(hearingBeforeUpdate.getCourtCentreId()));
        assertThat(actual.getAllocated(), is(hearingBeforeUpdate.getAllocated()));
        assertThat(actual.getListingCase(), is(hearingBeforeUpdate.getListingCase()));
    }

    private void assertDefendant(final Defendant actual, final Defendant defendant) {
        assertThat(actual.getBailStatus(), is(defendant.getBailStatus()));
        assertThat(actual.getDateOfBirth(), is(defendant.getDateOfBirth()));
        assertThat(actual.getDefenceOrganisation(), is(defendant.getDefenceOrganisation()));
        assertThat(actual.getFirstName(), is(defendant.getFirstName()));
        assertThat(actual.getDefendantId(), is(defendant.getDefendantId()));
        assertThat(actual.getLastName(), is(defendant.getLastName()));
        assertThat(actual.getPersonId(), is(defendant.getPersonId()));
        assertThat(actual.getListingDefendantId(), is(defendant.getListingDefendantId()));
    }

    private void assertOffence(final Offence actual, final Offence offence) {
        assertThat(actual.getEndDate(), is(offence.getEndDate()));
        assertThat(actual.getOffenceId(), is(offence.getOffenceId()));
        assertThat(actual.getOffenceCode(), is(offence.getOffenceCode()));
        assertThat(actual.getStartDate(), is(offence.getStartDate()));

        assertThat(actual.getStatementOfOffence().getLegislation(), is(offence
                .getStatementOfOffence().getLegislation()));
        assertThat(actual.getStatementOfOffence().getTitle(), is(offence
                .getStatementOfOffence().getTitle()));
    }

    private HearingUpdatedForListing createHearingUpdatedForListing() {
        return new HearingUpdatedForListing(HEARING_ID.toString(), JUDGE_ID_2.toString(), COURT_ROOM_ID_2.toString(),
                TYPE_2, new HearingPeriod(START_DATE_2, START_TIME_2, NOT_BEFORE_TRUE), ESTIMATE_MINS_2);
    }


    private HearingUpdatedForListing createHearingUpdatedForListingForOnlyMandatoryChanges() {
        return new HearingUpdatedForListing(HEARING_ID.toString(), null, null, TYPE_2,
                new HearingPeriod(START_DATE_2, null, null), ESTIMATE_MINS_2);
    }



    private Offence createOffence(final StatementOfOffence sof) {
        Offence offence = new OffenceBuilder()
                .setStatementOfOffence(sof)
                .setOffenceCode(OFFENCE_CODE)
                .setListingOffenceId(LISTING_OFFENCE_ID)
                .setOffenceId(OFFENCE_ID)
                .setEndDate(DATE)
                .setStartDate(DATE)
                .build();
        return offence;
    }

    private Defendant createDefendant(final Offence offence) {
        Defendant defandant = new DefendantBuilder()
                .setListingDefendantId(LISTING_DEFENDANT_ID)
                .setBailStatus(BAIL_STATUS)
                .setDefendantId(DEFENDANT_ID)
                .setPersonId(PERSON_ID)
                .setFirstName(FIRST_NAME)
                .setLastName(LAST_NAME)
                .setDefenceOrganisation(DEFENCE_ORGANISATION)
                .setDateOfBirth(DATE)
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

    private Hearing createHearing() {
        StatementOfOffence sof = createStatementOfOffence();
        Offence offence = createOffence(sof);
        Defendant defendant = createDefendant(offence);

        return new HearingBuilder()
                .setId(HEARING_ID)
                .setEstimateMinutes(ESTIMATE_MINS)
                .setCourtCentreId(COURT_CENTRE_ID)
                .setCourtRoomId(COURT_ROOM_ID)
                .setJudgeId(JUDGE_ID)
                .setNotBefore(NOT_BEFORE_FALSE)
                .setStartDate(START_DATE)
                .setStartTime(START_TIME)
                .setListingCase(createListingCase())
                .setType(TYPE)
                .setAllocated(ALLOCATED)
                .setDefendants(new HashSet<>(Arrays.asList(defendant)))
                .build();
    }

    private ListingCase createListingCase() {
        ListingCaseBuilder listingCaseBuilder = new ListingCaseBuilder();
        listingCaseBuilder.setCaseId(randomUUID())
                .setUrn(STRING.next());

        return listingCaseBuilder.build();
    }

}