package uk.gov.moj.cpp.listing.steps.data.factory;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.moj.cpp.listing.steps.data.CaseData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

public class CaseDataFactory {

    private static final int HEARING_ESTIMATE_MINUTES = 15;
    private static final String HEARING_TYPE = "PTP";

    public static CaseData caseData() {
        return new CaseData(randomUUID(), STRING.next(),
                manyRandomDefendants(2), LocalDate.now(), randomHearing());
    }

    private static List<OffenceData> manyRandomOffences(Integer numberOfOffences) {
        return IntStream.range(0, numberOfOffences)
                .mapToObj((int i) -> randomOffence())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendants(Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendant())
                .collect(toList());
    }

    private static OffenceData randomOffence() {
        return new OffenceData(randomUUID(), STRING.next(), STRING.next(), LocalDate.now(),
                LocalDate.now(), STRING.next(), STRING.next());
    }

    private static DefendantData randomDefendant() {
        return new DefendantData(randomUUID(), randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), STRING.next(), STRING.next(), manyRandomOffences(2));
    }

    private static HearingData randomHearing() {
        return new HearingData(randomUUID(), STRING.next(), HEARING_TYPE, LocalDate.now(),
                HEARING_ESTIMATE_MINUTES);
    }
}
