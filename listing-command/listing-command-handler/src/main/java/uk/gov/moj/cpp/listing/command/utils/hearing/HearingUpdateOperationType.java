package uk.gov.moj.cpp.listing.command.utils.hearing;


//please refer to https://tools.hmcts.net/confluence/x/QoovXg for details
public enum HearingUpdateOperationType {
    SPLIT,

    FULL_ALLOCATION,

    PARTIAL_ALLOCATION,

    UPDATE_SOURCE_ONLY,

    UNALLOCATED_NO_OFFENCE_CHANGE;


    HearingUpdateOperationType() {

    }
}
