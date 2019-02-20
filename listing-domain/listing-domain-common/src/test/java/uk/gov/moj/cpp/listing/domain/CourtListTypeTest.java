package uk.gov.moj.cpp.listing.domain;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class CourtListTypeTest {

    @Test
    public void testValueFor(){
        assertTrue(CourtListType.valueFor("Alphabetical").get().equals(CourtListType.ALPHABETICAL));
    }
    @Test
    public void testGetTemplate(){
        CourtListType type = CourtListType.valueFor("Alphabetical").get();
        assertTrue(type.getTemplateName().equals("CourtList"));
    }

    @Test
    public void testGetWelshTemplate(){
        CourtListType type = CourtListType.valueFor("Alphabetical").get();
        assertTrue(type.getWelshTemplateName().equals("CourtListEnglishWelsh"));
    }
}

