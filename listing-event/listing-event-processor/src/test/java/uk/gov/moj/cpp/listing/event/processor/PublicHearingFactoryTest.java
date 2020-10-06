package uk.gov.moj.cpp.listing.event.processor;

import static com.sun.org.apache.xerces.internal.util.PropertyState.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.OrganisationUnit;
import uk.gov.justice.listing.events.Type;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PublicHearingFactoryTest  {

    @Mock
    ReferenceDataService referenceDataService;

    @InjectMocks
    PublicHearingFactory publicHearingFactory;

    @Test
    public void shouldBuildCourtCentre() {
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final JsonEnvelope envelope = mock(JsonEnvelope.class);

        when(referenceDataService.getOrganizationUnitById(any(), eq(envelope))).thenReturn(OrganisationUnit.organisationUnit().withOucodeL3Name(java.util.Optional.of("test Court Centre")).build());

        final CourtCentre actual =  publicHearingFactory.buildCourtCentre(courtCentreId,courtRoomId,envelope);

        assertEquals(courtCentreId,actual.getId());
        assertEquals(Optional.of(courtRoomId),actual.getRoomId());
        assertEquals("test Court Centre",actual.getName());
    }

    @Test
    public void shouldBuildHearingDay() {
        final Integer durationInMinutes = 30;
        final Integer sequence = 2;
        final ZonedDateTime startTime = ZonedDateTime.now();
        uk.gov.justice.listing.events.HearingDay hd = HearingDay.hearingDay()
                .withDurationMinutes(durationInMinutes)
                .withSequence(sequence)
                .withStartTime(startTime)
                .build();
        final uk.gov.justice.core.courts.HearingDay actual = publicHearingFactory.buildHearingDay(hd);
        assertEquals(durationInMinutes,actual.getListedDurationMinutes());
        assertEquals(Optional.of(sequence),actual.getListingSequence());
        assertEquals(startTime,actual.getSittingDay());


    }

    @Test
    public void shouldBuildType() {
        final UUID id = UUID.randomUUID();
        uk.gov.justice.listing.events.Type type = Type.type().withDescription("TypeDescription")
                .withId(id).build();

        HearingType actual = publicHearingFactory.buildType(type);
        assertEquals(id,actual.getId());
        assertEquals("TypeDescription",actual.getDescription());
    }

    @Test
    public void shouldBuildJudicialRole() {
        final UUID judicialId = UUID.randomUUID();
        final Optional<UUID> userId = Optional.of(UUID.randomUUID());
        uk.gov.justice.listing.events.JudicialRole judicialRole = JudicialRole.judicialRole()
                .withJudicialId(judicialId)
                .withIsDeputy(Optional.of(false))
                .withIsBenchChairman(Optional.of(true))
                .withJudicialRoleType(JudicialRoleType.judicialRoleType().build())
                .withUserId(userId).build();

        final uk.gov.justice.core.courts.JudicialRole actual = publicHearingFactory.buildJudicialRole(judicialRole);

        assertEquals(judicialId,actual.getJudicialId());
        assertEquals(userId,actual.getUserId());

    }
}