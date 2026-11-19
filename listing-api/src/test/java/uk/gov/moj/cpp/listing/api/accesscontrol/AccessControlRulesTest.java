package uk.gov.moj.cpp.listing.api.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.AuthzPrincipal;
import uk.gov.moj.cpp.authz.http.RequestActionResolver;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.providers.RequestUserAndGroupProvider;

/**
 * Proves the ported DRL (ADR 005) gives the legacy allow/deny decisions under cp-auth-rules-filter,
 * and cross-walks every vendor media type in the OpenAPI spec to a rule (REVIEW §3.3: with
 * {@code deny-when-no-rules} a media type without a rule is a latent 403).
 */
class AccessControlRulesTest {

    private static final String DRL_PATTERN = "classpath:/uk.gov.moj.cpp.listing.api.accesscontrol.drl/**/*.drl";
    private static final String Q = "/query/api/rest/listing";
    private static final String C = "/command/api/rest/listing";

    /** Paths excluded from authz (application-authz.yml): legacy bypasses, §15.5. */
    private static final List<String> BYPASSED = List.of(
            Q + "/hearingSlots",
            Q + "/hearings/download-hearing-csv-report",
            Q + "/sessionAvailabilityValidation",
            Q + "/courtScheduleDraftStatus");

    /**
     * Media types the command RAML declares but no legacy DRL rule covers (and no command-api
     * {@code @Handles} method handles): denied on WildFly too, so deny-when-no-rules keeps parity.
     */
    private static final Set<String> DENIED_ON_WILDFLY_TOO = Set.of(
            "listing.command.update-defendants-for-hearing",
            "listing.command.delete-offences-for-hearing",
            "listing.command.add-offences-for-hearing",
            "listing.command.update-offences-for-hearing");

    /** Legacy rules that have no endpoint in the RAML (dead on WildFly too); kept unchanged. */
    private static final Set<String> RULES_WITHOUT_ENDPOINT = Set.of(
            "listing.public.list", "listing.correct-hearing-days-without-court-centre");

    private static DroolsAuthzEngine engine;

    @BeforeAll
    static void loadRules() {
        engine = new DroolsAuthzEngine(HttpAuthzProperties.builder()
                .droolsClasspathPattern(DRL_PATTERN)
                .denyWhenNoRules(true)
                .build());
    }

    private static boolean allowed(final String actionName, final String path, final String... groups) {
        final AuthzPrincipal principal = new AuthzPrincipal("user", null, null, null,
                new LinkedHashSet<>(List.of(groups)), List.of());
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("method", path.startsWith(C) ? "POST" : "GET");
        attributes.put("path", path);
        return engine.evaluate(new RequestUserAndGroupProvider(principal, new ObjectMapper()),
                new Action(actionName, attributes));
    }

    @ParameterizedTest(name = "{0} at {1} as [{2}] -> {3}")
    @CsvSource(delimiter = '|', value = {
            // commands: vendor token == legacy action name
            "listing.command.create-listing-note | /command/api/rest/listing/listing-note | Listing Officers | true",
            "listing.command.create-listing-note | /command/api/rest/listing/listing-note | Random Group | false",
            "listing.command.list-court-hearing | /command/api/rest/listing/cases | Magistrates | true",
            "listing.command.list-unscheduled-court-hearing | /command/api/rest/listing/cases | Magistrates | false",
            "listing.command.vacate-trial | /command/api/rest/listing/hearings/h1 | Court Clerks | true",
            "listing.command.vacate-trial | /command/api/rest/listing/hearings/h1 | Court Administrators | false",
            // commands: vendor token != legacy action name (rules renamed)
            "listing.next-hearings-v2 | /command/api/rest/listing/hearings/h1 | System Users | true",
            "listing.related-hearing | /command/api/rest/listing/hearings/h1 | Court Associate | true",
            "listing.duplicate-unallocated-hearing | /command/api/rest/listing/hearings/h1 | System Users | true",
            "listing.duplicate-unallocated-hearing | /command/api/rest/listing/hearings/h1 | Listing Officers | false",
            // legacy isSystemUser($action) == member of "System Users"
            "listing.command.publish-court-lists-for-crown-courts | /command/api/rest/listing/publishCourtListsForCrownCourts | System Users | true",
            "listing.command.publish-court-lists-for-crown-courts | /command/api/rest/listing/publishCourtListsForCrownCourts | Listing Officers | false",
            "listing.delete-hearing | /command/api/rest/listing/delete-hearing/h1 | System Users | true",
            // group names compare case-insensitively (cp-auth-rules-filter), as usersgroups returns them
            "listing.command.create-listing-note | /command/api/rest/listing/listing-note | listing officers | true",
            // queries sharing the listing.search.hearings token are told apart by path
            "listing.search.hearings | /query/api/rest/listing/hearings | Operational Delivery Admin | true",
            "listing.search.hearings | /query/api/rest/listing/hearings/available-search | Operational Delivery Admin | false",
            "listing.search.hearings | /query/api/rest/listing/hearings/range-search | Prison Admin | true",
            "listing.search.hearings | /query/api/rest/listing/hearings | Prison Admin | false",
            "listing.search.hearings | /query/api/rest/listing/hearings/allocated-and-unallocated | Judge | true",
            "listing.search.hearings | /query/api/rest/listing/hearings/cotr-search | Judge | false",
            "listing.search.hearings | /query/api/rest/listing/hearings/any-allocation | System Users | true",
            "listing.search.hearings | /query/api/rest/listing/hearings/any-allocation | Listing Officers | false",
            "listing.search.hearings | /query/api/rest/listing/hearings/unscheduled | CPS | true",
            "listing.search.hearings | /query/api/rest/listing/hearings/cotr-search | System Users | true",
            "listing.search.hearings.court.calendar | /query/api/rest/listing/hearings/range-search | Police Admin | true",
            "listing.search.hearing | /query/api/rest/listing/hearings/h1 | Listing Officers | true",
            "listing.search.hearing | /query/api/rest/listing/hearings/h1 | Random Group | false",
            "listing.get.cases-by-person-defendant | /query/api/rest/listing/cases/by-person-defendant-and-hearingDate | Defence Lawyers | true",
            "listing.get.cases-by-person-defendant | /query/api/rest/listing/cases/by-person-defendant-and-hearingDate | Listing Officers | false",
            "listing.get.cache-refdata-courtrooms-refresh | /query/api/rest/listing/cache-refdata-courtrooms/refresh | System Users | true",
            // an action with no rule is denied (deny-when-no-rules)
            "listing.unknown | /query/api/rest/listing/hearings/h1 | Listing Officers | false",
    })
    void legacyGroupRulesGiveTheSameDecision(final String action, final String path, final String group,
                                             final boolean expected) {
        assertThat(allowed(action, path, group)).isEqualTo(expected);
    }

    @Test
    void mediaTypesWithoutALegacyRuleStayDenied() {
        DENIED_ON_WILDFLY_TOO.forEach(action ->
                assertThat(allowed(action, C + "/hearings/h1", "Listing Officers", "System Users")).as(action).isFalse());
    }

    @Test
    void userWithNoGroupsIsDenied() {
        assertThat(allowed("listing.search.hearing", Q + "/hearings/h1")).isFalse();
    }

    @Test
    void everyVendorMediaTypeInTheSpecHasARuleAndEveryRuleHasAnEndpoint() throws IOException {
        final Set<String> ruleNames = ruleActionNames();
        final Set<String> specTokens = new TreeSet<>();
        final Set<String> missing = new TreeSet<>();
        for (final SpecMediaType mt : specVendorMediaTypes()) {
            final String token = RequestActionResolver.extractVendorAction(mt.mediaType());
            specTokens.add(token);
            if (BYPASSED.stream().noneMatch(mt.path()::startsWith) && !ruleNames.contains(token)
                    && !DENIED_ON_WILDFLY_TOO.contains(token)) {
                missing.add(token + " (" + mt.path() + ")");
            }
        }
        assertThat(missing).as("vendor media types without a DRL rule (would be 403)").isEmpty();

        final Set<String> orphanRules = new TreeSet<>(ruleNames);
        orphanRules.removeAll(specTokens);
        orphanRules.removeAll(RULES_WITHOUT_ENDPOINT);
        orphanRules.removeIf(name -> name.startsWith("GET /"));
        // Bypassed endpoints keep their legacy rules for reference.
        orphanRules.removeAll(Set.of("listing.search.hearing.slots", "listing.validate.session.availability",
                "listing.query.court.schedule.draft.status"));
        assertThat(orphanRules).as("DRL rules whose action no endpoint can produce").isEmpty();
    }

    private record SpecMediaType(String path, String mediaType) { }

    @SuppressWarnings("unchecked")
    private static List<SpecMediaType> specVendorMediaTypes() throws IOException {
        final Map<String, Object> spec;
        try (InputStream in = new ClassPathResource("openapi/listing-api.openapi.yml").getInputStream()) {
            spec = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
        }
        final List<SpecMediaType> result = new java.util.ArrayList<>();
        ((Map<String, Map<String, Object>>) spec.get("paths")).forEach((path, item) -> item.forEach((method, op) -> {
            final Map<String, Object> operation = (Map<String, Object>) op;
            if ("get".equals(method)) {
                // A GET's action is its Accept vendor token: the 200 response media types.
                final Map<String, Object> ok = (Map<String, Object>) ((Map<String, Object>) operation.get("responses")).get("200");
                ((Map<String, Object>) ok.getOrDefault("content", Map.of())).keySet().stream()
                        .filter(m -> m.startsWith("application/vnd."))
                        .forEach(m -> result.add(new SpecMediaType(path, m)));
            } else {
                // A body-carrying request's action is its Content-Type vendor token.
                final Map<String, Object> body = (Map<String, Object>) operation.get("requestBody");
                ((Map<String, Object>) body.get("content")).keySet().stream()
                        .filter(m -> m.startsWith("application/vnd."))
                        .forEach(m -> result.add(new SpecMediaType(path, m)));
            }
        }));
        assertThat(result).hasSizeGreaterThan(40);
        return result;
    }

    private static Set<String> ruleActionNames() throws IOException {
        final Pattern name = Pattern.compile("Action\\(name == \"([^\"]+)\"");
        final Set<String> names = new TreeSet<>();
        for (final var resource : new PathMatchingResourcePatternResolver().getResources(DRL_PATTERN)) {
            try (InputStream in = resource.getInputStream()) {
                final Matcher m = name.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                while (m.find()) {
                    // The filter lower-cases vendor tokens, so a vendor rule must be lower case to match.
                    assertThat(m.group(1).startsWith("GET /") || m.group(1).equals(m.group(1).toLowerCase(Locale.ROOT)))
                            .as("rule action %s must be lower case", m.group(1)).isTrue();
                    names.add(m.group(1));
                }
            }
        }
        return names;
    }
}
