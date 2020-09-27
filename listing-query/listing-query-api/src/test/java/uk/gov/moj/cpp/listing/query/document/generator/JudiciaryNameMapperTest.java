package uk.gov.moj.cpp.listing.query.document.generator;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JudiciaryNameMapperTest {

    private static final String TITLE_PREFIX = "titlePrefix";
    private static final String TITLE_SUFFIX = "titleSuffix";
    private static final String TITLE_JUDICIAL_PREFIX = "titleJudicialPrefix";
    private static final String SURNAME = "surname";
    private static final String ID = "id";

    @InjectMocks
    private JudiciaryNameMapper judiciaryNameMapper;

    @Test
    public void shouldReturnJudgeNameWithJudiciaryTitlePrefix() {
        final JsonObject judgeName = createJudiciaryWithJudiciaryTitlePrefixOnly();
        final String judgeNameActual = judiciaryNameMapper.getName(judgeName);
        assertThat(judgeNameActual, is("Her Honour Judge AMAKYE"));
    }

    @Test
    public void shouldReturnJudgeNameWithTitlePrefixAndSuffix() {
        final JsonObject judgeName = createJudiciaryWithTitlePrefix();
        final String judgeNameActual= judiciaryNameMapper.getName(judgeName);
        assertThat(judgeNameActual, is("Mrs Barnes JP"));
    }

    @Test
    public void shouldReturnJudgeNameWithJudiciaryTitlePrefixAndSuffix() {
        final JsonObject judgeName = createJudiciaryWithJudicialTitlePrefixAndSuffix();
        final String judgeNameActual= judiciaryNameMapper.getName(judgeName);
        assertThat(judgeNameActual, is("District Judge (MC) MCCLEAN Esq"));
    }

    @Test
    public void shouldReturnJudgeNameWithJudiciaryTitleOnly() {
        final JsonObject judgeName = createJudiciaryWithJudicialTitleOnly();
        final String judgeNameActual= judiciaryNameMapper.getName(judgeName);
        assertThat(judgeNameActual, is("District Judge Fine"));
    }

    private JsonObject createJudiciaryWithJudiciaryTitlePrefixOnly() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add(SURNAME, "AMAKYE");
        judiciaryBuilder.add(TITLE_JUDICIAL_PREFIX, "Her Honour Judge");
        judiciaryBuilder.add(TITLE_PREFIX, "Mrs");
        judiciaryBuilder.add(ID, randomUUID().toString());
        return judiciaryBuilder.build();
    }

    private JsonObject createJudiciaryWithTitlePrefix() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add(SURNAME, "Barnes");
        judiciaryBuilder.add(TITLE_SUFFIX, "JP");
        judiciaryBuilder.add(TITLE_PREFIX, "Mrs");
        judiciaryBuilder.add(ID, randomUUID().toString());
        return judiciaryBuilder.build();
    }

    private JsonObject createJudiciaryWithJudicialTitlePrefixAndSuffix() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add(SURNAME, "MCCLEAN");
        judiciaryBuilder.add(TITLE_SUFFIX, "Esq");
        judiciaryBuilder.add(TITLE_PREFIX, "Mrs");
        judiciaryBuilder.add(TITLE_JUDICIAL_PREFIX, "District Judge (MC)");
        judiciaryBuilder.add(ID, randomUUID().toString());
        return judiciaryBuilder.build();
    }

    private JsonObject createJudiciaryWithJudicialTitleOnly() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add(SURNAME, "Fine");
        judiciaryBuilder.add(TITLE_JUDICIAL_PREFIX, "District Judge");
        judiciaryBuilder.add(ID, randomUUID().toString());
        return judiciaryBuilder.build();
    }
}