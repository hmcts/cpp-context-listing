package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;

@ApplicationScoped
public class HearingJdbcRepository {

    private static final String UNALLOCATED_COMMON_SELECT_FROM = "select " +
            "h.id, h.properties,  " +
            "h.court_centre_id, " +
            "h.court_room_id, " +
            "h.type_id, " +
            "h.start_date, " +
            "h.end_date, " +
            "h.is_vacated_trial, " +
            "h.jurisdiction_type, " +
            "h.unscheduled, " +
            "h.week_commencing_start_date, " +
            "h.week_commencing_end_date, " +
            "h.allocated, " +
            "h.type_of_list_id, " +
            "count(1) OVER() as totalCount, " +
            "h.is_possible_disqualification " +
            "from hearing h " +
            "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
            " LEFT JOIN court_applications ca ON ca.hearing_id = h.id ";

    public static final String RANGE_SEARCH_FIELD_LIST = " h.id, h.properties,  " +
            "h.court_centre_id, " +
            "h.court_room_id, " +
            "h.type_id, " +
            "h.start_date, " +
            "h.end_date, " +
            "h.is_vacated_trial, " +
            "h.jurisdiction_type, " +
            "h.unscheduled, " +
            "h.week_commencing_start_date, " +
            "h.week_commencing_end_date, " +
            "h.allocated, " +
            "h.type_of_list_id, " +
            "h.is_possible_disqualification ";

    public static final String RANGE_SEARCH_FIELD_LIST_NO_ALIAS = RANGE_SEARCH_FIELD_LIST.replace("h.", "");

    @Inject
    private ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider;

    @Inject
    private ObjectMapperProducer objectMapperProducer;
    private DataSource dataSource;

    @PostConstruct
    protected void initialiseDataSource() {
        dataSource = viewStoreJdbcDataSourceProvider.getDataSource();
    }

    public List<Hearing> findHearings(final boolean allocated,
                                      final UUID courtCentreId,
                                      final UUID courtRoomId,
                                      final UUID authorityCode,
                                      final UUID hearingTypeId,
                                      final String jurisdictionType,
                                      final LocalDate startDate,
                                      final LocalDate endDate,
                                      final Integer offSet,
                                      final Integer pageSize) {
        final List<Hearing> hearingResults = new ArrayList<>();

        final String query = "select distinct " +
                "h.id, h.properties,  " +
                "h.court_centre_id, " +
                "h.court_room_id, " +
                "h.type_id, " +
                "h.start_date, " +
                "h.end_date, " +
                "h.is_vacated_trial, " +
                "h.jurisdiction_type, " +
                "h.unscheduled, " +
                "h.week_commencing_start_date, " +
                "h.week_commencing_end_date, " +
                "h.allocated, " +
                "h.type_of_list_id, " +
                "count(*) OVER() as totalCount, " +
                "h.is_possible_disqualification " +
                "from hearing h " +
                "LEFT JOIN hearing_days hd ON hd.hearing_id = h.id  " +
                "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
                "LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
                "where  " +
                "(h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                " and (lc.is_ejected is null or lc.is_ejected =false) " +
                " and (ca.is_ejected is null or ca.is_ejected =false) " +
                " and (h.allocated = ?)  " +
                " and (h.unscheduled is null or h.unscheduled = false) " +
                getAdditionalWhereClause(courtCentreId, courtRoomId, authorityCode, hearingTypeId, jurisdictionType) +
                " and ( " +
                "(h.start_date between ? and ? ) or " +
                "(h.end_date between ? and ? ) or " +
                "(h.start_date <=  ? and h.end_date >=  ?)  " +
                ") " +
                " order by h.id, h.court_centre_id ASC OFFSET (?) ROWS FETCH NEXT (?) ROWS ONLY";

        try (final Connection viewstoreConnection = dataSource.getConnection(); final PreparedStatement ps = viewstoreConnection.prepareStatement(query)) {
            int indexPointer = 1;
            ps.setBoolean(indexPointer++, allocated);

            indexPointer = setConditionalWhereClauseFields(courtCentreId, courtRoomId, authorityCode, hearingTypeId, jurisdictionType, ps, indexPointer);

            final Timestamp weekCommencingStartDateTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
            final Timestamp weekCommencingEndDateTimestamp = Timestamp.valueOf(endDate.atStartOfDay());
            ps.setTimestamp(indexPointer++, weekCommencingStartDateTimestamp);
            ps.setTimestamp(indexPointer++, weekCommencingEndDateTimestamp);

            ps.setTimestamp(indexPointer++, weekCommencingStartDateTimestamp);
            ps.setTimestamp(indexPointer++, weekCommencingEndDateTimestamp);

            ps.setTimestamp(indexPointer++, weekCommencingStartDateTimestamp);
            ps.setTimestamp(indexPointer++, weekCommencingEndDateTimestamp);

            ps.setInt(indexPointer++, offSet);
            ps.setInt(indexPointer, pageSize);


            try (final ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    hearingResults.add(entityFrom(resultSet));
                }
            }
        } catch (SQLException | IOException e) {
            throw new HearingJdbcException(format("Exception while executing query: %s", query), e);
        }

        return hearingResults;

    }


    public List<Hearing> findUnallocatedHearingsByWeekCommencingRange(
            final UUID courtCentreId,
            final UUID courtRoomId,
            final UUID authorityCode,
            final UUID hearingTypeId,
            final String jurisdictionType,
            final LocalDate weekCommencingStartDate,
            final LocalDate weekCommencingEndDate,
            final boolean allocated,
            final Integer offSet,
            final Integer pageSize) {
        final List<Hearing> hearingResults = new ArrayList<>();

        final String query = UNALLOCATED_COMMON_SELECT_FROM +
                "where  " +
                "(h.is_vacated_trial is null or h.is_vacated_trial != true)" +
                " and (lc.is_ejected is null or lc.is_ejected =false) " +
                " and (ca.is_ejected is null or ca.is_ejected =false) " +
                " and h.allocated = ?  " +
                " and (h.unscheduled is null or h.unscheduled = false)" +
                getAdditionalWhereClause(courtCentreId, courtRoomId, authorityCode, hearingTypeId, jurisdictionType) +
                " and ( " +
                "   ( h.week_commencing_start_date >= ? and h.week_commencing_start_date <= ? ) or " +
                "   ( h.week_commencing_end_date >= ? and h.week_commencing_end_date <= ? ) or " +
                "   ( h.start_date >= ? and h.start_date <= ? )  or " +
                "   ( h.end_date >= ? and h.end_date <= ? ) ) " +
                "group by h.id, h.properties " +
                "order by h.start_date," +
                "h.end_date," +
                "h.week_commencing_start_date," +
                "h.week_commencing_end_date" +
                " ASC OFFSET (?) ROWS FETCH NEXT (?) ROWS ONLY";

        try (final Connection viewstoreConnection = dataSource.getConnection();
             final PreparedStatement ps = viewstoreConnection.prepareStatement(query)) {

            int indexPointer = 1;
            ps.setBoolean(indexPointer++, allocated);

            indexPointer = setConditionalWhereClauseFields(courtCentreId, courtRoomId, authorityCode, hearingTypeId, jurisdictionType, ps, indexPointer);

            final Timestamp weekCommencingStartDateTimestamp = Timestamp.valueOf(weekCommencingStartDate.atStartOfDay());
            final Timestamp weekCommencingEndDateTimestamp = Timestamp.valueOf(weekCommencingEndDate.atStartOfDay());

            ps.setTimestamp(indexPointer++, weekCommencingStartDateTimestamp);
            ps.setTimestamp(indexPointer++, weekCommencingEndDateTimestamp);

            ps.setTimestamp(indexPointer++, weekCommencingStartDateTimestamp);
            ps.setTimestamp(indexPointer++, weekCommencingEndDateTimestamp);

            ps.setTimestamp(indexPointer++, weekCommencingStartDateTimestamp);
            ps.setTimestamp(indexPointer++, weekCommencingEndDateTimestamp);

            ps.setTimestamp(indexPointer++, weekCommencingStartDateTimestamp);
            ps.setTimestamp(indexPointer++, weekCommencingEndDateTimestamp);

            ps.setInt(indexPointer++, offSet);
            ps.setInt(indexPointer, pageSize);

            try (final ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    hearingResults.add(entityFrom(resultSet));
                }
            }
        } catch (SQLException | IOException e) {
            throw new HearingJdbcException(format("Exception while executing query: %s", query), e);
        }

        return hearingResults;
    }

    private static int setConditionalWhereClauseFields(UUID courtCentreId,
                                                       UUID courtRoomId,
                                                       UUID authorityCode,
                                                       UUID hearingTypeId,
                                                       final String jurisdictionType,
                                                       PreparedStatement ps,
                                                       int indexPointer) throws SQLException {
        if (courtCentreId != null) {
            ps.setObject(indexPointer++, courtCentreId);
            ps.setObject(indexPointer++, courtCentreId);
        }

        if (courtRoomId != null) {
            ps.setObject(indexPointer++, courtRoomId);
            ps.setObject(indexPointer++, courtRoomId);
        }

        if (authorityCode != null) {
            ps.setObject(indexPointer++, authorityCode);
            ps.setObject(indexPointer++, authorityCode);
            ps.setObject(indexPointer++, authorityCode);
        }

        if (hearingTypeId != null) {
            ps.setObject(indexPointer++, hearingTypeId);
            ps.setObject(indexPointer++, hearingTypeId);
        }

        if (jurisdictionType != null) {
            ps.setString(indexPointer++, jurisdictionType);
            ps.setString(indexPointer++, jurisdictionType);
        }
        return indexPointer;
    }

    private static String getAdditionalWhereClause(UUID courtCentreId,
                                                   UUID courtRoomId,
                                                   UUID authorityCode,
                                                   UUID hearingTypeId,
                                                   String jurisdictionType) {
        final String courtCentreIdWhereClause = "(? is null or h.court_centre_id = ?)  ";
        final String courtRoomIdWhereClause = "(? is null or h.court_room_id = ?)  ";
        final String authorityIdWhereClause = "(? is null or (lc.authority_id = ? or lc.prosecutor_id = ?))  ";
        final String typeIdWhereClause = "(? is null or h.type_id = ?)  ";
        final String jurisdictionTypeWhereClause = "(? is null or h.jurisdiction_type = ?)  ";

        final List<String> additionalWhereClauses = new ArrayList<>();

        if (courtCentreId != null) {
            additionalWhereClauses.add(courtCentreIdWhereClause);
        }

        if (courtRoomId != null) {
            additionalWhereClauses.add(courtRoomIdWhereClause);
        }

        if (authorityCode != null) {
            additionalWhereClauses.add(authorityIdWhereClause);
        }

        if (hearingTypeId != null) {
            additionalWhereClauses.add(typeIdWhereClause);
        }

        if (StringUtils.isNotEmpty(jurisdictionType)) {
            additionalWhereClauses.add(jurisdictionTypeWhereClause);
        }

        return additionalWhereClauses.isEmpty() ? "" : " and " + String.join(" and", additionalWhereClauses);
    }

    protected Hearing entityFrom(ResultSet resultSet) throws SQLException, IOException {
        final UUID id = (UUID) resultSet.getObject("id");
        final String propertiesRs = resultSet.getString("properties");
        final JsonNode properties = propertiesRs == null ? null : objectMapperProducer.objectMapper().readValue(propertiesRs, JsonNode.class);
        final UUID courtCentreId = (UUID) resultSet.getObject("court_centre_id");
        final UUID courtRoomId = (UUID) resultSet.getObject("court_room_id");
        final UUID typeId = (UUID) resultSet.getObject("type_id");
        final Timestamp startDateRs = resultSet.getTimestamp("start_date");
        final LocalDate startDate = startDateRs == null ? null : startDateRs.toLocalDateTime().toLocalDate();
        final Timestamp endDateRs = resultSet.getTimestamp("end_date");
        final LocalDate endDate = endDateRs == null ? null : endDateRs.toLocalDateTime().toLocalDate();
        final boolean isVacatedTrial = resultSet.getBoolean("is_vacated_trial");
        final String jurisdictionType = resultSet.getString("jurisdiction_type");
        final boolean unscheduled = resultSet.getBoolean("unscheduled");
        final Timestamp weekCommencingStartDateRs = resultSet.getTimestamp("week_commencing_start_date");
        final LocalDate weekCommencingStartDate = weekCommencingStartDateRs == null ? null : weekCommencingStartDateRs.toLocalDateTime().toLocalDate();
        final Timestamp weekCommencingEndDateRs = resultSet.getTimestamp("week_commencing_end_date");
        final LocalDate weekCommencingEndDate = weekCommencingEndDateRs == null ? null : weekCommencingEndDateRs.toLocalDateTime().toLocalDate();
        final boolean allocated = resultSet.getBoolean("allocated");
        final UUID typeOfListId = (UUID) resultSet.getObject("type_of_list_id");
        final long totalCount = resultSet.getLong("totalCount");
        final boolean isPossibleDisqualification = resultSet.getBoolean("is_possible_disqualification");
        final Hearing hearing = new Hearing(id, properties);
        hearing.setCourtCentreId(courtCentreId);
        hearing.setCourtRoomId(courtRoomId);
        hearing.setTypeId(typeId);
        hearing.setStartDate(startDate);
        hearing.setStartDate(endDate);
        hearing.setIsVacatedTrial(isVacatedTrial);
        hearing.setJurisdictionType(jurisdictionType);
        hearing.setUnscheduled(unscheduled);
        hearing.setWeekCommencingStartDate(weekCommencingStartDate);
        hearing.setWeekCommencingEndDate(weekCommencingEndDate);
        hearing.setAllocated(allocated);
        hearing.setTypeOfListId(typeOfListId);
        hearing.setTotalCount(totalCount);
        hearing.setPossibleDisqualification(isPossibleDisqualification);
        return hearing;
    }
}
