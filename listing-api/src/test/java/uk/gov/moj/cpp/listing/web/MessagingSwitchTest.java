package uk.gov.moj.cpp.listing.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.gov.moj.cpp.listing.messaging.inbound.InboundDedupPurge;
import uk.gov.moj.cpp.listing.messaging.inbound.PublicEventListener;
import uk.gov.moj.cpp.listing.messaging.outbox.EventOutbox;
import uk.gov.moj.cpp.listing.messaging.outbox.OutboxRelay;
import uk.gov.moj.cpp.listing.support.PostgresContainerSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * P2.2 (fixes the P2.0 open item): with {@code listing.messaging.enabled=false} there is no
 * public.event listener, no outbox relay, no inbound dedup purge and no JMS health contributor, so {@code /actuator/health}
 * is UP although no broker is reachable. The outbox writer is still there. Same properties as
 * {@link WebPlumbingTest}, so the Spring test context is shared.
 */
@SpringBootTest(properties = {"listing.messaging.enabled=false"})
@AutoConfigureMockMvc
class MessagingSwitchTest extends PostgresContainerSupport {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mvc;

    @Test
    void messagingDisabledMeansNoListenerNoRelayAndHealthUpWithoutABroker() throws Exception {
        assertThat(context.getBeanNamesForType(PublicEventListener.class)).isEmpty();
        assertThat(context.getBeanNamesForType(OutboxRelay.class)).isEmpty();
        assertThat(context.getBeanNamesForType(InboundDedupPurge.class)).isEmpty();
        assertThat(context.getBeanNamesForType(EventOutbox.class)).hasSize(1);
        assertThat(context.getEnvironment().getProperty("management.health.jms.enabled")).isEqualTo("false");

        mvc.perform(get("/listing-service/actuator/health").contextPath("/listing-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
