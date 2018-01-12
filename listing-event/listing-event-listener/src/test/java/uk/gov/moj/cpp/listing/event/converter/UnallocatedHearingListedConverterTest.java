package uk.gov.moj.cpp.listing.event.converter;


import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.event.UnallocatedHearingListed;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;;
import java.util.UUID;


import org.junit.Test;

public class UnallocatedHearingListedConverterTest {

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.parse("2018-06-01");
    private static final int ESTIMATE_MINUTES = 7200;
    private static final String TYPE = "TRIAL";

    private UnallocatedHearingListedConverter unallocatedHearingListedConverter = new UnallocatedHearingListedConverter();;

    
    @Test
    public void shouldConvertUnallocatedHearingListedToHearing() throws Exception {
        Hearing actual = unallocatedHearingListedConverter.convert(new UnallocatedHearingListed(HEARING_ID.toString(), TYPE, START_DATE, ESTIMATE_MINUTES,
                CASE_ID.toString(), COURT_CENTRE_ID.toString(), Arrays.asList(buildDefendant())));

        assertThat(actual.getId(), is(equalTo(HEARING_ID)));
        assertThat(actual.getListingCaseId(), is(equalTo(CASE_ID)));
        assertThat(actual.getCourtCentreId(), is(equalTo(COURT_CENTRE_ID)));
        assertThat(actual.getStartDate(), is(equalTo(START_DATE)));
        assertThat(actual.getEstimateMinutes(), is(equalTo(ESTIMATE_MINUTES)));
        assertThat(actual.getType(), is(equalTo(TYPE)));
    }
    

    public static Defendant buildDefendant() {
        StatementOfOffence statementOfOffence = createStatementOfOffence();
        Offence offence = createOffence(statementOfOffence);
        return createDefendant(offence);
    }


    private static Defendant createDefendant(final Offence offence) {
        return new Defendant(randomUUID().toString(), randomUUID().toString(), RandomGenerator.STRING.next(), RandomGenerator.STRING.next(),
                LocalDate.now(), RandomGenerator.STRING.next(), LocalDate.now(), RandomGenerator.STRING.next(), Collections.singletonList
                (offence));
    }

    private static StatementOfOffence createStatementOfOffence() {
        return new StatementOfOffence(RandomGenerator.STRING.next(), RandomGenerator.STRING.next());
    }

    private static Offence createOffence(final StatementOfOffence statementOfOffence) {
        return new Offence(randomUUID().toString(),  RandomGenerator.STRING.next(), LocalDate.now(), LocalDate.now(), statementOfOffence);
    }
}