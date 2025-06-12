package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.MAGISTRATES;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.listing.events.AddedCasesForHearing;
import uk.gov.justice.listing.events.AllocatedHearingDeleted;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2;
import uk.gov.justice.listing.events.ApplicantRespondent;
import uk.gov.justice.listing.events.AvailableSlotsForHearingFreed;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.CourtApplicationAddedToHearing;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdatedV2;
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing;
import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.DefendantOffenceIdsV2;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingListedCaseUpdated;
import uk.gov.justice.listing.events.HearingMarkedAsDeleted;
import uk.gov.justice.listing.events.HearingRequestedForListing;
import uk.gov.justice.listing.events.LaaReferenceForApplicationUpdated;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceIds;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2;
import uk.gov.justice.listing.events.RequestedHearingFromStagingHmi;
import uk.gov.justice.listing.events.SeedingHearing;
import uk.gov.justice.listing.events.UnallocatedHearingDeleted;
import uk.gov.justice.listing.events.UpdatedHearingInStagingHmi;
import uk.gov.justice.listing.events.UpdatedHmiFieldsForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyListingNeeds;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.Type;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
