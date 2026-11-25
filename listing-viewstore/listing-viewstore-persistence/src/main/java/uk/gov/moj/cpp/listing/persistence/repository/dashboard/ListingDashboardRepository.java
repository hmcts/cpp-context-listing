package uk.gov.moj.cpp.listing.persistence.repository.dashboard;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;
import uk.gov.moj.cpp.listing.persistence.entity.dashboard.ListingDashboardRow;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.HearingJdbcException;

/**
 * Retrieves one row per hearing day for a Crown court centre and date range, for the weekly
 * hearings query. Unlike the CSV report query there is no duration filter: every hearing is
 * returned. Case and application references are aggregated in SQL so the hearing's properties
 * JSON (which holds defendant and offence data) is never read into the application.
 */
@ApplicationScoped
public class ListingDashboardRepository {

    private static final String EXCEPTION_WHILE_EXECUTING_QUERY = "Exception while executing query: %s";

    static final String LISTING_DASHBOARD_QUERY = """
            SELECT
                h.id AS hearingId,
                hd.hearing_date AS hearingDate,
                h.week_commencing_start_date AS weekCommencingStartDate,
                h.week_commencing_end_date AS weekCommencingEndDate,
                COALESCE(hd.court_room_id, h.court_room_id) AS courtRoomId,
                crc.courtroom_name AS courtRoomName,
                hd.start_time AS startTime,
                COALESCE(hd.duration_minutes, h.estimated_minutes) AS durationMinutes,
                h.type_id AS hearingTypeId,
                h.properties -> 'type' ->> 'description' AS hearingType,
                CASE
                    WHEN hd.id IS NULL THEN NULL
                    ELSE (SELECT array_position(ARRAY_AGG(hd3.id ORDER BY hd3.hearing_date, hd3.start_time, hd3.sequence), hd.id)
                          FROM hearing_days hd3 WHERE hd3.hearing_id = h.id)
                END AS hearingDayNumber,
                (SELECT COUNT(1) FROM hearing_days hd2 WHERE hd2.hearing_id = h.id)::int AS totalHearingDays,
                ARRAY(
                    SELECT DISTINCT lc -> 'caseIdentifier' ->> 'caseReference'
                    FROM jsonb_array_elements(CASE WHEN jsonb_typeof(h.properties -> 'listedCases') = 'array'
                                                   THEN h.properties -> 'listedCases' ELSE '[]'::jsonb END) lc
                    WHERE lc -> 'caseIdentifier' ->> 'caseReference' IS NOT NULL
                ) AS caseUrns,
                ARRAY(
                    SELECT DISTINCT ca ->> 'applicationReference'
                    FROM jsonb_array_elements(CASE WHEN jsonb_typeof(h.properties -> 'courtApplications') = 'array'
                                                   THEN h.properties -> 'courtApplications' ELSE '[]'::jsonb END) ca
                    WHERE ca ->> 'applicationReference' IS NOT NULL
                ) AS applicationReferences,
                h.allocated AS allocated
            FROM hearing h
            LEFT JOIN hearing_days hd ON h.id = hd.hearing_id
            LEFT JOIN cache_refdata_courtroom crc ON (crc.id = COALESCE(hd.court_room_id, h.court_room_id))
            WHERE
                h.jurisdiction_type = 'CROWN'
                AND h.court_centre_id = CAST(? AS uuid)
                AND (h.unscheduled IS NULL OR h.unscheduled = false)
                AND (h.is_vacated_trial IS NULL OR h.is_vacated_trial != true)
                AND (
                    hd.hearing_date BETWEEN ? AND ?
                    OR (
                        h.allocated = false AND hd.hearing_date IS NULL AND (
                            (h.start_date <= ? AND COALESCE(h.end_date, h.start_date) >= ?)
                            OR (h.week_commencing_start_date <= ? AND COALESCE(h.week_commencing_end_date, h.week_commencing_start_date) >= ?)
                        )
                    )
                )
            ORDER BY
                hd.hearing_date NULLS LAST,
                crc.courtroom_name NULLS LAST,
                hd.start_time,
                hd.sequence,
                h.id
            LIMIT ?
            """;

    @Inject
    ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider;

    private DataSource dataSource;

    @PostConstruct
    protected void initialiseDataSource() {
        this.dataSource = this.viewStoreJdbcDataSourceProvider.getDataSource();
    }

    /**
     * Fetches the hearings of a Crown court centre between two dates (inclusive).
     *
     * @param courtCentreId the court centre ID
     * @param startDate     the first day of the range
     * @param endDate       the last day of the range
     * @param pageSize      the maximum number of rows to return
     * @return one row per hearing day, plus one row per undated unallocated hearing in the range
     */
    public List<ListingDashboardRow> findDashboardHearings(final String courtCentreId,
                                                     final LocalDate startDate,
                                                     final LocalDate endDate,
                                                     final long pageSize) {
        final List<ListingDashboardRow> rows = new ArrayList<>();

        try (final Connection viewstoreConnection = this.dataSource.getConnection();
             final PreparedStatement ps = viewstoreConnection.prepareStatement(LISTING_DASHBOARD_QUERY)) {

            ps.setString(1, courtCentreId);
            ps.setObject(2, startDate);
            ps.setObject(3, endDate);
            ps.setObject(4, endDate);
            ps.setObject(5, startDate);
            ps.setObject(6, endDate);
            ps.setObject(7, startDate);
            ps.setLong(8, pageSize);

            try (final ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(toRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new HearingJdbcException(String.format(EXCEPTION_WHILE_EXECUTING_QUERY, LISTING_DASHBOARD_QUERY), e);
        }

        return rows;
    }

    private ListingDashboardRow toRow(final ResultSet resultSet) throws SQLException {
        final ListingDashboardRow row = new ListingDashboardRow();
        row.setHearingId(resultSet.getString("hearingId"));
        row.setHearingDate(resultSet.getObject("hearingDate", LocalDate.class));
        row.setWeekCommencingStartDate(resultSet.getObject("weekCommencingStartDate", LocalDate.class));
        row.setWeekCommencingEndDate(resultSet.getObject("weekCommencingEndDate", LocalDate.class));
        row.setCourtRoomId(resultSet.getString("courtRoomId"));
        row.setCourtRoomName(resultSet.getString("courtRoomName"));
        final Timestamp startTime = resultSet.getTimestamp("startTime");
        row.setStartTime(startTime != null ? startTime.toInstant().toString() : null);
        row.setDurationMinutes(resultSet.getObject("durationMinutes", Integer.class));
        row.setHearingTypeId(resultSet.getString("hearingTypeId"));
        row.setHearingType(resultSet.getString("hearingType"));
        row.setHearingDayNumber(resultSet.getObject("hearingDayNumber", Integer.class));
        row.setTotalHearingDays(resultSet.getObject("totalHearingDays", Integer.class));
        row.setCaseUrns(toStringList(resultSet.getArray("caseUrns")));
        row.setApplicationReferences(toStringList(resultSet.getArray("applicationReferences")));
        row.setAllocated(resultSet.getObject("allocated", Boolean.class));
        return row;
    }

    private static List<String> toStringList(final Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }
        return Arrays.stream((Object[]) sqlArray.getArray()).map(String::valueOf).toList();
    }
}
