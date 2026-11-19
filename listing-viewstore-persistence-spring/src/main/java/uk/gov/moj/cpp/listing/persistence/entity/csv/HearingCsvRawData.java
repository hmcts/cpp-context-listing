package uk.gov.moj.cpp.listing.persistence.entity.csv;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;

/**
 * Raw data model for hearing information before JsonB extraction for CSV reports.
 * This class holds the basic hearing data and the raw properties JSON
 * that will be processed by the extraction service.
 */
public class HearingCsvRawData {
    private String id;
    private LocalDate hearingDate;
    private LocalDate weekCommencingStartDate;
    private LocalDate weekCommencingEndDate;
    private String courtroom;
    private String startTime;
    private Integer durationMinutes;
    private LocalDate startDate;
    private LocalDate endDate;
    private String day;
    private JsonNode properties;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getHearingDate() { return hearingDate; }
    public void setHearingDate(LocalDate hearingDate) { this.hearingDate = hearingDate; }

    public LocalDate getWeekCommencingStartDate() { return weekCommencingStartDate; }
    public void setWeekCommencingStartDate(LocalDate weekCommencingStartDate) { this.weekCommencingStartDate = weekCommencingStartDate; }

    public LocalDate getWeekCommencingEndDate() { return weekCommencingEndDate; }
    public void setWeekCommencingEndDate(LocalDate weekCommencingEndDate) { this.weekCommencingEndDate = weekCommencingEndDate; }

    public String getCourtroom() { return courtroom; }
    public void setCourtroom(String courtroom) { this.courtroom = courtroom; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public JsonNode getProperties() { return properties; }
    public void setProperties(JsonNode properties) { this.properties = properties; }

    @Override
    public String toString() {
        return "HearingCsvRawData{" +
                "id='" + id + '\'' +
                ", hearingDate=" + hearingDate +
                ", weekCommencingStartDate=" + weekCommencingStartDate +
                ", weekCommencingEndDate=" + weekCommencingEndDate +
                ", courtroom='" + courtroom + '\'' +
                ", startTime='" + startTime + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", day='" + day + '\'' +
                ", properties=" + (properties != null ? "present" : "null") +
                '}';
    }
}
