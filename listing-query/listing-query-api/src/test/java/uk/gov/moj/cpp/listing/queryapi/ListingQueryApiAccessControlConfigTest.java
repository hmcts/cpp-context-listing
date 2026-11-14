package uk.gov.moj.cpp.listing.queryapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Guards against the query API's RAML and its Drools access-control rules drifting apart.
 *
 * <p>The RAML declares the actions the query API accepts; the DRL grants access to them. Nothing
 * else checks that every RAML action has a matching rule. An action declared in the RAML with no
 * rule is refused at runtime for every caller, and every unit test still passes.
 */
public class ListingQueryApiAccessControlConfigTest {

    private static final String PATH_TO_RAML = "src/raml/listing-query-api.raml";
    private static final String PATH_TO_DRL =
            "src/main/resources/uk/gov/moj/cpp/listing/queryapi/accesscontrol/listing-query-api.drl";

    private static final String CONTEXT_PREFIX = "listing.";

    // Matches a RAML "name: <value>" mapping entry, e.g. "            name: listing.search.hearings"
    private static final Pattern RAML_NAME_PATTERN = Pattern.compile("name:\\s*(\\S+)");

    // Matches a Drools access-control rule condition, e.g. Action(name == "listing.search.hearings")
    private static final Pattern DRL_ACTION_PATTERN = Pattern.compile("Action\\(name == \"([^\"]+)\"\\)");

    /**
     * Every action the query API declares must have an access-control rule. Without one the
     * framework refuses the request for every caller, at runtime, with every unit test green.
     *
     * <p>Deliberately one-directional. The DRL legitimately carries rules with no action in this
     * RAML - {@code listing.public.list} is one - so asserting set equality would fail on arrival
     * for a pre-existing reason, and a test that fails on arrival gets disabled rather than fixed.
     */
    @Test
    public void everyRamlActionHasAnAccessControlRule() throws Exception {
        final Set<String> ramlActions = ramlActionNames();
        final Set<String> ruleActions = drlActionNames();

        assertThat("the RAML parse found no actions - the extraction is broken, not the config",
                ramlActions, is(not(empty())));

        final Set<String> missing = new TreeSet<>(ramlActions);
        missing.removeAll(ruleActions);

        assertThat("query API actions with no access-control rule - every caller gets refused at "
                + "runtime: " + missing, missing, is(empty()));
    }

    private Set<String> ramlActionNames() throws Exception {
        final Set<String> names = new TreeSet<>();
        for (final String line : readLines(PATH_TO_RAML)) {
            final Matcher matcher = RAML_NAME_PATTERN.matcher(line);
            if (matcher.find()) {
                final String name = matcher.group(1);
                if (name.startsWith(CONTEXT_PREFIX)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private Set<String> drlActionNames() throws Exception {
        final Set<String> names = new TreeSet<>();
        final String content = withoutComments(readFile(PATH_TO_DRL));
        final Matcher matcher = DRL_ACTION_PATTERN.matcher(content);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    /**
     * Drools honours both {@code //} line comments and {@code /* ... *}{@code /} block comments -
     * a rule hidden behind either is disabled at runtime exactly as if it were deleted, so it must
     * not count as an access-control rule here. Without this, commenting out a rule to "temporarily"
     * disable it would leave this guard green while the action it covered is refused for everyone.
     * Strip block comments first (they can span, and hide, whole {@code //} lines), then line
     * comments, then match what's left.
     */
    private String withoutComments(final String drlContent) {
        final String withoutBlockComments = drlContent.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("//[^\\n]*", "");
    }

    private List<String> readLines(final String path) throws Exception {
        return Files.readAllLines(new File(path).toPath(), StandardCharsets.UTF_8);
    }

    private String readFile(final String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }
}
