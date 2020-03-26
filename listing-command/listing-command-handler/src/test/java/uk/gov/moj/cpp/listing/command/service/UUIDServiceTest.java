package uk.gov.moj.cpp.listing.command.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.listing.commands.PublishCourtListType.FIRM;
import static uk.gov.justice.listing.commands.PublishCourtListType.WARN;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UUIDServiceTest {

    @InjectMocks
    private UUIDService uuidService;

    private UUID courtCentreId;
    private LocalDate startDate;

    @Before
    public void setUp() {
        courtCentreId = UUID.randomUUID();
        startDate = LocalDate.now();
    }

    @Test
    public void shouldReturnSameUuidForSameInput() {
        final UUID courtListId = uuidService.getCourtListId(courtCentreId, WARN, startDate);
        assertThat(courtListId, is(notNullValue()));

        final UUID courtListId2 = uuidService.getCourtListId(courtCentreId, WARN, startDate);

        assertThat(courtListId2, is(courtListId));
    }

    @Test
    public void shouldReturnDifferentUuidForDifferentInputs() {
        final UUID courtListId = uuidService.getCourtListId(courtCentreId, WARN, startDate);
        assertThat(courtListId, is(notNullValue()));

        final UUID courtListId2 = uuidService.getCourtListId(courtCentreId, FIRM, startDate);

        assertThat(courtListId2, is(not(courtListId)));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionIfAnyInputIsNull() {
        uuidService.getCourtListId(null, WARN, startDate);
    }
}
