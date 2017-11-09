package uk.gov.moj.cpp.listing.query.view.hearing;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingSummaryConverterTest {
    private static final UUID ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID LISTING_OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final String TYPE = RandomGenerator.STRING.next();
    private static final String FIRST_NAME = RandomGenerator.STRING.next();
    private static final String LAST_NAME = "Testing";
    private static final String BAIL_STATUS = RandomGenerator.STRING.next();
    private static final String DEFENCE_ORGANISATION = RandomGenerator.STRING.next();
    private static final LocalDate DATE = LocalDate.now();
    private static final String OFFENCE_CODE = RandomGenerator.STRING.next();
    private static final String LEGISLATION = RandomGenerator.STRING.next();
    private static final String TITLE = RandomGenerator.STRING.next();
    private static final String URN = RandomGenerator.STRING.next();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalTime START_TIME = LocalTime.now();
    private static final boolean NOT_BEFORE = false;
    private static final Boolean ALLOCATED = Boolean.TRUE;
    private static final UUID LISTING_DEFENDANT_ID = UUID.randomUUID();

    @InjectMocks
    private HearingSummaryConverter hearingSummaryConverter;

    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Test
    public void shouldConvertToHearingSummary() throws Exception {
        final Hearing hearing = createHearing();

        HearingSummary hearingSummary = hearingSummaryConverter.convert(hearing);

        assertThat(hearingSummary.getId().toString(), is(hearing.getId().toString()));
        assertThat(hearingSummary.getType(), is(hearing.getType()));
        assertThat(hearingSummary.getStartDate(), is(hearing.getStartDate()));
        assertThat(hearingSummary.getStartTime(), is(hearing.getStartTime()));
        assertThat(hearingSummary.getCourtCentreId(), is(hearing.getCourtCentreId()));
        assertThat(hearingSummary.getCourtRoomId(), is(hearing.getCourtRoomId()));
        assertThat(hearingSummary.getJudgeId(), is(hearing.getJudgeId()));
        assertThat(hearingSummary.getNotBefore(), is(hearing.getNotBefore()));
        assertThat(hearingSummary.getEstimateMinutes(), is(hearing.getEstimateMinutes()));
        assertThat(hearingSummary.getDefendants().size(), is(1));
        assertThat(hearingSummary.getDefendants(), contains(allOf(hasProperty("id", is(LISTING_DEFENDANT_ID)),
                hasProperty("firstName", is(FIRST_NAME)),
                hasProperty("lastName", is(LAST_NAME)),
                hasProperty("bailStatus", is(BAIL_STATUS)))));

        List<DefendantSummary> defendantSummaries = hearingSummary.getDefendants().stream().limit(1).collect(Collectors.toList());

        assertThat(defendantSummaries.get(0).getOffences(), contains(allOf(hasProperty("id", is(OFFENCE_ID.toString())),
                hasProperty("title", is(TITLE)))));
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
                .setEstimateMinutes(RandomGenerator.INTEGER.next())
                .setCourtCentreId(COURT_CENTRE_ID)
                .setCourtRoomId(COURT_ROOM_ID)
                .setJudgeId(JUDGE_ID)
                .setNotBefore(NOT_BEFORE)
                .setStartDate(START_DATE)
                .setStartTime(START_TIME)
                .setListingCase(createListingCase())
                .setType(TYPE)
                .setAllocated(ALLOCATED)
                .setDefendants(new HashSet<>(Arrays.asList(defendant)))
                .build();
    }

    private ListingCase createListingCase() {

        ListingCase aCase = new ListingCaseBuilder()
                .setCaseId(CASE_ID)
                .setUrn(URN)
                .build();
        return aCase;
    }
}