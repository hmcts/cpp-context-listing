package uk.gov.moj.cpp.listing.event.processor.azure.builder;

import uk.gov.moj.cpp.listing.event.processor.azure.data.SlotDetail;

public class SlotDetailBuilder {

    private String ouCode;
    private String businessType;
    private int courtRoomId;
    private String sessionDate;
    private String session;
    private String hearingId;
    private int duration;
    private String courtScheduleId;
    private String bookingId;

    private SlotDetailBuilder(){}

    public static SlotDetailBuilder slotDetail(){
        return  new SlotDetailBuilder();
    }

    public SlotDetailBuilder withOuCode(final String ouCode){
        this.ouCode = ouCode;
        return this;
    }

    public SlotDetailBuilder withBusinessType(final String businessType){
        this.businessType = businessType;
        return this;
    }

    public SlotDetailBuilder withCourtRoomId(final int courtRoomId){
        this.courtRoomId = courtRoomId;
        return this;
    }

    public SlotDetailBuilder withSessionDate(final String sessionDate){
        this.sessionDate = sessionDate;
        return this;
    }

    public SlotDetailBuilder withSession(final String session){
        this.session = session;
        return this;
    }

    public SlotDetailBuilder withHearingId(final String hearingId){
        this.hearingId = hearingId;
        return this;
    }

    public SlotDetailBuilder withDuration(final int duration){
        this.duration = duration;
        return this;
    }

    public SlotDetailBuilder withCourtScheduleId(final String courtScheduleId){
        this.courtScheduleId = courtScheduleId;
        return this;
    }

    public SlotDetailBuilder withBookingId(final String bookingId){
        this.bookingId = bookingId;
        return this;
    }

    public SlotDetail build(){
        return new SlotDetail(
                ouCode,
                businessType,
                courtRoomId,
                sessionDate,
                session,
                hearingId,
                duration,
                courtScheduleId,
                bookingId
        );
    }

}
