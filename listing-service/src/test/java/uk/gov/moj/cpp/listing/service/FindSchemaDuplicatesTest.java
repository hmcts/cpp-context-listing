package uk.gov.moj.cpp.listing.service;

import uk.gov.justice.services.test.utils.core.schema.SchemaDuplicateTestHelper;

import org.junit.Test;

public class FindSchemaDuplicatesTest {


   @Test
   public void testSchemaDuplicates() {
       SchemaDuplicateTestHelper.failTestIfDifferentSchemasWithSameName();
   }

}
