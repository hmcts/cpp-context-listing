package uk.gov.moj.cpp.listing.utils;

import static java.lang.System.getProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBUtil.class);

    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public int insert(final String table, final List<CSVRecord> params, final Map<String, Class> dataTypes) {

        final String query = buildQuery(table, params);

        int rows = 0;

        try (
                final Connection connection = getConnection();
                final PreparedStatement stmt = connection.prepareStatement(query)) {
            for (final CSVRecord record : params) {

                final AtomicInteger index = new AtomicInteger(1);

                record.toMap().entrySet().forEach(e -> {
                    try {
                        stmt.setObject(index.getAndIncrement(), getValue(e.getKey(), e.getValue(), dataTypes));
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                });
                stmt.addBatch();
            }

            final int[] ints = stmt.executeBatch();

            System.out.println("INSERT SIZE" + ints.length);

            connection.commit();
        } catch (Exception e) {
            LOGGER.error("Error while inserting test data", e);
        }

        return rows;
    }

    public void delete(final String table, final String criteria) {

        try (
                final Connection conn = getConnection();
                final PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + table + " WHERE " + criteria)) {
            stmt.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            LOGGER.error("Error while deleting previously inserted test data", e);
        }
    }


    private Object getValue(final String key, final String value, final Map<String, Class> dataTypes) {
        final Class type = dataTypes.getOrDefault(key, String.class);

        if (type.equals(Integer.class)) {
            return Integer.parseInt(value);
        }
        if (type.equals(Date.class)) {
            return LocalDate.parse(value, formatter);
        }

        return value;
    }

    private static String buildQuery(final String table, final List<CSVRecord> records) {
        final CSVRecord record = records.get(0);

        final Map<String, String> params = record.toMap();

        final StringBuilder qb = new StringBuilder("INSERT INTO ").append(table).append("(");
        params.entrySet().forEach(entry -> qb.append(entry.getKey()).append(","));
        qb.append(") VALUES (");
        params.entrySet().forEach(entry -> qb.append("?,"));
        qb.append(")");

        return qb.toString().replaceAll(",\\)", "\\)");
    }

    private Connection getConnection() {
        try {
            final String url = Optional.ofNullable(getProperty("scsl.db.url")).orElse("jdbc:sqlserver://sandl.database.windows.net:1433;database=sandldb;");
            final String username = Optional.ofNullable(getProperty("scsl.db.username")).orElse("sandldb");
            final String password = Optional.ofNullable(getProperty("scsl.db.password")).orElse("Passw0rd");

            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            LOGGER.error("Error while obtaining  DB connection ", e);
        }

        return null;
    }
}
