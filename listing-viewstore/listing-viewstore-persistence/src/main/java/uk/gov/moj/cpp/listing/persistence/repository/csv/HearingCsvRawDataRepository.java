package uk.gov.moj.cpp.listing.persistence.repository.csv;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;
import uk.gov.moj.cpp.listing.persistence.entity.csv.HearingCsvRawData;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.HearingJdbcException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import static java.lang.String.format;

/**
 * Repository for retrieving raw hearing data from the database.
 * This repository focuses solely on data retrieval without any business logic processing.
 */
@ApplicationScoped
public class HearingCsvRawDataRepository {

    private static final String EXCEPTION_WHILE_EXECUTING_QUERY = "Exception while executing query: %s";

    @Inject
    ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider;

    @Inject
    ObjectMapperProducer objectMapperProducer;

    private DataSource dataSource;

    @PostConstruct
    protected void initialiseDataSource() {
        dataSource = viewStoreJdbcDataSourceProvider.getDataSource();
    }

    /**
     * Fetches raw hearing data for CSV report generation using simplified query.
     * This method retrieves basic hearing information and raw JSON properties
     * without any complex JsonB extraction logic.
     *
     * @param courtCentreId the court centre ID
     * @param startDate     the start date
     * @param numberOfWeeks the number of weeks to include
     * @param pageSize      the page size limit
     * @return List of raw hearing data
     */
    public List<HearingCsvRawData> findHearingsRawDataForCsvReport(final String courtCentreId, 
                                                               final LocalDate startDate, 
                                                               final Integer numberOfWeeks, 
                                                               final Long pageSize) {
        final List<HearingCsvRawData> rawResults = new ArrayList<>();
        
        // Calculate end date based on start date and number of weeks
        final LocalDate endLocalDate = startDate.plusWeeks(numberOfWeeks).minusDays(1);

        final String query = """
            SELECT
                h.id,
                hd.hearing_date as hearingDate,
                h.week_commencing_start_date as weekCommencingStartDate,
                h.week_commencing_end_date as weekCommencingEndDate,
                crc.courtroom_name as courtroom,
                hd.start_time as startTime,
                hd.duration_minutes as durationMinutes,
                h.start_date,
                h.end_date,
                -- Hearing day: Show "X of Y" for multi-day hearings
                CASE 
                    WHEN hd.hearing_date IS NULL THEN ''
                    ELSE CONCAT(
                        (SELECT array_position(ARRAY_AGG(hd3.hearing_date ORDER BY hd3.hearing_date, hd3.start_time, hd3.sequence), hd.hearing_date) 
                         FROM hearing_days hd3 WHERE hd3.hearing_id = h.id), 
                        ' of ', 
                        (SELECT COUNT(1) FROM hearing_days hd2 WHERE hd2.hearing_id = h.id)
                    )
                END as day,
                h.properties::jsonb as properties
            FROM hearing h
            LEFT JOIN hearing_days hd ON h.id = hd.hearing_id
            LEFT JOIN cache_refdata_courtroom crc ON (crc.id = COALESCE(hd.court_room_id, h.court_room_id))
            WHERE
                h.jurisdiction_type = 'CROWN'
                AND h.court_centre_id = CAST(? AS uuid)
                AND (h.start_date < h.end_date OR h.week_commencing_start_date < h.week_commencing_end_date)
                AND (
                    (hd.hearing_date BETWEEN ? AND ?) OR 
                    (h.week_commencing_start_date BETWEEN ? AND ?) OR 
                    (h.week_commencing_end_date BETWEEN ? AND ?)
                )
            ORDER BY
                hd.start_time,
                h.start_date,
                crc.courtroom_name,
                hd.start_time,
                hd.sequence,
                h.id
            LIMIT ?
            """;

        try (final Connection viewstoreConnection = dataSource.getConnection(); 
             final PreparedStatement ps = viewstoreConnection.prepareStatement(query)) {
            
            final Timestamp startDateTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
            final Timestamp endDateTimestamp = Timestamp.valueOf(endLocalDate.atStartOfDay());

            ps.setString(1, courtCentreId);
            ps.setTimestamp(2, startDateTimestamp);
            ps.setTimestamp(3, endDateTimestamp);
            ps.setTimestamp(4, startDateTimestamp);
            ps.setTimestamp(5, endDateTimestamp);
            ps.setTimestamp(6, startDateTimestamp);
            ps.setTimestamp(7, endDateTimestamp);
            ps.setLong(8, pageSize);

            try (final ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    HearingCsvRawData rawData = new HearingCsvRawData();
                    rawData.setId(resultSet.getString("id"));
                    rawData.setHearingDate(resultSet.getObject("hearingDate", LocalDate.class));
                    rawData.setWeekCommencingStartDate(resultSet.getObject("weekCommencingStartDate", LocalDate.class));
                    rawData.setWeekCommencingEndDate(resultSet.getObject("weekCommencingEndDate", LocalDate.class));
                    rawData.setCourtroom(resultSet.getString("courtroom"));
                    rawData.setStartTime(resultSet.getTimestamp("startTime") !=null ? resultSet.getTimestamp("startTime").toInstant().toString() : "");
                    rawData.setDurationMinutes(resultSet.getObject("durationMinutes", Integer.class));
                    rawData.setStartDate(resultSet.getObject("start_date", LocalDate.class));
                    rawData.setEndDate(resultSet.getObject("end_date", LocalDate.class));
                    rawData.setDay(resultSet.getString("day"));
                    
                    // Parse JSON properties
                    String propertiesJson = resultSet.getString("properties");
                    propertyAsJsonNode(propertiesJson, rawData);

                    rawResults.add(rawData);
                }
            }
        } catch (SQLException e) {
            throw new HearingJdbcException(format(EXCEPTION_WHILE_EXECUTING_QUERY, query), e);
        }

        return rawResults;
    }

    private void propertyAsJsonNode(final String propertiesJson, final HearingCsvRawData rawData) {
        if (propertiesJson != null && !propertiesJson.trim().isEmpty()) {
            try {
                JsonNode properties = objectMapperProducer.objectMapper().readTree(propertiesJson);
                rawData.setProperties(properties);
            } catch (IOException e) {
                throw new HearingJdbcException("Failed to parse hearing properties JSON", e);
            }
        }
    }
}
