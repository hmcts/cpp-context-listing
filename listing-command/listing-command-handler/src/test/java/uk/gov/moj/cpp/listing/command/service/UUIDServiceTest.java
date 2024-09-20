package uk.gov.moj.cpp.listing.command.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.listing.commands.PublishCourtListType.FIRM;
import static uk.gov.justice.listing.commands.PublishCourtListType.WARN;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UUIDServiceTest {

    @InjectMocks
    private UUIDService uuidService;

    private UUID courtCentreId;
    private LocalDate startDate;

    @BeforeEach
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

    @Test
    public void shouldThrowNullPointerExceptionIfAnyInputIsNull() {
        assertThrows(NullPointerException.class, () -> uuidService.getCourtListId(null, WARN, startDate));
    }
}
