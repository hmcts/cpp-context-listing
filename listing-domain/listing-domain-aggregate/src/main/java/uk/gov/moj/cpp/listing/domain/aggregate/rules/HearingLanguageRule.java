package uk.gov.moj.cpp.listing.domain.aggregate.rules;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;

import java.util.List;
import java.util.Objects;
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
        if ((languageNeeds.isEmpty() && applicantHearingLanguageNeeds.isEmpty()) ||
                languageNeeds.contains(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.ENGLISH)
                || applicantHearingLanguageNeeds.contains(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.ENGLISH)) {
            return HearingLanguage.ENGLISH;
        }

        return HearingLanguage.WELSH;
    }

    public static HearingLanguage applyForEvent(final List<uk.gov.justice.listing.events.ListedCase> listedCases, final List<HearingLanguageNeeds> applicantHearingLanguageNeeds) {
        final List<uk.gov.justice.core.courts.HearingLanguage> languageNeeds = listedCases.stream()
                .map(listedCase -> listedCase.getDefendants().stream()
                        .map(uk.gov.justice.listing.events.Defendant::getHearingLanguageNeeds)
                        .distinct()
                        .filter(Objects::nonNull)
                        .collect(toList()))
                .flatMap(List::stream)
                .collect(toList());
        if ((languageNeeds.isEmpty() && applicantHearingLanguageNeeds.isEmpty()) ||
                languageNeeds.contains(uk.gov.justice.core.courts.HearingLanguage.ENGLISH)
                || applicantHearingLanguageNeeds.contains(uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds.ENGLISH)) {
            return HearingLanguage.ENGLISH;
        }

        return HearingLanguage.WELSH;
    }
}
