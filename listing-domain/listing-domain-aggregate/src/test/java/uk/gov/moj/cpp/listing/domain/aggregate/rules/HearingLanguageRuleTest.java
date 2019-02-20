package uk.gov.moj.cpp.listing.domain.aggregate.rules;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.justice.listing.events.HearingLanguage;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class HearingLanguageRuleTest {


    @Test
    public void shouldApplyEnglishIfNoDefendantsHaveRequestedLanguageNeeds() {
        List<ListedCase> listedCases = Arrays.asList(ListedCase.listedCase()
                .withDefendants(Arrays.asList(Defendant.defendant().withHearingLanguageNeeds(empty()).build()))
                .build());

        HearingLanguage actual = HearingLanguageRule.apply(listedCases);
        assertThat(actual, is(HearingLanguage.ENGLISH));
    }

    @Test
    public void shouldApplyEnglishIfAtLeastOneDefendantHasRequestedEnglish() {
        List<ListedCase> listedCases = Arrays.asList(
                ListedCase.listedCase()
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.WELSH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build())).build(),
                ListedCase.listedCase()
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.ENGLISH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build()))
                        .build());


        HearingLanguage actual = HearingLanguageRule.apply(listedCases);
        assertThat(actual, is(HearingLanguage.ENGLISH));
    }

    @Test
    public void shouldApplyWelshIfAllDefendantsHaveRequestedWelsh() {
        List<ListedCase> listedCases = Arrays.asList(
                ListedCase.listedCase()
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.WELSH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build())).build(),
                ListedCase.listedCase()
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withHearingLanguageNeeds(of(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.WELSH))
                                        .build(),
                                Defendant.defendant()
                                        .withHearingLanguageNeeds(of(HearingLanguageNeeds.WELSH))
                                        .build()))
                        .build());

        HearingLanguage actual = HearingLanguageRule.apply(listedCases);
        assertThat(actual, is(HearingLanguage.WELSH));
    }

}