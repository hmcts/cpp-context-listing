package uk.gov.moj.cpp.listing.event.listener;


import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.listing.events.AllocatedHearingDeleted;
import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.justice.listing.events.UnallocatedHearingDeleted;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingDay;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.SeedingHearing;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingMarkedAsDuplicateEventListenerTest {


    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Envelope<HearingMarkedAsDuplicate> hearingMarkedAsDuplicateEnvelope;

    @Mock
    private Envelope<AllocatedHearingDeleted> allocatedHearingDeletedEnvelope;

    @Mock
    private Envelope<UnallocatedHearingDeleted> unallocatedHearingDeletedEnvelope;

    @Mock
    private Envelope<OffencesRemovedFromHearing> offencesRemovedFromHearingEnvelope;

    @Mock
    private Envelope<OffencesRemovedFromExistingAllocatedHearing> offencesRemovedFromExistingAllocatedHearingEnvelope;

    @Mock
    private Envelope<OffencesRemovedFromExistingUnallocatedHearing> offencesRemovedFromExistingUnallocatedHearingEnvelope;

    @InjectMocks
    private HearingMarkedAsDuplicateEventListener hearingMarkedAsDuplicateEventListener;

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();


    @Test
    public void shouldDeleteHearingWhenMarkedAsDuplicate() {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.createHearingBuilder().setId(hearingId).build();

        when(hearingMarkedAsDuplicateEnvelope.payload()).thenReturn(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .build());
        when(hearingRepository.findBy(eq(hearingId)))
                .thenReturn(hearing);

        hearingMarkedAsDuplicateEventListener.hearingMarkedAsDuplicate(hearingMarkedAsDuplicateEnvelope);

        verify(hearingRepository).remove(eq(hearing));
    }

    @Test
    public void shouldNotDeleteHearingWhenHearingNotExistsInViewStore() {

        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.createHearingBuilder().setId(hearingId).build();

        when(hearingMarkedAsDuplicateEnvelope.payload()).thenReturn(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingId)
                .build());
        when(hearingRepository.findBy(eq(hearingId)))
                .thenReturn(null);

        hearingMarkedAsDuplicateEventListener.hearingMarkedAsDuplicate(hearingMarkedAsDuplicateEnvelope);

        verify(hearingRepository, never()).remove(eq(hearing));
    }

    @Test
    public void shouldDeleteHearingWhenAllocatedHearingDeleted() {

        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.createHearingBuilder().setId(hearingId).build();

        when(allocatedHearingDeletedEnvelope.payload()).thenReturn(AllocatedHearingDeleted.allocatedHearingDeleted()
                .withHearingId(hearingId)
                .build());

        when(hearingRepository.findBy(eq(hearingId)))
                .thenReturn(hearing);

        hearingMarkedAsDuplicateEventListener.handleAllocatedHearingDeleted(allocatedHearingDeletedEnvelope);

        verify(hearingRepository).remove(eq(hearing));
    }

    @Test
    public void shouldDeleteHearingWhenUnallocatedHearingDeleted() {

        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.createHearingBuilder().setId(hearingId).build();

        when(unallocatedHearingDeletedEnvelope.payload()).thenReturn(UnallocatedHearingDeleted.unallocatedHearingDeleted()
                .withHearingId(hearingId)
                .build());

        when(hearingRepository.findBy(eq(hearingId)))
                .thenReturn(hearing);

        hearingMarkedAsDuplicateEventListener.handleUnallocatedHearingDeleted(unallocatedHearingDeletedEnvelope);

        verify(hearingRepository).remove(eq(hearing));
    }

    @Test
    public void shouldUnallocatedHearingAndRemoveOffences() throws JsonProcessingException {
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();

        final UUID courtCentreId = randomUUID();
        final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = uk.gov.moj.cpp.listing.domain.Hearing.hearing()
                .withId(hearingId)
                .withCourtRoomId(of(randomUUID()))
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withCourtCentreId(of(courtCentreId))
                        .withCourtRoomId(of(randomUUID()))
                        .build()))
                .withListedCases(Arrays.asList(ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(offence1Id)
                                                .withSeedingHearing(of(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).build()))
                                                .build()))
                                        .build()))
                                .build(),
                        ListedCase.listedCase()
                                .withId(case2Id)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(of(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).build()))
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offence3Id)
                                                        .withSeedingHearing(empty())
                                                        .build()))
                                        .build()))
                                .build()))
                .build();

        final JsonNode hearingProperties = objectMapper.valueToTree(domainHearing);
        final Hearing hearing = Hearing.createHearingBuilder().setId(hearingId)
                .setProperties(hearingProperties)
                .build();

        final ArgumentCaptor<Hearing> argumentCaptor = ArgumentCaptor.forClass(Hearing.class);

        when(offencesRemovedFromHearingEnvelope.payload())
                .thenReturn(OffencesRemovedFromHearing.offencesRemovedFromHearing()
                        .withHearingId(hearingId)
                        .withCaseIdsSeededByOnlySeedingHearingId(Arrays.asList(case1Id))
                        .withSeedingHearingId(seedingHearingId)
                        .build());

        when(hearingRepository.findBy(eq(hearingId)))
                .thenReturn(hearing);

        hearingMarkedAsDuplicateEventListener.hearingUnAllocatedForListingV2(offencesRemovedFromHearingEnvelope);

        verify(hearingRepository).save(argumentCaptor.capture());

        final Hearing savedHearing = argumentCaptor.getValue();

        assertThat(savedHearing.getProperties().get("allocated").asBoolean(), is(false));
        assertThat(savedHearing.getProperties().get("courtRoomId"), nullValue());
        assertThat(savedHearing.getProperties().get("listedCases").size(), is(1));
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("id").asText(), is(case2Id.toString()));
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").size(), is(1));
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").get(0).get("id").asText(), is(offence3Id.toString()));
        assertThat(savedHearing.getProperties().get("hearingDays").get(0).get("courtRoomId"), nullValue());
        assertThat(savedHearing.getProperties().get("hearingDays").get(0).get("courtCentreId").asText(), is(courtCentreId.toString()));

    }


    @Test
    public void shouldDeleteOffencesFromExistingAllocatedHearing() {
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();

        final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = uk.gov.moj.cpp.listing.domain.Hearing.hearing()
                .withId(hearingId)
                .withAllocated(true)
                .withListedCases(Arrays.asList(ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(offence1Id)
                                                .withSeedingHearing(of(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).build()))
                                                .build()))
                                        .build()))
                                .build(),
                        ListedCase.listedCase()
                                .withId(case2Id)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(of(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).build()))
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offence3Id)
                                                        .withSeedingHearing(empty())
                                                        .build()))
                                        .build()))
                                .build()))
                .build();

        final JsonNode hearingProperties = objectMapper.valueToTree(domainHearing);
        final Hearing hearing = Hearing.createHearingBuilder().setId(hearingId)
                .setProperties(hearingProperties)
                .build();

        final ArgumentCaptor<Hearing> argumentCaptor = ArgumentCaptor.forClass(Hearing.class);

        when(offencesRemovedFromExistingAllocatedHearingEnvelope.payload())
                .thenReturn(OffencesRemovedFromExistingAllocatedHearing.offencesRemovedFromExistingAllocatedHearing()
                        .withHearingId(hearingId)
                        .withOffenceIds(Arrays.asList(offence1Id))
                        .build());

        when(hearingRepository.findBy(eq(hearingId)))
                .thenReturn(hearing);

        hearingMarkedAsDuplicateEventListener.removeOffencesFromExistingAllocatedHearing(offencesRemovedFromExistingAllocatedHearingEnvelope);

        verify(hearingRepository).save(argumentCaptor.capture());

        final Hearing savedHearing = argumentCaptor.getValue();

        assertThat(savedHearing.getProperties().get("allocated").asBoolean(), is(true));
        assertThat(savedHearing.getProperties().get("listedCases").size(), is(1));
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("id").asText(), is(case2Id.toString()));
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").size(), is(2));

    }

    @Test
    public void shouldDeleteOffencesFromExistingUnallocatedHearing() {

        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();

        final uk.gov.moj.cpp.listing.domain.Hearing domainHearing = uk.gov.moj.cpp.listing.domain.Hearing.hearing()
                .withId(hearingId)
                .withAllocated(false)
                .withListedCases(Arrays.asList(ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(offence1Id)
                                                .withSeedingHearing(of(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).build()))
                                                .build()))
                                        .build()))
                                .build(),
                        ListedCase.listedCase()
                                .withId(case2Id)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(of(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).build()))
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offence3Id)
                                                        .withSeedingHearing(empty())
                                                        .build()))
                                        .build()))
                                .build()))
                .build();

        final JsonNode hearingProperties = objectMapper.valueToTree(domainHearing);
        final Hearing hearing = Hearing.createHearingBuilder().setId(hearingId)
                .setProperties(hearingProperties)
                .build();

        final ArgumentCaptor<Hearing> argumentCaptor = ArgumentCaptor.forClass(Hearing.class);

        when(offencesRemovedFromExistingUnallocatedHearingEnvelope.payload())
                .thenReturn(OffencesRemovedFromExistingUnallocatedHearing.offencesRemovedFromExistingUnallocatedHearing()
                        .withHearingId(hearingId)
                        .withOffenceIds(Arrays.asList(offence1Id))
                        .build());

        when(hearingRepository.findBy(eq(hearingId)))
                .thenReturn(hearing);

        hearingMarkedAsDuplicateEventListener.removeOffencesFromExistingUnallocatedHearing(offencesRemovedFromExistingUnallocatedHearingEnvelope);

        verify(hearingRepository).save(argumentCaptor.capture());

        final Hearing savedHearing = argumentCaptor.getValue();

        assertThat(savedHearing.getProperties().get("allocated").asBoolean(), is(false));
        assertThat(savedHearing.getProperties().get("listedCases").size(), is(1));
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("id").asText(), is(case2Id.toString()));
        assertThat(savedHearing.getProperties().get("listedCases").get(0).get("defendants").get(0).get("offences").size(), is(2));

    }

}
