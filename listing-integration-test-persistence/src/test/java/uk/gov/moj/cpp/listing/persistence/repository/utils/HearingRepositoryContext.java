package uk.gov.moj.cpp.listing.persistence.repository.utils;

import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.Type;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class HearingRepositoryContext {

    private final UUID hearingId;
    private final UUID courtCentreId;
    private final UUID courtRoomId;
    private final Boolean allocated;
    private final Boolean vacated;
    private final UUID authorityId;
    private final Type hearingType;
    private final JurisdictionType jurisdictionType;
    private final String judicialId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ZonedDateTime startTime;
    private final ZonedDateTime endTime;
    private final ZonedDateTime hearingDate;
    private final LocalDate weekCommencingStartDate;
    private final LocalDate weekCommencingEndDate;
    private final String fileLocation;
    private final boolean multidayHearing;
    private final ZonedDateTime hearingDateDay1;
    private final ZonedDateTime hearingDateDay2;
    private final ZonedDateTime hearingDateDay3;
    private final ZonedDateTime startTimeDay1;
    private final ZonedDateTime endTimeDay1;
    private final ZonedDateTime startTimeDay2;
    private final ZonedDateTime endTimeDay2;
    private final ZonedDateTime startTimeDay3;
    private final ZonedDateTime endTimeDay3;
    private final Boolean cancelledDay1;
    private final Boolean cancelledDay2;
    private final Boolean cancelledDay3;
    private final Boolean unscheduled;
    private final UUID typeOfListId;
    private final Boolean possibleDisqualification;

    public HearingRepositoryContext(final UUID hearingId, final UUID courtCentreId, final UUID courtRoomId, final Boolean allocated,
                                    final Boolean vacated, final UUID authorityId, final Type hearingType, final JurisdictionType jurisdictionType,
                                    final String judicialId, final LocalDate startDate, final LocalDate endDate, final ZonedDateTime startTime,
                                    final ZonedDateTime endTime, final ZonedDateTime hearingDate, final LocalDate weekCommencingStartDate,
                                    final LocalDate weekCommencingEndDate, final String fileLocation, final boolean multidayHearing, final ZonedDateTime hearingDateDay1,
                                    final ZonedDateTime hearingDateDay2, final ZonedDateTime hearingDateDay3, final ZonedDateTime startTimeDay1, final ZonedDateTime endTimeDay1,
                                    final ZonedDateTime startTimeDay2, final ZonedDateTime endTimeDay2, final ZonedDateTime startTimeDay3, final ZonedDateTime endTimeDay3,
                                    final Boolean cancelledDay1, final Boolean cancelledDay2, final Boolean cancelledDay3, final Boolean unscheduled,
                                    final UUID typeOfListId, final Boolean possibleDisqualification) {
        this.hearingId = hearingId;
        this.courtCentreId = courtCentreId;
        this.courtRoomId = courtRoomId;
        this.allocated = allocated;
        this.vacated = vacated;
        this.authorityId = authorityId;
        this.hearingType = hearingType;
        this.jurisdictionType = jurisdictionType;
        this.judicialId = judicialId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hearingDate = hearingDate;
        this.weekCommencingStartDate = weekCommencingStartDate;
        this.weekCommencingEndDate = weekCommencingEndDate;
        this.fileLocation = fileLocation;
        this.multidayHearing = multidayHearing;
        this.hearingDateDay1 = hearingDateDay1;
        this.hearingDateDay2 = hearingDateDay2;
        this.hearingDateDay3 = hearingDateDay3;
        this.startTimeDay1 = startTimeDay1;
        this.endTimeDay1 = endTimeDay1;
        this.startTimeDay2 = startTimeDay2;
        this.endTimeDay2 = endTimeDay2;
        this.startTimeDay3 = startTimeDay3;
        this.endTimeDay3 = endTimeDay3;
        this.cancelledDay1 = cancelledDay1;
        this.cancelledDay2 = cancelledDay2;
        this.cancelledDay3 = cancelledDay3;
        this.unscheduled = unscheduled;
        this.typeOfListId = typeOfListId;
        this.possibleDisqualification = possibleDisqualification;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public Boolean isAllocated() {
        return allocated;
    }

    public Boolean isVacated() {
        return vacated;
    }

    public UUID getAuthorityId() {
        return authorityId;
    }

    public Type getHearingType() {
        return hearingType;
    }

    public JurisdictionType getJurisdictionType() {
        return jurisdictionType;
    }

    public String getJudicialId() {
        return judicialId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public ZonedDateTime getHearingDate() {
        return hearingDate;
    }

    public LocalDate getWeekCommencingStartDate() {
        return weekCommencingStartDate;
    }

    public LocalDate getWeekCommencingEndDate() {
        return weekCommencingEndDate;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public boolean isMultidayHearing() {
        return multidayHearing;
    }

    public ZonedDateTime getHearingDateDay1() {
        return hearingDateDay1;
    }

    public ZonedDateTime getHearingDateDay2() {
        return hearingDateDay2;
    }

    public ZonedDateTime getHearingDateDay3() {
        return hearingDateDay3;
    }

    public ZonedDateTime getStartTimeDay1() {
        return startTimeDay1;
    }

    public ZonedDateTime getEndTimeDay1() {
        return endTimeDay1;
    }

    public ZonedDateTime getStartTimeDay2() {
        return startTimeDay2;
    }

    public ZonedDateTime getEndTimeDay2() {
        return endTimeDay2;
    }

    public ZonedDateTime getStartTimeDay3() {
        return startTimeDay3;
    }

    public ZonedDateTime getEndTimeDay3() {
        return endTimeDay3;
    }

    public Boolean isCancelledDay1() {
        return cancelledDay1;
    }

    public Boolean isCancelledDay2() {
        return cancelledDay2;
    }

    public Boolean isCancelledDay3() {
        return cancelledDay3;
    }

    public Boolean isUnscheduled() {
        return unscheduled;
    }

    public UUID getTypeOfListId() {
        return typeOfListId;
    }

    public Boolean isPossibleDisqualification() {
        return possibleDisqualification;
    }

    public static Builder hearingRepositoryContext() {
        return new Builder();
    }

    public static final class Builder {
        private UUID hearingId;
        private UUID courtCentreId;
        private UUID courtRoomId;
        private Boolean allocated;
        private Boolean isPossibleDisqualification;
        private Boolean vacated;
        private UUID authorityId;
        private Type hearingType;
        private JurisdictionType jurisdictionType;
        private String judicialId;
        private LocalDate startDate;
        private LocalDate endDate;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
        private ZonedDateTime hearingDate;
        private LocalDate weekCommencingStartDate;
        private LocalDate weekCommencingEndDate;
        private String fileLocation;
        private boolean multidayHearing;
        private ZonedDateTime hearingDateDay1;
        private ZonedDateTime hearingDateDay2;
        private ZonedDateTime hearingDateDay3;
        private ZonedDateTime startTimeDay1;
        private ZonedDateTime endTimeDay1;
        private ZonedDateTime startTimeDay2;
        private ZonedDateTime endTimeDay2;
        private ZonedDateTime startTimeDay3;
        private ZonedDateTime endTimeDay3;
        private Boolean cancelledDay1;
        private Boolean cancelledDay2;
        private Boolean cancelledDay3;
        private Boolean unscheduled;
        private UUID typeOfListId;

        private Builder() {
        }

        public Builder withHearingId(UUID hearingId) {
            this.hearingId = hearingId;
            return this;
        }

        public Builder withCourtCentreId(UUID courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public Builder withCourtRoomId(UUID courtRoomId) {
            this.courtRoomId = courtRoomId;
            return this;
        }

        public Builder withAllocated(Boolean allocated) {
            this.allocated = allocated;
            return this;
        }

        public Builder withVacated(Boolean vacated) {
            this.vacated = vacated;
            return this;
        }

        public Builder withAuthorityId(UUID authorityId) {
            this.authorityId = authorityId;
            return this;
        }

        public Builder withHearingType(Type hearingType) {
            this.hearingType = hearingType;
            return this;
        }

        public Builder withJurisdictionType(JurisdictionType jurisdictionType) {
            this.jurisdictionType = jurisdictionType;
            return this;
        }

        public Builder withJudicialId(String judicialId) {
            this.judicialId = judicialId;
            return this;
        }

        public Builder withStartDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withEndDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder withStartTime(ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withEndTime(ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder withHearingDate(ZonedDateTime hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Builder withWeekCommencingStartDate(LocalDate weekCommencingStartDate) {
            this.weekCommencingStartDate = weekCommencingStartDate;
            return this;
        }

        public Builder withWeekCommencingEndDate(LocalDate weekCommencingEndDate) {
            this.weekCommencingEndDate = weekCommencingEndDate;
            return this;
        }

        public Builder withFileLocation(String fileLocation) {
            this.fileLocation = fileLocation;
            return this;
        }

        public Builder withMultidayHearing(boolean multidayHearing) {
            this.multidayHearing = multidayHearing;
            return this;
        }

        public Builder withHearingDateDay1(ZonedDateTime hearingDateDay1) {
            this.hearingDateDay1 = hearingDateDay1;
            return this;
        }

        public Builder withHearingDateDay2(ZonedDateTime hearingDateDay2) {
            this.hearingDateDay2 = hearingDateDay2;
            return this;
        }

        public Builder withHearingDateDay3(ZonedDateTime hearingDateDay3) {
            this.hearingDateDay3 = hearingDateDay3;
            return this;
        }

        public Builder withStartTimeDay1(ZonedDateTime startTimeDay1) {
            this.startTimeDay1 = startTimeDay1;
            return this;
        }

        public Builder withEndTimeDay1(ZonedDateTime endTimeDay1) {
            this.endTimeDay1 = endTimeDay1;
            return this;
        }

        public Builder withStartTimeDay2(ZonedDateTime startTimeDay2) {
            this.startTimeDay2 = startTimeDay2;
            return this;
        }

        public Builder withEndTimeDay2(ZonedDateTime endTimeDay2) {
            this.endTimeDay2 = endTimeDay2;
            return this;
        }

        public Builder withStartTimeDay3(ZonedDateTime startTimeDay3) {
            this.startTimeDay3 = startTimeDay3;
            return this;
        }

        public Builder withEndTimeDay3(ZonedDateTime endTimeDay3) {
            this.endTimeDay3 = endTimeDay3;
            return this;
        }

        public Builder withCancelledDay1(Boolean cancelledDay1) {
            this.cancelledDay1 = cancelledDay1;
            return this;
        }

        public Builder withCancelledDay2(Boolean cancelledDay2) {
            this.cancelledDay2 = cancelledDay2;
            return this;
        }

        public Builder withCancelledDay3(Boolean cancelledDay3) {
            this.cancelledDay3 = cancelledDay3;
            return this;
        }

        public Builder withUnscheduled(Boolean unscheduled) {
            this.unscheduled = unscheduled;
            return this;
        }

        public Builder withTypeOfListId(UUID typeOfListId) {
            this.typeOfListId = typeOfListId;
            return this;
        }

        public Builder withIsPossibleDisqualification(Boolean isPossibleDisqualification) {
            this.isPossibleDisqualification = isPossibleDisqualification;
            return this;
        }

        public HearingRepositoryContext build() {
            return new HearingRepositoryContext(hearingId, courtCentreId, courtRoomId, allocated, vacated, authorityId, hearingType, jurisdictionType, judicialId, startDate, endDate, startTime, endTime, hearingDate, weekCommencingStartDate, weekCommencingEndDate, fileLocation, multidayHearing, hearingDateDay1, hearingDateDay2, hearingDateDay3, startTimeDay1, endTimeDay1, startTimeDay2, endTimeDay2, startTimeDay3, endTimeDay3, cancelledDay1, cancelledDay2, cancelledDay3, unscheduled, typeOfListId, isPossibleDisqualification);
        }
    }
}
