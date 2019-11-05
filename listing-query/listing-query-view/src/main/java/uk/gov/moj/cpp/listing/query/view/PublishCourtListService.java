package uk.gov.moj.cpp.listing.query.view;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListPublishStatus;
import uk.gov.moj.cpp.listing.persistence.repository.CourtListRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

public class PublishCourtListService {

    @Inject
    private CourtListRepository courtListRepository;

    public List<CourtListPublishStatus> getCourtListPublishStatuses(final UUID courtCentreId,
                                                                    final Set<PublishCourtListType> courtListTypes) {
        return courtListRepository.courtListPublishStatuses(courtCentreId, courtListTypes);
    }
}
