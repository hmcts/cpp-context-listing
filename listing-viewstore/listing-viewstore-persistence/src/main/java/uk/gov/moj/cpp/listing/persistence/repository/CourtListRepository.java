package uk.gov.moj.cpp.listing.persistence.repository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;

@Repository
@ApplicationScoped
public abstract class CourtListRepository implements EntityRepository<CourtList, UUID>, CriteriaSupport<CourtList> {

    public static final String COURT_HOUSE_ID = "courtHouseId";
    public static final String DOCUMENT_TYPE = "documentType";
    public static final String STATUS = "status";
    public static final String DATE_ACTIONED = "dateActioned";

    @Inject
    private EntityManager entityManager;

    public List<CourtList> findCourtList(final Map<String, Object> queryParameters) {

        final CriteriaBuilder qb = entityManager.getCriteriaBuilder();
        final CriteriaQuery cq = qb.createQuery();
        final Root<CourtList> notification = cq.from(CourtList.class);

        final List<Predicate> predicates = new ArrayList<>();

        if (queryParameters.containsKey(STATUS)) {
            predicates.add(qb.equal(notification.get(STATUS), queryParameters.get(STATUS)));
        }

        if (queryParameters.containsKey(COURT_HOUSE_ID)) {
            predicates.add(qb.equal(notification.get(COURT_HOUSE_ID), queryParameters.get(COURT_HOUSE_ID)));
        }

        if (queryParameters.containsKey(DATE_ACTIONED)) {
            predicates.add(qb.equal(notification.get(DATE_ACTIONED), queryParameters.get(DATE_ACTIONED)));
        }

        if (queryParameters.containsKey(DOCUMENT_TYPE)) {
            predicates.add(qb.greaterThanOrEqualTo(notification.get(DOCUMENT_TYPE), (ZonedDateTime) queryParameters.get(DOCUMENT_TYPE)));
        }

        cq.select(notification).where(predicates.toArray(new Predicate[]{}));

        return entityManager.createQuery(cq).getResultList();
    }
}