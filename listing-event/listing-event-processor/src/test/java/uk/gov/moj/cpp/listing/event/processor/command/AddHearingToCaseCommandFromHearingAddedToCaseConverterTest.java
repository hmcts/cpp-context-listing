package uk.gov.moj.cpp.listing.event.processor.command;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@ExtendWith(MockitoExtension.class)
public class AddHearingToCaseCommandFromHearingAddedToCaseConverterTest {

    private AddHearingToCaseCommandFromHearingAddedToCaseConverter addHearingToCaseCommandFromHearingAddedToCaseConverter = new AddHearingToCaseCommandFromHearingAddedToCaseConverter();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @InjectMocks
    EventBuilder eventBuilder;


    @Test
    public void convert() {

        //given
        final CasesAddedToHearing casesAddedToHearing = eventBuilder.buildCasesAddedToHearing();

        //when
        final List<AddHearingToCaseCommand> actualList = addHearingToCaseCommandFromHearingAddedToCaseConverter.convert(casesAddedToHearing);

        //then
        assertThat(actualList.size(), is(1));
        final AddHearingToCaseCommand actualCommand = actualList.get(0);
        final UUID hearingId = casesAddedToHearing.getHearingId();
        final ListedCase unAlloatedListedCase = casesAddedToHearing.getUnAllocatedListedCases().get(0);

        assertThat(actualCommand.getCaseId(), is(unAlloatedListedCase.getId()));
        assertThat(actualCommand.getHearingId(), is(hearingId));
    }

    @Test
    public void convertV2() {

        //given
        final CasesAddedToHearing casesAddedToHearing = eventBuilder.buildCasesAddedToHearingV2();

        //when
        final List<AddHearingToCaseCommand> actualList = addHearingToCaseCommandFromHearingAddedToCaseConverter.convert(casesAddedToHearing);

        //then
        assertThat(actualList.size(), is(1));
        final AddHearingToCaseCommand actualCommand = actualList.get(0);
        final UUID hearingId = casesAddedToHearing.getHearingId();
        final ListedCase unAlloatedListedCase = casesAddedToHearing.getUnAllocatedListedCases().get(0);

        assertThat(actualCommand.getCaseId(), is(unAlloatedListedCase.getId()));
        assertThat(actualCommand.getHearingId(), is(hearingId));
    }


}
