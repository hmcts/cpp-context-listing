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
public class BaseOffenceRepositoryTest {

    @Inject
    private BaseOffenceRepository baseOffenceRepository;

    @Test
    public void shouldFindOffenceById() {
        final BaseOffence offence = saveOffence();

        final BaseOffence expectedBaseOffence = baseOffenceRepository.findBy(offence.getId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedBaseOffence, offence));
    }

    private BaseOffence saveOffence() {
        final BaseOffence baseOffence = createOffence();

        baseOffenceRepository.save(baseOffence);

        return baseOffence;
    }

    private BaseOffence createOffence() {
        CompositeOffenceId compositeOffenceId = createCompositeOffenceId();
        return new BaseOffenceBuilder()
                .setId(compositeOffenceId)
                .setStartDate(LocalDate.now())
                .setOffenceCode(STRING.next())
                .setEndDate(LocalDate.now())
                .build();
    }

    private CompositeOffenceId createCompositeOffenceId() {
        return new CompositeOffenceIdBuilder()
                    .setOffenceId(randomUUID())
                    .setHearingId(randomUUID())
                    .setDefendantId(randomUUID())
                    .build();
    }
}