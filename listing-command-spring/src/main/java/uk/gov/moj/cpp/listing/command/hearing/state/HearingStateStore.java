package uk.gov.moj.cpp.listing.command.hearing.state;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules.ViewstoreExtras;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRulesState;
import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.HearingRulesStateJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.HearingRulesStateJdbcRepository.StoredHearingRulesState;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads and keeps the state the Hearing rules decide on (MBD refactor P2.3.2, ADR 015): the
 * viewstore hearing row ({@code properties}: the listed hearing as the listener keeps it) plus the
 * command-side state in {@code hearing_rules_state}. There is no event stream and no snapshot of
 * the viewstore part: a command always sees the viewstore as the listener port left it.
 */
public class HearingStateStore {

    private final HearingRepository hearingRepository;
    private final HearingRulesStateJdbcRepository stateRepository;
    private final HearingRulesStateCodec codec = new HearingRulesStateCodec();
    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
    private final Clock clock;

    public HearingStateStore(final HearingRepository hearingRepository, final HearingRulesStateJdbcRepository stateRepository, final Clock clock) {
        this.hearingRepository = hearingRepository;
        this.stateRepository = stateRepository;
        this.clock = clock;
    }

    /**
     * The rules for {@code hearingId} in their current state (a fresh one for a hearing never listed).
     * First takes the hearing's lock for the rest of the transaction (P2.4,
     * {@code HearingRulesStateJdbcRepository#lock}), so commands and inbound events on one hearing
     * run one after the other and each decides on what the previous one committed.
     */
    public HearingRules load(final UUID hearingId) {
        stateRepository.lock(hearingId);
        final HearingRules rules = new HearingRules();
        final Optional<Hearing> row = Optional.ofNullable(hearingRepository.findBy(hearingId));
        final HearingRulesState state = stateRepository.find(hearingId).map(stored -> codec.fromJson(stored.stateJson())).orElse(null);
        if (row.isPresent() || nonNull(state)) {
            final JsonNode properties = row.map(Hearing::getProperties).orElse(null);
            rules.restore(nonNull(properties) ? listedHearing(hearingId, properties) : null, extras(properties), state);
        }
        return rules;
    }

    /** Keeps the command-side state of {@code rules} (nothing for a hearing that was never listed). */
    public void save(final HearingRules rules) {
        save(null, rules);
    }

    /**
     * Keeps the command-side state of {@code rules}, decided on for the hearing (former stream)
     * {@code hearingId}. A hearing listing never listed can still be tombstoned (e.g. hearing's
     * marked-as-duplicate, P2.4): the rules then have no hearing id of their own, and the row is
     * keyed by the stream id, as the JEE event stream was.
     */
    public void save(final UUID hearingId, final HearingRules rules) {
        final HearingRulesState state = rules.exportState();
        if (state != null) {
            final UUID key = nonNull(state.hearingId()) ? state.hearingId() : hearingId;
            if (key == null) {
                throw new IllegalStateException("Hearing rules state without a hearing id");
            }
            stateRepository.save(new StoredHearingRulesState(key, state.duplicate(), state.deleted(), codec.toJson(state)),
                    ZonedDateTime.now(clock));
        }
    }

    private uk.gov.justice.listing.events.Hearing listedHearing(final UUID hearingId, final JsonNode properties) {
        try {
            return mapper.treeToValue(properties, uk.gov.justice.listing.events.Hearing.class);
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot read the viewstore hearing " + hearingId, e);
        }
    }

    private static ViewstoreExtras extras(final JsonNode properties) {
        if (properties == null) {
            return new ViewstoreExtras(false, null, false, null);
        }
        return new ViewstoreExtras(
                properties.path("hasVideoLink").asBoolean(false),
                properties.hasNonNull("publicListNote") ? properties.get("publicListNote").asText() : null,
                properties.path("resulted").asBoolean(false),
                properties.hasNonNull("judiciaryAssignmentSource") ? properties.get("judiciaryAssignmentSource").asText() : null);
    }
}
