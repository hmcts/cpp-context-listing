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
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final String PLEA = RandomGenerator.STRING.next();
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
    private static final String COURT_CENTRE_ID = RandomGenerator.STRING.next();
    private static final Boolean ALLOCATED = Boolean.TRUE;

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
        assertThat(hearingSummary.getDate(), is(hearing.getStartDateTime()));
        assertThat(hearingSummary.getEstimate(), is(hearing.getEstimateMinutes()));
        assertThat(hearingSummary.getDefendants().size(), is(1));
        assertThat(hearingSummary.getDefendants(), contains(allOf(hasProperty("id", is(ID)),
                hasProperty("firstName", is(FIRST_NAME)),
                hasProperty("lastName", is(LAST_NAME)),
                hasProperty("bailStatus", is(BAIL_STATUS)))));

        List<DefendantSummary> defendantSummaries = hearingSummary.getDefendants().stream().limit(1).collect(Collectors.toList());

        assertThat(defendantSummaries.get(0).getOffences(), contains(allOf(hasProperty("id", is(ID.toString())),
                hasProperty("title", is(TITLE)))));
    }


    private Hearing createHearing() {
        return new HearingBuilder()
                .setId(UUID.randomUUID())
                .setEstimateMinutes(RandomGenerator.INTEGER.next())
                .setCourtCentreId(COURT_CENTRE_ID)
                .setListingCase(createListingCase())
                .setType(TYPE)
                .setAllocated(ALLOCATED)
                .build();
    }

    private ListingCase createListingCase() {

        StatementOfOffence statementOfOffence = new StatementOfOffenceBuilder()
                .setLegislation(LEGISLATION)
                .setTitle(TITLE)
                .build();

        Offence offence = new OffenceBuilder()
                .setStatementOfOffence(statementOfOffence)
                .setOffenceCode(OFFENCE_CODE)
                .setId(ID)
                .setPlea(PLEA)
                .setEndDate(DATE)
                .setStartDate(DATE)
                .build();

        Defendant defandant = new DefendantBuilder()
                .setBailStatus(BAIL_STATUS)
                .setId(ID)
                .setPersonId(PERSON_ID)
                .setFirstName(FIRST_NAME)
                .setLastName(LAST_NAME)
                .setDefenceOrganisation(DEFENCE_ORGANISATION)
                .setDateOfBirth(DATE)
                .setOffences(new HashSet<>(Arrays.asList(offence)))
                .build();

        ListingCase aCase = new ListingCaseBuilder()
                .setDefendants(new HashSet<>(Arrays.asList(defandant)))
                .setId(ID)
                .setUrn(URN)
                .setSendingCommittalDate(DATE)
                .build();
        return aCase;
    }
}