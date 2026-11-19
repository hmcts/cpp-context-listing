package uk.gov.moj.cpp.listing.query.view.dto;

import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.testutil.ReflectionUtil.setField;

import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaginationParameterFactoryTest {

    private final PaginationParameterFactory paginationParameterFactory = new PaginationParameterFactory(4000L);

    @Test
    void shouldCreatePaginationParameterWithMaxPageSize() {
        setField(paginationParameterFactory, "maxPageSize", 1000L);
        final JsonObject paginationParametersAsJson = createObjectBuilder().add("useMaxPageSize", true).build();
        final PaginationParameter parameters = paginationParameterFactory.newPaginationParameter(paginationParametersAsJson);
        assertThat(parameters.getPageSize(), is(1000));
        assertThat(parameters.getPageNumber(), is(1));
        assertThat(parameters.getOffSet(), is(0));
    }
}