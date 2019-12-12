package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.listing.persistence.repository.utils.FileUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

// This must be run only via PersistenceTestSuite
@RunWith(CdiTestRunner.class)
public class PublishedCourtListRepositoryTest extends BaseTransactionalTest {

    private final static UUID COURT_CENTRE_ID_ONE = UUID.fromString("247da57c-d739-4b88-ac16-0feaf8a59833");
    private final static UUID COURT_CENTRE_ID_TWO = UUID.fromString("d7a89a94-f6e9-40d5-804e-ded5626b474e");

    private final static LocalDate APRIL_FOOLS_DAY_2020 = LocalDate.of(2020, Month.APRIL, 1);
    private final static LocalDate BOXING_DAY_2020 = LocalDate.of(2020, Month.DECEMBER, 26);
    private final static ZonedDateTime LAST_UPDATED = ZonedDateTime.now();
    private final static ZonedDateTime LAST_EXPORTED = ZonedDateTime.now();

    @Inject
    private PublishedCourtListRepository publishedCourtListRepository;


    @Test
    public void shouldSuccessfullySaveWhenNoRecordExistsForPrimaryKey() throws Exception {


        final PublishedCourtListPrimaryKey publishedCourtListPrimaryKey
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );
        final PublishedCourtList proposedPublishedCourtList
                = generateProposedPublishedCourtList(publishedCourtListPrimaryKey, getContentTwo(), LAST_UPDATED, LAST_EXPORTED);


        assertNull(publishedCourtListRepository.findBy(publishedCourtListPrimaryKey));
        final PublishedCourtList savedPublishedCourtList = publishedCourtListRepository.save(proposedPublishedCourtList);
        assertEquals(proposedPublishedCourtList, savedPublishedCourtList);

        final PublishedCourtList returnedPublishedCourtListForFirstTime =
                publishedCourtListRepository.findBy(toKey(proposedPublishedCourtList));
        assertEquals(proposedPublishedCourtList, returnedPublishedCourtListForFirstTime);

    }

    @Test
    public void shouldSuccessfullySaveEvenWhenARecordAlreadyExistsForPrimaryKey() throws Exception {

        final PublishedCourtListPrimaryKey publishedCourtListPrimaryKey
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );
        final PublishedCourtList proposedPublishedCourtListHavingContentOne
                = generateProposedPublishedCourtList(publishedCourtListPrimaryKey, getContentOne(), LAST_UPDATED, LAST_EXPORTED);
        final PublishedCourtList proposedPublishedCourtListHavingContentTwo
                = generateProposedPublishedCourtList(publishedCourtListPrimaryKey, getContentTwo(), LAST_UPDATED, LAST_EXPORTED);

        assertNull(publishedCourtListRepository.findBy(publishedCourtListPrimaryKey));

        publishedCourtListRepository.save(proposedPublishedCourtListHavingContentOne);

        final PublishedCourtList foundPublishedCourtListForFirstTime =
                publishedCourtListRepository.findBy(toKey(proposedPublishedCourtListHavingContentOne));
        assertEquals(proposedPublishedCourtListHavingContentOne, foundPublishedCourtListForFirstTime);

        publishedCourtListRepository.save(proposedPublishedCourtListHavingContentTwo);

        final PublishedCourtList foundPublishedCourtListForSecondTime =
                publishedCourtListRepository.findBy(toKey(proposedPublishedCourtListHavingContentTwo));

        assertEquals(proposedPublishedCourtListHavingContentTwo, foundPublishedCourtListForSecondTime);

    }

    @Test
    public void shouldSuccessfullySaveTwoRecordsWithDifferentCourtCentreIds() throws Exception {

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreOne
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreTwo
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_TWO,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );

        assertThat(publishedCourtListRepository.count(), is(0L));

        final PublishedCourtList publishedCourtListOne =
                generateAndSave(primaryKeyUsingCourtCentreOne, getContentOne());
        final PublishedCourtList publishedCourtListTwo
                = generateAndSave(primaryKeyUsingCourtCentreTwo, getContentTwo());

        assertFoundAsExpected(publishedCourtListOne, primaryKeyUsingCourtCentreOne);
        assertFoundAsExpected(publishedCourtListTwo, primaryKeyUsingCourtCentreTwo);

        assertThat(publishedCourtListRepository.count(), is(2L));

    }

    @Test
    public void shouldSuccessfullySaveTwoRecordsWithDifferentTypes() throws Exception {

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreOne
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FIRM,
                APRIL_FOOLS_DAY_2020
        );

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreTwo
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );

        assertThat(publishedCourtListRepository.count(), is(0L));

        final PublishedCourtList publishedCourtListOne =
                generateAndSave(primaryKeyUsingCourtCentreOne, getContentOne());
        final PublishedCourtList publishedCourtListTwo
                = generateAndSave(primaryKeyUsingCourtCentreTwo, getContentTwo());

        assertFoundAsExpected(publishedCourtListOne, primaryKeyUsingCourtCentreOne);
        assertFoundAsExpected(publishedCourtListTwo, primaryKeyUsingCourtCentreTwo);

        assertThat(publishedCourtListRepository.count(), is(2L));

    }

    @Test
    public void shouldSuccessfullySaveTwoRecordsWithDifferentStartDates() throws Exception {

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreOne
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                APRIL_FOOLS_DAY_2020
        );

        final PublishedCourtListPrimaryKey primaryKeyUsingCourtCentreTwo
                = new PublishedCourtListPrimaryKey(
                COURT_CENTRE_ID_ONE,
                PublishCourtListType.FINAL,
                BOXING_DAY_2020
        );

        assertThat(publishedCourtListRepository.count(), is(0L));

        final PublishedCourtList publishedCourtListOne =
                generateAndSave(primaryKeyUsingCourtCentreOne, getContentOne());
        final PublishedCourtList publishedCourtListTwo
                = generateAndSave(primaryKeyUsingCourtCentreTwo, getContentTwo());

        assertFoundAsExpected(publishedCourtListOne, primaryKeyUsingCourtCentreOne);
        assertFoundAsExpected(publishedCourtListTwo, primaryKeyUsingCourtCentreTwo);

        assertThat(publishedCourtListRepository.count(), is(2L));

    }

    private void assertFoundAsExpected(final PublishedCourtList expectedPublishedCourtList, final PublishedCourtListPrimaryKey primaryKey) {
        assertEquals(publishedCourtListRepository.findBy(primaryKey), publishedCourtListRepository.findBy(primaryKey));
    }

    private PublishedCourtList generateAndSave(final PublishedCourtListPrimaryKey primaryKey, final JsonNode content) {
        final PublishedCourtList proposedPublishedCourtList
                = generateProposedPublishedCourtList(primaryKey, content, LAST_UPDATED, LAST_EXPORTED);
        final PublishedCourtList savedPublishedCourtList
                = publishedCourtListRepository.save(proposedPublishedCourtList);
        assertEquals(proposedPublishedCourtList, savedPublishedCourtList);
        return savedPublishedCourtList;
    }

    private JsonNode getContentOne() throws IOException {
        return new ObjectMapper().readTree(FileUtil.getPayload("test-data/courtListJson1.txt"));
    }

    private JsonNode getContentTwo() throws IOException {
        return new ObjectMapper().readTree(FileUtil.getPayload("test-data/courtListJson2.txt"));
    }

    private PublishedCourtListPrimaryKey toKey(final PublishedCourtList publishedCourtList) {
        return new PublishedCourtListPrimaryKey(
                publishedCourtList.getCourtCentreId(),
                publishedCourtList.getPublishCourtListType(),
                publishedCourtList.getStartDate());
    }

    private void assertEquals(final PublishedCourtList expectedPublishedCourtList, final PublishedCourtList savedPublishedCourtList) {

        assertThat(savedPublishedCourtList.getCourtCentreId(), is(expectedPublishedCourtList.getCourtCentreId()));
        assertThat(savedPublishedCourtList.getPublishCourtListType(), is(expectedPublishedCourtList.getPublishCourtListType()));
        assertThat(savedPublishedCourtList.getStartDate(), is(expectedPublishedCourtList.getStartDate()));
        assertThat(savedPublishedCourtList.getCourtListJson(), is(expectedPublishedCourtList.getCourtListJson()));
    }

    private PublishedCourtList generateProposedPublishedCourtList(
            final PublishedCourtListPrimaryKey publishedCourtListPrimaryKey,
            final JsonNode getCourtListJson,
            final ZonedDateTime lastUpdated,
            final ZonedDateTime lastExported) {
        return new PublishedCourtList(
                publishedCourtListPrimaryKey.getCourtCentreId(),
                publishedCourtListPrimaryKey.getPublishCourtListType(),
                publishedCourtListPrimaryKey.getStartDate(),
                getCourtListJson,
                lastUpdated,
                lastExported
        );
    }

}
