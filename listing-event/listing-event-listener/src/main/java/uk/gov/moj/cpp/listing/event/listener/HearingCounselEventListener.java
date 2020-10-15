package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Objects.isNull;
import static uk.gov.justice.listing.event.Action.ADD;
import static uk.gov.justice.listing.event.Action.REMOVE;
import static uk.gov.justice.listing.event.Action.UPDATE;

import uk.gov.justice.listing.event.CounselType;
import uk.gov.justice.listing.event.HearingCounselModified;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.IOException;
import java.util.EnumSet;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingCounselEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingCounselEventListener.class);
    private final HearingRepository hearingRepository;
    private final ObjectMapper mapper;


    @Inject
    public HearingCounselEventListener(final HearingRepository hearingRepository,
                                       final ObjectMapper mapper) {
        this.hearingRepository = hearingRepository;
        this.mapper = mapper;
    }

    @Handles("listing.event.hearing-counsel-modified")
    public void hearingCounselModified(final Envelope<HearingCounselModified> event) throws IOException {
        final HearingCounselModified hearingCounselModified = event.payload();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.event.hearing-counsel-modified' received hearingId {}  {}", hearingCounselModified.getHearingId(), hearingCounselModified.getPayload());
        }

        if (EnumSet.of(ADD, UPDATE).contains(hearingCounselModified.getAction())) {
            addOrUpdateCounsel(hearingCounselModified.getHearingId(), hearingCounselModified.getPayload(), hearingCounselModified.getCounselType());
        } else if (hearingCounselModified.getAction() == REMOVE) {
            removeCounselAndSave(hearingCounselModified.getHearingId(), hearingCounselModified.getPayload(), hearingCounselModified.getCounselType());
        }
    }

    private void addOrUpdateCounsel(final UUID hearingId, final String payload, final CounselType counselType) throws IOException {
        final Hearing hearing = hearingRepository.findBy(hearingId);
        final String counselNodeName = counselType.name().toLowerCase() + "Counsels";
        if (isNull(hearing.getProperties().get(counselNodeName))) {
            ((ObjectNode) hearing.getProperties()).putArray(counselNodeName);
        }

        final ArrayNode counsels = (ArrayNode) hearing.getProperties().get(counselNodeName);
        final JsonNode counsel = mapper.readTree(payload);
        final int index = removeCounselFromProperties(counsels, counsel.at("/id").textValue());
        if (index > -1) {
            counsels.insert(index, counsel);
        } else {
            counsels.add(counsel);
        }
        hearingRepository.save(hearing);
    }

    private void removeCounselAndSave(final UUID hearingId, final String payload, final CounselType counselType) throws IOException {
        final Hearing hearing = hearingRepository.findBy(hearingId);
        final String counselNodeName = counselType.name().toLowerCase() + "Counsels";
        if (isNull(hearing.getProperties().get(counselNodeName))) {
            return;
        }

        final ArrayNode counsels = (ArrayNode) hearing.getProperties().get(counselNodeName);
        if (removeCounselFromProperties(counsels, mapper.readTree(payload).textValue()) > -1) {
            hearingRepository.save(hearing);
        }
    }

    private int removeCounselFromProperties(final ArrayNode counsels, final String counselId) {
        int index = -1;
        for (final JsonNode jsonNode : counsels.findValues("id")) {
            index++;
            if (jsonNode.textValue().equals(counselId)) {
                counsels.remove(index);
                return index;
            }
        }
        return index;
    }
}
