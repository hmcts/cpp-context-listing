package uk.gov.moj.cpp.listing.query.api.courtcentre;

import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails.courtCentreDetails;
import static uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails.courtRoomDetails;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtCentreDetails;
import uk.gov.moj.cpp.listing.query.api.courtcentre.details.CourtRoomDetails;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtCentreFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtCentreFactory.class);
    private static final String OUCODE_L_3_NAME = "oucodeL3Name";
    private static final String OUCODE_L_3_WELSH_NAME = "oucodeL3WelshName";
    private static final String ADDRESS_1 = "address1";
    private static final String ADDRESS_2 = "address2";
    private static final String ADDRESS_3 = "address3";
    private static final String ADDRESS_4 = "address4";
    private static final String ADDRESS_5 = "address5";
    private static final String POSTCODE = "postcode";
    private static final String WELSH_ADDRESS_1 = "welshAddress1";
    private static final String WELSH_ADDRESS_2 = "welshAddress2";
    private static final String WELSH_ADDRESS_3 = "welshAddress3";
    private static final String WELSH_ADDRESS_4 = "welshAddress4";
    private static final String WELSH_ADDRESS_5 = "welshAddress5";
    private static final String COURTROOMS = "courtrooms";
    private static final String ID = "id";
    private static final String COURTROOM_NAME = "courtroomName";
    private static final String WELSH_COURTROOM_NAME = "welshCourtroomName";
    private static final String IS_WELSH = "isWelsh";

    @Inject
    private ReferenceDataService referenceDataService;

    public CourtCentreDetails getCourtCentre(final UUID courtCentreId, final JsonEnvelope envelope) {
        final JsonEnvelope courtCentreEnvelope = referenceDataService.getCourtCentreById(courtCentreId, envelope);
        final JsonObject jsonObject = courtCentreEnvelope.payloadAsJsonObject();
        LOGGER.info("courtCentreEnvelope response: {}", jsonObject);

        final String courtCentreName = jsonObject.getString(OUCODE_L_3_NAME);
        final String courtCentreNameWelsh = jsonObject.getString(OUCODE_L_3_WELSH_NAME, null);
        final String address1 = jsonObject.getString(ADDRESS_1);
        final String address2 = jsonObject.getString(ADDRESS_2);
        final String address3 = jsonObject.getString(ADDRESS_3, null);
        final String address4 = jsonObject.getString(ADDRESS_4, null);
        final String address5 = jsonObject.getString(ADDRESS_5, null);
        final String postcode = jsonObject.getString(POSTCODE, null);
        final String welshAddress1 = jsonObject.getString(WELSH_ADDRESS_1, null);
        final String welshAddress2 = jsonObject.getString(WELSH_ADDRESS_2, null);
        final String welshAddress3 = jsonObject.getString(WELSH_ADDRESS_3, null);
        final String welshAddress4 = jsonObject.getString(WELSH_ADDRESS_4, null);
        final String welshAddress5 = jsonObject.getString(WELSH_ADDRESS_5, null);
        final Boolean welsh = jsonObject.getBoolean(IS_WELSH, false);
        final Map<UUID, CourtRoomDetails> courtRooms = jsonObject.getJsonArray(COURTROOMS).getValuesAs(JsonObject.class).stream()
                .map(courtRoomJsonObject -> {
                    final String courtroomName = courtRoomJsonObject.getString(COURTROOM_NAME);
                    final String welshCourtroomName = courtRoomJsonObject.getString(WELSH_COURTROOM_NAME, courtroomName);
                    return courtRoomDetails()
                            .withId(fromString(courtRoomJsonObject.getString(ID)))
                            .withCourtRoomName(courtroomName)
                            .withWelshCourtRoomName(welshCourtroomName)
                            .build();
                }).collect(Collectors.toMap(CourtRoomDetails::getId, cc -> cc));

        return courtCentreDetails()
                .withId(courtCentreId)
                .withCourtCentreName(courtCentreName)
                .withWelshCourtCentreName(courtCentreNameWelsh)
                .withAddress1(address1)
                .withAddress2(address2)
                .withAddress3(address3)
                .withAddress4(address4)
                .withAddress5(address5)
                .withPostcode(postcode)
                .withWelshAddress1(welshAddress1)
                .withWelshAddress2(welshAddress2)
                .withWelshAddress3(welshAddress3)
                .withWelshAddress4(welshAddress4)
                .withWelshAddress5(welshAddress5)
                .withCourtRooms(courtRooms)
                .withWelsh(welsh)
                .build();
    }
}
