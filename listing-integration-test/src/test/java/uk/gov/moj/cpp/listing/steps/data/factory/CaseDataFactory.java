package uk.gov.moj.cpp.listing.steps.data.factory;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.moj.cpp.listing.steps.data.CaseData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class CaseDataFactory {

    private static final int HEARING_ESTIMATE_MINUTES = 15;
    private static final String PTP_HEARING_TYPE = "PTP";
    private static final String SENTENCE_HEARING_TYPE = "Sentence";
    private static final String BAIL_CONDITIONAL = "conditional";

    public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");

    public static CaseData caseData() {
        return new CaseData(randomUUID(), STRING.next(),
                manyRandomHearings(2));
    }

    public static CaseData caseWithAllocationHearingData() {
        return new CaseData(randomUUID(), STRING.next(),
                manyRandomHearingsWithAllFieldsSet(1));
    }




    public static CaseData caseDataExisting(final String existingCaseId, final String courtCentreId) {
        return new CaseData(UUID.fromString(existingCaseId), STRING.next(),
                Arrays.asList(randomHearing(courtCentreId)));
    }

    private static List<OffenceData> manyRandomOffences(final Integer numberOfOffences) {
        return IntStream.range(0, numberOfOffences)
                .mapToObj((int i) -> randomOffence())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendants(final Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendant())
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearings(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing())
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithAllFieldsSet(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithAllocationData())
                .collect(toList());
    }



    private static OffenceData randomOffence() {
        return new OffenceData(randomUUID(), STRING.next(), LocalDate.now(),
                LocalDate.now(), STRING.next(), STRING.next());
    }

    private static DefendantData randomDefendant() {
        return new DefendantData(randomUUID(), randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(2));
    }


    private static HearingData randomHearing() {
        return new HearingData(randomUUID(), randomUUID().toString(), PTP_HEARING_TYPE, LocalDate.now(),
                null, HEARING_ESTIMATE_MINUTES, manyRandomDefendants(3),
                null, null,null);
    }


    private static HearingData randomHearing(final String courtCentreId) {
        return new HearingData(randomUUID(), courtCentreId, SENTENCE_HEARING_TYPE, LocalDate.now(),
                null, HEARING_ESTIMATE_MINUTES, manyRandomDefendants(2),
                null, null,null);
    }

    private static HearingData randomHearingWithAllocationData() {
        final UUID id = randomUUID();
        return new HearingData(id,  randomUUID().toString(), SENTENCE_HEARING_TYPE, LocalDate.now(),
                LocalDate.now().plusDays(2), HEARING_ESTIMATE_MINUTES, manyRandomDefendants(2), randomUUID(),
                randomUUID(),  LocalTime.now().format(dtf));
    }
}

