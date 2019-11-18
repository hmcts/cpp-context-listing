package uk.gov.moj.cpp.listing.persistence.repository;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.DEFAULT_DB_NAME;
import static ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.DEFAULT_PASSWORD;
import static ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.DEFAULT_USER;

import java.io.IOException;
import java.util.List;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Platform;
import de.flapdoodle.embed.process.runtime.ICommandLinePostProcessor;
import de.flapdoodle.embed.process.store.PostgresArtifactStoreBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.PackagePaths;
import ru.yandex.qatools.embed.postgresql.config.PostgresDownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.ext.SubdirTempDir;

@RunWith(Suite.class)
@SuiteClasses({HearingRepositoryTest.class, CourtListRepositoryTest.class})
public class PersistenceTestSuite {


    private static final String POSTGRES_DOWNLOAD_LOCATION = "/postgres/";
    private static final int DEFAULT_PORT = 5533;
    private static final List<String> DEFAULT_ADD_PARAMS = asList(
            "-E", "SQL_ASCII",
            "--locale=C",
            "--lc-collate=C",
            "--lc-ctype=C");

    private static final EmbeddedPostgres postgres = new EmbeddedPostgres();
    private static final String RUN_PERSISTENCE_TEST_IN_PIPELINE = "RUN_PERSISTENCE_TEST_IN_PIPELINE_KEY";

    @BeforeClass
    public static void setupBeforeClass() {
        if (Boolean.valueOf(System.getProperty(RUN_PERSISTENCE_TEST_IN_PIPELINE, "true"))) {
            startForPipeline();
        } else {
            startForLocal();
        }
    }

    private static void startForPipeline() {
        final IRuntimeConfig config = getPipelineConfig();
        try {
            postgres.start(config, EmbeddedPostgres.DEFAULT_HOST, DEFAULT_PORT, DEFAULT_DB_NAME, DEFAULT_USER, DEFAULT_PASSWORD, DEFAULT_ADD_PARAMS);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to start Postgres for testing", e);
        }
    }

    private static void startForLocal() {
        try {
            postgres.start(EmbeddedPostgres.DEFAULT_HOST, DEFAULT_PORT, DEFAULT_DB_NAME, DEFAULT_USER, DEFAULT_PASSWORD);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to start Postgres for testing", e);
        }
    }

    private static IRuntimeConfig getPipelineConfig() {
        final Command cmd = Command.Postgres;
        return new RuntimeConfigBuilder()
                .defaults(cmd)
                .artifactStore(
                        (new PostgresArtifactStoreBuilder())
                                .defaults(cmd)
                                .download((new PostgresDownloadConfigBuilder())

                                        .defaultsForCommand(cmd)
                                        .downloadPath("file://" + PersistenceTestSuite.class.getResource(POSTGRES_DOWNLOAD_LOCATION).getPath())
                                        .packageResolver(new PackagePaths(cmd, SubdirTempDir.defaultInstance()))
                                        .build()
                                )
                ).commandLinePostProcessor(privilegedWindowsRunasPostprocessor()).build();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        postgres.stop();
    }


    private static ICommandLinePostProcessor privilegedWindowsRunasPostprocessor() {
        if (Platform.detect().equals(Platform.Windows)) {
            try {
                int adminCommandResult = Runtime.getRuntime().exec("net session").waitFor();
                if (adminCommandResult == 0) {
                    return runWithoutPrivileges();
                }
            } catch (Exception var1) {
            }
        }

        return doNothing();
    }


    private static ICommandLinePostProcessor runWithoutPrivileges() {
        return (distribution, args) -> {
            return args.size() > 0 && ((String) args.get(0)).endsWith("postgres.exe") ? asList("runas", "/trustlevel:0x20000", String.format("\"%s\"", String.join(" ", args))) : args;
        };
    }

    private static ICommandLinePostProcessor doNothing() {
        return (distribution, args) -> {
            return args;
        };
    }


}
