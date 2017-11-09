package uk.gov.moj.cpp.listing.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.listing.event.HearingUpdatedForListing;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingBuilder;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.time.LocalTime;
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

        hearingBuilder.setCourtRoomId(chooseHearingData(existingHearing.getCourtRoomId(), hearingEvent.getCourtRoomId()));
        hearingBuilder.setJudgeId(chooseHearingData(existingHearing.getJudgeId(),hearingEvent.getJudgeId()));
        hearingBuilder.setType(chooseHearingData(existingHearing.getType(), hearingEvent.getType()));
        hearingBuilder.setNotBefore(chooseHearingData(existingHearing.getNotBefore(), hearingEvent.getHearingPeriod().getNotBefore()));
        hearingBuilder.setStartDate(chooseHearingData(existingHearing.getStartDate(), hearingEvent.getHearingPeriod().getStartDate()));
        hearingBuilder.setStartTime(chooseHearingData(existingHearing.getStartTime(), hearingEvent.getHearingPeriod().getStartTime()));
        hearingBuilder.setEstimateMinutes(chooseHearingData(existingHearing.getEstimateMinutes(), hearingEvent.getEstimateMinutes()));

        final Hearing hearing = hearingBuilder.build();
        hearing.getDefendants().addAll(existingHearing.getDefendants());
        return hearing;
    }

    private String chooseHearingData(final String existingHearingData, final String newHearingData){
        if(StringUtils.isNotEmpty(newHearingData)){
            return newHearingData;
        }
        return existingHearingData;
    }

    private UUID chooseHearingData(final UUID existingHearingData, final String newHearingData){
        if(StringUtils.isNotEmpty(newHearingData)){
            return UUID.fromString(newHearingData);
        }
        return existingHearingData;
    }

    private LocalDate chooseHearingData(final LocalDate existingHearingData, final LocalDate newHearingData){
        if(newHearingData!=null){
            return newHearingData;
        }
        return existingHearingData;
    }

    private LocalTime chooseHearingData(final LocalTime existingHearingData, final  LocalTime newHearingData){
        if(newHearingData!=null){
            return newHearingData;
        }
        return existingHearingData;
    }

    private Integer chooseHearingData(final Integer existingHearingData, final Integer newHearingData){
        if(newHearingData!=null){
            return newHearingData;
        }
        return existingHearingData;
    }

    private Boolean chooseHearingData(final Boolean existingHearingData, final Boolean newHearingData){
        if(newHearingData!=null){
            return newHearingData;
        }
        return existingHearingData;
    }


}
