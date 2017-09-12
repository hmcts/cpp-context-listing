package uk.gov.moj.cpp.listing.event.listener.referencedata;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.CourtCentreAdded;
import uk.gov.moj.cpp.listing.event.CourtRoomAdded;
import uk.gov.moj.cpp.listing.event.JudgeAdded;
import uk.gov.moj.cpp.listing.event.converter.CourtCentreConverter;
import uk.gov.moj.cpp.listing.event.converter.CourtRoomConverter;
import uk.gov.moj.cpp.listing.event.converter.JudgeConverter;
import uk.gov.moj.cpp.listing.persistence.repository.CourtCentreRepository;
import uk.gov.moj.cpp.listing.persistence.repository.CourtRoomRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JudgeRepository;

import javax.inject.Inject;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class ReferenceDataEventListener {

    @Inject
    private JudgeRepository judgeRepository;

    @Inject
    private CourtCentreRepository courtCentreRepository;

    @Inject
    private CourtRoomRepository courtRoomRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private JudgeConverter judgeConverter;

    @Inject
    private CourtCentreConverter courtCentreConverter;

    @Inject
    private CourtRoomConverter courtRoomConverter;

    @Handles("listing.events.judge-added")
    public void handleJudgeAdded(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final JudgeAdded event = jsonObjectConverter.convert(payload, JudgeAdded.class);

        judgeRepository.save(judgeConverter.convert(event));
    }

    @Handles("listing.events.court-centre-added")
    public void handleCourtCentreAdded(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final CourtCentreAdded event = jsonObjectConverter.convert(payload, CourtCentreAdded.class);

        courtCentreRepository.save(courtCentreConverter.convert(event));
    }

    @Handles("listing.events.court-room-added")
    public void handleCourtRoomAdded(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final CourtRoomAdded event = jsonObjectConverter.convert(payload, CourtRoomAdded.class);

        courtRoomRepository.save(courtRoomConverter.convert(event));
    }

}