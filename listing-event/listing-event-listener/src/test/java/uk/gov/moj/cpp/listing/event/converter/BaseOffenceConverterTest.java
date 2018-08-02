package uk.gov.moj.cpp.listing.event.converter;

import org.junit.Test;
import uk.gov.justice.listing.events.OffenceUpdated;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.moj.cpp.listing.persistence.entity.BaseOffence;

import javax.swing.text.html.Option;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.listing.events.Offence.offence;
import static uk.gov.justice.listing.events.OffenceUpdated.offenceUpdated;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class BaseOffenceConverterTest {
    BaseOffenceConverter baseOffenceConverter = new BaseOffenceConverter();

    @Test
    public void shouldConvertOffenceUpdatedToSimpleOffence() throws Exception {

        StatementOfOffence soo = StatementOfOffence.statementOfOffence()
                .withLegislation(STRING.next())
                .withTitle(STRING.next())
                .build();

        OffenceUpdated offenceUpdated = offenceUpdated()
                .withOffence(offence().withDefendantId(randomUUID())
                        .withStartDate("2018-06-01")
                        .withOffenceCode(STRING.next())
                        .withId(randomUUID())
                        .withEndDate(Optional.ofNullable("2018-06-20"))
                        .withStatementOfOffence(soo)
                        .build())
                .build();

        BaseOffence baseOffence = baseOffenceConverter.convert(offenceUpdated);

        assertThat(baseOffence.getEndDate().format(DateTimeFormatter.ISO_DATE), is(equalTo(offenceUpdated.getOffence().getEndDate().get())));
        assertThat(baseOffence.getId().getOffenceId(), is(equalTo(offenceUpdated.getOffence().getId())));
        assertThat(baseOffence.getId().getDefendantId(), is(equalTo(offenceUpdated.getOffence().getDefendantId())));
        assertThat(baseOffence.getOffenceCode(), is(equalTo(offenceUpdated.getOffence().getOffenceCode())));
        assertThat(baseOffence.getStatementOfOffence().getTitle(), is(equalTo(offenceUpdated.getOffence().getStatementOfOffence().getTitle())));
        assertThat(baseOffence.getStatementOfOffence().getLegislation(), is(equalTo(offenceUpdated.getOffence().getStatementOfOffence().getLegislation())));
        assertThat(baseOffence.getStartDate().format(DateTimeFormatter.ISO_DATE), is(equalTo(offenceUpdated.getOffence().getStartDate())));
    }

}