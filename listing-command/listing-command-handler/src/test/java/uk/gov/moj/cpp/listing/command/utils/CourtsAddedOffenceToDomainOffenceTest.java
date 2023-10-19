package uk.gov.moj.cpp.listing.command.utils;


import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.core.courts.CustodyTimeLimit.custodyTimeLimit;
import static uk.gov.justice.core.courts.Offence.offence;

import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.courts.AddedOffences;
import uk.gov.moj.cpp.listing.domain.CaseOffences;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class CourtsAddedOffenceToDomainOffenceTest {


    private CourtsAddedOffenceToDomainOffence courtsAddedOffenceToDomainOffence = new CourtsAddedOffenceToDomainOffence();

    @Test
    public void shouldConvert() {

        final UUID offenceId = randomUUID();
        final UUID defenceId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID statusId = randomUUID();

        final CustodyTimeLimit custodyTimeLimit = custodyTimeLimit()
                .withTimeLimit("1")
                .withDaysSpent(1)
                .withIsCtlExtended(true)
                .build();

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .build();

        final LaaReference laaApplnReference =  LaaReference.laaReference().withStatusId(statusId).build();

        Offence offence = offence().withId(offenceId).withOffenceCode("1")
                .withCustodyTimeLimit(custodyTimeLimit)
                .withSeedingHearing(seedingHearing)
                .withLaaApplnReference(laaApplnReference)
                .build();

        AddedOffences addedOffences = AddedOffences.addedOffences()
                .withOffences(asList(offence))
                .withDefendantId(defenceId)
                .withProsecutionCaseId(caseId)
                .build();



        final List<CaseOffences> convert = courtsAddedOffenceToDomainOffence.convert(asList(addedOffences));

        assertThat(convert, hasSize(1));
        assertThat(convert.get(0).getCaseId(), is(caseId));
        assertThat(convert.get(0).getOffences(), hasSize(1));
        assertThat(convert.get(0).getOffences().get(0).getId(), is(offenceId));
        assertThat(convert.get(0).getDefendantId(), is(defenceId));
    }
}