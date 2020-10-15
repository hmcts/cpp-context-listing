package uk.gov.moj.cpp.listing.event.processor.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.event.utils.EventBuilder;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AddHearingToCaseCommandCollectionConverterTest {

    private final AddHearingToCaseCommandCollectionConverter addHearingToCaseCommandCollectionConverter = new AddHearingToCaseCommandCollectionConverter();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @InjectMocks
    private EventBuilder eventBuilder;

    @Test
    public void convert() {

        //given
        final HearingListed hearingListed = eventBuilder.buildHearingListed();

        //when
        final List<AddHearingToCaseCommand> actualList = addHearingToCaseCommandCollectionConverter.convert(hearingListed);

        //then
        assertThat(actualList.size(), is(1));
        final AddHearingToCaseCommand actualCommand = actualList.get(0);
        final Hearing listedHearing = hearingListed.getHearing();
        final ListedCase listedCase = listedHearing.getListedCases().get(0);

        assertThat(actualCommand.getCaseId(), is(listedCase.getId()));
        assertThat(actualCommand.getHearingId(), is(listedHearing.getId()));
    }
}