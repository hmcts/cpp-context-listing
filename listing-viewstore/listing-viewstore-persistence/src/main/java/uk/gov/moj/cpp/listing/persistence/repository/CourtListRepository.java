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
public abstract class CourtListRepository implements EntityRepository<CourtListPublishStatus, UUID>, CriteriaSupport<CourtListPublishStatus> {

    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String PUBLISH_COURT_LIST_TYPE = "publishCourtListType";
    private static final String LAST_UPDATED = "lastUpdated";
    @Inject
    private EntityManager entityManager;

    public List<CourtListPublishStatusResult> courtListPublishStatuses(final UUID courtCentreId,
                                                                       final Set<PublishCourtListType> courtListTypes) {

        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery criteriaQuery = criteriaBuilder.createQuery(CourtListPublishStatus.class);
        final Root<CourtListPublishStatus> mainQueryRoot = criteriaQuery.from(CourtListPublishStatus.class);

        final Subquery<Timestamp> maxSubQuery = criteriaQuery.subquery(Timestamp.class);
        final Root<CourtListPublishStatus> subQueryRoot = maxSubQuery.from(CourtListPublishStatus.class);
        maxSubQuery.select(criteriaBuilder.greatest(subQueryRoot.<Timestamp>get(LAST_UPDATED)));
        criteriaQuery.where(criteriaBuilder.equal(mainQueryRoot.get(LAST_UPDATED), maxSubQuery));

        final Predicate conditionCourtCentreIdPredicate = criteriaBuilder.equal(mainQueryRoot.get(COURT_CENTRE_ID),
                courtCentreId);

        final Expression<String> exp = mainQueryRoot.get(PUBLISH_COURT_LIST_TYPE);
        final Predicate courtListTypePredicate = exp.in(courtListTypes);
        final Predicate combinedPredicate = criteriaBuilder.and(conditionCourtCentreIdPredicate, courtListTypePredicate);
        criteriaQuery.where(combinedPredicate);
        final List resultList = entityManager.createQuery(criteriaQuery).getResultList();
        return courtListPublishStatuses(resultList);
    }

    private List<CourtListPublishStatusResult> courtListPublishStatuses(final List<CourtListPublishStatus> courtLists) {
        return courtLists.stream().map(x -> {
            final CourtListPublishStatusResult courtListPublishStatus = new CourtListPublishStatusResult(x.getCourtCentreId(),
                    x.getPublishCourtListType(), x.getLastUpdated(), x.getPublishStatus());
            courtListPublishStatus.setFailureMessage(x.getErrorMessage());
            return courtListPublishStatus;
        }).collect(toList());
    }

}
