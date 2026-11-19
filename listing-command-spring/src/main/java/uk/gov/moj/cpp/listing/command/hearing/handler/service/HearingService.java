package uk.gov.moj.cpp.listing.command.hearing.handler.service;

import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;
import uk.gov.moj.cpp.listing.query.view.exception.QueryNotFoundException;

import java.util.UUID;

import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The handler's viewstore read of a hearing ({@code listing.search.hearing}, in-process as in JEE;
 * MBD refactor P2.3.2). Runs in the command's transaction, so it sees the command's own writes.
 *
 * <p>A missing hearing is detected here, before the {@code @Transactional} query view is called:
 * the same {@link QueryNotFoundException} the view throws, but not thrown through a transactional
 * proxy, which would mark the command's transaction rollback-only — update-hearings-for-listing
 * catches it per hearing and goes on with the others, as the JEE handler did.
 */
@Component("hearingHandlerHearingService")
public class HearingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingService.class);

    private final HearingQueryView hearingQueryView;
    private final HearingRepository hearingRepository;

    public HearingService(final HearingQueryView hearingQueryView, final HearingRepository hearingRepository) {
        this.hearingQueryView = hearingQueryView;
        this.hearingRepository = hearingRepository;
    }

    public JsonObject getHearingById(final UUID hearingId) {
        final JsonObject payload = createObjectBuilder().add("id", hearingId.toString()).build();
        LOGGER.info("listing.search.hearing request with payload {}", payload);
        if (hearingRepository.findBy(hearingId) == null) {
            throw new QueryNotFoundException("There is no Hearing for this ID: " + hearingId);
        }
        return hearingQueryView.getHearingById(payload);
    }
}
