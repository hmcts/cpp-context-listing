package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SequenceHearingData {

    private final UUID hearingId;
    private final Map<LocalDate, Integer> sequencedDays;
    private final UpdatedHearingData updatedHearingData;


    public SequenceHearingData(UpdatedHearingData updatedHearingData) {
        this.hearingId = updatedHearingData.getHearingId();
        this.sequencedDays = createSequenceMap(updatedHearingData.getEndDate());
        this.updatedHearingData = updatedHearingData;

    }

    public UUID getHearingId() {
        return hearingId;
    }


    public UpdatedHearingData getUpdatedHearingData() {
        return updatedHearingData;
    }

    public Map<LocalDate, Integer> getSequencedDays() {
        return sequencedDays;
    }


    private Map<LocalDate, Integer> createSequenceMap(String endDate) {
        return getLocalDateRange(endDate).stream().collect(Collectors.toMap(ld -> ld, ld -> new Random().nextInt(100)));
    }

    private List<LocalDate> getLocalDateRange(final String endDate) {

        final LocalDate start = LocalDate.parse(LocalDate.now().toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        final LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        final int days = (int) start.until(end, ChronoUnit.DAYS);

        return Stream.iterate(start, d -> d.plusDays(1))
                .limit(days + 1)
                .collect(Collectors.toList());
    }


}


