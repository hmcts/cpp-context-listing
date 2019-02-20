# Instructions for installing listing-viewstore-persistence for the *first time* only.

Run with the following command:
    mvn clean install -DRUN_PERSISTENCE_TEST_IN_PIPELINE_KEY=false

The above command will trigger a download of a version of Postgres that can be started in
embedded mode.  The downloaded package will be cached in the folder ~/.embedpostgresql/ .  Once
cached the module can be built without the '-D' system parameter.

