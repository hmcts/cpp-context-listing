package uk.gov.moj.cpp.listing.persistence.persistence;

import static org.junit.Assert.*;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class HearingRepositoryTest {

    @Inject
    private HearingRepository hearingRepository;

    @Test
    public void shouldFindCaseById() {

        final Hearing actualHearing = saveHearing();
        hearingRepository.save(actualHearing);

        final Hearing expectedCaseDetail = hearingRepository.findBy(actualHearing.getId());

        assertTrue(EqualsBuilder.reflectionEquals(expectedCaseDetail, actualHearing));
    }

    private Hearing saveHearing() {
        final Hearing hearing = createHearing() ;

        hearingRepository.save(hearing);

        return hearing;
    }

    private Hearing createHearing() {
        return new HearingBuilder()
                .setId(UUID.randomUUID())
                .build();
    }
}