package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;

public class RequestedNameMapperTest {
    private static final String REQUESTED_NAME = "requestedName";
    private static final String TITLE_PREFIX = "titlePrefix";
    private static final String FORENAMES = "forenames";
    private static final String SURNAME = "surname";
    private static final String TITLE_JUDICIAL_PREFIX = "titleJudicialPrefix";
    private static final String TITLE_SUFFIX = "titleSuffix";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private RequestedNameMapper requestedNameMapper = new RequestedNameMapper();

    @Test
    public void shouldReturnRequestedNameAsJudgeName() {

        final String judgeName = requestedNameMapper.getRequestedJudgeName(createJudiciaryWithRequestedName());
        assertThat(judgeName, is(REQUESTED_NAME));
    }

    @Test
    public void shouldNotReturnRequestedNameAsJudgeName() {

        final JsonObject judiciary = createJudiciaryWithoutRequestedName();
        final String formattedName = format("%s %s %s", judiciary.getString(TITLE_JUDICIAL_PREFIX, judiciary.getString(TITLE_PREFIX, EMPTY)), judiciary.getString(SURNAME), judiciary.getString(TITLE_SUFFIX, EMPTY)).trim();
        final String judgeName = requestedNameMapper.getRequestedJudgeName(createJudiciaryWithoutRequestedName());
        assertThat(judgeName, is(formattedName));
    }

    @Test
    public void shouldReturnJusticeName() {

        final JsonObject judiciary = createJudiciaryWithoutRequestedName();
        final String formattedName = format("%s %s %s %s", judiciary.getString(TITLE_PREFIX, EMPTY), judiciary.getString(FORENAMES), judiciary.getString(SURNAME), judiciary.getString(TITLE_SUFFIX, EMPTY)).trim();
        final String judgeName = requestedNameMapper.getRequestedJusticeName(createJudiciaryWithoutRequestedName());
        assertThat(judgeName, is(formattedName));
    }

    @Test
    public void shouldReturnCitizenName() {

        final JsonObject judiciary = createCitizenRequestedName();
        final String formattedName = format("%s %s", judiciary.getString(FIRST_NAME), judiciary.getString(LAST_NAME)).trim();
        final String judgeName = requestedNameMapper.getRequestedCitizenName(FIRST_NAME, LAST_NAME);
        assertThat(judgeName, is(formattedName));
    }

    @Test
    public void shouldReturnSpace() {
        final String judgeName = requestedNameMapper.getRequestedCitizenName(EMPTY, EMPTY);
        assertThat(judgeName, is(SPACE));
    }

    @Test
    void shouldTruncateCitizenNameToThirtyFiveCharacters() {
        final String longFirst = "FirstnameFirstnameFirstname"; // 27 chars
        final String longLast = "LastnameLastnameLastname";   // 24 chars
        final String full = format("%s %s", longFirst, longLast).trim();
        final String expected = full.substring(0, 35);

        final String actual = requestedNameMapper.getRequestedCitizenName(longFirst, longLast);

        assertThat(actual, is(expected));
    }

    @Test
    void shouldReturnSurnameUnchangedWhenLengthAtMostThirtyFive() {
        final String surname = "Anderson-Smith-Johnson-Brown"; // length 31
        final String actual = requestedNameMapper.getCitizenNameSurname(surname);
        assertThat(actual, is(surname));
    }

    @Test
    void shouldTruncateSurnameToThirtyFiveCharacters() {
        final String longSurname = "Anderson-Smith-Johnson-Brown-Williams"; // > 35
        final String expected = longSurname.substring(0, 35);
        final String actual = requestedNameMapper.getCitizenNameSurname(longSurname);
        assertThat(actual, is(expected));
    }

    @Test
    void shouldReturnEmptyWhenSurnameIsEmpty() {
        final String actual = requestedNameMapper.getCitizenNameSurname(EMPTY);
        assertThat(actual, is(EMPTY));
    }

    @Test
    void shouldReturnNullWhenSurnameIsNull() {
        final String actual = requestedNameMapper.getCitizenNameSurname(null);
        assertThat(actual, is((String) null));
    }


    private JsonObject createJudiciaryWithRequestedName() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add(REQUESTED_NAME, REQUESTED_NAME);
        judiciaryBuilder.add(SURNAME, SURNAME);
        judiciaryBuilder.add(FORENAMES, FORENAMES);
        judiciaryBuilder.add(TITLE_SUFFIX, TITLE_SUFFIX);
        judiciaryBuilder.add(TITLE_JUDICIAL_PREFIX, TITLE_JUDICIAL_PREFIX);
        judiciaryBuilder.add(TITLE_PREFIX, TITLE_PREFIX);
        return judiciaryBuilder.build();
    }

    private JsonObject createJudiciaryWithoutRequestedName() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add(SURNAME, SURNAME);
        judiciaryBuilder.add(FORENAMES, FORENAMES);
        judiciaryBuilder.add(TITLE_SUFFIX, TITLE_SUFFIX);
        judiciaryBuilder.add(TITLE_JUDICIAL_PREFIX, TITLE_JUDICIAL_PREFIX);
        judiciaryBuilder.add(TITLE_PREFIX, TITLE_PREFIX);
        return judiciaryBuilder.build();
    }

    private JsonObject createCitizenRequestedName() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add(LAST_NAME, LAST_NAME);
        judiciaryBuilder.add(FIRST_NAME, FIRST_NAME);
        return judiciaryBuilder.build();
    }
}