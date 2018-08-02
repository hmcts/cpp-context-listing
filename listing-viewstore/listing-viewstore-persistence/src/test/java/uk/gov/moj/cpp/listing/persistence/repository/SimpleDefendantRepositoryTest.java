package uk.gov.moj.cpp.listing.persistence.repository;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.moj.cpp.listing.persistence.entity.*;

import javax.inject.Inject;
import java.time.LocalDate;

import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertTrue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

@RunWith(CdiTestRunner.class)
public class SimpleDefendantRepositoryTest {

    @Inject
    private SimpleDefendantRepository simpleDefendantRepository;

    @Test
    public void shouldFindHSimpleDefendantById() {
        final SimpleDefendant simpleDefendant = saveSimpleDefendant();

        final SimpleDefendant expectedSimpleDefendant = simpleDefendantRepository.findBy(simpleDefendant.getId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedSimpleDefendant, simpleDefendant));
    }

    private SimpleDefendant saveSimpleDefendant() {
        final SimpleDefendant simpleDefendant = createSimpleDefendant();

        simpleDefendantRepository.save(simpleDefendant);

        return simpleDefendant;
    }

    private SimpleDefendant createSimpleDefendant() {
        CompositeDefendantId compositeDefendantId = createCompositeODefedantId();
        return new SimpleDefendantBuilder()
                .setCompositeDefendantId(compositeDefendantId)
                .setCustodyTimeLimit(LocalDate.now())
                .setPersonId(randomUUID())
                .setBailStatus(STRING.next())
                .setDateOfBirth(LocalDate.now())
                .setDefenceOrganisation(STRING.next())
                .setFirstName(STRING.next())
                .setLastName(STRING.next())
                .build();
    }

    private CompositeDefendantId createCompositeODefedantId() {
        return new CompositeDefendantIdBuilder()
                .setHearingId(randomUUID())
                .setDefendantId(randomUUID())
                .build();
    }
}