package uk.gov.moj.cpp.listing.command.hearing.handler.factory;

import uk.gov.justice.listing.events.Hearing;
import uk.gov.moj.cpp.listing.command.hearing.handler.service.HearingService;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;

import java.util.UUID;

import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** JEE {@code HearingFactory}: the viewstore hearing ({@code listing.search.hearing}) as the event-shaped {@link Hearing}. */
@Component("hearingHandlerHearingFactory")
public class HearingFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingFactory.class);

    private final HearingService hearingService;
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    public HearingFactory(final HearingService hearingService) {
        this.hearingService = hearingService;
    }

    public Hearing getHearingById(final UUID hearingId) {
        final JsonObject jsonObject = hearingService.getHearingById(hearingId);
        final Hearing hearing = jsonObjectToObjectConverter.convert(jsonObject, Hearing.class);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("hearingEnvelope response: {}", jsonObject);
        }
        return hearing;
    }
}
