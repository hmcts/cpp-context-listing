package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.listing.event.PublishCourtListType.FIRM;
import static uk.gov.justice.listing.event.PublishStatus.COURT_LIST_REQUESTED;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishStatus;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CourtListRepositoryTest extends BaseTransactionalTest {

    @Inject
    private CourtListRepository courtListRepository;

    @Test
    public void shouldReturnPublishStatus() {
        final UUID courtCentreId = randomUUID();
        final PublishStatus publishStatus = COURT_LIST_REQUESTED;
        final UUID courtListFileId = randomUUID();
        final String courtListFileName = "c1";
        final PublishCourtListType publishCourtListType = FIRM;
        final ZonedDateTime lastUpdated = ZonedDateTime.now();
        final CourtListPK courtListPK = new CourtListPK(courtCentreId,publishCourtListType);

        final CourtList courtList = new CourtList(courtListPK, publishStatus, courtListFileId, courtListFileName,  lastUpdated);

        courtListRepository.save(courtList);

        final Set<PublishCourtListType> courtListTypes = new HashSet<>();
        courtListTypes.add(publishCourtListType);
        final List<CourtListPublishStatus> courtListPublishStatuses =
                courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes);

        assertThat(courtListPublishStatuses.size(), is(1));
    }

}