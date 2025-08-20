package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.listing.events.CourtApplicationAddedToHearing;
import uk.gov.justice.listing.events.LaaReferenceForApplicationUpdated;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ApplicationAggregateTest {

    @InjectMocks
    private Application applicationAggregate;

    @Test
    public void shouldRaiseLaaReferenceForApplicationUpdated() {

        final LaaReferenceForApplicationUpdated laaReferenceForApplicationUpdated = LaaReferenceForApplicationUpdated.laaReferenceForApplicationUpdated()
                .withApplicationId(randomUUID())
                .withOffenceId(randomUUID())
                .withSubjectId(randomUUID())
                .withLaaReference(LaaReference.laaReference().build())
                .build();

        applicationAggregate.apply(CourtApplicationAddedToHearing.courtApplicationAddedToHearing().withHearingId(randomUUID()).withApplicationId(laaReferenceForApplicationUpdated.getApplicationId()).build());

        final List<Object> listedHearing = applicationAggregate.updateLaaReference(laaReferenceForApplicationUpdated.getApplicationId(), laaReferenceForApplicationUpdated.getSubjectId(), laaReferenceForApplicationUpdated.getOffenceId(), laaReferenceForApplicationUpdated.getLaaReference()).collect(Collectors.toList());
        assertThat(listedHearing.size(), is(1));
        final LaaReferenceForApplicationUpdated laaReferenceForApplicationUpdatedProduced = (LaaReferenceForApplicationUpdated) listedHearing.get(0);
        assertThat(laaReferenceForApplicationUpdatedProduced.getApplicationId(), is(laaReferenceForApplicationUpdated.getApplicationId()));
        assertThat(laaReferenceForApplicationUpdatedProduced.getOffenceId(), is(laaReferenceForApplicationUpdated.getOffenceId()));
        assertThat(laaReferenceForApplicationUpdatedProduced.getSubjectId(), is(laaReferenceForApplicationUpdated.getSubjectId()));
        assertThat(laaReferenceForApplicationUpdatedProduced.getLaaReference(), is(laaReferenceForApplicationUpdated.getLaaReference()));
    }



}
