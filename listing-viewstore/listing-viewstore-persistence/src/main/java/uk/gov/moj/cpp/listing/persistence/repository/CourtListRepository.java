package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.event.PublishCourtListType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;


@Repository
@ApplicationScoped
public abstract class CourtListRepository extends AbstractEntityRepository<CourtListPublishStatus, UUID>
        implements EntityRepository<CourtListPublishStatus, UUID>, CriteriaSupport<CourtListPublishStatus> {

    @Inject
    private EntityManager entityManager;

    private static final String COURT_LIST_PUBLISH_STATUS_QUERY =
            "SELECT * FROM court_list_publish_status WHERE court_centre_id = :courtCenterId " +
                    " AND publish_court_list_type IN (:publishCourtListTypes)  " +
                    " ORDER  BY last_updated DESC limit 1";

    public List<CourtListPublishStatusResult> courtListPublishStatuses(final UUID courtCentreId,
                                                                        final Set<PublishCourtListType> publishCourtListTypes) {
        final List<String> types = publishCourtListTypes.stream().map(PublishCourtListType::toString).collect(toList());
        final List resultList = entityManager.createNativeQuery(COURT_LIST_PUBLISH_STATUS_QUERY, CourtListPublishStatus.class)
                .setParameter("courtCenterId", courtCentreId)
                .setParameter("publishCourtListTypes", types)
                .getResultList();
        return courtListPublishStatuses(resultList);
    }

    private List<CourtListPublishStatusResult> courtListPublishStatuses(final List<CourtListPublishStatus> courtLists) {
        return courtLists.stream().map(cps -> mapTo(cps)).collect(toList());
    }

    private CourtListPublishStatusResult mapTo(final CourtListPublishStatus courtListPublishStatus) {
        final CourtListPublishStatusResult courtListPublishStatusResult
                = new CourtListPublishStatusResult(courtListPublishStatus.getCourtCentreId(),
                courtListPublishStatus.getPublishCourtListType(), courtListPublishStatus.getLastUpdated(),
                courtListPublishStatus.getPublishStatus());
        courtListPublishStatusResult.setFailureMessage(courtListPublishStatus.getErrorMessage());
        return courtListPublishStatusResult;
    }
}
