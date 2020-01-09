package uk.gov.moj.cpp.listing.command.service;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.listing.commands.PublishCourtListType.WARN;
import static uk.gov.moj.cpp.systemidmapper.client.ResultCode.OK;

import uk.gov.justice.listing.commands.PublishCourtListType;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SystemIdMapperServiceTest {

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private SystemIdMapperClient systemIdMapperClient;

    @InjectMocks
    private SystemIdMapperService systemIdMapperService;

    private final UUID courtCentreId = UUID.randomUUID();
    private final PublishCourtListType publishCourtListType = WARN;
    private final LocalDate startDate = LocalDate.now();
    private final UUID userId = randomUUID();

    @Test
    public void shouldReturnCaseIdWhenCaseIdMappingExists() {

        final UUID mappedCourtListId = randomUUID();
        final String courtListPrimaryKey = String.format("%s/%s/%s", courtCentreId.toString(), publishCourtListType.name(), startDate.toString());

        final SystemIdMapping systemIdMapping = new SystemIdMapping(randomUUID(), courtListPrimaryKey, "", mappedCourtListId, "", now());

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(courtListPrimaryKey, "COURT_LIST_PK", "COURT_LIST_ID", userId)).thenReturn(Optional.of(systemIdMapping));

        final UUID courtListId = systemIdMapperService.getCourtListId(courtCentreId, publishCourtListType, startDate);

        assertThat(courtListId, is(mappedCourtListId));
    }

    @Test
    public void shouldReturnCaseIdWhenNoMappingExists() {
        final String courtListPrimaryKey = String.format("%s/%s/%s", courtCentreId.toString(), publishCourtListType.name(), startDate.toString());
        ArgumentCaptor<SystemIdMap> systemIdMapArgumentCaptor = ArgumentCaptor.forClass(SystemIdMap.class);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
        when(systemIdMapperClient.findBy(courtListPrimaryKey, "COURT_LIST_PK", "COURT_LIST_ID", userId)).thenReturn(Optional.empty());
        when(systemIdMapperClient.add(systemIdMapArgumentCaptor.capture(), any())).thenReturn(new AdditionResponse(randomUUID(), OK, empty()));

        final UUID courtListId = systemIdMapperService.getCourtListId(courtCentreId, publishCourtListType, startDate);

        assertThat("courtListId should match", courtListId, is(systemIdMapArgumentCaptor.getValue().getTargetId()));
    }
}
