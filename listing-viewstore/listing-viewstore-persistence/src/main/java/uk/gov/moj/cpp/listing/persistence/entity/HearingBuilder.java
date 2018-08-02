package uk.gov.moj.cpp.listing.persistence.entity;

import static javax.json.Json.createObjectBuilder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;


@SuppressWarnings("squid:S00107")
public class HearingBuilder {

    private static final String START_TIMES = "startTimes";
    private UUID id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String startTimes;
    private String nonSittingDays;
    private Integer estimateMinutes;
    private String type;
    private UUID courtCentreId;
    private UUID courtRoomId;
    private UUID judgeId;
    private UUID listingCaseId;
    private Boolean allocated;
    private Set<Defendant> defendants = new LinkedHashSet<>();

    public HearingBuilder setId(final UUID id) {
        this.id = id;
        return this;
    }

    public HearingBuilder setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public HearingBuilder setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }


    public HearingBuilder setStartTimes(final List<ZonedDateTime> startTimes) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        if(startTimes!=null && !startTimes.isEmpty()) {
            startTimes.forEach(st -> arrayBuilder.add(st.toInstant().toString()));
            final JsonObjectBuilder startTimesJsonStringBuilder = createObjectBuilder();

            startTimesJsonStringBuilder.add(START_TIMES, arrayBuilder.build());
            this.startTimes = startTimesJsonStringBuilder.build().toString();
        }
        return this;
    }

    public HearingBuilder setNonSittingDays(final List<LocalDate> nonSittingDays) {
        if(nonSittingDays!=null && !nonSittingDays.isEmpty()) {
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            nonSittingDays.forEach(d -> arrayBuilder.add(d.toString()));
            final JsonObjectBuilder nonSittingDaysJsonStringBuilder = createObjectBuilder();

            nonSittingDaysJsonStringBuilder.add("nonSittingDays", arrayBuilder.build());
            this.nonSittingDays = nonSittingDaysJsonStringBuilder.build().toString();
        }
        return this;
    }

    public HearingBuilder setEstimateMinutes(final Integer estimateMinutes) {
        this.estimateMinutes = estimateMinutes;
        return this;
    }

    public HearingBuilder setType(final String type) {
        this.type = type;
        return this;
    }

    public HearingBuilder setCourtCentreId(final UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
        return this;
    }

    public HearingBuilder setCourtRoomId(final UUID courtRoomId) {
        this.courtRoomId = courtRoomId;
        return this;
    }

    public HearingBuilder setJudgeId(final UUID judgeId) {
        this.judgeId = judgeId;
        return this;
    }

    public HearingBuilder setListingCaseId(final UUID listingCaseId) {
        this.listingCaseId = listingCaseId;
        return this;
    }

    public HearingBuilder setAllocated(final Boolean allocated) {
        this.allocated = allocated;
        return this;
    }

    public HearingBuilder setDefendants(final Set<Defendant> defendants) {
        this.defendants = defendants;
        return this;
    }



    public Hearing build() {
        return new Hearing(id, listingCaseId, allocated, defendants,
                new Hearing.HearingDetails(startDate, startTimes, estimateMinutes, type, courtCentreId,
                        courtRoomId, judgeId, nonSittingDays, endDate));
    }
}                                      