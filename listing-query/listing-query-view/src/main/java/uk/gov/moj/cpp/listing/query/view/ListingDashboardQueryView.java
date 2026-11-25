package uk.gov.moj.cpp.listing.query.view;

import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.dashboard.ListingDashboardRow;
import uk.gov.moj.cpp.listing.persistence.repository.dashboard.ListingDashboardRepository;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameterFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns every Crown hearing at a court centre for one week (Monday to Sunday) as flat rows.
 * The week is the one containing {@code startDate}, or today when no date is given.
 */
public class ListingDashboardQueryView {

    public static final String LISTING_DASHBOARD_QUERY_NAME = "listing.query.listing-dashboard";

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingDashboardQueryView.class);
    private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";

    @Inject
    private ListingDashboardRepository listingDashboardRepository;

    @Inject
    private PaginationParameterFactory paginationParameterFactory;

    @Inject
    private UtcClock utcClock;

    public JsonEnvelope getListingDashboard(final JsonEnvelope query) {
        final JsonObject payload = query.payloadAsJsonObject();
        final String courtCentreId = validCourtCentreId(payload.getString(COURT_CENTRE_ID));
        final LocalDate weekStart = weekStart(payload.getString(START_DATE, null));
        final LocalDate weekEnd = weekStart.plusDays(6);

        LOGGER.info("Retrieving listing dashboard for courtCentreId: {}, week: {} to {}", courtCentreId, weekStart, weekEnd);

        final List<ListingDashboardRow> rows = listingDashboardRepository.findDashboardHearings(
                courtCentreId, weekStart, weekEnd, paginationParameterFactory.getMaxPageSize());

        final JsonArrayBuilder hearings = createArrayBuilder();
        rows.forEach(row -> hearings.add(toJson(row)));

        final JsonObject response = createObjectBuilder()
                .add(COURT_CENTRE_ID, courtCentreId)
                .add(START_DATE, weekStart.toString())
                .add(END_DATE, weekEnd.toString())
                .add("hearings", hearings)
                .build();

        return envelopeFrom(metadataFrom(query.metadata()).withName(LISTING_DASHBOARD_QUERY_NAME), response);
    }

    LocalDate weekStart(final String startDate) {
        final LocalDate date;
        if (startDate == null || startDate.isBlank()) {
            date = utcClock.now().withZoneSameInstant(UK_ZONE).toLocalDate();
        } else {
            try {
                date = LocalDate.parse(startDate);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid start date: " + startDate);
            }
        }
        return date.with(previousOrSame(MONDAY));
    }

    private static String validCourtCentreId(final String courtCentreId) {
        try {
            return UUID.fromString(courtCentreId).toString();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid court centre id: " + courtCentreId);
        }
    }

    private static JsonObject toJson(final ListingDashboardRow row) {
        final JsonObjectBuilder builder = createObjectBuilder();
        addIfPresent(builder, "hearingId", row.getHearingId());
        addIfPresent(builder, "hearingDate", row.getHearingDate());
        addIfPresent(builder, "weekCommencingStartDate", row.getWeekCommencingStartDate());
        addIfPresent(builder, "weekCommencingEndDate", row.getWeekCommencingEndDate());
        addIfPresent(builder, "courtRoomId", row.getCourtRoomId());
        addIfPresent(builder, "courtRoomName", row.getCourtRoomName());
        addIfPresent(builder, "startTime", row.getStartTime());
        if (row.getDurationMinutes() != null) {
            builder.add("durationMinutes", row.getDurationMinutes());
        }
        addIfPresent(builder, "hearingTypeId", row.getHearingTypeId());
        addIfPresent(builder, "hearingType", row.getHearingType());
        if (row.getHearingDayNumber() != null) {
            builder.add("hearingDayNumber", row.getHearingDayNumber());
        }
        if (row.getTotalHearingDays() != null) {
            builder.add("totalHearingDays", row.getTotalHearingDays());
        }
        builder.add("caseUrns", toJsonArray(row.getCaseUrns()));
        builder.add("applicationReferences", toJsonArray(row.getApplicationReferences()));
        if (row.getAllocated() != null) {
            builder.add("allocated", row.getAllocated());
        }
        return builder.build();
    }

    private static void addIfPresent(final JsonObjectBuilder builder, final String name, final Object value) {
        if (value != null) {
            builder.add(name, value.toString());
        }
    }

    private static JsonArrayBuilder toJsonArray(final List<String> values) {
        final JsonArrayBuilder array = createArrayBuilder();
        if (values != null) {
            values.forEach(array::add);
        }
        return array;
    }
}
