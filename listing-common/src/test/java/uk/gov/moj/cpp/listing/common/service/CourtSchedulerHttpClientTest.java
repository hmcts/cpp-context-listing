package uk.gov.moj.cpp.listing.common.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

class CourtSchedulerHttpClientTest {

    @Test
    void shouldGiveUpOnACourtschedulerThatNeverAnswers() throws Exception {
        final CountDownLatch unblock = new CountDownLatch(1);
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/unconfirmedBooking", exchange -> {
            try {
                unblock.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
        try (CloseableHttpClient client = new CourtSchedulerHttpClient(5000, 300).create()) {
            final HttpGet get = new HttpGet("http://127.0.0.1:" + server.getAddress().getPort() + "/unconfirmedBooking");

            assertTimeoutPreemptively(Duration.ofSeconds(5),
                    () -> assertThrows(SocketTimeoutException.class, () -> client.execute(get)));
        } finally {
            unblock.countDown();
            server.stop(0);
        }
    }

    @Test
    void shouldBuildAClientWithTheDefaultTimeOuts() throws Exception {
        final CourtSchedulerHttpClient courtSchedulerHttpClient = new CourtSchedulerHttpClient();
        courtSchedulerHttpClient.init();

        try (CloseableHttpClient client = courtSchedulerHttpClient.create()) {
            assertThat(client, notNullValue());
        }
    }
}
