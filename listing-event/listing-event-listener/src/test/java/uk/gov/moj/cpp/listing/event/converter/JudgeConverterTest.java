package uk.gov.moj.cpp.listing.event.converter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.event.JudgeAdded;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

@RunWith(MockitoJUnitRunner.class)
public class JudgeConverterTest {

    @InjectMocks
    private JudgeConverter judgeConverter;

    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldConvertToJudge() throws Exception {
        JudgeAdded event = new JudgeAdded(randomUUID().toString(), STRING.next(), STRING.next(), STRING.next());

        uk.gov.moj.cpp.listing.persistence.entity.Judge judge = judgeConverter.convert(event);

        assertThat(judge.getId().toString(), is(event.getId().toString()));
        assertThat(judge.getTitle(), is(event.getTitle()));
        assertThat(judge.getFirstName(), is(event.getFirstName()));
        assertThat(judge.getLastName(), is(event.getLastName()));

    }

}