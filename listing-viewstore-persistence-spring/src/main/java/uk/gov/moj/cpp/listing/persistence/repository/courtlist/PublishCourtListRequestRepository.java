package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** {@code publish_court_list_request} (changeset 032, ADR 015). */
@Repository
public interface PublishCourtListRequestRepository extends JpaRepository<PublishCourtListRequest, UUID> {
}
