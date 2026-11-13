package uk.gov.moj.cpp.listing.query.document.generator;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.json.JsonObject;

public class JudiciaryNameMapper {

    private static final String SPACE = " ";
    private static final String BLANK_STRING = "";
    private static final String TITLE_PREFIX = "titlePrefix";
    private static final String SURNAME = "surname";
    private static final String TITLE_JUDICIAL_PREFIX = "titleJudicialPrefix";
    private static final String TITLE_SUFFIX = "titleSuffix";
    private static final String TITLE_PREFIX_WELSH = "titlePrefixWelsh";
    private static final String TITLE_JUDICIARY_PREFIX_WELSH = "titleJudiciaryPrefixWelsh";
    private static final String TITLE_SUFFIX_WELSH = "titleSuffixWelsh";


    public String getName(final JsonObject judge) {
        final String titleJudicialPrefix = judge.getString(TITLE_JUDICIAL_PREFIX, BLANK_STRING);
        if (isNotBlank(titleJudicialPrefix)) {
            return getJudgeName(titleJudicialPrefix, judge, SURNAME, TITLE_SUFFIX);
        }
        return getJudgeName(judge.getString(TITLE_PREFIX, BLANK_STRING), judge, SURNAME, TITLE_SUFFIX);
    }

    public String getWelshName(final JsonObject judge) {
        final String titleJudiciaryPrefixWelsh = judge.getString(TITLE_JUDICIARY_PREFIX_WELSH, BLANK_STRING);
        if (isNotBlank(titleJudiciaryPrefixWelsh)) {
            return getJudgeName(titleJudiciaryPrefixWelsh, judge, SURNAME, TITLE_SUFFIX_WELSH);
        }
        return getJudgeName(judge.getString(TITLE_PREFIX_WELSH, BLANK_STRING), judge, SURNAME, TITLE_SUFFIX_WELSH);
    }

    private String getJudgeName(final String prefix, final JsonObject judge, final String surnameKey, final String titleSuffixKey) {
        return prefix + SPACE + judge.getString(surnameKey, BLANK_STRING) + (judge.getString(titleSuffixKey, BLANK_STRING).equals(BLANK_STRING) ? BLANK_STRING : SPACE + judge.getString(titleSuffixKey));
    }
}
