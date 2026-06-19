package uk.gov.moj.cpp.listing.domain;

import static java.time.Month.APRIL;
import static java.time.Month.AUGUST;
import static java.time.Month.DECEMBER;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.MARCH;
import static java.time.Month.MAY;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;

import java.time.Month;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public enum WelshMonth {
    IONAWR(JANUARY),
    CHWEFROR(FEBRUARY),
    MAWRTH(MARCH),
    EBRILL(APRIL),
    MAI(MAY),
    MEHEFIN(JUNE),
    GORFFENNAF(JULY),
    AWST(AUGUST),
    MEDI(SEPTEMBER),
    HYDREF(OCTOBER),
    TACHWEDD(NOVEMBER),
    RHAGFYR(DECEMBER);
    private final Month eng;
    private static final Map<Month, WelshMonth> aMap = initialiseValueMap();
    WelshMonth(Month eng) {
        this.eng = eng;
    }

    private static Map<Month, WelshMonth> initialiseValueMap() {
        final Map<Month, WelshMonth> englishWelshMap = new EnumMap<>(Month.class);
        for (final WelshMonth w : WelshMonth.values()) {
            englishWelshMap.put(w.eng, w);
        }
        return englishWelshMap;
    }


    @Override
    public String toString() {
        return eng.name();
    }

    public static Optional<WelshMonth> valueFor(final Month eng) {
        if (aMap.containsKey(eng)) {
            return Optional.of(aMap.get(eng));
        }
        return Optional.empty();
    }
}
