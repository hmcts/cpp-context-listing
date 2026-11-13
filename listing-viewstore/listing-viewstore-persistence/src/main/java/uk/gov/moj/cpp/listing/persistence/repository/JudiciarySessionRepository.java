package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.query.JudiciarySession;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.query.NativeQuery;
import org.hibernate.type.StringType;

/**
 * Judiciary sitting in a court room on a date with their court session, from allocated, non-vacated hearings (CHD-3323).
 */
@ApplicationScoped
public class JudiciarySessionRepository {

    private static final String JUDICIAL_ID = "judicial_id";
    private static final String SESSION = "session";

    /*
     * The session of a hearing day comes from its UK local start and end times: PM when it starts at or after 13:00,
     * AM when it ends before 13:00, otherwise AD (all day, sitting in both AM and PM).
     */
    private static final String JUDICIARY_SESSIONS_QUERY = """
            SELECT DISTINCT judiciary->>'judicialId' AS judicial_id,
              CASE
                WHEN CAST(hd.start_time AT TIME ZONE 'Europe/London' AS time) >= TIME '13:00' THEN 'PM'
                WHEN CAST(hd.end_time AT TIME ZONE 'Europe/London' AS time) < TIME '13:00' THEN 'AM'
                ELSE 'AD'
              END AS session
            FROM hearing h
            JOIN hearing_days hd ON hd.hearing_id = h.id
            CROSS JOIN LATERAL jsonb_array_elements(
              CASE
                WHEN jsonb_typeof(h.properties->'judiciary') = 'array' THEN h.properties->'judiciary'
                ELSE CAST('[]' AS jsonb)
              END
            ) AS judiciary
            WHERE hd.court_centre_id = :courtCentreId
              AND hd.court_room_id = :courtRoomId
              AND hd.hearing_date = :searchDate
              AND h.allocated = true
              AND (h.is_vacated_trial IS NULL OR h.is_vacated_trial != true)
              AND judiciary->>'judicialId' IS NOT NULL
            ORDER BY judicial_id
            """;

    @Inject
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<JudiciarySession> findByCourtCentreAndRoomAndSearchDate(final UUID courtCentreId,
                                                                        final UUID courtRoomId,
                                                                        final LocalDate searchDate) {
        final List<Object[]> rows = entityManager.createNativeQuery(JUDICIARY_SESSIONS_QUERY)
                .setParameter("courtCentreId", courtCentreId)
                .setParameter("courtRoomId", courtRoomId)
                .setParameter("searchDate", searchDate)
                .unwrap(NativeQuery.class)
                .addScalar(JUDICIAL_ID, StringType.INSTANCE)
                .addScalar(SESSION, StringType.INSTANCE)
                .getResultList();

        return rows.stream()
                .map(row -> new JudiciarySession((String) row[0], (String) row[1]))
                .toList();
    }
}
