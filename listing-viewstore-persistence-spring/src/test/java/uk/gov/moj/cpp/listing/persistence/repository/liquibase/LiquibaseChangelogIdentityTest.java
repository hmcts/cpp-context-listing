package uk.gov.moj.cpp.listing.persistence.repository.liquibase;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * D21 (docs/MBD_REFACTOR_PROMPT.md §14): at cut-over the Spring app runs Liquibase against the
 * viewstore the JEE app's Liquibase populated. Liquibase identifies a changeset by
 * (id, author, filename); if Spring recorded a different filename it would try to re-run every
 * changeset. This test replays the changelog the Spring way (spring.liquibase.change-log, as in
 * listing-api's application-datasource.yml) and asserts the recorded identities equal the ones
 * captured from a JEE-deployed viewstore ({@code liquibase-identity/jee-databasechangelog.psv}).
 *
 * <p>The capture holds every row of the JEE viewstore's {@code databasechangelog}. Rows written by
 * the framework's own changelogs (event buffer, subscription repository: not part of
 * {@code liquibase/db-changelog.xml}, never run by the Spring app) are excluded from the
 * comparison. Changesets added after the capture are listed in {@link #ADDED_AFTER_CAPTURE}; each
 * carries a {@code logicalFilePath}, so the JEE app records the same identity when it runs them.
 */
class LiquibaseChangelogIdentityTest extends AbstractRepositoryTest {

    private static final String JEE_CAPTURE = "/liquibase-identity/jee-databasechangelog.psv";

    /** Framework changelog rows in the JEE viewstore (framework-owned tables, not in our changelog). */
    private static final List<String> FRAMEWORK_ID_PREFIXES = List.of("event-buffer-", "stream_buffer-", "subscription-repository-");

    /** Changesets newer than the JEE capture (id|author|filename). */
    private static final Set<String> ADDED_AFTER_CAPTURE = Set.of(
            "031-create-inbound-event-dedup|listing|031-create-inbound-event-dedup.xml",
            "032-create-publish-court-list-request|listing|032-create-publish-court-list-request.xml",
            "033-create-hearing-rules-state|listing|033-create-hearing-rules-state.xml",
            "034-create-case-application-seed-rules-state|listing|034-create-case-application-seed-rules-state.xml");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void springRecordsTheSameChangesetIdentitiesAsTheJeeDeployment() throws IOException {
        final Set<String> spring = new LinkedHashSet<>(jdbcTemplate.query(
                "SELECT id, author, filename FROM databasechangelog ORDER BY orderexecuted",
                (rs, i) -> rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3)));

        final Set<String> expected = jeeListingRows();
        expected.addAll(ADDED_AFTER_CAPTURE);

        assertThat(spring).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static Set<String> jeeListingRows() throws IOException {
        try (InputStream in = Objects.requireNonNull(LiquibaseChangelogIdentityTest.class.getResourceAsStream(JEE_CAPTURE), JEE_CAPTURE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF_8))) {
            return reader.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .filter(line -> FRAMEWORK_ID_PREFIXES.stream().noneMatch(line::startsWith))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
