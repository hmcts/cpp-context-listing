package uk.gov.moj.cpp.listing.persistence.repository;


import org.apache.deltaspike.data.api.EntityManagerDelegate;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.util.List;
import java.util.UUID;

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
public interface HearingRepository extends EntityRepository<Hearing, UUID>,
        EntityManagerDelegate<Hearing> {

    String ALL_AUTHORITY_CODES_SEARCH = "[ ]";
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
            "and (?2 is null or properties ->> 'courtCentreId' = cast(?2 as text))  " +
            "and (?3 is null or properties ->> 'courtRoomId' = cast(?3 as text))  " +
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
    @Query(value = "select id, properties  " +
            "from hearing  " +
            "where  " +
            "cast(properties ->> 'allocated' as boolean) = ?1  " +
            "and (?2 is null or properties ->> 'courtCentreId' = cast(?2 as text))  " +
            "and (?3 is null or properties ->> 'courtRoomId' = cast(?3 as text))  " +
            "and (?4  = '" + ALL_AUTHORITY_CODES_SEARCH + "' or properties -> 'listedCases' @> cast(?4 as jsonb))  " +
            "and (?5 is null or properties -> 'type' ->> 'id' = cast(?5 as text))  " +
            "and (?6 is null or properties ->> 'jurisdictionType' = cast(?6 as text))  " +
            "and ( " +
            "   cast(properties ->> 'startDate' as date) between cast(?7 as date) and cast(?8 as date) or  " +
            "   cast(properties ->> 'endDate' as date) between cast(?7 as date) and cast(?8 as date) or  " +
            "   ( cast(properties ->> 'startDate' as date) <= cast(?7 as date) and cast(properties ->> 'endDate' as date) >= cast(?8 as date) )  " +
            ")"
            , isNative = true)
    List<Hearing> findHearings(final boolean allocated,
                               final String courtCentreId,
                               final String courtRoomId,
                               final String authorityCode,
                               final String hearingTypeId,
                               final String jurisdictionType,
                               final String startDate,
                               final String endDate);

    /**
     * Find {@link Hearing}s based on the query parameters.  This query will be used by the 'Public
     * List'
     *
     * @param allocated     property to search for - mandatory.
     * @param courtCentreId to search for or <code>null</code> for any courtCentreId - mandatory.
     * @param startDate     to search for - mandatory.
     * @param endDate       to search for - mandatory.
     * @return Hearings.
     */
    @Query(value =
            "select 'd9ea61d4-2441-42bd-9089-510b1c069fb5' as id," +
            "( " +
            "select row_to_json(combinedJudiciaryAndHearings) as properties " +
            "    from " +
            "    ( " +
            "        select * " +
            "        from  " +
            "        ( " +
            "            select json_agg(uniqueJudiciary) as judiciary " +
            "            from  " +
            "            ( " +
            "                select distinct judiciary -> 'judicialId' as \"judicialId\" " +
            "                from  " +
            "                ( " +
            "                    select jsonb_array_elements(properties -> 'judiciary') judiciary " +
            "                    from hearing" +
            "                    where properties ->> 'courtCentreId' = cast(?2 as text) " +
            "                    and cast(properties ->> 'allocated' as boolean) = ?1 " +
            "                ) judicialId " +
            "            ) uniqueJudiciary " +
            "        ) a," +
            "        ( " +
            "            select json_agg(hrngByCourtCentreId) as hearings " +
            "            from  " +
            "            ( " +
            "                select distinct on (properties ->> 'courtCentreId') properties ->> 'courtCentreId' as \"courtCentreId\", " +
            "                ( " +
            "                    select json_agg(hbsd) as \"hearingsByCourtCentreId\" " +
            "                    from  " +
            "                    ( " +
            "                        select hearingDate as \"hearingDate\"," +
            "                        ( " +
            "                            select jsonb_agg(hearings) as \"hearingsByHearingDate\" " +
            "                            from  " +
            "                            ( " +
            "                                select properties as hearing" +
            "                                from hearing " +
            "                                where properties ->> 'courtCentreId' = cast(?2 as text) " +
            "                                and cast(properties ->> 'allocated' as boolean) = ?1 " +
            "                                and properties -> 'hearingDays' @> cast(concat('[{\"hearingDate\": \"', h3.hearingDate, '\"}]') as jsonb) " +
            "                            ) hearings " +
            "                        )  " +
            "                        from  " +
            "                        ( " +
            "                            select distinct hearingDays ->> 'hearingDate' as hearingDate " +
            "                            from  " +
            "                            ( " +
            "                                select jsonb_array_elements(properties -> 'hearingDays') hearingDays " +
            "                                from hearing" +
            "                                where properties ->> 'courtCentreId' = cast(?2 as text) " +
            "                                and cast(properties ->> 'allocated' as boolean) = ?1 " +
            "                            ) as h2 " +
            "                        ) as h3 " +
            "                    ) hbsd " +
            "                    where cast(\"hearingDate\" as date) between cast(?3 as date) and cast(?4 as date) " +
            "                ) from hearing h4  " +
            "            ) hrngByCourtCentreId " +
            "            where \"courtCentreId\" = cast(?2 as text) " +
            "        ) b " +
            "    ) combinedJudiciaryAndHearings " +
            ")"
        , isNative = true)
    Hearing findHearingsForPublicStandardList(final boolean allocated,
                                              final String courtCentreId,
                                              final String startDate,
                                              final String endDate);


    @Query(value =
        "select 'd9ea61d4-2441-42bd-9089-510b1c069fb5' as id, "+
        "(  "+
        "    select jsonb_agg(hrngByCourtCentreId) as properties "+
        "    from  "+
        "    (  "+
        "        select hearingDate as \"hearingDate\", "+
        "        (  "+
        "            select jsonb_agg(hearings) \"hearingsByHearingDate\" "+
        "            from (  "+
        "                select hearingDays -> 'startTime' \"startTime\", listedCases -> 'defendants' defendants,  listedCases -> 'caseIdentifier' \"caseIdentifier\", properties -> 'courtCentreId' \"courtCentreId\" , properties -> 'courtRoomId' \"courtRoomId\" "+
        "                from hearing, jsonb_array_elements(properties -> 'hearingDays') hearingDays, jsonb_array_elements(properties -> 'listedCases') listedCases  "+
        "                where properties -> 'hearingDays' @> cast(concat('[{\"hearingDate\": \"', hrngByHearingDate.hearingDate, '\"}]') as jsonb) "+
        "                and cast(properties ->> 'allocated' as boolean) = ?1 "+
        "                and (listedCases->>'isEjected' is null or cast(listedCases->>'isEjected' as boolean) = false) "+
        "                and properties ->> 'courtCentreId' = cast(?2 as text) " +
        "                and hearingDays ->> 'hearingDate' = hrngByHearingDate.hearingDate "+
        "            ) hearings  "+
        "        )  "+
        "        from (  "+
        "            select distinct hearingDays ->> 'hearingDate' hearingDate   "+
        "            from (  "+
        "                select jsonb_array_elements(properties -> 'hearingDays') hearingDays   "+
        "                from hearing  "+
        "                where properties ->> 'courtCentreId' = cast(?2 as text) "+
        "                and cast(properties ->> 'allocated' as boolean) = ?1 "+
        "            ) as h2  "+
        "        ) as hrngByHearingDate   "+
        "        where cast(hearingDate as date) = cast(?3 as date) "+
        "    ) hrngByCourtCentreId  "+
        ") "
            , isNative = true)
    List<Hearing> findHearingsForAlphabeticalList(final boolean allocated,
                                                  final String courtCentreId,
                                                  final String hearingDate);
}
