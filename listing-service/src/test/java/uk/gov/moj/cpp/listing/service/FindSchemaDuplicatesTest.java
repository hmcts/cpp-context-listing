package uk.gov.moj.cpp.listing.service;

import uk.gov.justice.services.test.utils.core.schema.SchemaDuplicateTestHelper;

import org.junit.Ignore;
import org.junit.Test;

public class FindSchemaDuplicatesTest {

   @Ignore
   @Test
   public void testSchemaDuplicates() {
       SchemaDuplicateTestHelper.failTestIfDifferentSchemasWithSameName();
   }

}
