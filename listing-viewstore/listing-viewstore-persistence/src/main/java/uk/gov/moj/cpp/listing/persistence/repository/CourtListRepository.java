package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.event.PublishCourtListType;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;

@Repository
@ApplicationScoped
public abstract class CourtListRepository implements EntityRepository<CourtList, UUID>, CriteriaSupport<CourtList> {

    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String PUBLISH_COURT_LIST_TYPE = "publishCourtListType";
    private static final String LAST_UPDATED = "lastUpdated";
    private static final String COURTLIST_PK = "courtListPK";
    @Inject
    private EntityManager entityManager;

    public List<CourtListPublishStatus> courtListPublishStatuses(final UUID courtCentreId, final Set<PublishCourtListType> courtListTypes) {

        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery criteriaQuery = criteriaBuilder.createQuery(CourtList.class);
        final Root<CourtList> mainQueryRoot = criteriaQuery.from(CourtList.class);

        final Subquery<Timestamp> maxSubQuery = criteriaQuery.subquery(Timestamp.class);
        final Root<CourtList> subQueryRoot = maxSubQuery.from(CourtList.class);
        maxSubQuery.select(criteriaBuilder.greatest(subQueryRoot.<Timestamp>get(LAST_UPDATED)));
        criteriaQuery.where(criteriaBuilder.equal(mainQueryRoot.get(LAST_UPDATED), maxSubQuery));

        final Predicate conditionCourtCentreIdPredicate = criteriaBuilder.equal(mainQueryRoot.get(COURTLIST_PK).get(COURT_CENTRE_ID),
                courtCentreId);

        final Expression<String> exp = mainQueryRoot.get(COURTLIST_PK).get(PUBLISH_COURT_LIST_TYPE);
        final Predicate courtListTypePredicate = exp.in(courtListTypes);
        final Predicate combinedPredicate = criteriaBuilder.and(conditionCourtCentreIdPredicate, courtListTypePredicate);
        criteriaQuery.where(combinedPredicate);
        final List resultList = entityManager.createQuery(criteriaQuery).getResultList();
        return courtListPublishStatuses(resultList);
    }

    private List<CourtListPublishStatus> courtListPublishStatuses(final List<CourtList> courtLists) {
        return courtLists.stream().map(x -> {
            final CourtListPublishStatus courtListPublishStatus = new CourtListPublishStatus(x.getCourtListPK().getCourtCentreId(),
                    x.getCourtListPK().getPublishCourtListType(), x.getLastUpdated(), x.getPublishStatus());
            courtListPublishStatus.setFailureMessage(x.getErrorMessage());
            return courtListPublishStatus;
        }).collect(toList());
    }

}
