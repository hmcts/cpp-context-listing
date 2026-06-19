package uk.gov.moj.cpp.listing.domain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CourtListTypeTest {

    public static Stream<Arguments> validCourtListType() {
        return Stream.of(
                Arguments.of(new CourtListTypeTestType("Alphabetical", CourtListType.ALPHABETICAL)),
                Arguments.of(new CourtListTypeTestType("Standard", CourtListType.STANDARD)),
                Arguments.of(new CourtListTypeTestType("Public", CourtListType.PUBLIC)),
                Arguments.of(new CourtListTypeTestType("Bench", CourtListType.BENCH)),
                Arguments.of(new CourtListTypeTestType("Judge", CourtListType.JUDGE))
        );
    }

    @ParameterizedTest
    @MethodSource("validCourtListType")
    public void testValueFor(final CourtListTypeTestType courtListTypeTestType){
        assertThat(CourtListType.valueFor(courtListTypeTestType.getCourtListTypeName()).get(), is(courtListTypeTestType.getCourtListType()));
    }

    @Test
    public void testGetTemplate(){
        CourtListType type = CourtListType.valueFor("Alphabetical").get();
        assertThat(type.getTemplateName(), is("CourtList"));
    }

    @Test
    public void testGetWelshTemplate(){
        CourtListType type = CourtListType.valueFor("Alphabetical").get();
        assertThat(type.getWelshTemplateName(), is("CourtListEnglishWelsh"));
    }

    @Test
    public void testValueForInvalid(){
        assertThat(CourtListType.valueFor("Test").isPresent(), is(false));
    }


    private static class CourtListTypeTestType {
        private String courtListTypeName;
        private CourtListType courtListType;

        public CourtListTypeTestType(final String courtListTypeNameArg, final CourtListType courtListTypeArg) {

            courtListTypeName = courtListTypeNameArg;
            courtListType = courtListTypeArg;
        }

        public String getCourtListTypeName() {
            return courtListTypeName;
        }

        public CourtListType getCourtListType() {
            return courtListType;
        }
    }
}

