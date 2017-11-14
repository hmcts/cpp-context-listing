package uk.gov.moj.cpp.listing.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.event.HearingUpdatedForListing;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class HearingUpdatedConverter implements Converter<HearingUpdatedForListing,  Hearing> {

    @Inject
    private HearingRepository hearingRepository;

    @Override
    public Hearing convert(final HearingUpdatedForListing event) {

        final Hearing existingHearing = hearingRepository.findBy(UUID.fromString(event.getHearingId()));
        return buildHearing(event, existingHearing);
    }

    private Hearing buildHearing(final HearingUpdatedForListing hearingEvent, final Hearing existingHearing) {
        final HearingBuilder hearingBuilder = new HearingBuilder();

        hearingBuilder.setId(existingHearing.getId());
        hearingBuilder.setListingCase(existingHearing.getListingCase());
        hearingBuilder.setCourtCentreId(existingHearing.getCourtCentreId());
        hearingBuilder.setAllocated(existingHearing.getAllocated());

        hearingBuilder.setCourtRoomId(chooseHearingDataOrNull(hearingEvent.getCourtRoomId()));
        hearingBuilder.setJudgeId(chooseHearingDataOrNull(hearingEvent.getJudgeId()));
        hearingBuilder.setType(hearingEvent.getType());
        hearingBuilder.setNotBefore(hearingEvent.getHearingPeriod().getNotBefore());
        hearingBuilder.setStartDate(hearingEvent.getHearingPeriod().getStartDate());
        hearingBuilder.setStartTime(hearingEvent.getHearingPeriod().getStartTime());
        hearingBuilder.setEstimateMinutes(hearingEvent.getEstimateMinutes());

        final Hearing hearing = hearingBuilder.build();
        hearing.getDefendants().addAll(existingHearing.getDefendants());
        return hearing;
    }

    private UUID chooseHearingDataOrNull(final String newHearingData){
        UUID uuid = null;
        if(StringUtils.isNotEmpty(newHearingData)){
            uuid = UUID.fromString(newHearingData);
        }
        return uuid;
    }
}
