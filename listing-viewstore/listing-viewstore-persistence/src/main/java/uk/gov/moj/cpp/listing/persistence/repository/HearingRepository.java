package uk.gov.moj.cpp.listing.persistence.repository;

import static uk.gov.moj.cpp.listing.persistence.repository.courtlist.HearingJdbcRepository.NULL_FLAT_HEARING_FIELDS;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.HearingJdbcRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Repository for {@link Hearing}
 * <p>
 * The two search queries differ in how dates and times are searched for.  For the search where a
 * <code>searchDate</code> is provided you you must also provide a <code>startTime</code> and
 * <code>endTime</code> time range.  Whereas the other search that accepts a <code>startDate</code>
 * and <code>endDate</code> date range does not accept a time range.  The date range search does not
 * accept a time range for the following reasons.
 * <ul>
 * <li>From a usability perspective its unclear what a time range would be where you have a date
 * range spanning more
 * than one day</li>
 * <li>Including a time range in this query would make the query inefficient because for each
 * hearing that matches the date
 * range the query would then have to iterate th hearingDay collection to find matching times.  For
 * wide date ranges this could result in a large number of hearing day collections being
 * searched.</li>
 * <li>The search that accepts a searchDate would return a much smaller number of hearings and
 * therefore the cost of
 * searching the hearing days collection would be much less</li>
 * </ul>
 * </p>
 */
// TODO(java25-upgrade): TEMPORARY STUB — DeltaSpike @Query methods not yet migrated to JPA. Do not release.
@SuppressWarnings({"squid:S00107", "squid:S1214"})
@ApplicationScoped
public class HearingRepository {

    private static final String PARAM_ALLOCATED = "allocated";
    private static final String PARAM_COURT_CENTRE_ID = "courtCentreId";

    private static final String UNALLOCATED_COMMON_SELECT_FROM = "select h.id, h.properties,  " +
            "h.court_centre_id, " +
            "h.court_room_id, " +
            "h.type_id, " +
            "h.start_date, " +
            "h.end_date, " +
            "h.is_vacated_trial, " +
            "h.jurisdiction_type, " +
            "h.unscheduled, " +
            "h.week_commencing_start_date, " +
            "h.week_commencing_end_date, " +
            "h.allocated, " +
            "h.type_of_list_id, " +
            "h.estimated_minutes, " +
            "count(1) OVER() as totalCount, " +
            "h.is_possible_disqualification ,  " + NULL_FLAT_HEARING_FIELDS +
            "from hearing h " +
            "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
            " LEFT JOIN court_applications ca ON ca.hearing_id = h.id ";

    private static final String WEEK_COMMENCING_CORE_QUERY_FOR_ALLOCATED = "(h.unscheduled is null or h.unscheduled = false)" +
            "and (cast(?3 as varchar) is null or (lc.authority_id = cast(cast(?3 as varchar) as uuid) or lc.prosecutor_id = cast(cast(?3 as varchar) as uuid)))  " +
            "and (cast(?4 as varchar) is null or h.type_id = cast(cast(?4 as varchar) as uuid))  " +
            "and (cast(?5 as varchar) is null or h.jurisdiction_type = cast(?5 as text))  " +
            "and ( " +
            "   ( h.week_commencing_start_date >= ?6 and h.week_commencing_start_date <= ?7 ) or " +
            "   ( h.week_commencing_end_date >= ?6 and h.week_commencing_end_date <= ?7 ) or " +
            "   ( ?6 between h.week_commencing_start_date and h.week_commencing_end_date ) or " +
            "   ( ?7 between h.week_commencing_start_date and h.week_commencing_end_date ) or " +
            "   ( ?6 between h.start_date and h.end_date ) or " +
            "   ( ?7 between h.start_date and h.end_date ) or " +
            "   ( h.start_date >= ?6 and h.start_date <= ?7 )  or " +
            "   ( h.end_date >= ?6 and h.end_date <= ?7 ) ) " +
            "group by h.id, h.properties " +
            "order by h.start_date," +
            "h.end_date," +
            "h.week_commencing_start_date," +
            "h.week_commencing_end_date";

    @PersistenceContext(unitName = "listing-persistence-unit")
    EntityManager entityManager;

    @Inject
    private HearingJdbcRepository hearingJdbcRepository;

    public Hearing findBy(final UUID id) {
        return findByHearingId(id.toString());
    }

    public Hearing save(final Hearing hearing) {
        // Hearing maps read-only virtual columns (hearing_date, totalCount, hearing_day_count,
        // hearing_day_position) that exist ONLY as native-query aliases, not as real table columns.
        // merge() would issue an existence-SELECT including those columns, which fails against the real
        // schema. Hearing is loaded only via the native queries (managed), so a non-contained instance
        // is treated as new and persisted. (Java 25 spike assumption — listing team to verify.)
        if (!entityManager.contains(hearing)) {
            entityManager.persist(hearing);
        }
        return hearing;
    }

    public void remove(final Hearing hearing) {
        final Hearing managed = entityManager.contains(hearing)
                ? hearing
                : findByHearingId(hearing.getId().toString());
        if (managed != null) {
            entityManager.remove(managed);
        }
    }

    public void flush() {
        entityManager.flush();
    }

    public List<Hearing> findAll() {
        return entityManager.createQuery("SELECT h FROM Hearing h", Hearing.class).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearings(final boolean allocated,
                                      final String courtCentreId,
                                      final String courtRoomId,
                                      final String authorityId,
                                      final String hearingTypeId,
                                      final String jurisdictionType,
                                      final LocalDate searchDate,
                                      ZonedDateTime startTime,
                                      final ZonedDateTime endTime) {
        return entityManager.createNativeQuery("select distinct h.id, h.properties,  " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.properties->>'allocated' as allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "1 as totalCount, " +
                        "h.is_possible_disqualification , " + NULL_FLAT_HEARING_FIELDS +
                        "from hearing h INNER JOIN hearing_days hd on hd.hearing_id = h.id  " +
                        "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
                        "where  " +
                        "cast(h.properties->>'allocated' as boolean) = :allocated  " +
                        "and (h.unscheduled is null or h.unscheduled = false) " +
                        "and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                        "and (cast(:courtCentreId as varchar) is null or coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(:courtCentreId as varchar) as uuid))  " +
                        "and (cast(:courtRoomId as varchar) is null or coalesce(hd.court_room_id, h.court_room_id) = cast(cast(:courtRoomId as varchar) as uuid))  " +
                        "and (cast(:authorityId as varchar) is null or (lc.authority_id = cast(cast(:authorityId as varchar) as uuid) or lc.prosecutor_id = cast(cast(:authorityId as varchar) as uuid)))  " +
                        "and (cast(:typeId as varchar) is null or h.type_id = cast(cast(:typeId as varchar) as uuid))  " +
                        "and (cast(:jurisdictionType as varchar) is null or h.jurisdiction_type = cast(:jurisdictionType as text))  " +
                        "and (:searchDate between h.start_date and h.end_date )  " +
                        "and (hd.start_time between :startTime and :endTime) "
                        , Hearing.class)
                .setParameter(PARAM_ALLOCATED, allocated)
                .setParameter(PARAM_COURT_CENTRE_ID, courtCentreId)
                .setParameter("courtRoomId", courtRoomId)
                .setParameter("authorityId", authorityId)
                .setParameter("typeId", hearingTypeId)
                .setParameter("jurisdictionType", jurisdictionType)
                .setParameter("searchDate", searchDate)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    public List<Hearing> findHearings(final boolean allocated,
                                      final UUID courtCentreId,
                                      final UUID courtRoomId,
                                      final UUID authorityCode,
                                      final UUID hearingTypeId,
                                      final String jurisdictionType,
                                      final LocalDate startDate,
                                      final LocalDate endDate,
                                      final Integer offSet,
                                      final Integer pageSize) {
        return hearingJdbcRepository.findHearings(allocated, courtCentreId, courtRoomId,
                authorityCode, hearingTypeId, jurisdictionType, startDate, endDate, offSet, pageSize);
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearings(final String allocated,
                                      final String courtCentreId,
                                      final String courtRoomId,
                                      final String authorityCode,
                                      final String hearingTypeId,
                                      final String jurisdictionType,
                                      final LocalDate startDate,
                                      final LocalDate endDate,
                                      final boolean possibleDisqualification,
                                      final Integer offSet,
                                      final Integer pageSize) {
        return entityManager.createNativeQuery("select distinct h.id, h.properties,  " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "count(*) OVER() as totalCount, " +
                        "h.is_possible_disqualification , " + NULL_FLAT_HEARING_FIELDS +
                        "from hearing h " +
                        "LEFT JOIN hearing_days hd ON hd.hearing_id = h.id  " +
                        "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
                        "LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
                        "where  " +
                        "cast(h.allocated as varchar) = cast(?1 as varchar)  " +
                        "and (h.unscheduled is null or h.unscheduled = false) " +
                        "and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                        "and (cast(?2 as varchar) is null or coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(?2 as varchar) as uuid))  " +
                        "and (cast(?3 as varchar) is null or coalesce(hd.court_room_id, h.court_room_id) = cast(cast(?3 as varchar) as uuid))  " +
                        "and (cast(?4 as varchar) is null or (lc.authority_id = cast(cast(?4 as varchar) as uuid) or lc.prosecutor_id = cast(cast(?4 as varchar) as uuid)))  " +
                        "and (cast(?5 as varchar) is null or h.type_id = cast(cast(?5 as varchar) as uuid))  " +
                        "and (cast(?6 as varchar) is null or h.jurisdiction_type = cast(?6 as text))  " +
                        "and (lc.is_ejected is null or lc.is_ejected =false) " +
                        "and (ca.is_ejected is null or ca.is_ejected =false) " +
                        "and (lc.id is not null or ca.id is not null) " +
                        "and ( " +
                        "(h.start_date between ?7 and ?8 ) or " +
                        "(h.end_date between ?7 and ?8 ) or " +
                        "((h.start_date <= ?7 ) and (h.end_date >= ?8 ) )  " +
                        ") " +
                        "and h.is_possible_disqualification = ?9 " +
                        " group by  h.id " +
                        " order by h.id, h.court_centre_id ASC OFFSET (?10) ROWS FETCH NEXT (?11) ROWS ONLY", Hearing.class)
                .setParameter(1, allocated)
                .setParameter(2, courtCentreId)
                .setParameter(3, courtRoomId)
                .setParameter(4, authorityCode)
                .setParameter(5, hearingTypeId)
                .setParameter(6, jurisdictionType)
                .setParameter(7, startDate)
                .setParameter(8, endDate)
                .setParameter(9, possibleDisqualification)
                .setParameter(10, offSet)
                .setParameter(11, pageSize)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearingsForCotr(final Set<String> hearingTypeIds, final String courtCentreId, final LocalDate startDate, final LocalDate endDate) {
        return entityManager.createNativeQuery("select distinct h.id, h.properties,  " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "count(*) OVER() as totalCount, " +
                        "h.is_possible_disqualification , " + NULL_FLAT_HEARING_FIELDS +
                        "from hearing h " +
                        "LEFT JOIN hearing_days hd ON hd.hearing_id = h.id  " +
                        "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
                        "LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
                        "where  " +
                        "(h.unscheduled is null or h.unscheduled = false) " +
                        "and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                        "and (cast(h.type_id as varchar) in (?1))  " +
                        "and (cast(?2 as varchar) is null or coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(?2 as varchar) as uuid))  " +
                        "and (lc.is_ejected is null or lc.is_ejected = false) " +
                        "and (ca.is_ejected is null or ca.is_ejected = false) " +
                        "and (lc.id is not null or ca.id is not null) " +
                        "and ( " +
                        "(h.start_date between ?3 and ?4) or " +
                        "(h.end_date between ?3 and ?4) or " +
                        "((h.start_date <= ?3) and (h.end_date >= ?4)) " +
                        ") " +
                        " group by  h.id " +
                        "order by h.id, h.court_centre_id ASC", Hearing.class)
                .setParameter(1, hearingTypeIds)
                .setParameter(2, courtCentreId)
                .setParameter(3, startDate)
                .setParameter(4, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearings(final String allocated,
                                      final String courtCentreId,
                                      final String courtRoomId,
                                      final String authorityCode,
                                      final String hearingTypeId,
                                      final String jurisdictionType,
                                      final LocalDate startDate,
                                      final LocalDate endDate) {
        return entityManager.createNativeQuery("select distinct h.id, h.properties,  " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.properties ->>'allocated' as allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "1 as totalCount, " +
                        "h.is_possible_disqualification , " + NULL_FLAT_HEARING_FIELDS +
                        "from hearing h " +
                        "LEFT JOIN hearing_days hd ON hd.hearing_id = h.id " +
                        "AND (hd.court_centre_id IS NULL OR hd.court_centre_id = cast(cast(?2 as varchar) as uuid)) " +
                        "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
                        "where  " +
                        "cast(h.allocated as varchar) = cast(?1 as varchar)  " +
                        "and (h.unscheduled is null or h.unscheduled = false) " +
                        "and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                        "and (cast(?2 as varchar) is null or h.court_centre_id = cast(cast(?2 as varchar) as uuid))  " +
                        "and (cast(?3 as varchar) is null or coalesce(hd.court_room_id, h.court_room_id) = cast(cast(?3 as varchar) as uuid))  " +
                        "and (cast(?4 as varchar) is null or (lc.authority_id = cast(cast(?4 as varchar) as uuid) or lc.prosecutor_id = cast(cast(?4 as varchar) as uuid)))  " +
                        "and (cast(?5 as varchar) is null or h.type_id = cast(cast(?5 as varchar) as uuid))  " +
                        "and (cast(?6 as varchar) is null or h.jurisdiction_type = cast(?6 as text))  " +
                        "and ( " +
                        "(h.start_date between ?7 and ?8 ) or " +
                        "(h.end_date between ?7 and ?8 ) or " +
                        "((h.start_date <= ?7 ) and (h.end_date >= ?8 ) )  " +
                        ")", Hearing.class)
                .setParameter(1, allocated)
                .setParameter(2, courtCentreId)
                .setParameter(3, courtRoomId)
                .setParameter(4, authorityCode)
                .setParameter(5, hearingTypeId)
                .setParameter(6, jurisdictionType)
                .setParameter(7, startDate)
                .setParameter(8, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearingsByWeekCommencingRange(
            final String courtCentreId,
            final String courtRoomId,
            final String authorityCode,
            final String hearingTypeId,
            final String jurisdictionType,
            final LocalDate weekCommencingStartDate,
            final LocalDate weekCommencingEndDate, final Integer offSet, final Integer pageSize) {
        return entityManager.createNativeQuery("select h.id, h.properties,  " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "count(*) OVER() as totalCount, " +
                        "h.is_possible_disqualification , " + NULL_FLAT_HEARING_FIELDS +
                        "from hearing h " +
                        "LEFT JOIN hearing_days hd ON hd.hearing_id = h.id " +
                        "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
                        "LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
                        "where  " +
                        "(h.is_vacated_trial is null or h.is_vacated_trial != true) and " +
                        "(cast(?1 as varchar) is null or h.court_centre_id = cast(cast(?1 as varchar) as uuid))  " +
                        "and (cast(?2 as varchar) is null or coalesce(hd.court_room_id, h.court_room_id) = cast(cast(?2 as varchar) as uuid))  " +
                        "  and (lc.is_ejected is null or lc.is_ejected =false) " +
                        "  and (ca.is_ejected is null or ca.is_ejected =false) " +
                        "  and (lc.id is not null or ca.id is not null)" +
                        "and " +
                        WEEK_COMMENCING_CORE_QUERY_FOR_ALLOCATED +
                        " ASC OFFSET (?8) ROWS FETCH NEXT (?9) ROWS ONLY"
                        , Hearing.class)
                .setParameter(1, courtCentreId)
                .setParameter(2, courtRoomId)
                .setParameter(3, authorityCode)
                .setParameter(4, hearingTypeId)
                .setParameter(5, jurisdictionType)
                .setParameter(6, weekCommencingStartDate)
                .setParameter(7, weekCommencingEndDate)
                .setParameter(8, offSet)
                .setParameter(9, pageSize)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearingsByWeekCommencingRangeWithNoPagination(
            final String courtCentreId,
            final String courtRoomId,
            final String authorityCode,
            final String hearingTypeId,
            final String jurisdictionType,
            final LocalDate weekCommencingStartDate,
            final LocalDate weekCommencingEndDate) {
        return entityManager.createNativeQuery("select h.id, h.properties,  " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "count(*) OVER() as totalCount, " +
                        "h.is_possible_disqualification , " + NULL_FLAT_HEARING_FIELDS +
                        "from hearing h " +
                        "LEFT JOIN hearing_days hd ON hd.hearing_id = h.id  " +
                        "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
                        "LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
                        "where  " +
                        "(h.is_vacated_trial is null or h.is_vacated_trial != true) and " +
                        "(cast(?1 as varchar) is null or h.court_centre_id = cast(cast(?1 as varchar) as uuid))  " +
                        "and (cast(?2 as varchar) is null or coalesce(hd.court_room_id, h.court_room_id) = cast(cast(?2 as varchar) as uuid))  " +
                        "  and (lc.is_ejected is null or lc.is_ejected =false) " +
                        "  and (ca.is_ejected is null or ca.is_ejected =false) " +
                        "  and (lc.id is not null or ca.id is not null)" +
                        "and " +
                        WEEK_COMMENCING_CORE_QUERY_FOR_ALLOCATED +
                        " ASC "
                        , Hearing.class)
                .setParameter(1, courtCentreId)
                .setParameter(2, courtRoomId)
                .setParameter(3, authorityCode)
                .setParameter(4, hearingTypeId)
                .setParameter(5, jurisdictionType)
                .setParameter(6, weekCommencingStartDate)
                .setParameter(7, weekCommencingEndDate)
                .getResultList();
    }

    public List<Hearing> findUnallocatedHearingsByWeekCommencingRange(
            final UUID courtCentreId,
            final UUID courtRoomId,
            final UUID authorityCode,
            final UUID hearingTypeId,
            final String jurisdictionType,
            final LocalDate weekCommencingStartDate,
            final LocalDate weekCommencingEndDate,
            final boolean allocated,
            final Integer offSet,
            final Integer pageSize) {
        return hearingJdbcRepository.findUnallocatedHearingsByWeekCommencingRange(courtCentreId, courtRoomId, authorityCode, hearingTypeId, jurisdictionType, weekCommencingStartDate, weekCommencingEndDate, allocated, offSet, pageSize);
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findUnallocatedHearingsByWeekCommencingRangeAndPossibleDisqualification(
            final String courtCentreId,
            final String courtRoomId,
            final String authorityCode,
            final String hearingTypeId,
            final String jurisdictionType,
            final LocalDate weekCommencingStartDate,
            final LocalDate weekCommencingEndDate,
            final boolean allocated,
            final boolean possibleDisqualification,
            final Integer offSet, final Integer pageSize) {
        return entityManager.createNativeQuery(UNALLOCATED_COMMON_SELECT_FROM +
                        "where  " +
                        "(h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                        "and h.allocated = ?8  " +
                        "and h.is_possible_disqualification = ?9  " +
                        "and (cast(?1 as varchar) is null or h.court_centre_id = cast(cast(?1 as varchar) as uuid))  " +
                        "and (cast(?2 as varchar) is null or h.court_room_id = cast(cast(?2 as varchar) as uuid))  " +
                        "  and (lc.is_ejected is null or lc.is_ejected =false) " +
                        "  and (ca.is_ejected is null or ca.is_ejected =false) " +
                        "  and (lc.id is not null or ca.id is not null)" +
                        "and " +
                        WEEK_COMMENCING_CORE_QUERY_FOR_ALLOCATED +
                        " ASC OFFSET (?10) ROWS FETCH NEXT (?11) ROWS ONLY"
                        , Hearing.class)
                .setParameter(1, courtCentreId)
                .setParameter(2, courtRoomId)
                .setParameter(3, authorityCode)
                .setParameter(4, hearingTypeId)
                .setParameter(5, jurisdictionType)
                .setParameter(6, weekCommencingStartDate)
                .setParameter(7, weekCommencingEndDate)
                .setParameter(8, allocated)
                .setParameter(9, possibleDisqualification)
                .setParameter(10, offSet)
                .setParameter(11, pageSize)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public Hearing findHearingsForPublicStandardList(final boolean allocated,
                                                     final String courtCentreId,
                                                     final LocalDate startDate,
                                                     final LocalDate endDate,
                                                     final Set<String> excludedHearingTypeIds) {
        final List<Hearing> results = entityManager.createNativeQuery("with filtered_hearings as (select distinct h.id, hd.hearing_date as hearingDate, h.properties as properties from hearing h " +
                        "inner join hearing_days hd on hd.hearing_id = h.id where coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(:courtCentreId as varchar) as uuid) " +
                        "and cast(h.properties->>'allocated' as boolean) = :allocated and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                        "and (h.type_id IS NULL or (" +
                               "cast(h.type_id as varchar) not in (:excludedHearingTypeIds))) " +
                        "and hd.hearing_date between :startDate and :endDate) " +
                        "select cast('d9ea61d4-2441-42bd-9089-510b1c069fb5' as uuid) as id, " +
                        "cast(cast(:courtCentreId as varchar) as uuid) as court_centre_id, " +
                        "null as court_room_id, " +
                        "null as type_id, " +
                        "null as start_date, " +
                        "null as end_date, " +
                        "null as is_vacated_trial, " +
                        "null as jurisdiction_type, " +
                        "null as unscheduled, " +
                        "null as week_commencing_start_date, " +
                        "null as week_commencing_end_date, " +
                        "null as allocated, " +
                        "null as type_of_list_id, " +
                        "null as totalCount, " +
                        "null as estimated_minutes, " +
                        "null as is_possible_disqualification, " + NULL_FLAT_HEARING_FIELDS + ", " +
                        "(select row_to_json(combinedJudiciaryAndHearings) as properties from " +
                        "   (select * from " +
                        "       (select json_agg(uniqueJudiciary) as judiciary from " +
                        "               (select distinct jsonb_array_elements(h.properties -> 'judiciary') ->> 'judicialId' as \"judicialId\" from filtered_hearings h) uniqueJudiciary) a, " +
                        "   (select json_agg(hrngByCourtCentreId) as hearings from " +
                        "       (select :courtCentreId as \"courtCentreId\", " +
                        "               (select json_agg(hbsd) as \"hearingsByCourtCentreId\" from " +
                        "                   (select h.hearingDate as \"hearingDate\", " +
                        "                           (select jsonb_agg(hearings) as \"hearingsByHearingDate\" from  " +
                        "                               (select distinct properties as hearing from filtered_hearings fh where fh.hearingDate = h.hearingDate) hearings) " +
                        "                   from (select distinct hearingDate from filtered_hearings) h) " +
                        "               hbsd)) " +
                        "   hrngByCourtCentreId) b) " +
                        "combinedJudiciaryAndHearings)", Hearing.class)
                .setParameter(PARAM_ALLOCATED, allocated)
                .setParameter(PARAM_COURT_CENTRE_ID, courtCentreId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("excludedHearingTypeIds", excludedHearingTypeIds)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearingsForAlphabeticalList(final boolean allocated,
                                                         final String courtCentreId,
                                                         final LocalDate hearingDate,
                                                         final Set<String> excludedHearingTypeIds) {
        return entityManager.createNativeQuery("with filtered_hearings as (select distinct h.id, hd.hearing_date as hearingDate, h.properties as properties from hearing h " +
                        "inner join hearing_days hd on hd.hearing_id = h.id " +
                        "where ((hd.court_centre_id = cast(cast(:courtCentreId as varchar) as uuid)) " +
                        "or (hd.court_centre_id is null " +
                        "and h.court_centre_id = cast(cast(:courtCentreId as varchar) as uuid))) " +
                        "and cast(h.properties->>'allocated' as boolean) = :allocated " +
                        "and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                        "and (h.type_id IS NULL or (cast(h.type_id as varchar) not in (:excludedHearingTypeIds)))  " +
                        "and hd.hearing_date = :hearingDate) " +
                        "select cast('d9ea61d4-2441-42bd-9089-510b1c069fb5' as uuid) as id, " +
                        "cast(cast(:courtCentreId as varchar) as uuid) as court_centre_id, " +
                        "null as court_room_id, " +
                        "null as type_id, " +
                        "null as start_date, " +
                        "null as end_date, " +
                        "null as is_vacated_trial, " +
                        "null as jurisdiction_type, " +
                        "null as unscheduled, " +
                        "null as week_commencing_start_date, " +
                        "null as week_commencing_end_date, " +
                        "null as allocated, " +
                        "null as type_of_list_id, " +
                        "null as totalCount, " +
                        "null as estimated_minutes, " +
                        "null as is_possible_disqualification, " + NULL_FLAT_HEARING_FIELDS + ", " +
                        "(select jsonb_agg(hrngByCourtCentreId) as \"properties\" from " +
                        "(select h.hearingDate as \"hearingDate\", " +
                        "(select jsonb_agg(hearings) as \"hearingsByHearingDate\" from " +
                        "(select distinct properties as hearing from filtered_hearings fh " +
                        "where fh.hearingDate = h.hearingDate) hearings) from " +
                        "(select distinct filtered_hearings.hearingDate from filtered_hearings) h) " +
                        "hrngByCourtCentreId)", Hearing.class)
                .setParameter(PARAM_ALLOCATED, allocated)
                .setParameter(PARAM_COURT_CENTRE_ID, courtCentreId)
                .setParameter("hearingDate", hearingDate)
                .setParameter("excludedHearingTypeIds", excludedHearingTypeIds)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearings(String caseUrn, String typeOfList, Integer offSet, Integer pageSize) {
        return entityManager.createNativeQuery("select distinct h.id, properties, " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "count(*) OVER() as totalCount, " +
                        "h.is_possible_disqualification, " + NULL_FLAT_HEARING_FIELDS +
                        " from hearing h " +
                        " LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
                        " LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
                        " where (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                        "  and  h.allocated = false " +
                        "  and  h.unscheduled = true " +
                        "  and ( (lc.hearing_id is null and cast(?1 as varchar) is null ) or (cast(?1 as varchar) is null or UPPER(lc.case_reference) = cast(?1 as varchar)) " +
                        "           or (cast(?1 as varchar) is null or UPPER(ca.application_reference) = cast(?1 as varchar)) " +
                        "      ) " +
                        "  and (cast(?2 as varchar) is null" +
                        "    or h.type_of_list_id = cast(cast(?2 as varchar) as uuid))" +
                        "  and (lc.id is not null or ca.id is not null) " +
                        "  and (lc.is_ejected is null or lc.is_ejected =false) " +
                        "  and (ca.is_ejected is null or ca.is_ejected =false) " +
                        " group by  h.id " +
                        " order by h.id, h.court_centre_id ASC OFFSET (?3) ROWS FETCH NEXT (?4) ROWS ONLY"
                        , Hearing.class)
                .setParameter(1, caseUrn)
                .setParameter(2, typeOfList)
                .setParameter(3, offSet)
                .setParameter(4, pageSize)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearings(final boolean allocated,
                                      final Set<String> jurisdictionTypes,
                                      final String hearingId,
                                      final Set<String> caseUrnSet,
                                      final Set<String> masterDefendantIdSet,
                                      final Set<String> linkedCaseUrn,
                                      final String caseUrnForLinkedCases,
                                      final LocalDate currentDate) {
        return entityManager.createNativeQuery("select h.id, h.properties, " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "1 as totalCount, " +
                        "h.is_possible_disqualification, " + NULL_FLAT_HEARING_FIELDS +
                        "from hearing h " +
                        "where " +
                        "h.allocated = :allocated " +
                        "and (h.unscheduled is null or h.unscheduled = false) " +
                        "and (h.jurisdiction_type in (:jurisdictionTypes)) " +
                        "and (h.end_date is null OR h.end_date >= :currentDate) " +
                        "and (cast(:hearingId as varchar) is null or h.id != cast(cast(:hearingId as varchar) as uuid)) " +
                        "AND " +
                        "(h.id in (select distinct h.id from hearing h  " +
                        " inner join listed_cases lc on lc.hearing_id = h.id where UPPER(lc.case_reference) in (:caseUrnSet)  " +
                        " and (cast(:hearingId as varchar) is null or h.id != cast(cast(:hearingId as varchar) as uuid))) " +
                        "OR " +
                        "(h.id in (SELECT distinct(hrng.id) FROM hearing hrng inner join listed_cases lc3 on lc3.hearing_id = hrng.id " +
                        "   WHERE lc3.case_reference IN (select lnkCase.case_urn as linkedCaseUrn from hearing h2  " +
                        "   inner join listed_cases listCase on listCase.hearing_id = h2.id  " +
                        "   inner join linked_case lnkCase on lnkCase.listed_case_id = listCase.id  " +
                        "   where listCase.case_reference " + "= cast(:caseUrnForLinkedCases as text)))) " +
                        "OR " +
                        " (h.id in (select distinct h5.id from " +
                        "hearing h5 inner join listed_cases lc6 on lc6.hearing_id = h5.id where " +
                        "lc6.case_id in (select distinct lc5.case_id from hearing h4 inner join listed_cases lc5 on lc5.hearing_id = h4.id  " +
                        "inner join defendant d on d.listed_case_id = lc5.id where cast(d.master_defendant_id as varchar) in (:masterDefendantIdSet)) " +
                        " and (cast(:hearingId as varchar) is null or h5.id != cast(cast(:hearingId as varchar) as uuid))) " +
                        "OR " +
                        "(h.id in (select distinct h.id from hearing h  " +
                        " inner join listed_cases lc on lc.hearing_id = h.id where UPPER(lc.case_reference) in (:linkedCaseUrn)  " +
                        " and (cast(:hearingId as varchar) is null or h.id != cast(cast(:hearingId as varchar) as uuid))) " +
                        ")))"
                        , Hearing.class)
                .setParameter(PARAM_ALLOCATED, allocated)
                .setParameter("jurisdictionTypes", jurisdictionTypes)
                .setParameter("hearingId", hearingId)
                .setParameter("caseUrnSet", caseUrnSet)
                .setParameter("masterDefendantIdSet", masterDefendantIdSet)
                .setParameter("linkedCaseUrn", linkedCaseUrn)
                .setParameter("caseUrnForLinkedCases", caseUrnForLinkedCases)
                .setParameter("currentDate", currentDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearings(final Set<String> jurisdictionTypes,
                                      final String hearingId,
                                      final Set<String> caseUrnSet,
                                      final Set<String> masterDefendantIdSet,
                                      final Set<String> linkedCaseUrn,
                                      final String caseUrnForLinkedCases,
                                      final LocalDate currentDate) {
        return entityManager.createNativeQuery("select h.id, h.properties, " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "1 as totalCount, " +
                        "h.is_possible_disqualification, " + NULL_FLAT_HEARING_FIELDS +
                        "from hearing h " +
                        "where (h.unscheduled is null or h.unscheduled = false) " +
                        "and (h.jurisdiction_type in (:jurisdictionTypes)) " +
                        "and (h.end_date is null OR h.end_date >= :currentDate) " +
                        "and (cast(:hearingId as varchar) is null or h.id != cast(cast(:hearingId as varchar) as uuid)) " +
                        "AND " +
                        "(h.id in (select distinct h.id from hearing h  " +
                        " inner join listed_cases lc on lc.hearing_id = h.id where UPPER(lc.case_reference) in (:caseUrnSet)  " +
                        " and (cast(:hearingId as varchar) is null or h.id != cast(cast(:hearingId as varchar) as uuid))) " +
                        "OR " +
                        "(h.id in (SELECT distinct(hrng.id) FROM hearing hrng inner join listed_cases lc3 on lc3.hearing_id = hrng.id " +
                        "   WHERE lc3.case_reference IN (select lnkCase.case_urn as linkedCaseUrn from hearing h2  " +
                        "   inner join listed_cases listCase on listCase.hearing_id = h2.id  " +
                        "   inner join linked_case lnkCase on lnkCase.listed_case_id = listCase.id  " +
                        "   where listCase.case_reference " + "= cast(:caseUrnForLinkedCases as text)))) " +
                        "OR " +
                        " (h.id in (select distinct h5.id from " +
                        "hearing h5 inner join listed_cases lc6 on lc6.hearing_id = h5.id where " +
                        "lc6.case_id in (select distinct lc5.case_id from hearing h4 inner join listed_cases lc5 on lc5.hearing_id = h4.id  " +
                        "inner join defendant d on d.listed_case_id = lc5.id where cast(d.master_defendant_id as varchar) in (:masterDefendantIdSet)) " +
                        " and (cast(:hearingId as varchar) is null or h5.id != cast(cast(:hearingId as varchar) as uuid))) " +
                        "OR " +
                        "(h.id in (select distinct h.id from hearing h  " +
                        " inner join listed_cases lc on lc.hearing_id = h.id where UPPER(lc.case_reference) in (:linkedCaseUrn)  " +
                        " and (cast(:hearingId as varchar) is null or h.id != cast(cast(:hearingId as varchar) as uuid))) " +
                        ")))"
                        , Hearing.class)
                .setParameter("jurisdictionTypes", jurisdictionTypes)
                .setParameter("hearingId", hearingId)
                .setParameter("caseUrnSet", caseUrnSet)
                .setParameter("masterDefendantIdSet", masterDefendantIdSet)
                .setParameter("linkedCaseUrn", linkedCaseUrn)
                .setParameter("caseUrnForLinkedCases", caseUrnForLinkedCases)
                .setParameter("currentDate", currentDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearingsByCaseUrnAndAnyAllocationState(String caseUrn, LocalDate startDate) {
        return entityManager.createNativeQuery("select h.id, " +
                        "h.properties, " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "1 as totalCount, " +
                        "h.is_possible_disqualification, " + NULL_FLAT_HEARING_FIELDS +
                        " from hearing h " +
                        " where h.unscheduled is not true " +
                        "  and (h.start_date >= :startdate or h.week_commencing_start_date >= :startdate) " +
                        "  and ( " +
                        "    exists (select 1 from listed_cases lc where lc.hearing_id = h.id and lc.case_reference " + "= :caseorapplicationreference) " +
                        "    or exists (select 1 from court_applications ca where ca.hearing_id = h.id and ca.application_reference = :caseorapplicationreference) " +
                        "  )"
                        , Hearing.class)
                .setParameter("caseorapplicationreference", caseUrn)
                .setParameter("startdate", startDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findHearings(String caseUrn, String typeOfList, Set<String> courtCentreIds, Integer offSet, Integer pageSize) {
        return entityManager.createNativeQuery("select distinct h.id, h.properties, " +
                        "h.court_centre_id, " +
                        "h.court_room_id, " +
                        "h.type_id, " +
                        "h.start_date, " +
                        "h.end_date, " +
                        "h.is_vacated_trial, " +
                        "h.jurisdiction_type, " +
                        "h.unscheduled, " +
                        "h.week_commencing_start_date, " +
                        "h.week_commencing_end_date, " +
                        "h.allocated, " +
                        "h.type_of_list_id, " +
                        "h.estimated_minutes, " +
                        "count(*) OVER() as totalCount, " +
                        "h.is_possible_disqualification, " + NULL_FLAT_HEARING_FIELDS +
                        " from hearing h" +
                        " LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
                        " LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
                        " where (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
                        "  and  h.allocated = false " +
                        "  and  h.unscheduled = true " +
                        "  and ( (lc.hearing_id is null and cast(?1 as varchar) is null ) or (cast(?1 as varchar) is null or UPPER(lc.case_reference) = cast(?1 as varchar)) " +
                        "           or (cast(?1 as varchar) is null or UPPER(ca.application_reference) = cast(?1 as varchar)) " +
                        "      ) " +
                        "  and (cast(?2 as varchar) is null" +
                        "    or h.type_of_list_id = cast(cast(?2 as varchar) as uuid))" +
                        "  and cast(h.court_centre_id as varchar) in (?3)" +
                        "  and (lc.id is not null or ca.id is not null) " +
                        "  and (lc.is_ejected is null or lc.is_ejected =false) " +
                        "  and (ca.is_ejected is null or ca.is_ejected =false) " +
                        " group by  h.id " +
                        " order by h.id, h.court_centre_id ASC OFFSET (?4) ROWS FETCH NEXT (?5) ROWS ONLY"
                        , Hearing.class)
                .setParameter(1, caseUrn)
                .setParameter(2, typeOfList)
                .setParameter(3, courtCentreIds)
                .setParameter(4, offSet)
                .setParameter(5, pageSize)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findAllocatedAndUnallocatedHearingsByCaseId(String caseId) {
        return entityManager.createNativeQuery("select id, properties," +
                        "null as court_centre_id, " +
                        "null as court_room_id, " +
                        "null as type_id, " +
                        "null as start_date, " +
                        "null as end_date, " +
                        "null as is_vacated_trial, " +
                        "null as jurisdiction_type, " +
                        "null as unscheduled, " +
                        "null as week_commencing_start_date, " +
                        "null as week_commencing_end_date, " +
                        "null as allocated, " +
                        "null as type_of_list_id, " +
                        "null as totalCount, " +
                        "null as estimated_minutes, " +
                        "null as is_possible_disqualification, " + NULL_FLAT_HEARING_FIELDS +
                        " from ( " +
                        " select distinct h.id as id, h.properties as properties, h.start_date as startDate, h.end_date as endDate " +
                        " from hearing h " +
                        " LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
                        " where ((h.allocated is null or h.allocated = false) or (h.allocated = true and (h.unscheduled is null or h.unscheduled = false))) " +
                        " and (cast(?1 as varchar) is null or lc.case_id = cast(cast(?1 as varchar) as uuid)) " +
                        " ) as all_hearing " +
                        " order by all_hearing.startDate desc, all_hearing.endDate desc "
                        , Hearing.class)
                .setParameter(1, caseId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findAllocatedAndUnallocatedHearingsByCaseId(String caseId, String applicationId) {
        return entityManager.createNativeQuery("select id, properties," +
                        "null as court_centre_id, " +
                        "null as court_room_id, " +
                        "null as type_id, " +
                        "null as start_date, " +
                        "null as end_date, " +
                        "null as is_vacated_trial, " +
                        "null as jurisdiction_type, " +
                        "null as unscheduled, " +
                        "null as week_commencing_start_date, " +
                        "null as week_commencing_end_date, " +
                        "null as allocated, " +
                        "null as type_of_list_id, " +
                        "null as totalCount, " +
                        "null as estimated_minutes, " +
                        "null as is_possible_disqualification, " + NULL_FLAT_HEARING_FIELDS +
                        " from ( " +
                        " select distinct h.id as id, h.properties as properties, h.start_date as startDate, h.end_date as endDate " +
                        " from hearing h " +
                        " LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
                        " LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
                        " where ((h.allocated is null or h.allocated = false) or (h.allocated = true and (h.unscheduled is null or h.unscheduled = false))) " +
                        " and (cast(?1 as varchar) is null or lc.case_id = cast(cast(?1 as varchar) as uuid)) " +
                        " and (cast(?2 as varchar) is null or ca.application_id = cast(cast(?2 as varchar) as uuid)) " +
                        " ) as all_hearing " +
                        " order by all_hearing.startDate desc, all_hearing.endDate desc "
                        , Hearing.class)
                .setParameter(1, caseId)
                .setParameter(2, applicationId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    Hearing findByHearingId(final String hearingId) {
        final List<Hearing> results = entityManager.createNativeQuery(
                        "select *, 1 as totalCount , " + NULL_FLAT_HEARING_FIELDS
                                + " from hearing where id = cast(cast(?1 as varchar) as uuid)",
                        Hearing.class)
                .setParameter(1, hearingId)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Hearing> findAllCourtSchedulerHearingByIds(final List<UUID> hearingIds) {
        return entityManager.createNativeQuery("select h.*, 0 as totalCount , " + NULL_FLAT_HEARING_FIELDS + " FROM hearing h where h.id in (:hearingIds)", Hearing.class)
                .setParameter("hearingIds", hearingIds)
                .getResultList();
    }

    public List<Hearing> findAllocatedHearingsForCourtCalendar(final UUID courtCentreId,
                                                               final UUID courtRoomId,
                                                               final UUID authorityCode,
                                                               final UUID hearingTypeId,
                                                               final String jurisdictionType,
                                                               final LocalDate startDate,
                                                               final LocalDate endDate,
                                                               final Instant exactHearingStartDateTime,
                                                               final Integer offSet,
                                                               final Integer pageSize) {
        return hearingJdbcRepository.findAllocatedHearingsForCourtCalendar(courtCentreId, courtRoomId,
                authorityCode, hearingTypeId, jurisdictionType, startDate, endDate, exactHearingStartDateTime, offSet, pageSize);
    }
}
