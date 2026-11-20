# listing-viewstore-liquibase

The viewstore Liquibase changelogs (`src/main/resources/liquibase/db-changelog.xml` and
`liquibase/listing-view-store-db-changesets/*.xml`), packaged as a resources-only jar that
`listing-api` puts on its runtime classpath. Spring Boot runs them at startup
(`spring.liquibase.change-log=classpath:liquibase/db-changelog.xml`, `application-datasource.yml`);
there is no separate Liquibase jar or container step any more.

- Moved here from the Maven module `listing-viewstore/listing-viewstore-liquibase` at P2.6 of the MBD
  refactor. The changelog files are unchanged; the Maven `pom.xml` and `liquibase.properties` (the
  Maven plugin's config) were removed.
- **Do not rename or move the changeset files.** The 9 changesets without `logicalFilePath` are
  recorded in `DATABASECHANGELOG.FILENAME` as `liquibase/listing-view-store-db-changesets/<file>`,
  the same value the JEE deployment wrote (D21). `LiquibaseChangelogIdentityTest`
  (listing-viewstore-persistence-spring) checks this against a capture of a JEE-deployed viewstore.
- New changesets are additive only (MBD_REFACTOR_PROMPT.md §6).

Apply them to a local database without the app, e.g. with the Liquibase CLI:

    liquibase --changelog-file=liquibase/db-changelog.xml \
      --search-path=listing-viewstore-liquibase/src/main/resources \
      --url=jdbc:postgresql://localhost:5432/listingviewstore --username=listing --password=listing update
