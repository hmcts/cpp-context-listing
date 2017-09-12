package uk.gov.moj.cpp.listing.steps.data.factory;

import uk.gov.moj.cpp.listing.steps.data.*;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class ReferenceDataFactory {

    public static JudgeData judgeData() {
        return new JudgeData(randomUUID().toString(), STRING.next(), STRING.next(), STRING.next());
    }

    public static CourtCentreData courtCentreData() {
        return new CourtCentreData(randomUUID().toString(), STRING.next());
    }

    public static CourtRoomData courtRoomData() {
        return new CourtRoomData(randomUUID().toString(), STRING.next(), STRING.next());
    }

}
