package uk.gov.moj.cpp.listing.persistence.repository;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
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
public interface HearingRepository extends EntityRepository<Hearing, UUID>, EntityManagerDelegate<Hearing> {

    String ALL_AUTHORITY_CODES_SEARCH = "[ ]";

    String WEEK_COMMENCING_CORE_QUERY = "(properties ->> 'unscheduled' is null or cast(properties ->> 'unscheduled' as boolean) = false)" +
            "and (?3 = '" + ALL_AUTHORITY_CODES_SEARCH + "' or properties -> 'listedCases' @> cast(?3 as jsonb))  " +
            "and (?4 is null or properties -> 'type' ->> 'id' = cast(?4 as text))  " +
            "and (?5 is null or properties ->> 'jurisdictionType' = cast(?5 as text))  " +
            "and ( " +
            "   ( cast(properties ->> 'weekCommencingStartDate' as date) >= cast(?6 as date) and cast(properties ->> 'weekCommencingStartDate' as date) <= cast(?7 as date) ) or " +
            "   ( cast(properties ->> 'weekCommencingEndDate' as date) >= cast(?6 as date) and cast(properties ->> 'weekCommencingEndDate' as date) <= cast(?7 as date) ) or " +
            "   ( cast(properties ->> 'startDate' as date) >= cast(?6 as date) and cast(properties ->> 'startDate' as date) <= cast(?7 as date) )  or " +
            "   ( cast(properties ->> 'endDate' as date) >= cast(?6 as date) and cast(properties ->> 'endDate' as date) <= cast(?7 as date) ) ) " +
            "group by id, properties " +
            "order by cast(properties ->> 'startDate' as date)," +
            "cast(properties ->> 'endDate' as date)," +
            "cast(properties ->> 'weekCommencingStartDate' as date)," +
            "cast(properties ->> 'weekCommencingEndDate' as date )";

    String AUTHORITY_ID_SEARCH = "[ { \"caseIdentifier\": { \"authorityId\": \"%s\" } } ]";
    String EARLIEST_SEARCH_DATE = "1900-01-01";
    String LATEST_SEARCH_DATE = "9999-01-01";

    /**
     * Find {@link Hearing}s based on the following parameters
     *
     * @param allocated        property to search for - mandatory.
     * @param courtCentreId    to search for or <code>null</code> for any courtCentreId - optional.
     * @param courtRoomId      to search for or <code>null</code> for any courtRoomId - optional.
     * @param authorityCode    to search for or <code>null</code> for any authorityCode - optional.
     * @param hearingTypeId    to search for or <code>null</code> for any hearingType - optional.
     * @param jurisdictionType to search for or <code>null</code> for any jurisdictionType -
     *                         optional.
     * @param searchDate       to search for - mandatory.
     * @param startTime        to search for  - mandatory.
     * @param endTime          to search for - mandatory.
     * @return Hearings.
     */
    @Query(value = "select distinct id, properties  " +
            "from hearing, jsonb_array_elements(properties -> 'hearingDays') hearingDays  " +
            "where  " +
            "cast(properties ->> 'allocated' as boolean) = ?1  " +
            "and (properties ->> 'unscheduled' is null or cast(properties ->> 'unscheduled' as boolean) = false)" +
            "and (properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true) " +
            "and (?2 is null or coalesce(hearingDays ->> 'courtCentreId', properties ->> 'courtCentreId') = cast(?2 as text))  " +
            "and (?3 is null or coalesce(hearingDays ->> 'courtRoomId', properties ->> 'courtRoomId') = cast(?3 as text))  " +
            "and (?4  = '" + ALL_AUTHORITY_CODES_SEARCH + "' or properties -> 'listedCases' @> cast(?4 as jsonb))  " +
            "and (?5 is null or properties -> 'type' ->> 'id' = cast(?5 as text))  " +
            "and (?6 is null or properties ->> 'jurisdictionType' = cast(?6 as text))  " +
            "and cast(?7 as date) between cast(properties ->> 'startDate' as date) and cast(properties ->> 'endDate' as date)  " +
            "and cast(hearingDays ->> 'startTime' as timestamp) >= cast(?8 as timestamp)  " +
            "and cast(hearingDays ->> 'startTime' as timestamp) <= cast(?9 as timestamp)"
            , isNative = true)
    List<Hearing> findHearings(final boolean allocated,
                               final String courtCentreId,
                               final String courtRoomId,
                               final String authorityCode,
                               final String hearingTypeId,
                               final String jurisdictionType,
                               final String searchDate,
                               final String startTime,
                               final String endTime);


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
    @Query(value = "select id, properties  " +
            "from hearing  " +
            "where  " +
            "(cast(properties ->> 'allocated' as boolean) = ?1  " +
            "and (properties ->> 'unscheduled' is null or cast(properties ->> 'unscheduled' as boolean) = false)" +
            "and (properties ->> 'jurisdictionType' in (?2))  " +
            "and (properties ->> 'endDate' is null or cast(properties ->> 'endDate' as date) >= CURRENT_DATE)  " +
            "and (?3 is null or properties ->> 'id' != cast(?3 as text)))  " +
            "AND " +
            "(id in ( select distinct(b.id) from " +
            " (select id, properties, jsonb_array_elements(properties->'listedCases') ->> 'id' as allCaseId from hearing ) as b " +
            "where  allCaseId in (select cases ->> 'id' as caseId from " +
            "  ( select jsonb_array_elements(properties -> 'listedCases') as cases from hearing ) " +
            " as a where UPPER(cases ->'caseIdentifier' ->> 'caseReference') in (?4)) " +
            " and (?3 is null or properties ->> 'id' != cast(?3 as text))) " +
            "OR " +
            "(id in (SELECT distinct(id) " +
            "   FROM ( " +
            "   SELECT id FROM ( " +
            "   SELECT id, JSONB_ARRAY_ELEMENTS(properties -> 'listedCases') -> 'caseIdentifier' AS linkedCaseIdentifier FROM hearing) a " +
            "   WHERE UPPER(linkedCaseIdentifier ->> 'caseReference') IN ( " +
            "   SELECT linkedCaseUrn FROM ( " +
            "   SELECT id, properties, JSONB_ARRAY_ELEMENTS(JSONB_ARRAY_ELEMENTS(properties -> 'listedCases') -> 'linkedCases') ->> 'caseUrn' AS linkedCaseUrn " +
            "   FROM (SELECT id, properties, JSONB_ARRAY_ELEMENTS(properties -> 'listedCases') -> 'caseIdentifier' AS linkedCaseIdentifier FROM hearing) allLinkedCaseIdentifier " +
            "   WHERE UPPER(linkedCaseIdentifier ->> 'caseReference') = CAST(?7 as text) " +
            "   ) AS allLinkedCaseReference " +
            "   )) linkedCaseHearing)) " +
            "OR" +
            " (id in ( select distinct(b.id) from " +
            "(select id, properties, jsonb_array_elements(properties->'listedCases') ->> 'id' as matchingCaseId from hearing )as b where " +
            "matchingCaseId in ((select listedCases ->> 'id' from " +
            "(select jsonb_array_elements(listedCases -> 'defendants') as defendants, listedCases from " +
            "(select jsonb_array_elements(properties -> 'listedCases') as listedCases from hearing) as hearings) as masterDefendantId " +
            "where defendants ->> 'masterDefendantId' in  " +
            "( select defendants ->> 'masterDefendantId' as masterDefendantIds from " +
            "(select jsonb_array_elements(listedCases -> 'defendants') as defendants from " +
            "(select jsonb_array_elements(properties -> 'listedCases') as listedCases from hearing)as defendants) as masterDefendantIds " +
            " where defendants ->> 'masterDefendantId' in (?5)))) " +
            " and (?3 is null or properties ->> 'id' != cast(?3 as text))) " +
            "OR " +
            "(id in (select distinct( id ) from (" +
            "SELECT id FROM ( " +
            "SELECT id, Jsonb_array_elements(  properties -> 'listedCases')  -> 'caseIdentifier' AS linkedCaseIdentifier FROM   hearing) allLinkedCaseIdentifier " +
            "WHERE UPPER(linkedCaseIdentifier ->> 'caseReference') IN (?6) " +
            "and (?3 is null or properties ->> 'id' != cast(?3 as text))) allLinkedCaseReference) " +
            "))) "
            , isNative = true)
    List<Hearing> findHearings(final boolean allocated,
                               final Set<String> jurisdictionTypes,
                               final String hearingId,
                               final Set<String> caseUrnSet,
                               final Set<String> masterDefendantIdSet,
                               final Set<String> linkedCaseUrn,
                               final String caseUrnForLinkedCases);


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
    @Query(value = "select distinct id, properties  " +
            "from hearing LEFT JOIN jsonb_array_elements(properties -> 'hearingDays') hearingDays ON TRUE  " +
            "where  " +
            "properties -> 'allocated'  @> cast(?1 as jsonb)  " +
            "and (properties -> 'unscheduled' is null or properties -> 'unscheduled' @> cast('false' as jsonb))" +
            "and (properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true) " +
            "and (?2 is null or coalesce(hearingDays ->> 'courtCentreId', properties ->> 'courtCentreId') = cast(?2 as text))  " +
            "and (?3 is null or coalesce(hearingDays ->> 'courtRoomId', properties ->> 'courtRoomId') = cast(?3 as text))  " +
            "and (?4 = '" + ALL_AUTHORITY_CODES_SEARCH + "' or properties -> 'listedCases' @> cast(?4 as jsonb))  " +
            "and (?5 is null or properties -> 'type' ->> 'id' = cast(?5 as text) )  " +
            "and (?6 is null or properties ->> 'jurisdictionType' = cast(?6 as text))  " +
            "and ( " +
            "   cast(properties ->> 'startDate' as date) between cast(?7 as date) and cast(?8 as date) or  " +
            "   cast(properties ->> 'endDate' as date) between cast(?7 as date) and cast(?8 as date) or  " +
            "   ( cast(properties ->> 'startDate' as date) <= cast(?7 as date) and cast(properties ->> 'endDate' as date) >= cast(?8 as date) )  " +
            ")", isNative = true)
    List<Hearing> findHearings(final String allocated,
                               final String courtCentreId,
                               final String courtRoomId,
                               final String authorityCode,
                               final String hearingTypeId,
                               final String jurisdictionType,
                               final String startDate,
                               final String endDate);

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
    @Query(value = "select id, properties  " +
            "from hearing LEFT JOIN jsonb_array_elements(properties -> 'hearingDays') hearingDays ON TRUE  " +
            "where  " +
            "(properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true) and " +
            "(?1 is null or coalesce(hearingDays ->> 'courtCentreId', properties ->> 'courtCentreId') = cast(?1 as text))  " +
            "and (?2 is null or coalesce(hearingDays ->> 'courtRoomId', properties ->> 'courtRoomId') = cast(?2 as text))  " +
            "and " +
            WEEK_COMMENCING_CORE_QUERY
            , isNative = true)
    List<Hearing> findHearingsByWeekCommencingRange(
            final String courtCentreId,
            final String courtRoomId,
            final String authorityCode,
            final String hearingTypeId,
            final String jurisdictionType,
            final String weekCommencingStartDate,
            final String weekCommencingEndDate);

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
    @Query(value = "select id, properties  " +
            "from hearing  " +
            "where  " +
            "(properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true) and " +
            "cast(properties ->> 'allocated' as boolean) = ?8  " +
            "and " +
            "(?1 is null or (properties ->> 'courtCentreId' = cast(?1 as text)))  " +
            "and (?2 is null or (properties ->> 'courtRoomId' = cast(?2 as text)))  " +
            "and " +
            WEEK_COMMENCING_CORE_QUERY
            , isNative = true)
    List<Hearing> findUnallocatedHearingsByWeekCommencingRange(
            final String courtCentreId,
            final String courtRoomId,
            final String authorityCode,
            final String hearingTypeId,
            final String jurisdictionType,
            final String weekCommencingStartDate,
            final String weekCommencingEndDate,
            final boolean allocated);

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
    @Query(value = "select 'd9ea61d4-2441-42bd-9089-510b1c069fb5' as id,\n" +
            "(\n" +
            "select row_to_json(combinedJudiciaryAndHearings) as properties\n" +
            "    from\n" +
            "    (\n" +
            "        select *\n" +
            "        from\n" +
            "        (\n" +
            "            select json_agg(uniqueJudiciary) as judiciary\n" +
            "            from\n" +
            "            (\n" +
            "                select distinct judiciary -> 'judicialId' as \"judicialId\"\n" +
            "                from\n" +
            "                (\n" +
            "                    select jsonb_array_elements(properties -> 'judiciary') judiciary\n" +
            "                    from hearing, jsonb_array_elements(properties -> 'hearingDays') hearingDays \n" +
            "                    where coalesce(hearingDays ->> 'courtCentreId', properties ->> 'courtCentreId') = cast(?2 as text)\n" +
            "                    and cast(properties ->> 'allocated' as boolean) = ?1\n" +
            "                    and (properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true) " +
            "                ) judicialId\n" +
            "            ) uniqueJudiciary\n" +
            "        ) a,\n" +
            "        (\n" +
            "            select json_agg(hrngByCourtCentreId) as hearings\n" +
            "            from\n" +
            "            (\n" +
            "                select distinct on (hearingDays ->> 'courtCentreId') hearingDays ->> 'courtCentreId' as \"courtCentreId\",\n" +
            "                (\n" +
            "                    select json_agg(hbsd) as \"hearingsByCourtCentreId\"\n" +
            "                    from\n" +
            "                    (\n" +
            "                        select hearingDate as \"hearingDate\",\n" +
            "                        (\n" +
            "                            select jsonb_agg(hearings) as \"hearingsByHearingDate\"\n" +
            "                            from\n" +
            "                            (\n" +
            "                                select distinct properties as hearing\n" +
            "                                from hearing, jsonb_array_elements(properties -> 'hearingDays') hearingDays\n" +
            "                                where coalesce(hearingDays ->> 'courtCentreId', properties ->> 'courtCentreId') = cast(?2 as text)\n" +
            "                                and cast(properties ->> 'allocated' as boolean) = ?1\n" +
            "                                and (properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true) " +
            "                                and properties -> 'hearingDays' @> cast(concat('[{\"hearingDate\": \"', h3.hearingDate, '\"}]') as jsonb)\n" +
            "                                and hearingDays ->> 'hearingDate' = h3.hearingDate\n" +
            "                            ) hearings\n" +
            "                        )\n" +
            "                        from\n" +
            "                        (\n" +
            "                            select distinct hearingDays ->> 'hearingDate' as hearingDate\n" +
            "                            from hearing, jsonb_array_elements(properties -> 'hearingDays') hearingDays\n" +
            "                            where coalesce(hearingDays ->> 'courtCentreId', properties ->> 'courtCentreId') = cast(?2 as text)\n" +
            "                            and cast(properties ->> 'allocated' as boolean) = ?1\n" +
            "                            and (properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true) " +
            "                        ) as h3\n" +
            "                    ) hbsd\n" +
            "                    where cast(\"hearingDate\" as date) between cast(?3 as date) and cast(?4 as date)\n" +
            "                ) from hearing h4, jsonb_array_elements(h4.properties -> 'hearingDays') hearingDays \n" +
            "            ) hrngByCourtCentreId\n" +
            "            where \"courtCentreId\" = cast(?2 as text)\n" +
            "        ) b\n" +
            "    ) combinedJudiciaryAndHearings\n" +
            ")", isNative = true)
    Hearing findHearingsForPublicStandardList(final boolean allocated,
                                              final String courtCentreId,
                                              final String startDate,
                                              final String endDate);

    /**
     * Find {@link Hearing}s based on the query parameters. This query will be used by the
     * 'Alphabetical List'
     *
     * @param allocated     property to search for - mandatory.
     * @param courtCentreId to search for or <code>null</code> for any courtCentreId - mandatory.
     * @param hearingDate   to search for - mandatory.
     * @return Hearings.
     */
    @Query(value = "select 'd9ea61d4-2441-42bd-9089-510b1c069fb5' as id,\n" +
            "(\n" +
            "    select jsonb_agg(hrngByCourtCentreId) as properties\n" +
            "    from\n" +
            "    (\n" +
            "        select hearingDate as \"hearingDate\",\n" +
            "        (\n" +
            "            select jsonb_agg(hearings) \"hearingsByHearingDate\"\n" +
            "            from (\n" +
            "                select distinct properties as hearing\n" +
            "                from hearing, jsonb_array_elements(properties -> 'hearingDays') hearingDays\n" +
            "                where properties -> 'hearingDays' @> cast(concat('[{\"hearingDate\": \"', hrngByHearingDate.hearingDate, '\"}]') as jsonb)\n" +
            "                and coalesce(hearingDays ->> 'courtCentreId', properties ->> 'courtCentreId') = cast(?2 as text)\n" +
            "                and cast(properties ->> 'allocated' as boolean) = ?1\n" +
            "                and (properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true) " +
            "                and hearingDays ->> 'hearingDate' = hrngByHearingDate.hearingDate\n" +
            "            ) hearings\n" +
            "        )\n" +
            "        from (\n" +
            "                select distinct hearingDays ->> 'hearingDate' hearingDate\n" +
            "                from hearing, jsonb_array_elements(properties -> 'hearingDays') hearingDays\n" +
            "                where coalesce(hearingDays ->> 'courtCentreId', properties ->> 'courtCentreId') = cast(?2 as text)\n" +
            "                and cast(properties ->> 'allocated' as boolean) = ?1\n" +
            "                and (properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true) " +
            "        ) as hrngByHearingDate\n" +
            "        where cast(hearingDate as date) = cast(?3 as date)\n" +
            "    ) hrngByCourtCentreId\n" +
            ")", isNative = true)
    List<Hearing> findHearingsForAlphabeticalList(final boolean allocated,
                                                  final String courtCentreId,
                                                  final String hearingDate);


    @Query(value = "select distinct id, properties" +
            " from hearing " +
            " where (properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true)" +
            "  and cast(properties ->> 'allocated' as boolean) = false" +
            "  and cast(properties ->> 'unscheduled' as boolean) = true" +
            "  and ( ( properties -> 'listedCases' is null and ?1 is null ) " +
            "           or id in (select hearingId" +
            "             from (" +
            "                      select id as hearingId, jsonb_array_elements(properties -> 'listedCases') as cases" +
            "                      from hearing) as allCases" +
            "             where ?1 is null" +
            "                or UPPER(allCases.cases -> 'caseIdentifier' ->> 'caseReference') = cast(?1 as text))" +
            "           or id in (select hearingId" +
            "             from (" +
            "                      select id as hearingId, jsonb_array_elements(properties -> 'courtApplications') as application" +
            "                      from hearing) as allApplications" +
            "             where ?1 is null" +
            "                or UPPER(allApplications.application ->> 'applicationReference') = cast(?1 as text))" +
            "      ) " +
            "  and (?2 is null" +
            "    or properties -> 'typeOfList' ->> 'id' = cast(?2 as text))"
            , isNative = true)
    List<Hearing> findHearings(String caseUrn, String typeOfList);

    /**
     * @param caseUrn
     * @return
     */
    @Query(value = "select distinct id, properties" +
            " from hearing " +
            " where (properties ->> 'unscheduled' is null or cast(properties ->> 'unscheduled' as boolean) = false)" +
            "  and (  id in (select hearingId" +
            "             from (" +
            "                      select id as hearingId, jsonb_array_elements(properties -> 'listedCases') as cases" +
            "                      from hearing) as allCases" +
            "             where ?1 is null" +
            "                or UPPER(allCases.cases -> 'caseIdentifier' ->> 'caseReference') = cast(?1 as text))" +
            "           or id in (select hearingId" +
            "             from (" +
            "                      select id as hearingId, jsonb_array_elements(properties -> 'courtApplications') as application" +
            "                      from hearing) as allApplications" +
            "             where ?1 is null" +
            "                or UPPER(allApplications.application ->> 'applicationReference') = cast(?1 as text))" +
            "      ) "
            , isNative = true)
    List<Hearing> findHearingsByCaseUrnAndAnyAllocationState(String caseUrn);

    /**
     * @param caseUrn
     * @param typeOfList
     * @param courtCentreIds
     * @return
     */
    @Query(value = "select distinct id, properties" +
            " from hearing" +
            " where (properties ->> 'isVacatedTrial' is null or cast(properties ->> 'isVacatedTrial' as boolean) != true)" +
            "  and cast(properties ->> 'allocated' as boolean) = false" +
            "  and cast(properties ->> 'unscheduled' as boolean) = true" +
            "  and ( ( properties -> 'listedCases' is null and ?1 is null ) " +
            "           or id in (select hearingId" +
            "             from (" +
            "                      select id as hearingId, jsonb_array_elements(properties -> 'listedCases') as cases" +
            "                      from hearing) as allCases" +
            "             where ?1 is null" +
            "                or UPPER(allCases.cases -> 'caseIdentifier' ->> 'caseReference') = cast(?1 as text))" +
            "           or id in (select hearingId" +
            "             from (" +
            "                      select id as hearingId, jsonb_array_elements(properties -> 'courtApplications') as application" +
            "                      from hearing) as appApplications" +
            "             where ?1 is null" +
            "                or UPPER(appApplications.application ->> 'applicationReference') = cast(?1 as text))" +
            "      ) " +
            "  and (?2 is null" +
            "    or properties -> 'typeOfList' ->> 'id' = cast(?2 as text))" +
            "  and (properties ->> 'courtCentreId' in (?3))"
            , isNative = true)
    List<Hearing> findHearings(String caseUrn, String typeOfList, Set<String> courtCentreIds);

    /**
     * @param caseId
     * @return
     */
    @Query(value =  "select id, properties from ( " +
            "select distinct id, properties" +
            " from hearing " +
            "where " +
            "cast(properties ->> 'allocated' as boolean) = false " +
            "and ( ?1 is null or cast(?1 as text) IN " +
            "(" +
            "  SELECT jsonb_array_elements(properties -> 'listedCases') ->> 'id' " +
            ") ) " +
            "and ( ?2 is null or cast(?2 as text) IN " +
            "(" +
            "  SELECT jsonb_array_elements(properties -> 'courtApplications') ->> 'id' " +
            ") ) " +
            " UNION " +
            "select distinct id, properties " +
            "            from hearing " +
            "            where " +
            "            cast(properties ->> 'allocated' as boolean) = true " +
            "            and (properties ->> 'unscheduled' is null or cast(properties ->> 'unscheduled' as boolean) = false)  " +
            "and (  ?1 is null or cast(?1 as text) IN " +
            "(" +
            "  SELECT jsonb_array_elements(properties -> 'listedCases') ->> 'id' " +
            ") ) "+
            "and ( ?2 is null or cast(?2 as text) IN " +
            "(" +
            "  SELECT jsonb_array_elements(properties -> 'courtApplications') ->> 'id' " +
            ") ) " +
            ") as all_hearing "+
            "order by cast(all_hearing.properties ->> 'startDate' as date) desc," +
            "cast(all_hearing.properties ->> 'endDate' as date) desc"
            , isNative = true)
    List<Hearing> findAllocatedAndUnallocatedHearingsByCaseId(String caseId, String applicationId);


}
