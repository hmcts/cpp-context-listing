package uk.gov.moj.cpp.listing.command.utils;

import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.listing.commands.Defendant.defendant;
import static uk.gov.justice.listing.events.Offence.offence;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.listing.commands.Defendant;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.ReportingRestriction;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.listing.events.LaaReference;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;


public class CommandDefendantToDomainConverterTest {


    private CommandDefendantToDomainConverter commandDefendantToDomainConverter = new CommandDefendantToDomainConverter();


    @Test
    public void shouldConvert(){

        final UUID defendantId = randomUUID();

        final UUID offenceId = randomUUID();
        final UUID statusId = randomUUID();
        final UUID restrictionId = randomUUID();

        final StatementOfOffence statementOfOffence = StatementOfOffence.statementOfOffence()
                .withTitle("title")
                .withLegislation("legislation")
                .build();

        final ReportingRestriction reportingRestrictions = ReportingRestriction.reportingRestriction().withId(restrictionId).build();

        final Offence offence = offence()
                .withId(offenceId)
                .withStatementOfOffence(statementOfOffence)
                .withLaaApplnReference(LaaReference.laaReference().withStatusId(statusId).build())
                .withReportingRestrictions(asList(reportingRestrictions))
                .build();

        final Defendant defendant = defendant()
                .withId(defendantId)
                .withOffences(asList(offence))
                .withAddress(Address.address().
                        withAddress1("adressLine1")
                        .withAddress2("adressLine2")
                        .withAddress3("adressLine3")
                        .withAddress4("adressLine4")
                        .withAddress5("adressLine5")
                        .withPostcode("IG1 1NL")
                        .build())
                .build();


        final List<uk.gov.moj.cpp.listing.domain.Defendant> defendants = commandDefendantToDomainConverter.convert(asList(defendant));

        assertThat(defendants, hasSize(1));
        assertThat(defendants.get(0).getId(), is(defendantId));
        assertThat(defendants.get(0).getOffences(), hasSize(1));
        assertThat(defendants.get(0).getOffences().get(0).getId(), is(offenceId));
        assertThat(defendants.get(0).getAddress().get().getAddress1(), is(defendant.getAddress().getAddress1()));
        assertThat(defendants.get(0).getAddress().get().getAddress2().get(), is(defendant.getAddress().getAddress2()));
        assertThat(defendants.get(0).getAddress().get().getAddress3().get(), is(defendant.getAddress().getAddress3()));
        assertThat(defendants.get(0).getAddress().get().getAddress4().get(), is(defendant.getAddress().getAddress4()));
        assertThat(defendants.get(0).getAddress().get().getAddress5().get(), is(defendant.getAddress().getAddress5()));
        assertThat(defendants.get(0).getAddress().get().getPostcode().get(), is(defendant.getAddress().getPostcode()));





    }

}