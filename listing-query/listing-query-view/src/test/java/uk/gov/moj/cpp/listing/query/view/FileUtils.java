package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

public class FileUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<Hearing> createHearings(final String... filePaths) throws IOException {
        final List<Hearing> hearings = newArrayList();
        for (final String filePath : filePaths) {
            hearings.add(createHearing(filePath));
        }

        return hearings;
    }

    public static Hearing createHearing(final String filePath) throws IOException {
        return createHearing(filePath, randomUUID());
    }

    public static Hearing createHearing(final String filePath, final UUID hearingId) throws IOException {
        final StringWriter writer = new StringWriter();

        final InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(filePath);
        assertThat(inputStream, notNullValue());
        IOUtils.copy(inputStream, writer, UTF_8);

        return new Hearing(hearingId, OBJECT_MAPPER.readTree(writer.toString()));
    }

}
