package uk.gov.moj.cpp.listing.domain.aggregate.rules;

import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.justice.listing.events.HearingLanguage;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;

import java.util.List;
import java.util.Optional;

public class HearingLanguageRule {

    private HearingLanguageRule() {
    }

    public static HearingLanguage apply(final List<uk.gov.moj.cpp.listing.domain.ListedCase> listedCases, final List<HearingLanguageNeeds> applicantHearingLanguageNeeds) {
        List<uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds> languageNeeds = listedCases.stream()
                .map(listedCase -> listedCase.getDefendants().stream()
                        .map(Defendant::getHearingLanguageNeeds)
                        .distinct()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toList()))
                .flatMap(List::stream)
                .collect(toList());
        if((languageNeeds.isEmpty() && applicantHearingLanguageNeeds.isEmpty()) ||
                languageNeeds.contains(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.ENGLISH)
                || applicantHearingLanguageNeeds.contains(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.ENGLISH)){
            return HearingLanguage.ENGLISH;
        }

        return HearingLanguage.WELSH;
    }
}
