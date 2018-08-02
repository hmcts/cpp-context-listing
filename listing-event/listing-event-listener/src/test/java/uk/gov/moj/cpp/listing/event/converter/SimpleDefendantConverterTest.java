package uk.gov.moj.cpp.listing.event.converter;

import org.junit.Test;
import uk.gov.justice.listing.events.DefendantDetailsUpdated;
import uk.gov.moj.cpp.listing.persistence.entity.SimpleDefendant;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.google.common.base.Ascii.toUpperCase;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.listing.events.BailStatus.CONDITIONAL;
import static uk.gov.justice.listing.events.BaseDefendant.baseDefendant;
import static uk.gov.justice.listing.events.DefendantDetailsUpdated.defendantDetailsUpdated;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class SimpleDefendantConverterTest {

    SimpleDefendantConverter simpleDefendantConverter = new SimpleDefendantConverter();

    @Test
    public void shouldConvertDefendantDetailsUpdatedToSimpleDefendant() throws Exception {

        DefendantDetailsUpdated defendantDetailsUpdated = defendantDetailsUpdated()
                .withDefendant(baseDefendant()
                        .withId(randomUUID())
                        .withHearingId(randomUUID())
                        .withBailStatus(CONDITIONAL)
                        .withCustodyTimeLimit(Optional.of("2018-07-01"))
                        .withDateOfBirth("1980-01-01")
                        .withDefenceOrganisation(STRING.next())
                        .withFirstName(STRING.next())
                        .withLastName(STRING.next())
                        .withPersonId(randomUUID())
                        .build())
                .withHearingId(randomUUID())
                .build();

        SimpleDefendant simpleDefendant = simpleDefendantConverter.convert(defendantDetailsUpdated);

        assertThat(simpleDefendant.getId().getDefendantId(), is(equalTo(defendantDetailsUpdated.getDefendant().getId())));
        assertThat(simpleDefendant.getId().getHearingId(), is(equalTo(defendantDetailsUpdated.getHearingId())));
        assertThat(simpleDefendant.getBailStatus(), is(equalTo(defendantDetailsUpdated.getDefendant().getBailStatus().toString())));
        assertThat(simpleDefendant.getCustodyTimeLimit().format(DateTimeFormatter.ISO_DATE), is(equalTo(defendantDetailsUpdated.getDefendant().getCustodyTimeLimit().get())));
        assertThat(simpleDefendant.getDateOfBirth().format(DateTimeFormatter.ISO_DATE), is(equalTo(defendantDetailsUpdated.getDefendant().getDateOfBirth())));
        assertThat(simpleDefendant.getDefenceOrganisation(), is(equalTo(defendantDetailsUpdated.getDefendant().getDefenceOrganisation())));
        assertThat(simpleDefendant.getFirstName(), is(equalTo(defendantDetailsUpdated.getDefendant().getFirstName())));
        assertThat(simpleDefendant.getLastName(), is(equalTo(defendantDetailsUpdated.getDefendant().getLastName())));
        assertThat(simpleDefendant.getPersonId(), is(equalTo(defendantDetailsUpdated.getDefendant().getPersonId())));

    }
}