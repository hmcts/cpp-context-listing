package uk.gov.moj.cpp.listing.common.client;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The outbound {@link RestClient} (§6): JDK {@link HttpClient} with configured timeouts. Not
 * RestTemplate, which adds {@code Content-Length: 0} that the JDK client rejects (courtscheduler).
 */
@Configuration
@EnableConfigurationProperties(ListingClientsProperties.class)
public class CppRestClientConfig {

    @Bean
    public RestClient cppRestClient(final ListingClientsProperties properties) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
                .build();
        final JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory).build();
    }
}
