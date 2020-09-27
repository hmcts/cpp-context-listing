package uk.gov.moj.cpp.listing.query.document.generator;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

public class JudiciaryNameMapper {

    private static final String SPACE = " ";
    private static final String BLANK_STRING = "";
    private static final String TITLE_PREFIX = "titlePrefix";
    private static final String SURNAME = "surname";
    private static final String TITLE_JUDICIAL_PREFIX = "titleJudicialPrefix";
    private static final String TITLE_SUFFIX = "titleSuffix";


    public String getName(final JsonObject judge) {
        final String titleJudicialPrefix = judge.getString(TITLE_JUDICIAL_PREFIX, BLANK_STRING);
        if (isNotBlank(titleJudicialPrefix)) {
            return getJudgeName(titleJudicialPrefix, judge);
        }
        return getJudgeName(judge.getString(TITLE_PREFIX, BLANK_STRING), judge);
    }

    private String getJudgeName(final String prefix, final JsonObject judge) {
        return prefix + SPACE + judge.getString(SURNAME, BLANK_STRING) + (judge.getString(TITLE_SUFFIX, BLANK_STRING).equals(BLANK_STRING) ? BLANK_STRING : SPACE + judge.getString(TITLE_SUFFIX));
    }
}
