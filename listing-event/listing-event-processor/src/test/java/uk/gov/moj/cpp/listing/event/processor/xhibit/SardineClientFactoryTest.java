package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.github.sardine.Sardine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class SardineClientFactoryTest {

    @InjectMocks
    private SardineClientFactory sardineClientFactory;

    @Test
    public void shouldCreateTheSardineClient() throws Exception {
        assertThat(sardineClientFactory.createSardineClient("username", "password"), is(instanceOf(Sardine.class)));
    }
}
