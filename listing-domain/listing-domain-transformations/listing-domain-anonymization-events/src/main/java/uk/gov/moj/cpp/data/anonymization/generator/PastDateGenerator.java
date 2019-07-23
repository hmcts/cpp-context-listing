package uk.gov.moj.cpp.data.anonymization.generator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@SuppressWarnings({"squid:S2119", "squid:S2245"})
public class PastDateGenerator implements Generator<String> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String convert(final String fieldValue) {
        final LocalDate localDate = createRandomDate(LocalDate.now().getYear()-100, LocalDate.now().getYear());
        return localDate.format(FORMATTER);

    }

    private  int createRandomIntBetween(int start, int end) {
        final Random r = new Random();
        return r.nextInt((end - start) + 1) + start;
    }

    public  LocalDate createRandomDate(int startYear, int endYear) {
        final int day = createRandomIntBetween(1, 28);
        final int month = createRandomIntBetween(1, 12);
        final int year = createRandomIntBetween(startYear, endYear);
        return LocalDate.of(year, month, day);
    }
}
