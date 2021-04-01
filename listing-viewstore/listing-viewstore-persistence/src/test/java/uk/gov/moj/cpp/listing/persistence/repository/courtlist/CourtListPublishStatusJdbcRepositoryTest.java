package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.listing.event.PublishCourtListType.FIRM;
import static uk.gov.justice.listing.event.PublishCourtListType.WARN;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishStatus;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.naming.Context;
import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CourtListPublishStatusJdbcRepositoryTest extends BaseTransactionalTest {
    final BasicDataSource dataSource = new BasicDataSource();

    private static final String LIQUIBASE_MAPPING_CHANGELOG_XML = "liquibase/listing-view-store-db-changelog.xml";

    protected CourtListPublishStatusJdbcRepository courtListRepository;

    private final UUID courtCentreId = randomUUID();

    public CourtListPublishStatusJdbcRepositoryTest() {
    }

    @Before
    public void initializeDependencies() throws Exception {
        registerDataSource();
    }

    protected void registerDataSource() throws Exception {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.naming.java.javaURLContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES,
                "org.apache.naming");

        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres");
        dataSource.setUrl("jdbc:postgresql://localhost:5533/" + "postgres");
        initDatabase(dataSource);
    }

    private void initDatabase(final DataSource dataSource) throws Exception {
        courtListRepository = new CourtListPublishStatusJdbcRepository();
        setField(courtListRepository, "dataSource", dataSource);
        Liquibase liquibase = new Liquibase(LIQUIBASE_MAPPING_CHANGELOG_XML,
                new ClassLoaderResourceAccessor(), new JdbcConnection(dataSource.getConnection()));
        liquibase.dropAll();
        liquibase.update("");
        CourtListPublishStatusDBCleaner databaseCleaner = new CourtListPublishStatusDBCleaner(dataSource);
        databaseCleaner.cleanTable("court_list_publish_status");
        setUpTestData(courtCentreId, now(), WARN, FIRM);
    }

    private void setUpTestData(UUID courtCentreId, LocalDate fixedPublishDate, PublishCourtListType publishFirmCourtListType, PublishCourtListType publishWarnCourtListType) throws InterruptedException {
        try (final Connection connection = dataSource.getConnection()) {

            final LocalDate fixedPublishDatePlus1Day = fixedPublishDate.plusDays(1);
            final LocalDate fixedPublishDateMinus1Day = fixedPublishDate.minusDays(1);
            final ZonedDateTime now = ZonedDateTime.now();

            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDateMinus1Day, now, false);
            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_PRODUCED, fixedPublishDateMinus1Day, now.plusSeconds(1), false);

            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDateMinus1Day, now.plusSeconds(2), false);
            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_PRODUCED, fixedPublishDateMinus1Day, now.plusSeconds(3), false);

            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDate, now.plusSeconds(4), false);
            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_PRODUCED, fixedPublishDate, now.plusSeconds(5), false);
            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.EXPORT_SUCCESSFUL, fixedPublishDate, now.plusSeconds(6), false);

            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDate, now.plusSeconds(7), false);
            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_PRODUCED, fixedPublishDate, now.plusSeconds(8), false);
            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.EXPORT_SUCCESSFUL, fixedPublishDate, now.plusSeconds(9), false);

            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDatePlus1Day, now.plusSeconds(10), false);
            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDatePlus1Day, now.plusSeconds(11), false);

            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDateMinus1Day, now.plusSeconds(12), true);
            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_PRODUCED, fixedPublishDateMinus1Day, now.plusSeconds(13), true);

            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDateMinus1Day, now.plusSeconds(14), true);
            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_PRODUCED, fixedPublishDateMinus1Day, now.plusSeconds(15), true);

            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDate, now.plusSeconds(16), true);
            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_PRODUCED, fixedPublishDate, now.plusSeconds(17), true);
            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.EXPORT_SUCCESSFUL, fixedPublishDate, now.plusSeconds(18), true);

            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDate, now.plusSeconds(19), true);
            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_PRODUCED, fixedPublishDate, now.plusSeconds(20), true);
            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.EXPORT_SUCCESSFUL, fixedPublishDate, now.plusSeconds(21), true);

            create(connection, courtCentreId, publishWarnCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDatePlus1Day, now.plusSeconds(22), true);
            create(connection, courtCentreId, publishFirmCourtListType, PublishStatus.COURT_LIST_REQUESTED, fixedPublishDatePlus1Day, now.plusSeconds(23), true);


        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void shouldReturnPublishStatusForFixedDateMinusOneDay() {
        final LocalDate fixedPublishDateMinusOneDay = now().minusDays(1);

        final PublishCourtListType publishFirmCourtListType = FIRM;
        final PublishCourtListType publishWarnCourtListType = WARN;

        final Set<PublishCourtListType> courtListTypes = new HashSet<>();
        courtListTypes.add(publishWarnCourtListType);
        courtListTypes.add(publishFirmCourtListType);

        final List<CourtListPublishStatusResult> courtListPublishStatuses =
                courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes, fixedPublishDateMinusOneDay, false);

        MatcherAssert.assertThat(courtListPublishStatuses.size(), CoreMatchers.is(2));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishStatus(), CoreMatchers.is(PublishStatus.COURT_LIST_PRODUCED));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishCourtListType(), CoreMatchers.is(FIRM));

        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishStatus(), CoreMatchers.is(PublishStatus.COURT_LIST_PRODUCED));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishCourtListType(), CoreMatchers.is(WARN));
    }

    @Test
    public void shouldReturnPublishStatusForFixedDate() {
        final LocalDate fixedPublishDate = now();

        final PublishCourtListType publishFirmCourtListType = FIRM;
        final PublishCourtListType publishWarnCourtListType = WARN;

        final Set<PublishCourtListType> courtListTypes = new HashSet<>();
        courtListTypes.add(publishWarnCourtListType);
        courtListTypes.add(publishFirmCourtListType);

        final List<CourtListPublishStatusResult> courtListPublishStatuses =
                courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes, fixedPublishDate, false);

        MatcherAssert.assertThat(courtListPublishStatuses.size(), CoreMatchers.is(2));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishStatus(), CoreMatchers.is(PublishStatus.EXPORT_SUCCESSFUL));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishCourtListType(), CoreMatchers.is(FIRM));

        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishStatus(), CoreMatchers.is(PublishStatus.EXPORT_SUCCESSFUL));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishCourtListType(), CoreMatchers.is(WARN));
    }

    @Test
    public void shouldReturnPublishStatusForFixedDatePlusOneDay() {
        final LocalDate fixedPublishDatePlusOneDay = now().plusDays(1);

        final PublishCourtListType publishFirmCourtListType = FIRM;
        final PublishCourtListType publishWarnCourtListType = WARN;

        final Set<PublishCourtListType> courtListTypes = new HashSet<>();
        courtListTypes.add(publishWarnCourtListType);
        courtListTypes.add(publishFirmCourtListType);

        final List<CourtListPublishStatusResult> courtListPublishStatuses =
                courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes, fixedPublishDatePlusOneDay, false);

        MatcherAssert.assertThat(courtListPublishStatuses.size(), CoreMatchers.is(2));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishStatus(), CoreMatchers.is(PublishStatus.COURT_LIST_REQUESTED));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishCourtListType(), CoreMatchers.is(FIRM));

        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishStatus(), CoreMatchers.is(PublishStatus.COURT_LIST_REQUESTED));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishCourtListType(), CoreMatchers.is(WARN));
    }


    @Test
    public void shouldNotReturnPublishStatusForFixedDateWhenPublishHasNotBeenDoneBefore() {
        final LocalDate fixedPublishDate = now().plusDays(2);

        final PublishCourtListType publishFirmCourtListType = FIRM;
        final PublishCourtListType publishWarnCourtListType = WARN;

        final Set<PublishCourtListType> courtListTypes = new HashSet<>();
        courtListTypes.add(publishWarnCourtListType);
        courtListTypes.add(publishFirmCourtListType);

        final List<CourtListPublishStatusResult> courtListPublishStatuses =
                courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes, fixedPublishDate, false);

        MatcherAssert.assertThat(courtListPublishStatuses.size(), CoreMatchers.is(0));
    }

    @Test
    public void shouldReturnPublishStatusForWeekCommencingMinusOneDay() {
        final LocalDate weekCommencingPublishDateMinusOneDay = now().minusDays(1);

        final PublishCourtListType publishFirmCourtListType = FIRM;
        final PublishCourtListType publishWarnCourtListType = WARN;

        final Set<PublishCourtListType> courtListTypes = new HashSet<>();
        courtListTypes.add(publishWarnCourtListType);
        courtListTypes.add(publishFirmCourtListType);

        final List<CourtListPublishStatusResult> courtListPublishStatuses =
                courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes, weekCommencingPublishDateMinusOneDay, true);

        MatcherAssert.assertThat(courtListPublishStatuses.size(), CoreMatchers.is(2));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishStatus(), CoreMatchers.is(PublishStatus.COURT_LIST_PRODUCED));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishCourtListType(), CoreMatchers.is(FIRM));

        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishStatus(), CoreMatchers.is(PublishStatus.COURT_LIST_PRODUCED));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishCourtListType(), CoreMatchers.is(WARN));
    }

    @Test
    public void shouldReturnPublishStatusForWeekCommencing() {
        final LocalDate weekCommencingPublishDate = now();

        final PublishCourtListType publishFirmCourtListType = FIRM;
        final PublishCourtListType publishWarnCourtListType = WARN;

        final Set<PublishCourtListType> courtListTypes = new HashSet<>();
        courtListTypes.add(publishWarnCourtListType);
        courtListTypes.add(publishFirmCourtListType);

        final List<CourtListPublishStatusResult> courtListPublishStatuses =
                courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes, weekCommencingPublishDate, true);

        MatcherAssert.assertThat(courtListPublishStatuses.size(), CoreMatchers.is(2));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishStatus(), CoreMatchers.is(PublishStatus.EXPORT_SUCCESSFUL));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishCourtListType(), CoreMatchers.is(FIRM));

        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishStatus(), CoreMatchers.is(PublishStatus.EXPORT_SUCCESSFUL));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishCourtListType(), CoreMatchers.is(WARN));

    }

    @Test
    public void shouldReturnPublishStatusForWeekCommencingDatePlusOneDay() {
        final LocalDate weekCommencingPublishDatePlusOneDay = now().plusDays(1);

        final PublishCourtListType publishFirmCourtListType = FIRM;
        final PublishCourtListType publishWarnCourtListType = WARN;

        final Set<PublishCourtListType> courtListTypes = new HashSet<>();
        courtListTypes.add(publishWarnCourtListType);
        courtListTypes.add(publishFirmCourtListType);

        final List<CourtListPublishStatusResult> courtListPublishStatuses =
                courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes, weekCommencingPublishDatePlusOneDay, true);

        MatcherAssert.assertThat(courtListPublishStatuses.size(), CoreMatchers.is(2));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishStatus(), CoreMatchers.is(PublishStatus.COURT_LIST_REQUESTED));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(0).getPublishCourtListType(), CoreMatchers.is(FIRM));

        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishStatus(), CoreMatchers.is(PublishStatus.COURT_LIST_REQUESTED));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getCourtCentreId(), CoreMatchers.is(courtCentreId));
        MatcherAssert.assertThat(courtListPublishStatuses.get(1).getPublishCourtListType(), CoreMatchers.is(WARN));
    }

    @Test
    public void shouldNotReturnPublishStatusForWeekCommencingDateWhenPublishHasNotBeenDoneBefore() {
        final LocalDate fixedPublishDate = now().plusDays(2);

        final PublishCourtListType publishFirmCourtListType = FIRM;
        final PublishCourtListType publishWarnCourtListType = WARN;

        final Set<PublishCourtListType> courtListTypes = new HashSet<>();
        courtListTypes.add(publishWarnCourtListType);
        courtListTypes.add(publishFirmCourtListType);

        final List<CourtListPublishStatusResult> courtListPublishStatuses =
                courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes, fixedPublishDate, true);

        MatcherAssert.assertThat(courtListPublishStatuses.size(), CoreMatchers.is(0));
    }

    public void create(final Connection connection,
                       final UUID courtCentreId,
                       final PublishCourtListType publishCourtListType,
                       final PublishStatus publishStatus,
                       final LocalDate publishDate,
                       final ZonedDateTime lastUpdated,
                       final boolean weekCommencing) {

        final UUID courtListFileId = randomUUID();
        final String courtListFileName = "c1";
        final CourtListPublishStatus courtListPublishStatus = new CourtListPublishStatus(randomUUID(), courtCentreId, publishCourtListType, publishStatus, lastUpdated,
                courtListFileId, courtListFileName, "", publishDate, weekCommencing);
        courtListRepository.save(courtListPublishStatus, connection);
    }
}
