package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.listing.events.CourtApplication.courtApplication;
import static uk.gov.justice.listing.events.Hearing.hearing;
import static uk.gov.justice.listing.events.HearingListed.hearingListed;

import uk.gov.justice.listing.commands.AddApplicationToHearingCommand;
import uk.gov.justice.listing.events.HearingListed;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class AddCourtApplicationToHearingCommandCollectionConverterTest {

    private AddCourtApplicationToHearingCommandCollectionConverter addCourtApplicationToHearingCommandCollectionConverter = new AddCourtApplicationToHearingCommandCollectionConverter();

    @Test
    public void shouldConvert() {
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final HearingListed event = hearingListed()
                .withHearing(hearing()
                        .withId(hearingId)
                        .withCourtApplications(asList(courtApplication()
                                .withId(courtApplicationId)
                                .build()))
                        .build())
                .build();
        final List<AddApplicationToHearingCommand> result = addCourtApplicationToHearingCommandCollectionConverter.convert(event);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getApplicationId(), is(courtApplicationId));
        assertThat(result.get(0).getHearingId(), is(hearingId));
    }
}