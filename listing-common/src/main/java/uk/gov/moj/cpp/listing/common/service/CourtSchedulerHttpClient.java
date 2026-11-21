package uk.gov.moj.cpp.listing.common.service;

import static java.lang.Math.toIntExact;

import uk.gov.justice.services.common.configuration.Value;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * The HTTP client for calls to courtscheduler. They run inside listing commands, so without
 * time-outs a courtscheduler that accepts the connection and never answers would hold the command
 * indefinitely. Same keys and defaults as hearing's and progression's courtscheduler calls.
 */
@ApplicationScoped
public class CourtSchedulerHttpClient {

    @Inject
    @Value(key = "courtscheduler.timeout.connect.ms", defaultValue = "5000")
    long connectTimeoutMs = 5000;

    @Inject
    @Value(key = "courtscheduler.timeout.ms", defaultValue = "15000")
    long socketTimeoutMs = 15000;

    private RequestConfig requestConfig;

    public CourtSchedulerHttpClient() {
    }

    public CourtSchedulerHttpClient(final long connectTimeoutMs, final long socketTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.socketTimeoutMs = socketTimeoutMs;
        init();
    }

    @PostConstruct
    void init() {
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(toIntExact(connectTimeoutMs))
                .setSocketTimeout(toIntExact(socketTimeoutMs))
                .build();
    }

    public CloseableHttpClient create() {
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
