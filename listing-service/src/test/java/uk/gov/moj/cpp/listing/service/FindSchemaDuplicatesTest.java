package uk.gov.moj.cpp.listing.service;

import uk.gov.justice.services.test.utils.core.schema.SchemaDuplicateTestHelper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class FindSchemaDuplicatesTest {

    @Test
    @Disabled("will be addressed later")
    public void shouldFindSchemaDuplicatesTest() {
        SchemaDuplicateTestHelper.failTestIfDifferentSchemasWithSameName();
    }
}
