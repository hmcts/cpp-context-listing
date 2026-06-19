# !/bin/bash
#
#####################################################################################################################
# Author: Shyam                                                                               Date: 23/02/2022      #
# Description: This script procures the Event IDs from EventStore and restores into ViewStore for fixing duplicate  #
#              reporting restrictions.                                                                              #
#                                                                                                                   #
# V1.0    2022-02-20    Shyam     Original version                                                                  #
# v1.1    2022-02-25    Arcadius Ahouansou: Added union and delete stetements                                       #
#####################################################################################################################

# Retrieve password from Vault for Production

# Create an ARRAY with 3 elements, hostname, DBname, username

#export SELECTIVE_CATCHUP_DB_CONN="host=psf-dev-ccm02-listing.postgres.database.azure.com port=5432 dbname=listingviewstore user=listing password=listing sslmode=require"
export SELECTIVE_CATCHUP_DB_CONN="postgresql://listing:listing@localhost/listingviewstore"
psql -v ON_ERROR_STOP=1 ${SELECTIVE_CATCHUP_DB_CONN} <<EOF
   \echo '===Copying streamids to csv file'
   
   \COPY ((select distinct (hearingid) as stream_id from ( select *, count(1) from ( select id as hearingId,offe->'id' as offenceID,jsonb_array_elements(offe->'reportingRestrictions')->'label' as rrLabel from ( select id, jsonb_array_elements(jsonb_array_elements(jsonb_array_elements(properties->'listedCases')->'defendants')->'offences') as offe from hearing) as hearingInfo where offe->'reportingRestrictions' is not null) as heringWithCount group by  hearingId, offenceid,rrLabel having count(1)>1 order by count(1) desc ) as distinctStream) union ( select distinct (hearingid) as stream_id from ( select *, count(1) from ( select id as hearingId, offe->'id' as offenceID, jsonb_array_elements(offe->'reportingRestrictions')->'label' as rrLabel from ( select id, jsonb_array_elements(jsonb_array_elements(properties->'courtApplications')->'offences') as offe from hearing) as hearingInfo where offe->'reportingRestrictions' is not null) as heringWithCount group by hearingId, offenceid, rrLabel having count(1)>1 order by count(1) desc ) as distinctStream) union (select distinct hearingid as stream_id from (select hearingid, offenceid, count(1) from ( select id as hearingId, offe->'id' as offenceID from ( select id, jsonb_array_elements(jsonb_array_elements(jsonb_array_elements(properties->'listedCases')->'defendants')->'offences') as offe from hearing) as hearingInfo) as aa  group by hearingid, offenceid having count(1)>1) as outhearing)) TO '/tmp/streamids.csv' DELIMITER ',' CSV HEADER;

   \echo '===Connecting to eventstore'
   \c listingeventstore
   
   \echo '===Dropping tmp_stream_id'
   DROP TABLE IF EXISTS tmp_stream_id CASCADE;
   
   \echo '===Creating tmp_stream_id'
   CREATE TABLE tmp_stream_id( stream_id uuid primary key);

   \echo '===Loading data from streamids.csv'
   \COPY tmp_stream_id FROM '/tmp/streamids.csv' CSV header;

   \echo '===Copying data into eventids_streamid.csv'
   \COPY (SELECT el.id, el.stream_id FROM event_log el, tmp_stream_id tsid WHERE el.stream_id = tsid.stream_id) TO '/tmp/eventids_streamid.csv' DELIMITER ',' CSV HEADER;

   \echo '===Clearing tmp_stream_id'
   DROP TABLE IF EXISTS tmp_stream_id CASCADE;

   \echo '===Connecting to viewstore'
   \c listingviewstore
   
   \echo '===Dropping tmp_selective_cu_stream'
   DROP TABLE IF EXISTS tmp_selective_cu_stream CASCADE;
   
   \echo '===Creating tmp_selective_cu_stream'
   CREATE TABLE tmp_selective_cu_stream(event_id uuid primary key, stream_id uuid not null);

   \echo '===Loading data into tmp_selective_cu_stream from eventids_streamid.csv'
   \COPY tmp_selective_cu_stream FROM '/tmp/eventids_streamid.csv' CSV header;

-- EXECUTE DELETE STATEMENT HERE


\echo '=== delete stream data from stream_status'

 delete
from
	stream_status
where
	component = 'EVENT_LISTENER'
	and stream_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);

\echo '=== delete stream data from stream_buffer'
delete
from
	stream_buffer
where
	component = 'EVENT_LISTENER'
	and stream_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);
	
\echo '=== delete from processed_event using event id'
 delete
from
	processed_event
where
	component = 'EVENT_LISTENER'
	and event_id in (
	select
		event_id
	from
		tmp_selective_cu_stream);
	
\echo '=== delete stream data from hearing'
 delete
from
	hearing
where
	id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);
		
\echo '=== delete stream data from court_application'	
 delete
from
	court_applications
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);
		
\echo '=== delete from hearing_days'	
 delete
from
	hearing_days
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);
\echo '=== delete from listed_cases'			
 delete
from
	listed_cases
where
	hearing_id in (
	select
		distinct stream_id
	from
		tmp_selective_cu_stream);


-- Clear temporary table, once process completes
--   DROP TABLE IF EXISTS tmp_selective_cu_stream CASCADE;
EOF

rm /tmp/streamids.csv
rm /tmp/eventids_streamid.csv
