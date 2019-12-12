package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import static java.lang.String.format;
import static uk.gov.justice.listing.event.PublishStatus.valueFor;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.toSqlTimestamp;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishStatus;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapper;
import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;

@ApplicationScoped
public class CourtListPublishStatusJdbcRepository {

    private static final String COURT_LIST_PUBLISH_STATUS_INSERT_QUERY = "INSERT INTO court_list_publish_status values(?,?,?,?,?,?,?,?,?,?)";

    private static final String COURT_LIST_PUBLISH_STATUS_QUERY_PREFIX =
            "SELECT * FROM court_list_publish_status WHERE last_updated in (SELECT MAX(last_updated) FROM court_list_publish_status " +
                    " WHERE court_centre_id = ? AND publish_court_list_type IN ( ";

    private static final String COURT_LIST_PUBLISH_STATUS_QUERY_SUFFIX = " AND publish_date = ? AND week_commencing = ? GROUP BY publish_court_list_type)";

    @Inject
    private ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider;

    private DataSource dataSource;

    @PostConstruct
    protected void initialiseDataSource() {
        dataSource = viewStoreJdbcDataSourceProvider.getDataSource();
    }


    public List<CourtListPublishStatusResult> courtListPublishStatuses(final UUID courtCentreId,
                                                                       final Set<PublishCourtListType> publishCourtListTypes,
                                                                       final LocalDate publishDate,
                                                                       final boolean weekCommencing) {
        final List<CourtListPublishStatusResult> courtListPublishStatusResults = new ArrayList();
        final String query = buildQuery(publishCourtListTypes);

        try (final Connection viewstoreConnection = dataSource.getConnection();
             final PreparedStatementWrapper ps = PreparedStatementWrapper.valueOf(viewstoreConnection, query)) {
            ps.setObject(1, courtCentreId);
            int index = 2;
            for (final PublishCourtListType publishCourtListType : publishCourtListTypes) {
                ps.setString(index++, publishCourtListType.toString());
            }
            ps.setObject(index++, publishDate);
            ps.setObject(index++, weekCommencing);
            try (final ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    courtListPublishStatusResults.add(entityFrom(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new CourtListPublishStatusJdbcException(format("Exception while executing query: %s", query), e);
        }

        return courtListPublishStatusResults;
    }

    private String buildQuery(final Set<PublishCourtListType> publishCourtListTypes) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < publishCourtListTypes.size(); i++) {
            builder.append("?,");
        }

        return COURT_LIST_PUBLISH_STATUS_QUERY_PREFIX
                + builder.deleteCharAt(builder.length() - 1).append(")").toString()
                + COURT_LIST_PUBLISH_STATUS_QUERY_SUFFIX;
    }

    protected CourtListPublishStatusResult entityFrom(ResultSet resultSet) throws SQLException {
        final UUID courtCentreId = (UUID) resultSet.getObject("court_centre_id");
        final PublishCourtListType publishCourtListType = PublishCourtListType.valueOf(resultSet.getString("publish_court_list_type"));
        final ZonedDateTime lastUpdated = fromSqlTimestamp(resultSet.getTimestamp("last_updated"));
        final PublishStatus status = valueFor(resultSet.getString("publish_status")).orElse(PublishStatus.INVALID_STATUS);
        final String failureMessage = resultSet.getString("error_message");
        return new CourtListPublishStatusResult(courtCentreId, publishCourtListType, lastUpdated, status, failureMessage);
    }

    public void save(final CourtListPublishStatus courtListPublishStatus) {
        try(final Connection connection = dataSource.getConnection()) {
            save(courtListPublishStatus, connection);
        } catch (final SQLException e) {
            throw new CourtListPublishStatusJdbcException(format("Exception while inserting: %s", COURT_LIST_PUBLISH_STATUS_INSERT_QUERY), e);
        }
    }

    public void save(final CourtListPublishStatus courtListPublishStatus, final Connection connection) {
        try (final PreparedStatement ps = connection.prepareStatement(COURT_LIST_PUBLISH_STATUS_INSERT_QUERY)) {
            ps.setObject(1, courtListPublishStatus.getPublishCourtListStatusId());
            ps.setObject(2, courtListPublishStatus.getCourtCentreId());
            ps.setString(3, courtListPublishStatus.getPublishStatus().toString());
            ps.setString(4, courtListPublishStatus.getPublishCourtListType().toString());
            ps.setTimestamp(5, toSqlTimestamp(courtListPublishStatus.getLastUpdated()));
            ps.setObject(6, courtListPublishStatus.getCourtListFileId());
            ps.setString(7, courtListPublishStatus.getCourtListFileName());
            ps.setString(8, courtListPublishStatus.getErrorMessage());
            ps.setObject(9, courtListPublishStatus.getPublishDate());
            ps.setBoolean(10, courtListPublishStatus.isWeekCommencing());
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new CourtListPublishStatusJdbcException(format("Exception while inserting: %s", COURT_LIST_PUBLISH_STATUS_INSERT_QUERY), e);
        }
    }
}
