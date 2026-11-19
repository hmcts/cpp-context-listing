package uk.gov.moj.cpp.listing.persistence.repository.csv;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.entity.csv.HearingCsvRawData;
import uk.gov.moj.cpp.listing.persistence.repository.AbstractRepositoryTest;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DB-backed replacement for the legacy mock-JDBC HearingCsvRawDataRepositoryTest: the same
 * mapping assertions, plus the query's filters (CROWN only, date window, "X of Y" day column,
 * court room name from cache_refdata_courtroom).
 */
class HearingCsvRawDataRepositoryTest extends AbstractRepositoryTest {

    private static final LocalDate START = LocalDate.of(2024, 1, 15);

    @Autowired
    private HearingCsvRawDataRepository repository;

    @Autowired
    private EntityManager entityManager;

    private final UUID courtCentreId = randomUUID();
    private final UUID courtRoomId = randomUUID();

    @BeforeEach
    void seed() {
        entityManager.persist(new CacheRefDataCourtroom(courtRoomId, "Courtroom 1"));
    }

    private Hearing multiDayHearing(final String jurisdiction, final LocalDate start, final int days) {
        final UUID id = randomUUID();
        final ObjectNode properties = new ObjectMapper().createObjectNode();
        properties.put("id", id.toString());
        properties.put("judiciary", "Judge Smith");
        final Hearing hearing = Hearing.builder()
                .withId(id)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withJurisdictionType(jurisdiction)
                .withStartDate(start)
                .withEndDate(start.plusDays(days - 1L))
                .withAllocated(true)
                .withProperties(properties)
                .build();
        final Set<HearingDays> hearingDays = new HashSet<>();
        for (int i = 0; i < days; i++) {
            final LocalDate date = start.plusDays(i);
            hearingDays.add(new HearingDays(randomUUID(), i,
                    ZonedDateTime.of(date.atTime(16, 0), ZoneOffset.UTC), ZonedDateTime.of(date.atTime(9, 0), ZoneOffset.UTC),
                    courtRoomId, date, courtCentreId, 120, hearing));
        }
        hearing.setHearingDays(hearingDays);
        entityManager.persist(hearing);
        return hearing;
    }

    @Test
    void findsTheDaysOfAMultiDayCrownHearingInTheWindow() {
        final Hearing crown = multiDayHearing("CROWN", START, 3);
        multiDayHearing("MAGISTRATES", START, 3);
        entityManager.flush();

        final List<HearingCsvRawData> result = repository.findHearingsRawDataForCsvReport(courtCentreId.toString(), START, 2, 100L);

        assertThat(result).hasSize(3).allMatch(row -> row.getId().equals(crown.getId().toString()));
        assertThat(result).extracting(HearingCsvRawData::getDay).containsExactlyInAnyOrder("1 of 3", "2 of 3", "3 of 3");
        final HearingCsvRawData first = result.stream().filter(r -> "1 of 3".equals(r.getDay())).findFirst().orElseThrow();
        assertThat(first.getHearingDate()).isEqualTo(START);
        assertThat(first.getCourtroom()).isEqualTo("Courtroom 1");
        assertThat(first.getStartTime()).isEqualTo("2024-01-15T09:00:00Z");
        assertThat(first.getDurationMinutes()).isEqualTo(120);
        assertThat(first.getStartDate()).isEqualTo(START);
        assertThat(first.getEndDate()).isEqualTo(START.plusDays(2));
        assertThat(first.getWeekCommencingStartDate()).isNull();
        assertThat(first.getProperties().get("judiciary").asText()).isEqualTo("Judge Smith");
    }

    @Test
    void leavesOutDaysOutsideTheWindowAndOtherCourtCentres() {
        multiDayHearing("CROWN", START.plusWeeks(3), 2);
        entityManager.flush();

        assertThat(repository.findHearingsRawDataForCsvReport(courtCentreId.toString(), START, 2, 100L)).isEmpty();
        assertThat(repository.findHearingsRawDataForCsvReport(randomUUID().toString(), START.plusWeeks(3), 2, 100L)).isEmpty();
    }

    @Test
    void appliesThePageSizeLimit() {
        multiDayHearing("CROWN", START, 3);
        entityManager.flush();

        assertThat(repository.findHearingsRawDataForCsvReport(courtCentreId.toString(), START, 2, 2L)).hasSize(2);
    }
}
