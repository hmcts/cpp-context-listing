package uk.gov.moj.cpp.listing.domain.aggregate.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.justice.core.courts.HearingLanguage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class HearingLanguageRuleTest {


    @Test
    public void shouldApplyEnglishIfNoDefendantsHaveRequestedLanguageNeeds() {
        List<ListedCase> listedCases = asList(ListedCase.listedCase()
                .withDefendants(asList(Defendant.defendant().withHearingLanguageNeeds(empty()).build()))
                .build());

        HearingLanguage actual = HearingLanguageRule.apply(listedCases, emptyList());
        assertThat(actual, is(HearingLanguage.ENGLISH));
    }

    @Test
    public void shouldApplyEnglishIfAtLeastOneDefendantHasRequestedEnglish() {
        List<ListedCase> listedCases = asList(
                ListedCase.listedCase()
                        .withDefendants(asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.WELSH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build())).build(),
                ListedCase.listedCase()
                        .withDefendants(asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.ENGLISH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build()))
                        .build());


        HearingLanguage actual = HearingLanguageRule.apply(listedCases, asList(HearingLanguageNeeds.ENGLISH, HearingLanguageNeeds.WELSH));
        assertThat(actual, is(HearingLanguage.ENGLISH));
    }

    @Test
    public void shouldApplyWelshIfAllDefendantsHaveRequestedWelsh() {
        List<ListedCase> listedCases = asList(
                ListedCase.listedCase()
                        .withDefendants(asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.WELSH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build())).build(),
                ListedCase.listedCase()
                        .withDefendants(asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.WELSH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build()))
                        .build());

        HearingLanguage actual = HearingLanguageRule.apply(listedCases, asList(HearingLanguageNeeds.WELSH));
        assertThat(actual, is(HearingLanguage.WELSH));
    }
    @Test
    public void shouldApplyEnglishIfAllDefentantButOneApplicantRequestedWelsh(){
        List<ListedCase> listedCases = asList(
                ListedCase.listedCase()
                        .withDefendants(asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.WELSH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build())).build(),
                ListedCase.listedCase()
                        .withDefendants(asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.WELSH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build()))
                        .build());
        HearingLanguage actual = HearingLanguageRule.apply(listedCases, asList(HearingLanguageNeeds.ENGLISH, HearingLanguageNeeds.WELSH));
        assertThat(actual, is(HearingLanguage.ENGLISH));
    }
}
