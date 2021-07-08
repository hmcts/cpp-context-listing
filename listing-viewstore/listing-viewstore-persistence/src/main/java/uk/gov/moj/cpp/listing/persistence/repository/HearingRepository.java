package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

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
@SuppressWarnings({"squid:S00107", "squid:S1214"})
@Repository
public abstract class HearingRepository implements EntityRepository<Hearing, UUID>, EntityManagerDelegate<Hearing> {

    private static final String WEEK_COMMENCING_CORE_QUERY_FOR_ALLOCATED = "(h.unscheduled is null or h.unscheduled = false)" +
            "and (?3 is null or (lc.authority_id = cast(cast(?3 as varchar) as uuid) or lc.prosecutor_id = cast(cast(?3 as varchar) as uuid)))  " +
            "and (?4 is null or h.type_id = cast(cast(?4 as varchar) as uuid))  " +
            "and (?5 is null or h.jurisdiction_type = cast(?5 as text))  " +
            "and ( " +
            "   ( h.week_commencing_start_date >= ?6 and h.week_commencing_start_date <= ?7 ) or " +
            "   ( h.week_commencing_end_date >= ?6 and h.week_commencing_end_date <= ?7 ) or " +
            "   ( h.start_date >= ?6 and h.start_date <= ?7 )  or " +
            "   ( h.end_date >= ?6 and h.end_date <= ?7 ) ) " +
            "group by h.id, h.properties " +
            "order by h.start_date," +
            "h.end_date," +
            "h.week_commencing_start_date," +
            "h.week_commencing_end_date";

    /**
     * Find {@link Hearing}s based on the following parameters
     *
     * @param allocated        property to search for - mandatory.
     * @param courtCentreId    to search for or <code>null</code> for any courtCentreId - optional.
     * @param courtRoomId      to search for or <code>null</code> for any courtRoomId - optional.
     * @param authorityId    to search for or <code>null</code> for any authorityCode - optional.
     * @param hearingTypeId    to search for or <code>null</code> for any hearingType - optional.
     * @param jurisdictionType to search for or <code>null</code> for any jurisdictionType -
     *                         optional.
     * @param searchDate       to search for - mandatory.
     * @param startTime        to search for  - mandatory.
     * @param endTime          to search for - mandatory.
     * @return Hearings.
     */
    @Query(value = "select distinct h.id, h.properties,  " +
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
            "1 as totalCount " +
            "from hearing h INNER JOIN hearing_days hd on hd.hearing_id = h.id  " +
            "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
            "where  " +
            "h.allocated = :allocated  " +
            "and (h.unscheduled is null or h.unscheduled = false) " +
            "and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
            "and (:courtCentreId is null or coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(:courtCentreId as varchar) as uuid))  " +
            "and (:courtRoomId is null or coalesce(hd.court_room_id, h.court_room_id) = cast(cast(:courtRoomId as varchar) as uuid))  " +
            "and (:authorityId is null or (lc.authority_id = cast(cast(:authorityId as varchar) as uuid) or lc.prosecutor_id = cast(cast(:authorityId as varchar) as uuid)))  " +
            "and (:typeId is null or h.type_id = cast(cast(:typeId as varchar) as uuid))  " +
            "and (:jurisdictionType is null or h.jurisdiction_type = cast(:jurisdictionType as text))  " +
            "and (:searchDate between h.start_date and h.end_date )  " +
            "and (hd.start_time between :startTime and :endTime) "
            , isNative = true)
    public abstract List<Hearing> findHearings(@QueryParam("allocated") final boolean allocated,
                               @QueryParam("courtCentreId") final String courtCentreId,
                               @QueryParam("courtRoomId") final String courtRoomId,
                               @QueryParam("authorityId") final String authorityId,
                               @QueryParam("typeId") final String hearingTypeId,
                               @QueryParam("jurisdictionType") final String jurisdictionType,
                               @QueryParam("searchDate") final LocalDate searchDate,
                               @QueryParam("startTime") ZonedDateTime startTime,
                               @QueryParam("endTime") final ZonedDateTime endTime);



    /**
     * Find {@link Hearing}s based on the query parameters
     *
     * @param allocated        property to search for - mandatory.
     * @param courtCentreId    to search for or <code>null</code> for any courtCentreId - optional.
     * @param courtRoomId      to search for or <code>null</code> for any courtRoomId - optional.
     * @param authorityCode    to search for or <code>null</code> for any authorityCode - optional.
     * @param hearingTypeId    to search for or <code>null</code> for any hearingType - optional.
     * @param jurisdictionType to search for or <code>null</code> for any jurisdictionType -
     *                         optional.
     * @param startDate        to search for - mandatory.
     * @param endDate          to search for - mandatory.
     * @return Hearings.
     */
    @Query(value = "select distinct h.id, h.properties,  " +
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
            "count(*) OVER() as totalCount " +
            "from hearing h " +
            "LEFT JOIN hearing_days hd ON hd.hearing_id = h.id  " +
            "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
            "LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
            "where  " +
            "cast(h.allocated as varchar) = cast(?1 as varchar)  " +
            "and (h.unscheduled is null or h.unscheduled = false) " +
            "and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
            "and (?2 is null or coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(?2 as varchar) as uuid))  " +
            "and (?3 is null or coalesce(hd.court_room_id, h.court_room_id) = cast(cast(?3 as varchar) as uuid))  " +
            "and (?4 is null or (lc.authority_id = cast(cast(?4 as varchar) as uuid) or lc.prosecutor_id = cast(cast(?4 as varchar) as uuid)))  " +
            "and (?5 is null or h.type_id = cast(cast(?5 as varchar) as uuid))  " +
            "and (?6 is null or h.jurisdiction_type = cast(?6 as text))  " +
            "and (lc.is_ejected is null or lc.is_ejected =false) " +
            "and (ca.is_ejected is null or ca.is_ejected =false) " +
            "and (lc.id is not null or ca.id is not null) " +
            "and ( " +
            "(h.start_date between ?7 and ?8 ) or " +
            "(h.end_date between ?7 and ?8 ) or " +
            "((h.start_date <= ?7 ) and (h.end_date >= ?8 ) )  " +
            ") order by h.id, h.court_centre_id ASC OFFSET (?9) ROWS FETCH NEXT (?10) ROWS ONLY", isNative = true)
    public abstract List<Hearing> findHearings(final String allocated,
                               final String courtCentreId,
                               final String courtRoomId,
                               final String authorityCode,
                               final String hearingTypeId,
                               final String jurisdictionType,
                               final LocalDate startDate,
                               final LocalDate endDate, final Integer offSet, final Integer pageSize);

    /**
     * Find {@link Hearing}s based on the query parameters
     *
     * @param allocated        property to search for - mandatory.
     * @param courtCentreId    to search for or <code>null</code> for any courtCentreId - optional.
     * @param courtRoomId      to search for or <code>null</code> for any courtRoomId - optional.
     * @param authorityCode    to search for or <code>null</code> for any authorityCode - optional.
     * @param hearingTypeId    to search for or <code>null</code> for any hearingType - optional.
     * @param jurisdictionType to search for or <code>null</code> for any jurisdictionType -
     *                         optional.
     * @param startDate        to search for - mandatory.
     * @param endDate          to search for - mandatory.
     * @return Hearings.
     */
    @Query(value = "select distinct h.id, h.properties,  " +
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
            "1 as totalCount " +
            "from hearing h " +
            "LEFT JOIN hearing_days hd ON hd.hearing_id = h.id  " +
            "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
            "where  " +
            "cast(h.allocated as varchar) = cast(?1 as varchar)  " +
            "and (h.unscheduled is null or h.unscheduled = false) " +
            "and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
            "and (?2 is null or coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(?2 as varchar) as uuid))  " +
            "and (?3 is null or coalesce(hd.court_room_id, h.court_room_id) = cast(cast(?3 as varchar) as uuid))  " +
            "and (?4 is null or (lc.authority_id = cast(cast(?4 as varchar) as uuid) or lc.prosecutor_id = cast(cast(?4 as varchar) as uuid)))  " +
            "and (?5 is null or h.type_id = cast(cast(?5 as varchar) as uuid))  " +
            "and (?6 is null or h.jurisdiction_type = cast(?6 as text))  " +
            "and ( " +
            "(h.start_date between ?7 and ?8 ) or " +
            "(h.end_date between ?7 and ?8 ) or " +
            "((h.start_date <= ?7 ) and (h.end_date >= ?8 ) )  " +
            ")", isNative = true)
    public abstract List<Hearing> findHearings(final String allocated,
                                               final String courtCentreId,
                                               final String courtRoomId,
                                               final String authorityCode,
                                               final String hearingTypeId,
                                               final String jurisdictionType,
                                               final LocalDate startDate,
                                               final LocalDate endDate);

    /**
     * Find {@link Hearing}s based on the query parameters
     *
     * @param courtCentreId           to search for or <code>null</code> for any courtCentreId -
     *                                optional.
     * @param courtRoomId             to search for or <code>null</code> for any courtRoomId -
     *                                optional.
     * @param authorityCode           to search for or <code>null</code> for any authorityCode -
     *                                optional.
     * @param hearingTypeId           to search for or <code>null</code> for any hearingType -
     *                                optional.
     * @param jurisdictionType        to search for or <code>null</code> for any jurisdictionType -
     *                                optional.
     * @param weekCommencingStartDate to search for - mandatory.
     * @param weekCommencingEndDate   to search for - mandatory.
     * @return Hearings.
     */
    @Query(value = "select h.id, h.properties,  " +
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
            "count(*) OVER() as totalCount " +
            "from hearing h " +
            "LEFT JOIN hearing_days hd ON hd.hearing_id = h.id  " +
            "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id  " +
            "LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
            "where  " +
            "(h.is_vacated_trial is null or h.is_vacated_trial != true) and " +
            "(?1 is null or coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(?1 as varchar) as uuid))  " +
            "and (?2 is null or coalesce(hd.court_room_id, h.court_room_id) = cast(cast(?2 as varchar) as uuid))  " +
            "  and (lc.is_ejected is null or lc.is_ejected =false) " +
            "  and (ca.is_ejected is null or ca.is_ejected =false) " +
            "  and (lc.id is not null or ca.id is not null)" +
            "and " +
            WEEK_COMMENCING_CORE_QUERY_FOR_ALLOCATED +
            " ASC OFFSET (?8) ROWS FETCH NEXT (?9) ROWS ONLY"
            , isNative = true)
    public abstract List<Hearing> findHearingsByWeekCommencingRange(
            final String courtCentreId,
            final String courtRoomId,
            final String authorityCode,
            final String hearingTypeId,
            final String jurisdictionType,
            final LocalDate weekCommencingStartDate,
            final LocalDate weekCommencingEndDate, final Integer offSet, final Integer pageSize);


    /**
     * Find {@link Hearing}s based on the query parameters
     *
     * @param allocated               property to search for -mandatory.
     * @param courtCentreId           to search for or <code>null</code> for any courtCentreId -
     *                                optional.
     * @param courtRoomId             to search for or <code>null</code> for any courtRoomId -
     *                                optional.
     * @param authorityCode           to search for or <code>null</code> for any authorityCode -
     *                                optional.
     * @param hearingTypeId           to search for or <code>null</code> for any hearingType -
     *                                optional.
     * @param jurisdictionType        to search for or <code>null</code> for any jurisdictionType -
     *                                optional.
     * @param weekCommencingStartDate to search for - mandatory.
     * @param weekCommencingEndDate   to search for - mandatory.
     * @return Hearings.
     */
    @Query(value = "select h.id, h.properties,  " +
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
            "count(*) OVER() as totalCount " +
            "from hearing h " +
            "LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
            " LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
            "where  " +
            "(h.is_vacated_trial is null or h.is_vacated_trial != true) and " +
            "h.allocated = ?8  " +
            "and " +
            "(?1 is null or h.court_centre_id = cast(cast(?1 as varchar) as uuid))  " +
            "and (?2 is null or h.court_room_id = cast(cast(?2 as varchar) as uuid))  " +
            "  and (lc.is_ejected is null or lc.is_ejected =false) " +
            "  and (ca.is_ejected is null or ca.is_ejected =false) " +
            "  and (lc.id is not null or ca.id is not null)" +
            "and " +
            WEEK_COMMENCING_CORE_QUERY_FOR_ALLOCATED +
            " ASC OFFSET (?9) ROWS FETCH NEXT (?10) ROWS ONLY"
            , isNative = true)
    public abstract List<Hearing> findUnallocatedHearingsByWeekCommencingRange(
            final String courtCentreId,
            final String courtRoomId,
            final String authorityCode,
            final String hearingTypeId,
            final String jurisdictionType,
            final LocalDate weekCommencingStartDate,
            final LocalDate weekCommencingEndDate,
            final boolean allocated, final Integer offSet, final Integer pageSize);

    /**
     * Find {@link Hearing}s based on the query parameters.  This query will be used by the 'Public
     * List' and the 'Standard List'
     *
     * @param allocated     property to search for - mandatory.
     * @param courtCentreId to search for or <code>null</code> for any courtCentreId - mandatory.
     * @param startDate     to search for - mandatory.
     * @param endDate       to search for - mandatory.
     * @return Hearings.
     */
    @Query(value = "with filtered_hearings as (select distinct h.id, hd.hearing_date as hearingDate, h.properties as properties from hearing h " +
            "inner join hearing_days hd on hd.hearing_id = h.id where coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(:courtCentreId as varchar) as uuid) " +
            "and h.allocated = :allocated and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
            "and hd.hearing_date between :startDate and :endDate) " +
            "select 'd9ea61d4-2441-42bd-9089-510b1c069fb5' as id, " +
            ":courtCentreId as court_centre_id, " +
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
            "combinedJudiciaryAndHearings)", isNative = true)
    public abstract Hearing findHearingsForPublicStandardList(@QueryParam("allocated") final boolean allocated,
                                              @QueryParam("courtCentreId") final String courtCentreId,
                                              @QueryParam("startDate") final LocalDate startDate,
                                              @QueryParam("endDate") final LocalDate endDate);

    /**
     * Find {@link Hearing}s based on the query parameters. This query will be used by the
     * 'Alphabetical List'
     *
     * @param allocated     property to search for - mandatory.
     * @param courtCentreId to search for or <code>null</code> for any courtCentreId - mandatory.
     * @param hearingDate   to search for - mandatory.
     * @return Hearings.
     */
    @Query(value = "with filtered_hearings as (select distinct h.id, hd.hearing_date as hearingDate, h.properties as properties from hearing h " +
            "inner join hearing_days hd on hd.hearing_id = h.id where coalesce(hd.court_centre_id, h.court_centre_id) = cast(cast(:courtCentreId as varchar) as uuid) " +
            "and h.allocated = :allocated and (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
            "and hd.hearing_date = :hearingDate) " +
            "select 'd9ea61d4-2441-42bd-9089-510b1c069fb5' as id, " +
            ":courtCentreId as court_centre_id, " +
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
            "(select jsonb_agg(hrngByCourtCentreId) as properties from " +
            "    (select h.hearingDate as \"hearingDate\", " +
            "        (select jsonb_agg(hearings) as \"hearingsByHearingDate\" from  " +
            "           (select distinct properties as hearing from filtered_hearings fh where fh.hearingDate = h.hearingDate) hearings) " +
            "        from (select distinct hearingDate from filtered_hearings) h) " +
            "hrngByCourtCentreId)", isNative = true)
    public abstract List<Hearing> findHearingsForAlphabeticalList(@QueryParam("allocated") final boolean allocated,
                                                  @QueryParam("courtCentreId") final String courtCentreId,
                                                  @QueryParam("hearingDate") final LocalDate hearingDate);


    @Query(value = "select distinct h.id, properties, " +
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
            "count(*) OVER() as totalCount" +
            " from hearing h " +
            " LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
            " LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
            " where (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
            "  and  h.allocated = false " +
            "  and  h.unscheduled = true " +
            "  and ( (lc.hearing_id is null and ?1 is null ) or (?1 is null or UPPER(lc.case_reference) = cast(?1 as varchar)) " +
            "           or (?1 is null or UPPER(ca.application_reference) = cast(?1 as varchar)) " +
            "      ) " +
            "  and (?2 is null" +
            "    or h.type_of_list_id = cast(cast(?2 as varchar) as uuid))" +
            "  and (lc.id is not null or ca.id is not null) " +
            "  and (lc.is_ejected is null or lc.is_ejected =false) " +
            "  and (ca.is_ejected is null or ca.is_ejected =false) " +
            " order by h.id, h.court_centre_id ASC OFFSET (?3) ROWS FETCH NEXT (?4) ROWS ONLY"
            , isNative = true)
    public abstract List<Hearing> findHearings(String caseUrn, String typeOfList, Integer offSet, Integer pageSize);

    /**
     * Find {@link Hearing}s based on the following parameters
     *
     * @param allocated             property to search for - mandatory.
     * @param jurisdictionTypes     to search for or <code>null</code> for any jurisdictionTypes.
     * @param hearingId             property to search for - mandatory.
     * @param caseUrnSet            to search for or <code>empty string</code> for any case urn.
     * @param masterDefendantIdSet  to search for or <code>empty string</code> for any master
     *                              defendant id.
     * @param linkedCaseUrn         to search for or <code>empty string</code> for any linked case
     *                              urn.
     * @param caseUrnForLinkedCases to search for or <code>empty string</code> for any linked case
     *                              urn.
     * @return Hearings.
     */
    @Query(value = "select h.id, h.properties, " +
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
            "1 as totalCount " +
            "from hearing h " +
            "where " +
            "h.allocated = :allocated " +
            "and (h.unscheduled is null or h.unscheduled = false) " +
            "and (h.jurisdiction_type in (:jurisdictionTypes)) " +
            "and (h.end_date is null OR h.end_date >= :currentDate) " +
            "and (:hearingId is null or h.id != cast(cast(:hearingId as varchar) as uuid)) " +
            "AND " +
            "(h.id in (select distinct h.id from hearing h  " +
            " inner join listed_cases lc on lc.hearing_id = h.id where UPPER(lc.case_reference) in (:caseUrnSet)  " +
            " and (:hearingId is null or h.id != cast(cast(:hearingId as varchar) as uuid))) " +
            "OR " +
            "(h.id in (SELECT distinct(hrng.id) FROM hearing hrng inner join listed_cases lc3 on lc3.hearing_id = hrng.id " +
            "   WHERE lc3.case_reference IN (select lnkCase.case_urn as linkedCaseUrn from hearing h2  " +
            "   inner join listed_cases listCase on listCase.hearing_id = h2.id  " +
            "   inner join linked_case lnkCase on lnkCase.listed_case_id = listCase.id  " +
            "   where listCase.case_reference = cast(:caseUrnForLinkedCases as text)))) " +
            "OR " +
            " (h.id in (select distinct h5.id from " +
            "hearing h5 inner join listed_cases lc6 on lc6.hearing_id = h5.id where " +
            "lc6.case_id in (select distinct lc5.case_id from hearing h4 inner join listed_cases lc5 on lc5.hearing_id = h4.id  " +
            "inner join defendant d on d.listed_case_id = lc5.id where cast(d.master_defendant_id as varchar) in (:masterDefendantIdSet)) " +
            " and (:hearingId is null or h5.id != cast(cast(:hearingId as varchar) as uuid))) " +
            "OR " +
            "(h.id in (select distinct h.id from hearing h  " +
            " inner join listed_cases lc on lc.hearing_id = h.id where UPPER(lc.case_reference) in (:linkedCaseUrn)  " +
            " and (:hearingId is null or h.id != cast(cast(:hearingId as varchar) as uuid))) " +
            ")))"
            , isNative = true)
    public abstract List<Hearing> findHearings(@QueryParam("allocated") final boolean allocated,
                               @QueryParam("jurisdictionTypes") final Set<String> jurisdictionTypes,
                               @QueryParam("hearingId") final String hearingId,
                               @QueryParam("caseUrnSet") final Set<String> caseUrnSet,
                               @QueryParam("masterDefendantIdSet") final Set<String> masterDefendantIdSet,
                               @QueryParam("linkedCaseUrn") final Set<String> linkedCaseUrn,
                               @QueryParam("caseUrnForLinkedCases") final String caseUrnForLinkedCases,
                               @QueryParam("currentDate") final LocalDate currentDate);

    /**
     * @param caseUrn
     * @return
     */
    @Query(value = "select distinct h.id, h.properties, " +
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
            "1 as totalCount " +
            " from hearing h " +
            " LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
            " LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
            " where (h.unscheduled is null or h.unscheduled = false) " +
            "  and ( (?1 is null or UPPER(cast(lc.case_reference as varchar)) = cast(?1 as varchar)) " +
            "           or (?1 is null or UPPER(cast(ca.application_reference as varchar)) = cast(?1 as varchar))" +
            "      ) "
            , isNative = true)
    public abstract List<Hearing> findHearingsByCaseUrnAndAnyAllocationState(String caseUrn);

    /**
     * @param caseUrn
     * @param typeOfList
     * @param courtCentreIds
     * @return
     */
    @Query(value = "select distinct h.id, h.properties, " +
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
            "count(*) OVER() as totalCount" +
            " from hearing h" +
            " LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
            " LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
            " where (h.is_vacated_trial is null or h.is_vacated_trial != true) " +
            "  and  h.allocated = false " +
            "  and  h.unscheduled = true " +
            "  and ( (lc.hearing_id is null and ?1 is null ) or (?1 is null or UPPER(lc.case_reference) = cast(?1 as varchar)) " +
            "           or (?1 is null or UPPER(ca.application_reference) = cast(?1 as varchar)) " +
            "      ) " +
            "  and (?2 is null" +
            "    or h.type_of_list_id = cast(cast(?2 as varchar) as uuid))" +
            "  and cast(h.court_centre_id as varchar) in (?3)" +
            "  and (lc.id is not null or ca.id is not null) " +
            "  and (lc.is_ejected is null or lc.is_ejected =false) " +
            "  and (ca.is_ejected is null or ca.is_ejected =false) " +
            " order by h.id, h.court_centre_id ASC OFFSET (?4) ROWS FETCH NEXT (?5) ROWS ONLY"
            , isNative = true)
    public abstract List<Hearing> findHearings(String caseUrn, String typeOfList, Set<String> courtCentreIds , Integer offSet, Integer pageSize);

    /**
     * @param caseId
     * @return
     */
    @Query(value = "select id, properties," +
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
            "null as totalCount " +
            " from ( " +
            " select distinct h.id as id, h.properties as properties, h.start_date as startDate, h.end_date as endDate " +
            " from hearing h " +
            " LEFT JOIN court_applications ca ON ca.hearing_id = h.id " +
            " LEFT JOIN listed_cases lc ON lc.hearing_id = h.id " +
            " where ((h.allocated is null or h.allocated = false) or (h.allocated = true and (h.unscheduled is null or h.unscheduled = false))) " +
            " and (?1 is null or lc.case_id = cast(cast(?1 as varchar) as uuid)) " +
            " and (?2 is null or ca.application_id = cast(cast(?2 as varchar) as uuid)) " +
            " ) as all_hearing " +
            " order by all_hearing.startDate desc, all_hearing.endDate desc "
            , isNative = true)
    public abstract List<Hearing> findAllocatedAndUnallocatedHearingsByCaseId(String caseId, String applicationId);

    @Query(value = "select *, 1 as totalCount from hearing where id = cast(cast(?1 as varchar) as uuid)", isNative = true)
    abstract Hearing findByHearingId(final String hearingId);

    public Hearing findBy(final UUID hearingId){
        return  findByHearingId(hearingId.toString());
    }
}
