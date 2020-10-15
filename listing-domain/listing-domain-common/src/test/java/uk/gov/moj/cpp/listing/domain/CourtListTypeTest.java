package uk.gov.moj.cpp.listing.domain;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class CourtListTypeTest {


    @DataProvider
    public static Object[][] validCourtListType() {
        return new Object[][]{
                {new CourtListTypeTestType("Alphabetical", CourtListType.ALPHABETICAL)},
                {new CourtListTypeTestType("Standard", CourtListType.STANDARD)},
                {new CourtListTypeTestType("Public", CourtListType.PUBLIC)},
                {new CourtListTypeTestType("Bench", CourtListType.BENCH)},
                {new CourtListTypeTestType("Judge", CourtListType.JUDGE)}
        };
    }

    @Test
    @UseDataProvider("validCourtListType")
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

