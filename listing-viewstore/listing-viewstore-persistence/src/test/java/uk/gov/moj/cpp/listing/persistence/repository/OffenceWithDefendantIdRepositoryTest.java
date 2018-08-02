package uk.gov.moj.cpp.listing.persistence.repository;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.moj.cpp.listing.persistence.entity.*;

import javax.ejb.Local;
import javax.inject.Inject;
import java.time.LocalDate;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

@RunWith(CdiTestRunner.class)
public class OffenceWithDefendantIdRepositoryTest {

    @Inject
    private OffenceWithDefendantIdRepository offenceRespository;

    @Inject
    private SimpleDefendantRepository defendantRepository;

    @Inject
    private OffenceWithDefendantIdRepository repository;

    @Test
    public void shouldFindOffenceById() {
        final SimpleDefendant defendant = saveDefendant();

        final OffenceWithDefendantId baseOffence = saveOffence(defendant);

        final OffenceWithDefendantId expectedBaseOffence = offenceRespository.findBy(baseOffence.getId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedBaseOffence, baseOffence));
    }

    private SimpleDefendant saveDefendant() {
        final SimpleDefendant defendant = createDefendant();

        defendantRepository.save(defendant);

        return defendant;
    }

    private OffenceWithDefendantId saveOffence(SimpleDefendant defendant) {
        final OffenceWithDefendantId offence = createSimpleOffence(defendant);

        offenceRespository.save(offence);

        return offence;
    }

    private OffenceWithDefendantId createSimpleOffence(SimpleDefendant defendant) {
        CompositeOffenceId compositeOffenceId = createCompositeOffenceId();
        StatementOfOffence statementOfOffence = createStatementOfOffence();
        return new OffenceWithDefendantIdBuilder()
                .setId(compositeOffenceId)
                .setStartDate(LocalDate.now())
                .setOffenceCode(STRING.next())
                .setEndDate(LocalDate.now())
                .setStatementOfOffence(statementOfOffence)
                .setMappedHearingId(defendant.getId().getHearingId())
                .setMappedDefendantId(defendant.getId().getDefendantId())
                .build();
    }

    private SimpleDefendant createDefendant() {
        CompositeDefendantId id = createCompositeDefendantId();
        return new SimpleDefendantBuilder()
                .setLastName(STRING.next())
                .setFirstName(STRING.next())
                .setDefenceOrganisation(STRING.next())
                .setDateOfBirth(LocalDate.now())
                .setBailStatus(STRING.next())
                .setPersonId(randomUUID())
                .setCustodyTimeLimit(LocalDate.now())
                .setCompositeDefendantId(id)
                .build();
    }

    private StatementOfOffence createStatementOfOffence() {
        return new StatementOfOffenceBuilder()
                    .setTitle(STRING.next())
                    .setLegislation(STRING.next())
                    .build();
    }

    private CompositeOffenceId createCompositeOffenceId() {
        return new CompositeOffenceIdBuilder()
                .setHearingId(randomUUID())
                .setOffenceId(randomUUID())
                .setDefendantId(randomUUID())
                .build();
    }

    private CompositeDefendantId createCompositeDefendantId() {
        return new CompositeDefendantIdBuilder()
                .setHearingId(randomUUID())
                .setDefendantId(randomUUID())
                .build();
    }
}