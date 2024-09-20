package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class SittingKeyByJudiciariesTest {

    @Test
    public void shouldSameJudiciariesKeyEquateReturnTrue() {

        LocalDate now = LocalDate.now();
        Optional<UUID> courtRoom = Optional.of(randomUUID());
        Optional<List<Judiciaries>> judiciaries = Optional.of(
                asList(buildJudiciary(randomUUID(), "Judge"),
                        buildJudiciary(randomUUID(), "Magistrate"))
        );


        SittingKeyByJudiciaries key1 = new SittingKeyByJudiciaries(now, courtRoom, judiciaries);
        SittingKeyByJudiciaries key2 = new SittingKeyByJudiciaries(now, courtRoom, judiciaries);

        assertThat(key1, is(key2));
    }


    @Test
    public void shouldOneEmptyJudiciariesAndOtherExistKeySameForTheHearingDateAndCourtRoomWhenEquateShouldReturnFalse() {

        LocalDate now = LocalDate.now();
        Optional<UUID> courtRoom = Optional.of(randomUUID());
        Optional<List<Judiciaries>> judiciaries = Optional.of(
                asList(buildJudiciary(randomUUID(), "Judge"),
                        buildJudiciary(randomUUID(), "Magistrate"))
        );


        SittingKeyByJudiciaries key1 = new SittingKeyByJudiciaries(now, courtRoom, judiciaries);
        SittingKeyByJudiciaries key2 = new SittingKeyByJudiciaries(now, courtRoom, Optional.empty());

        assertThat(key1.equals(key2), is(false));
    }


    @Test
    public void shouldEmptyJudiciariesReturnTrue() {

        LocalDate now = LocalDate.now();
        Optional<UUID> courtRoom = Optional.of(randomUUID());

        SittingKeyByJudiciaries key1 = new SittingKeyByJudiciaries(now, courtRoom, Optional.empty());
        SittingKeyByJudiciaries key2 = new SittingKeyByJudiciaries(now, courtRoom, Optional.empty());

        assertThat(key1.equals(key2), is(true));
    }


    @Test
    public void shouldDifferentJudiciariesKeyShouldReturnFalse() {

        LocalDate now = LocalDate.now();
        Optional<UUID> courtRoom = Optional.of(randomUUID());
        Optional<List<Judiciaries>> judiciaries = Optional.of(
                asList(buildJudiciary(randomUUID(), "Judge"),
                        buildJudiciary(randomUUID(), "Magistrate"))
        );

        Optional<List<Judiciaries>> judiciaries1 = Optional.of(
                asList(buildJudiciary(randomUUID(), "Judge"),
                        buildJudiciary(randomUUID(), "Magistrate"),
                        buildJudiciary(randomUUID(), "Magistrate"))
        );

        SittingKeyByJudiciaries key1 = new SittingKeyByJudiciaries(now, courtRoom, judiciaries);
        SittingKeyByJudiciaries key2 = new SittingKeyByJudiciaries(now, courtRoom, judiciaries1);

        assertThat(key1.equals(key2), is(false));
    }

    private Judiciaries buildJudiciary(final UUID judiciaryId, final String judiciaryType) {
        return new Judiciaries(judiciaryId, judiciaryType);
    }
}